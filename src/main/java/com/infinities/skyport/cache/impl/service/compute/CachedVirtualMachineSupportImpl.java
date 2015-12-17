/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport.cache.impl.service.compute;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VMLaunchOptions.NICConfig;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.dasein.cloud.compute.VmState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.service.compute.AsyncVirtualMachineSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport;
import com.infinities.skyport.distributed.DistributedAtomicLong;
import com.infinities.skyport.distributed.DistributedAtomicReference;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedCondition;
import com.infinities.skyport.distributed.DistributedLock;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.distributed.util.DistributedUtil;
import com.infinities.skyport.model.Time;
import com.infinities.skyport.model.compute.VirtualMachineSupportProxy;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.model.configuration.compute.VirtualMachineConfiguration;
import com.infinities.skyport.proxy.VirtualMachineProductProxy;
import com.infinities.skyport.proxy.VirtualMachineProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzConfiguration.Precondition;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.quartz.callable.VmCallable;
import com.infinities.skyport.quartz.callable.VmProductCallable;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.virtualmachine.VirtualMachineFailureEvent;
import com.infinities.skyport.service.event.compute.virtualmachine.VirtualMachineRefreshedEvent;
import com.infinities.skyport.service.event.compute.virtualmachineproduct.VirtualMachineProductFailureEvent;
import com.infinities.skyport.service.event.compute.virtualmachineproduct.VirtualMachineProductRefreshedEvent;

