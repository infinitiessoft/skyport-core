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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.service.compute.AsyncVolumeSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.Time;
import com.infinities.skyport.model.compute.VolumeSupportProxy;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.model.configuration.compute.VolumeConfiguration;
import com.infinities.skyport.proxy.VolumeProductProxy;
import com.infinities.skyport.proxy.VolumeProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzConfiguration.Precondition;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.quartz.callable.VolumeCallable;
import com.infinities.skyport.quartz.callable.VolumeProductCallable;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.volume.VolumeFailureEvent;
import com.infinities.skyport.service.event.compute.volume.VolumeRefreshedEvent;
import com.infinities.skyport.service.event.compute.volumeproduct.VolumeProductFailureEvent;
import com.infinities.skyport.service.event.compute.volumeproduct.VolumeProductRefreshedEvent;

public class CachedVolumeSupportImpl extends VolumeSupportProxy implements CachedVolumeSupport,
		ConfigurationLifeCycleListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(CachedVolumeSupportImpl.class);
	private AsyncVolumeSupport inner;
	private Configuration configuration;
	private final DistributedObjectFactory objectFactory;
	private final FirstLevelDispatcher dispatcher;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private final DistributedCache<String, VolumeProxy> volumeCache;
	private final DistributedCache<String, VolumeProductProxy> volumeProductCache;
	private final QuartzServiceImpl<ComputeQuartzType> quartzService;
	private final Map<ComputeQuartzType, QuartzConfiguration<?>> quartzs =
			new EnumMap<ComputeQuartzType, QuartzConfiguration<?>>(ComputeQuartzType.class);
	private final List<CachedVolumeListener> volumeListeners = new ArrayList<CachedVolumeListener>();
	private final List<CachedVolumeProductListener> volumeProductListeners = new ArrayList<CachedVolumeProductListener>();


	public CachedVolumeSupportImpl(ConfigurationHome home, AsyncVolumeSupport inner, Configuration configuration,
			QuartzServiceImpl<ComputeQuartzType> quartz, DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap,
			FirstLevelDispatcher dispatcher, DistributedObjectFactory objectFactory) {
		super(inner);
		home.addLifeCycleListener(this);
		this.inner = inner;
		this.configuration = configuration;
		this.dispatcher = dispatcher;
		this.objectFactory = objectFactory;
		this.quartzService = quartz;
		this.typeMap = typeMap;
		volumeCache = objectFactory.getCache("volumeCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
		volumeProductCache =
				objectFactory
						.getCache("volumeProductCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
	}

	public synchronized void initialize() {
		if (isInitialized.compareAndSet(false, true)) {
			// super.initialize();
			setUpSchedule(configuration.getComputeConfiguration().getVolumeConfiguration());
			logger.info("initialize cache");
		} else {
			throw new IllegalStateException("object has been initialized");
		}
	}

	public synchronized void close() {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				volumeCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			try {
				volumeProductCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			for (Iterator<CachedVolumeListener> it = volumeListeners.iterator(); it.hasNext();) {
				CachedVolumeListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
			for (Iterator<CachedVolumeProductListener> it = volumeProductListeners.iterator(); it.hasNext();) {
				CachedVolumeProductListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
		}
	}

	private void setUpSchedule(VolumeConfiguration volumeConfiguration) {
		if (configuration.getStatus()) {
			Time volumeTime = volumeConfiguration.getListVolumes().getDelay();
			Precondition volumeCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(ComputeQuartzType.Volume);
				}

			};
			QuartzConfiguration<Iterable<Volume>> volumeConfig =
					new QuartzConfiguration.Builder<Iterable<Volume>>(new VolumeCallable(inner.getSupport()),
							volumeCondition).addCallback(getVolumemCallback()).delay(volumeTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("Volume:" + configuration.getCloudName()).build();
			quartzs.put(ComputeQuartzType.Volume, volumeConfig);

			Time productTime = volumeConfiguration.getListVolumeProducts().getDelay();
			Precondition productCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(ComputeQuartzType.VolumeProduct);
				}

			};
			QuartzConfiguration<Iterable<VolumeProduct>> productConfig =
					new QuartzConfiguration.Builder<Iterable<VolumeProduct>>(new VolumeProductCallable(inner.getSupport()),
							productCondition).addCallback(getVolumeProductCallback()).delay(productTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("Volume Product:" + configuration.getCloudName()).build();
			quartzs.put(ComputeQuartzType.VolumeProduct, productConfig);

			fireQuartz();
		}
	}

	private FutureCallback<Iterable<Volume>> getVolumemCallback() {
		return new FutureCallback<Iterable<Volume>>() {

			@Override
			public void onSuccess(Iterable<Volume> result) {
				List<Volume> volumeList = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} volume success: {}", new Object[] { name, volumeList.size() });
				refreshVolumeCache(result);
				dispatcher.fireRefreshedEvent(new VolumeRefreshedEvent(volumeCache.values(), id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} volume failed", name);
				logger.warn("list volume failed", t);
				volumeCache.reload(t);
				dispatcher.fireFaiureEvent(new VolumeFailureEvent(id, t));
			}

		};
	}

	private FutureCallback<Iterable<VolumeProduct>> getVolumeProductCallback() {
		return new FutureCallback<Iterable<VolumeProduct>>() {

			@Override
			public void onSuccess(Iterable<VolumeProduct> result) {
				List<VolumeProduct> list = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} volume product success: {}", new Object[] { name, list.size() });
				refreshVolumeProductCache(result);

				List<VolumeProduct> values = Lists.newArrayList();
				for (VolumeProductProxy proxy : volumeProductCache.values()) {
					values.add(proxy.getProduct());
				}

				dispatcher.fireRefreshedEvent(new VolumeProductRefreshedEvent(values, id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} volume product failed", name);
				logger.warn("list volume failed", t);
				volumeProductCache.reload(t);
				dispatcher.fireFaiureEvent(new VolumeProductFailureEvent(id, t));
			}

		};
	}

	protected void refreshVolumeCache(Iterable<Volume> result) {
		Map<String, VolumeProxy> volumeMap = new HashMap<String, VolumeProxy>();
		Iterator<Volume> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			Volume volume = iterator.next();
			VolumeProxy proxy =
					new VolumeProxy(volume, configName, configId, this.getObjectFactory().getAtomicLong(
							"volume_" + volume.getProviderVolumeId()));
			volumeMap.put(volume.getProviderVolumeId(), proxy);
		}
		this.volumeCache.reload(volumeMap);
	}

	protected void refreshVolumeProductCache(Iterable<VolumeProduct> result) {
		Map<String, VolumeProductProxy> productMap = new HashMap<String, VolumeProductProxy>();
		Iterator<VolumeProduct> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			VolumeProduct product = iterator.next();
			VolumeProductProxy proxy = new VolumeProductProxy(product, configName, configId);
			productMap.put(product.getProviderProductId(), proxy);
		}
		this.volumeProductCache.reload(productMap);
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

	@Override
	public AsyncResult<Iterable<VolumeProduct>> listVolumeProducts() throws InternalException, CloudException {
		List<VolumeProduct> list = new ArrayList<VolumeProduct>();
		for (VolumeProductProxy product : volumeProductCache.values()) {
			list.add(product.getProduct());
		}
		ListenableFuture<Iterable<VolumeProduct>> future = Futures.immediateFuture((Iterable<VolumeProduct>) list);
		AsyncResult<Iterable<VolumeProduct>> ret = new AsyncResult<Iterable<VolumeProduct>>(future);
		return ret;
	}

	@Override
	public AsyncResult<Iterable<Volume>> listVolumes() throws InternalException, CloudException {
		Iterable<Volume> list = new ArrayList<Volume>(volumeCache.values());
		ListenableFuture<Iterable<Volume>> future = Futures.immediateFuture(list);
		AsyncResult<Iterable<Volume>> ret = new AsyncResult<Iterable<Volume>>(future);
		return ret;
	}

	protected DistributedObjectFactory getObjectFactory() {
		return objectFactory;
	}

	@Override
	public synchronized void addVolumeListener(CachedVolumeListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!volumeListeners.contains(service)) {
			dispatcher.addListener(service);
			volumeListeners.add(service);
		}
	}

	@Override
	public synchronized void removeVolumeListener(CachedVolumeListener service) {
		if (volumeListeners.contains(service)) {
			dispatcher.removeListener(service);
			volumeListeners.remove(service);
		}
	}

	@Override
	public synchronized void addVolumeProductListener(CachedVolumeProductListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!volumeProductListeners.contains(service)) {
			dispatcher.addListener(service);
			volumeProductListeners.add(service);
		}
	}

	@Override
	public synchronized void removeVolumeProductListener(CachedVolumeProductListener service) {
		if (volumeProductListeners.contains(service)) {
			dispatcher.removeListener(service);
			volumeProductListeners.remove(service);
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
			Time volumeDelay = configuration.getComputeConfiguration().getVolumeConfiguration().getListVolumes().getDelay();
			if (quartzs.containsKey(ComputeQuartzType.Volume)) {
				Time oldDelay = quartzs.get(ComputeQuartzType.Volume).getTime();
				if (!oldDelay.equals(volumeDelay)) {
					quartzs.get(ComputeQuartzType.Volume).setTime(volumeDelay);
				}
			}

			// change listAllProducts delay
			Time productDelay =
					configuration.getComputeConfiguration().getVolumeConfiguration().getListVolumeProducts().getDelay();
			if (quartzs.containsKey(ComputeQuartzType.VolumeProduct)) {
				Time oldDelay = quartzs.get(ComputeQuartzType.VolumeProduct).getTime();
				if (!oldDelay.equals(productDelay)) {
					quartzs.get(ComputeQuartzType.VolumeProduct).setTime(productDelay);
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
