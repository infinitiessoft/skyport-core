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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.service.compute.AsyncMachineImageSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedMachineImageSupport;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.Time;
import com.infinities.skyport.model.compute.MachineImageSupportProxy;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.model.configuration.compute.MachineImageConfiguration;
import com.infinities.skyport.proxy.MachineImageProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzConfiguration.Precondition;
import com.infinities.skyport.quartz.QuartzService;
import com.infinities.skyport.quartz.callable.ImageCallable;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.machineimage.MachineImageFailureEvent;
import com.infinities.skyport.service.event.compute.machineimage.MachineImageRefreshedEvent;

public class CachedMachineImageSupportImpl extends MachineImageSupportProxy implements CachedMachineImageSupport,
		ConfigurationLifeCycleListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(CachedMachineImageSupportImpl.class);
	private final AsyncMachineImageSupport inner;
	private final DistributedObjectFactory objectFactory;
	private final FirstLevelDispatcher dispatcher;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private final DistributedCache<String, MachineImageProxy> imageCache;
	private final QuartzService<ComputeQuartzType> quartzService;
	private final Map<ComputeQuartzType, QuartzConfiguration<?>> quartzs = Maps.newEnumMap(ComputeQuartzType.class);
	private final List<CachedMachineImageListener> machineImageListeners = new ArrayList<CachedMachineImageListener>();
	private Configuration configuration;


	public CachedMachineImageSupportImpl(ConfigurationHome home, AsyncMachineImageSupport inner,
			Configuration configuration, QuartzService<ComputeQuartzType> quartz,
			DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap, FirstLevelDispatcher dispatcher,
			DistributedObjectFactory objectFactory) {
		super(inner);
		home.addLifeCycleListener(this);
		this.inner = inner;
		this.configuration = configuration;
		this.quartzService = quartz;
		this.typeMap = typeMap;
		this.dispatcher = dispatcher;
		this.objectFactory = objectFactory;
		imageCache = objectFactory.getCache("imageCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
	}

	public synchronized void initialize() {
		if (isInitialized.compareAndSet(false, true)) {
			// super.initialize();
			setUpSchedule(configuration.getComputeConfiguration().getMachineImageConfiguration());
			logger.info("initialize cache");
		} else {
			throw new IllegalStateException("object has been initialized");
		}
	}

	public synchronized void close() {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				imageCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			logger.debug("close CacheDriver");
			for (Iterator<CachedMachineImageListener> it = machineImageListeners.iterator(); it.hasNext();) {
				CachedMachineImageListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
		}

	}

	@Override
	public AsyncResult<Iterable<MachineImage>> listImages(ImageFilterOptions options) throws CloudException,
			InternalException {
		if (options == null || (!options.hasCriteria() && !options.isMatchesAny())) {
			Iterable<MachineImage> list = new ArrayList<MachineImage>(imageCache.values());
			ListenableFuture<Iterable<MachineImage>> future = Futures.immediateFuture(list);
			AsyncResult<Iterable<MachineImage>> ret = new AsyncResult<Iterable<MachineImage>>(future);
			return ret;
		} else {
			return inner.listImages(options);
		}
	}

	private void setUpSchedule(MachineImageConfiguration machineImageConfiguration) {
		if (configuration.getStatus()) {
			Time imageDelay = machineImageConfiguration.getListImages().getDelay();
			Precondition imageCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(ComputeQuartzType.MachineImage);
				}

			};
			QuartzConfiguration<Iterable<MachineImage>> config =
					new QuartzConfiguration.Builder<Iterable<MachineImage>>(new ImageCallable(inner.getSupport()),
							imageCondition).addCallback(getImageCallback()).delay(imageDelay)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("Image:" + configuration.getCloudName()).build();
			quartzs.put(ComputeQuartzType.MachineImage, config);

			fireQuartz();
		}
	}

	private FutureCallback<Iterable<MachineImage>> getImageCallback() {
		return new FutureCallback<Iterable<MachineImage>>() {

			@Override
			public void onSuccess(Iterable<MachineImage> result) {
				List<MachineImage> vmList = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} MachineImage success: {}", new Object[] { name, vmList.size() });
				refreshImageCache(result);
				dispatcher.fireRefreshedEvent(new MachineImageRefreshedEvent(imageCache.values(), id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} MachineImage failed", name);
				logger.warn("list MachineImage failed", t);
				imageCache.reload(t);
				dispatcher.fireFaiureEvent(new MachineImageFailureEvent(id, t));
			}

		};
	}

	protected void refreshImageCache(Iterable<MachineImage> result) {
		Map<String, MachineImageProxy> imageMap = new HashMap<String, MachineImageProxy>();
		Iterator<MachineImage> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			MachineImage image = iterator.next();
			MachineImageProxy proxy = new MachineImageProxy(image, configName, configId);
			imageMap.put(image.getProviderMachineImageId(), proxy);
		}
		this.imageCache.reload(imageMap);
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
	public synchronized void addMachineImageListener(CachedMachineImageListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!machineImageListeners.contains(service)) {
			dispatcher.addListener(service);
			machineImageListeners.add(service);
		}
	}

	@Override
	public synchronized void removeMachineImageListener(CachedMachineImageListener service) {
		if (machineImageListeners.contains(service)) {
			dispatcher.removeListener(service);
			machineImageListeners.remove(service);
		}
	}

	@Override
	public void persist(Configuration configuration) {
		// ignore
	}

	@Override
	public synchronized void lightMerge(Configuration configuration) {
		if (configuration.getId().equals(this.configuration.getId())) {
			this.configuration = configuration;
			// change listImages delay
			Time delay = configuration.getComputeConfiguration().getMachineImageConfiguration().getListImages().getDelay();
			if (quartzs.containsKey(ComputeQuartzType.MachineImage)) {
				Time oldDelay = quartzs.get(ComputeQuartzType.MachineImage).getTime();
				if (!oldDelay.equals(delay)) {
					quartzs.get(ComputeQuartzType.MachineImage).setTime(delay);
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