public class CachedVirtualMachineSupportImpl extends VirtualMachineSupportProxy implements CachedVirtualMachineSupport,
		ConfigurationLifeCycleListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(CachedVirtualMachineSupportImpl.class);
	private static int UUID_LENGTH = 36;
	private AsyncVirtualMachineSupport inner;
	private Configuration configuration;
	private final DistributedObjectFactory objectFactory;
	private final DistributedAtomicReference<Boolean> heartbeat;
	private final FirstLevelDispatcher dispatcher;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private final DistributedCache<String, VirtualMachineProxy> vmCache;
	private final DistributedCache<String, VirtualMachineProductProxy> productCache;
	private final QuartzServiceImpl<ComputeQuartzType> quartzService;
	private final Map<ComputeQuartzType, QuartzConfiguration<?>> quartzs = Maps.newEnumMap(ComputeQuartzType.class);
	private final List<CachedVirtualMachineListener> virtualMachineListeners = new ArrayList<CachedVirtualMachineListener>();
	private final List<CachedVirtualMachineProductListener> virtualMachineProductListeners =
			new ArrayList<CachedVirtualMachineProductListener>();
	private final String distributedKey;
	private final DistributedLock lock;
	private final DistributedCondition isVmsRefresh;
	protected final DistributedAtomicLong refreshDate;
	private final DistributedMap<String, String> creatingVmMap;


	public CachedVirtualMachineSupportImpl(ConfigurationHome home, AsyncVirtualMachineSupport inner,
			Configuration configuration, QuartzServiceImpl<ComputeQuartzType> quartz,
			DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap, FirstLevelDispatcher dispatcher,
			DistributedObjectFactory objectFactory) {
		super(inner);
		home.addLifeCycleListener(this);
		this.inner = inner;
		this.configuration = configuration;
		this.distributedKey = DistributedUtil.generateDistributedName(configuration);
		this.dispatcher = dispatcher;
		this.objectFactory = objectFactory;
		this.typeMap = typeMap;
		this.quartzService = quartz;
		this.vmCache = objectFactory.getCache("vmCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
		this.productCache =
				objectFactory.getCache("vmProductCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
		this.heartbeat = objectFactory.getAtomicReference("heartbeat");
		heartbeat.set(false);
		this.creatingVmMap = this.getObjectFactory().getMap("creatingVmMap");
		this.lock = this.getObjectFactory().getLock("vmLock");
		this.isVmsRefresh = lock.newCondition("isVmsRefresh");
		this.refreshDate = this.getObjectFactory().getAtomicLong("vmRefreshDate");
	}

	public synchronized void initialize() {
		if (isInitialized.compareAndSet(false, true)) {
			// super.initialize();
			setUpSchedule(configuration.getComputeConfiguration().getVirtualMachineConfiguration());
			logger.info("initialize cache");
		} else {
			throw new IllegalStateException("object has been initialized");
		}
	}

	public synchronized void close() {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				vmCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			try {
				productCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			for (Iterator<CachedVirtualMachineListener> it = virtualMachineListeners.iterator(); it.hasNext();) {
				CachedVirtualMachineListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
			for (Iterator<CachedVirtualMachineProductListener> i = virtualMachineProductListeners.iterator(); i.hasNext();) {
				CachedVirtualMachineProductListener listener = i.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				i.remove();
			}
		}

	}

	@Override
	public AsyncResult<VirtualMachine> alterVirtualMachineProduct(final String virtualMachineId, final String productId)
			throws InternalException, CloudException {
		AbstractLockVmFunction<VirtualMachine> function = new AbstractLockVmFunction<VirtualMachine>(virtualMachineId) {

			@Override
			public AsyncResult<VirtualMachine> executeInner() throws InternalException, CloudException {
				return inner.alterVirtualMachineProduct(virtualMachineId, productId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<VirtualMachine> alterVirtualMachineSize(final String virtualMachineId, final String cpuCount,
			final String ramInMB) throws InternalException, CloudException {
		AbstractLockVmFunction<VirtualMachine> function = new AbstractLockVmFunction<VirtualMachine>(virtualMachineId) {

			@Override
			public AsyncResult<VirtualMachine> executeInner() throws InternalException, CloudException {
				return inner.alterVirtualMachineSize(virtualMachineId, cpuCount, ramInMB);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<VirtualMachine> alterVirtualMachineFirewalls(final String virtualMachineId, final String[] firewalls)
			throws InternalException, CloudException {
		AbstractLockVmFunction<VirtualMachine> function = new AbstractLockVmFunction<VirtualMachine>(virtualMachineId) {

			@Override
			public AsyncResult<VirtualMachine> executeInner() throws InternalException, CloudException {
				return inner.alterVirtualMachineFirewalls(virtualMachineId, firewalls);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<VirtualMachine> clone(String vmId, String intoDcId, String name, String description, boolean powerOn,
			String... firewallIds) throws InternalException, CloudException {
		final String uuid = UUID.randomUUID().toString();
		description = Strings.isNullOrEmpty(description) ? "{uuid:" + uuid + "}" : description + " {uuid:" + uuid + "}";
		creatingVmMap.set(uuid, name);
		AsyncResult<VirtualMachine> future = inner.clone(vmId, intoDcId, name, description, powerOn, firewallIds);
		Futures.addCallback(future, new UnlockCallback<VirtualMachine>(vmId));
		Futures.addCallback(future, new UnlockCallback<VirtualMachine>(uuid));
		return future;
	}

	@Override
	public AsyncResult<VirtualMachine> launch(VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
		final String uuid = UUID.randomUUID().toString();
		String description =
				Strings.isNullOrEmpty(withLaunchOptions.getDescription()) ? "{uuid:" + uuid + "}" : withLaunchOptions
						.getDescription() + " {uuid:" + uuid + "}";
		VMLaunchOptions options = withDescription(withLaunchOptions, description);
		creatingVmMap.set(uuid, options.getFriendlyName());
		AsyncResult<VirtualMachine> future = inner.launch(options);
		Futures.addCallback(future, new UnlockCallback<VirtualMachine>(uuid));
		return future;
	}

	@Override
	public AsyncResult<Iterable<String>> launchMany(VMLaunchOptions withLaunchOptions, int count) throws CloudException,
			InternalException {
		final String uuid = UUID.randomUUID().toString();
		String description =
				Strings.isNullOrEmpty(withLaunchOptions.getDescription()) ? "{uuid:" + uuid + "}" : withLaunchOptions
						.getDescription() + " {uuid:" + uuid + "}";
		VMLaunchOptions options = withDescription(withLaunchOptions, description);
		creatingVmMap.set(uuid, options.getFriendlyName());
		return inner.launchMany(withLaunchOptions, count);
	}

	@SuppressWarnings("deprecation")
	private VMLaunchOptions withDescription(VMLaunchOptions withLaunchOptions, String description) {
		VMLaunchOptions options =
				VMLaunchOptions
						.getInstance(withLaunchOptions.getStandardProductId(), withLaunchOptions.getMachineImageId(),
								withLaunchOptions.getHostName(), withLaunchOptions.getFriendlyName(), description)
						.withBoostrapKey(withLaunchOptions.getBootstrapKey())
						.withBootstrapUser(withLaunchOptions.getBootstrapUser(), withLaunchOptions.getBootstrapPassword())
						.inDataCenter(withLaunchOptions.getDataCenterId())
						.behindFirewalls(withLaunchOptions.getFirewallIds())
						.withIoOptimized(withLaunchOptions.isIoOptimized())
						.withIpForwardingAllowed(withLaunchOptions.isIpForwardingAllowed())
						.withExtendedImages(withLaunchOptions.getKernelId(), withLaunchOptions.getRamdiskId())
						.inVirtualMachineGroup(withLaunchOptions.getVirtualMachineGroup())
						.withResourcePoolId(withLaunchOptions.getResourcePoolId())
						.withMetaData(withLaunchOptions.getMetaData())
						.inSubnet(withLaunchOptions.getNetworkProductId(), withLaunchOptions.getDataCenterId(),
								withLaunchOptions.getVlanId(), withLaunchOptions.getSubnetId())
						.withPrivateIp(withLaunchOptions.getPrivateIp())
						.withProvisionPublicIp(withLaunchOptions.isProvisionPublicIp())
						.withRootVolumeProduct(withLaunchOptions.getRootVolumeProductId())
						.withStaticIps(withLaunchOptions.getStaticIpIds()).withUserData(withLaunchOptions.getUserData())
						.withRoleId(withLaunchOptions.getRoleId())
						.withAffinityGroupId(withLaunchOptions.getAffinityGroupId())
						.withStoragePoolId(withLaunchOptions.getStoragePoolId())
						.withVMFolderId(withLaunchOptions.getVmFolderId()).withDnsDomain(withLaunchOptions.getDnsDomain())
						.withDnsServerList(withLaunchOptions.getDnsServerList())
						.withDnsSuffixList(withLaunchOptions.getDnsSuffixList())
						.withGatewayList(withLaunchOptions.getGatewayList()).withNetMask(withLaunchOptions.getNetMask())
						.withWinWorkgroupName(withLaunchOptions.getWinWorkgroupName())
						.withWinOwnerName(withLaunchOptions.getWinOwnerName())
						.withWinOrgName(withLaunchOptions.getWinOrgName())
						.withWinProductSerialNum(withLaunchOptions.getWinProductSerialNum())
						.withClientRequestToken(withLaunchOptions.getClientRequestToken())
						.withAttachments(withLaunchOptions.getVolumes());

		if (withLaunchOptions.isExtendedAnalytics()) {
			options.withExtendedAnalytics();
		}

		if (withLaunchOptions.isPreventApiTermination()) {
			options.preventAPITermination();
		}

		if (withLaunchOptions.getNetworkInterfaces() != null) {
			for (NICConfig nicConfig : withLaunchOptions.getNetworkInterfaces()) {
				options.withNetworkInterfaces(nicConfig.nicToCreate);
			}
		}

		return options;
	}

	@Override
	public AsyncResult<Void> pause(final String vmId) throws InternalException, CloudException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.pause(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> reboot(final String vmId) throws CloudException, InternalException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.reboot(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> resume(final String vmId) throws CloudException, InternalException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.resume(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> start(final String vmId) throws InternalException, CloudException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.start(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> stop(final String vmId) throws InternalException, CloudException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.stop(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> stop(final String vmId, final boolean force) throws InternalException, CloudException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.stop(vmId, force);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> suspend(final String vmId) throws CloudException, InternalException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.suspend(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> terminate(final String vmId) throws InternalException, CloudException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.terminate(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> terminate(final String id, final String explanation) throws InternalException, CloudException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(id) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.terminate(id, explanation);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Void> unpause(final String vmId) throws CloudException, InternalException {
		AbstractLockVmFunction<Void> function = new AbstractLockVmFunction<Void>(vmId) {

			@Override
			public AsyncResult<Void> executeInner() throws InternalException, CloudException {
				return inner.unpause(vmId);
			}

		};
		return function.execute();
	}

	@Override
	public AsyncResult<Iterable<VirtualMachineProduct>> listProducts(@Nonnull VirtualMachineProductFilterOptions options)
			throws InternalException, CloudException {
		Preconditions.checkNotNull(options);
		if (options == null || (!options.hasCriteria() && !options.isMatchesAny())) {
			Iterable<VirtualMachineProduct> list = new ArrayList<VirtualMachineProduct>(productCache.values());
			ListenableFuture<Iterable<VirtualMachineProduct>> future = Futures.immediateFuture(list);
			AsyncResult<Iterable<VirtualMachineProduct>> ret = new AsyncResult<Iterable<VirtualMachineProduct>>(future);
			return ret;
		} else {
			return inner.listProducts(options);
		}
	}

	@Override
	public AsyncResult<Iterable<VirtualMachine>> listVirtualMachines() throws InternalException, CloudException {
		Iterable<VirtualMachine> list = new ArrayList<VirtualMachine>(vmCache.values());
		ListenableFuture<Iterable<VirtualMachine>> future = Futures.immediateFuture(list);
		AsyncResult<Iterable<VirtualMachine>> ret = new AsyncResult<Iterable<VirtualMachine>>(future);
		return ret;
	}

	private void setUpSchedule(VirtualMachineConfiguration virtualMachineConfiguration) {
		if (configuration.getStatus()) {
			Time vmTime = virtualMachineConfiguration.getListVirtualMachines().getDelay();
			Precondition vmCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(ComputeQuartzType.VirtualMachine);
				}

			};
			QuartzConfiguration<Iterable<VirtualMachine>> vmConfig =
					new QuartzConfiguration.Builder<Iterable<VirtualMachine>>(new VmCallable(inner.getSupport()),
							vmCondition).addCallback(getVmCallback()).delay(vmTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("Vm:" + configuration.getCloudName()).build();
			quartzs.put(ComputeQuartzType.VirtualMachine, vmConfig);

			Time productTime = virtualMachineConfiguration.getListAllProducts().getDelay();
			Precondition productCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(ComputeQuartzType.VirtualMachineProduct);
				}

			};
			QuartzConfiguration<Iterable<VirtualMachineProduct>> vmProductConfig =
					new QuartzConfiguration.Builder<Iterable<VirtualMachineProduct>>(new VmProductCallable(
							inner.getSupport()), productCondition).addCallback(getVmProductCallback()).delay(productTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("VmProduct:" + configuration.getCloudName()).build();
			quartzs.put(ComputeQuartzType.VirtualMachineProduct, vmProductConfig);

			fireQuartz();
		}
	}

	private FutureCallback<Iterable<VirtualMachine>> getVmCallback() {
		return new FutureCallback<Iterable<VirtualMachine>>() {

			@Override
			public void onSuccess(Iterable<VirtualMachine> result) {
				List<VirtualMachine> vmList = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} vm success: {}", new Object[] { name, vmList.size() });
				setHeartbeat(true);
				refreshVmCache(result);
				dispatcher.fireRefreshedEvent(new VirtualMachineRefreshedEvent(vmCache.values(), id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} vm failed", name);
				logger.warn("list vm failed", t);
				setHeartbeat(false);
				vmCache.reload(t);
				dispatcher.fireFaiureEvent(new VirtualMachineFailureEvent(id, t));
			}

		};
	}

	private FutureCallback<Iterable<VirtualMachineProduct>> getVmProductCallback() {
		return new FutureCallback<Iterable<VirtualMachineProduct>>() {

			@Override
			public void onSuccess(Iterable<VirtualMachineProduct> result) {
				List<VirtualMachineProduct> list = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} vm product success: {}", new Object[] { name, list.size() });
				setHeartbeat(true);
				refreshProductCache(result);
				dispatcher.fireRefreshedEvent(new VirtualMachineProductRefreshedEvent(productCache.values(), id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} vm failed", name);
				logger.warn("list vm product failed", t);
				productCache.reload(t);
				dispatcher.fireFaiureEvent(new VirtualMachineProductFailureEvent(id, t));
			}

		};
	}

	protected void refreshVmCache(Iterable<VirtualMachine> result) {
		String configName = configuration.getCloudName();
		String configId = configuration.getId();

		Map<String, VirtualMachine> vmMap = new HashMap<String, VirtualMachine>();
		Iterator<VirtualMachine> iterator = result.iterator();
		while (iterator.hasNext()) {
			VirtualMachine vm = iterator.next();
			vmMap.put(vm.getProviderVirtualMachineId(), vm);
		}

		Collection<VirtualMachineProxy> values = Collections.emptySet();
		try {
			values = vmCache.values();
		} catch (IllegalStateException e) {
			// ignore
		}
		logger.debug("old cache size: {}", values.size());

		for (VirtualMachineProxy proxy : values) {
			try {
				VirtualMachine vm = vmMap.get(proxy.getProviderVirtualMachineId());
				if (vm == null) { // vm removed
					vmCache.delete(proxy.getProviderVirtualMachineId());
					logger.debug("vm: {} removed", proxy.getProviderVirtualMachineId());
					continue;
				}
				if (proxy.isLocked()) {
					vm.setCurrentState(VmState.PENDING);
					logger.debug("old vm: {} is lock", vm.getName());
				}
				proxy.setVirtualMachine(vm);
				logger.debug("old vm: {}, status: {}", new Object[] { vm.getName(), vm.getCurrentState() });
				// v_logger.debug(net.logstash.logback.marker.Markers.appendFields(new
				// VmMarker(vm)), "vm status");
				vmCache.set(proxy.getProviderVirtualMachineId(), proxy);
			} catch (Exception e) {
				logger.error("encounter exception while refresh vm", e);
			} finally {
				vmMap.remove(proxy.getProviderVirtualMachineId());
			}
		}

		for (VirtualMachine vm : vmMap.values()) { // new vm
			// getVmCache().lock(vm.getVmid());
			// try {
			VirtualMachineProxy proxy =
					new VirtualMachineProxy(vm, configName, configId, distributedKey, this.getObjectFactory().getAtomicLong(
							"vm_" + vm.getProviderVirtualMachineId()));
			VmState status = vm.getCurrentState();
			final String uuid = getUUID(vm.getDescription());
			if (!Strings.isNullOrEmpty(uuid)) { // if vm is creating by
												// skyport, then lock
				logger.debug("new vm's uuid: {}", uuid);
				String name = creatingVmMap.get(uuid);
				logger.debug("new vm's name expected: {}, actual: {} ", new Object[] { name, vm.getName() });
				// not unlock yet
				if (!Strings.isNullOrEmpty(name) && vm.getName() != null && vm.getName().contains(name)) {
					// VmWrapper newVm = new VmWrapper(vm);
					status = VmState.PENDING;
					proxy.lock();
					creatingVmMap.set(uuid, vm.getProviderVirtualMachineId());
					logger.debug("new vm: {} is lock", vm.getName());
				}
			}
			vm.setCurrentState(status);
			logger.debug("new vm: {}, status: {}", new Object[] { vm.getName(), vm.getCurrentState() });
			// v_logger.debug(net.logstash.logback.marker.Markers.appendFields(new
			// VmMarker(vm)), "vm status");
			vmCache.set(vm.getProviderVirtualMachineId(), proxy);
			// }
			// finally {
			// try {
			// getVmCache().unlock(vm.getVmid());
			// } catch (Exception e) {
			// logger.error("unlock error", e);
			// }
			// }
		}

		lock.lock();
		try {
			vmCache.refresh(); // clean exception in cache;
			refreshDate.set(System.currentTimeMillis());
			isVmsRefresh.signalAll();
		} finally {
			lock.unlock();
		}
	}

	private String getUUID(String desc) {
		if (Strings.isNullOrEmpty(desc) || !desc.contains("{uuid:")) {
			return null;
		}
		final int begin = desc.indexOf("{uuid:") + "{uuid:".length();
		final int end = begin + UUID_LENGTH;

		final String uuid = desc.substring(begin, end);

		return uuid;
	}

	protected void refreshProductCache(Iterable<VirtualMachineProduct> result) {
		Map<String, VirtualMachineProductProxy> productMap = new HashMap<String, VirtualMachineProductProxy>();
		Iterator<VirtualMachineProduct> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			VirtualMachineProduct product = iterator.next();
			VirtualMachineProductProxy proxy = new VirtualMachineProductProxy(product, configName, configId);
			productMap.put(product.getProviderProductId(), proxy);
		}
		this.productCache.reload(productMap);
	}

	private void setHeartbeat(boolean heartbeat) {
		this.heartbeat.set(heartbeat);
	}

	private void fireQuartz() {
		for (Entry<ComputeQuartzType, QuartzConfiguration<?>> quartz : quartzs.entrySet()) {
			try {
				quartzService.schedule(quartz.getKey(), quartz.getValue()).get();
			} catch (Exception e) {
				logger.warn("data collecting failed", e);
			}
		}
	}

	protected DistributedObjectFactory getObjectFactory() {
		return objectFactory;
	}

	@Override
	public synchronized void addVirtualMachineListener(CachedVirtualMachineListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!virtualMachineListeners.contains(service)) {
			dispatcher.addListener(service);
			virtualMachineListeners.add(service);
		}
	}

	@Override
	public synchronized void removeVirtualMachineListener(CachedVirtualMachineListener service) {
		if (virtualMachineListeners.contains(service)) {
			dispatcher.removeListener(service);
			virtualMachineListeners.remove(service);
		}
	}

	@Override
	public synchronized void addVirtualMachineProductListener(CachedVirtualMachineProductListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!virtualMachineProductListeners.contains(service)) {
			dispatcher.addListener(service);
			virtualMachineProductListeners.add(service);
		}
	}

	@Override
	public synchronized void removeVirtualMachineProductListener(CachedVirtualMachineProductListener service) {
		if (virtualMachineProductListeners.contains(service)) {
			dispatcher.removeListener(service);
			virtualMachineProductListeners.remove(service);
		}
	}


	private abstract class AbstractLockVmFunction<R> {

		private String virtualMachineId;


		public AbstractLockVmFunction(String virtualMachineId) {
			this.virtualMachineId = virtualMachineId;
		}

		public AsyncResult<R> execute() throws InternalException, CloudException {
			final VirtualMachineProxy vm = vmCache.get(virtualMachineId);
			if (vm != null) {
				if (vm.lock()) {
					try {
						AsyncResult<R> future = executeInner();
						Futures.addCallback(future, new UnlockCallback<R>(virtualMachineId));
						return future;
					} catch (Exception e) {
						vm.unlock();
						throw e;
					}
				} else {
					throw new IllegalStateException("vm is lock: " + virtualMachineId);
				}
			}
			return executeInner();
		}

		public abstract AsyncResult<R> executeInner() throws InternalException, CloudException;

	}

	private class UnlockCallback<O> implements FutureCallback<O> {

		private final String uuid;


		public UnlockCallback(String uuid) {
			checkNotNull(uuid, "invalid uuid");
			this.uuid = uuid;
		}

		@Override
		public void onSuccess(O result) {
			try {
				lazyUnlock(uuid);
			} catch (Exception e) {
				logger.error("unlock uuid failed", e);
			}
		}

		@Override
		public void onFailure(Throwable t) {
			try {
				lazyUnlock(uuid);
			} catch (Exception e) {
				logger.error("unlock uuid failed", e);
			}
		}

		// private void lazyUnlock(String uuid) throws Exception {
		// lazyUnlock(objectFactory, uuid);
		// }

		private void lazyUnlock(String uuid) {
			try {
				logger.debug("start lazyUnlock uuid: {}", uuid);
				// wait for the next refresh
				DistributedAtomicLong refreshDate = objectFactory.getAtomicLong("vmRefreshDate");
				long date = refreshDate.get();
				lock.lock();
				logger.debug("start await: {}", date);
				try {
					long lastRefreshDate = 0;
					while (date >= lastRefreshDate) {
						try {
							logger.debug("lastRefreshDate: {}", lastRefreshDate);
							isVmsRefresh.await();
							// force ignore first refresh and await twice in
							// order
							// to avoid dirty data in first round
							if (lastRefreshDate == 0) {
								lastRefreshDate = date;
							} else {
								lastRefreshDate = refreshDate.get();
							}
							logger.debug("async task thread awaked? {}", String.valueOf(date >= lastRefreshDate));
						} catch (InterruptedException e) {
							logger.warn("unexpected interrupt", e);
						}
					}
				} finally {
					logger.debug("waiting for unlock vm");
					lock.unlock();
				}
				logger.debug("start vm unlock");
				// if the new vm cannot found in the last refresh,
				// removing uuid from createVmMap will let vm won't be lock
				// if new vm was found in the last refresh, then vmid will be
				// filled. then we can unlock vm.
				creatingVmMap.lock(uuid);
				String vmid = null;
				try {
					vmid = creatingVmMap.remove(uuid);
				} finally {
					creatingVmMap.unlock(uuid);
				}
				if (Strings.isNullOrEmpty(vmid)) {
					vmid = uuid;
				}
				VirtualMachineProxy vm = vmCache.get(vmid);
				if (vm != null) {
					vm.unlock();
					logger.debug("vm: {} unlock", vmid);
				}
			} catch (Exception e) {
				logger.warn("encounter exception when unlock vm, the cacheMap might be empty.", e);
			}

		}

	}


	@Override
	public void persist(Configuration configuration) {
		// ignore
	}

	@Override
	public synchronized void lightMerge(Configuration configuration) {
		if (configuration.getId().equals(this.configuration.getId())) {
			// change listVirtualMachines delay
			Time vmDelay =
					configuration.getComputeConfiguration().getVirtualMachineConfiguration().getListVirtualMachines()
							.getDelay();
			if (quartzs.containsKey(ComputeQuartzType.VirtualMachine)) {
				Time oldDelay = quartzs.get(ComputeQuartzType.VirtualMachine).getTime();
				if (!oldDelay.equals(vmDelay)) {
					quartzs.get(ComputeQuartzType.VirtualMachine).setTime(vmDelay);
				}
			}

			// change listAllProducts delay
			Time productDelay =
					configuration.getComputeConfiguration().getVirtualMachineConfiguration().getListAllProducts().getDelay();
			if (quartzs.containsKey(ComputeQuartzType.VirtualMachineProduct)) {
				Time oldDelay = quartzs.get(ComputeQuartzType.VirtualMachineProduct).getTime();
				if (!oldDelay.equals(productDelay)) {
					quartzs.get(ComputeQuartzType.VirtualMachineProduct).setTime(productDelay);
				}
			}
		}
	}

	@Override
	public void heavyMerge(Configuration configuration) {
		// ignore
	}

	@Override
	public void remove(Configuration configuration) {
		// ignore
	}

	@Override
	public void clear() {
		// ignore
	}

}
