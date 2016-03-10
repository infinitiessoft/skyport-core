package com.infinities.skyport.cache.impl.service.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.service.network.AsyncVLANSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedNetworkServices.NetworkQuartzType;
import com.infinities.skyport.cache.service.network.CachedVLANSupport;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.Time;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.model.configuration.network.VLANConfiguration;
import com.infinities.skyport.model.network.VLANSupportProxy;
import com.infinities.skyport.proxy.network.SubnetProxy;
import com.infinities.skyport.proxy.network.VLANProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.quartz.QuartzConfiguration.Precondition;
import com.infinities.skyport.quartz.callable.SubnetCallable;
import com.infinities.skyport.quartz.callable.VLANCallable;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.network.vlan.SubnetFailureEvent;
import com.infinities.skyport.service.event.network.vlan.SubnetRefreshedEvent;
import com.infinities.skyport.service.event.network.vlan.VLANFailureEvent;
import com.infinities.skyport.service.event.network.vlan.VLANRefreshedEvent;

public class CachedVLANSupportImpl extends VLANSupportProxy implements CachedVLANSupport, ConfigurationLifeCycleListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(CachedVLANSupportImpl.class);
	private AsyncVLANSupport inner;
	private Configuration configuration;
	private final DistributedObjectFactory objectFactory;
	private final FirstLevelDispatcher dispatcher;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final DistributedMap<NetworkQuartzType, NetworkQuartzType> typeMap;
	private final DistributedCache<String, VLANProxy> vlanCache;
	private final DistributedCache<String, SubnetProxy> subnetCache;
	private final QuartzServiceImpl<NetworkQuartzType> quartzService;
	private final Map<NetworkQuartzType, QuartzConfiguration<?>> quartzs = Maps.newEnumMap(NetworkQuartzType.class);
	private final List<CachedVLANListener> vlanListeners = new ArrayList<CachedVLANListener>();
	private final List<CachedSubnetListener> subnetListeners = new ArrayList<CachedSubnetListener>();

	public CachedVLANSupportImpl(ConfigurationHome home, AsyncVLANSupport inner,
			Configuration configuration, QuartzServiceImpl<NetworkQuartzType> quartz,
			DistributedMap<NetworkQuartzType, NetworkQuartzType> typeMap, FirstLevelDispatcher dispatcher,
			DistributedObjectFactory objectFactory) {
		super(inner);
		home.addLifeCycleListener(this);
		this.inner = inner;
		this.configuration = configuration;
		this.dispatcher = dispatcher;
		this.objectFactory = objectFactory;
		this.typeMap = typeMap;
		this.quartzService = quartz;
		this.vlanCache = objectFactory.getCache("vlanCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
		this.subnetCache = objectFactory.getCache("subnetCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
	}
	
	public synchronized void initialize() {
		if (isInitialized.compareAndSet(false, true)) {
			// super.initialize();
			setUpSchedule(configuration.getNetworkConfiguration().getvLANConfiguration());
			logger.info("initialize cache");
		} else {
			throw new IllegalStateException("object has been initialized");
		}
	}
	
	public synchronized void close() {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				vlanCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			try {
				subnetCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			for (Iterator<CachedVLANListener> it = vlanListeners.iterator(); it.hasNext();) {
				CachedVLANListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
			for (Iterator<CachedSubnetListener> it = subnetListeners.iterator(); it.hasNext();) {
				CachedSubnetListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
		}
	}
	
	private void setUpSchedule(VLANConfiguration vlanConfiguration) {
		if (configuration.getStatus()) {
			Time vlanTime = vlanConfiguration.getListVlans().getDelay();

			Precondition vlanCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(NetworkQuartzType.VLAN);
				}

			};
			QuartzConfiguration<Iterable<VLAN>> vlanConfig =
					new QuartzConfiguration.Builder<Iterable<VLAN>>(new VLANCallable(inner.getSupport()),
							vlanCondition).addCallback(getVLANCallback()).delay(vlanTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("VLAN:" + configuration.getCloudName()).build();
			quartzs.put(NetworkQuartzType.VLAN, vlanConfig);
			
			QuartzConfiguration<Iterable<Subnet>> subnetConfig =
					new QuartzConfiguration.Builder<Iterable<Subnet>>(new SubnetCallable(inner.getSupport()),
							vlanCondition).addCallback(getSubnetCallback()).delay(vlanTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("Subnet:" + configuration.getCloudName()).build();
			quartzs.put(NetworkQuartzType.SUBNET, subnetConfig);

			fireQuartz();
		}
	}
	
	private FutureCallback<Iterable<VLAN>> getVLANCallback() {
		return new FutureCallback<Iterable<VLAN>>() {

			@Override
			public void onSuccess(Iterable<VLAN> result) {
				List<VLAN> vlanList = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} vlan success: {}", new Object[] { name, vlanList.size() });
				refreshVLANCache(result);
				dispatcher.fireRefreshedEvent(new VLANRefreshedEvent(vlanCache.values(), id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} vlan failed", name);
				logger.warn("list vlan failed", t);
				vlanCache.reload(t);
				dispatcher.fireFaiureEvent(new VLANFailureEvent(id, t));
			}

		};
	}
	
	protected void refreshVLANCache(Iterable<VLAN> result) {
		Map<String, VLANProxy> vlanMap = new HashMap<String, VLANProxy>();
		Iterator<VLAN> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			VLAN vlan = iterator.next();
			VLANProxy proxy =
					new VLANProxy(vlan, configName, configId, this.getObjectFactory().getAtomicLong(
							"vlan_" + vlan.getProviderVlanId()));
			vlanMap.put(vlan.getProviderVlanId(), proxy);
		}
		this.vlanCache.reload(vlanMap);
	}
	
	private FutureCallback<Iterable<Subnet>> getSubnetCallback() {
		return new FutureCallback<Iterable<Subnet>>() {

			@Override
			public void onSuccess(Iterable<Subnet> result) {
				List<Subnet> subnetList = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} subnet success: {}", new Object[] { name, subnetList.size() });
				refreshSubnetCache(result);
				dispatcher.fireRefreshedEvent(new SubnetRefreshedEvent(subnetCache.values(), id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} subnet failed", name);
				logger.warn("list subnet failed", t);
				vlanCache.reload(t);
				dispatcher.fireFaiureEvent(new SubnetFailureEvent(id, t));
			}

		};
	}
	
	protected void refreshSubnetCache(Iterable<Subnet> result) {
		Map<String, SubnetProxy> subnetMap = new HashMap<String, SubnetProxy>();
		Iterator<Subnet> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			Subnet subnet = iterator.next();
			SubnetProxy proxy =
					new SubnetProxy(subnet, configName, configId, this.getObjectFactory().getAtomicLong(
							"subnet_" + subnet.getProviderSubnetId()));
			subnetMap.put(subnet.getProviderSubnetId(), proxy);
		}
		this.subnetCache.reload(subnetMap);
	}
	
	private void fireQuartz() {
		for (Entry<NetworkQuartzType, QuartzConfiguration<?>> quartz : quartzs.entrySet()) {
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
	public AsyncResult<Iterable<VLAN>> listVlans() throws InternalException, CloudException {
		Iterable<VLAN> list = new ArrayList<VLAN>(vlanCache.values());
		ListenableFuture<Iterable<VLAN>> future = Futures.immediateFuture(list);
		AsyncResult<Iterable<VLAN>> ret = new AsyncResult<Iterable<VLAN>>(future);
		return ret;
	}
	
	@Override
	public AsyncResult<Iterable<Subnet>> listSubnets(String vlanId) throws InternalException, CloudException {
		Iterable<Subnet> list = new ArrayList<Subnet>(subnetCache.values());
		ListenableFuture<Iterable<Subnet>> future = Futures.immediateFuture(list);
		AsyncResult<Iterable<Subnet>> ret = new AsyncResult<Iterable<Subnet>>(future);
		return ret;
	}

	@Override
	public void persist(Configuration configuration) {
		// ignore
	}

	@Override
	public void lightMerge(Configuration configuration) {
		if (configuration.getId().equals(this.configuration.getId())) {
			// change listSnapshots delay
			Time delay = configuration.getNetworkConfiguration().getvLANConfiguration().getListVlans().getDelay();
			if (quartzs.containsKey(NetworkQuartzType.VLAN)) {
				Time oldDelay = quartzs.get(NetworkQuartzType.VLAN).getTime();
				if (!oldDelay.equals(delay)) {
					quartzs.get(NetworkQuartzType.VLAN).setTime(delay);
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

	@Override
	public void addVLANListener(CachedVLANListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!vlanListeners.contains(service)) {
			dispatcher.addListener(service);
			vlanListeners.add(service);
		}
	}

	@Override
	public void removeVLANListener(CachedVLANListener service) {
		if (vlanListeners.contains(service)) {
			dispatcher.removeListener(service);
			vlanListeners.remove(service);
		}
	}

	@Override
	public void addSubnetListener(CachedSubnetListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!subnetListeners.contains(service)) {
			dispatcher.addListener(service);
			subnetListeners.add(service);
		}
	}

	@Override
	public void removeSubnetListener(CachedSubnetListener service) {
		if (subnetListeners.contains(service)) {
			dispatcher.removeListener(service);
			subnetListeners.remove(service);
		}
	}

}
