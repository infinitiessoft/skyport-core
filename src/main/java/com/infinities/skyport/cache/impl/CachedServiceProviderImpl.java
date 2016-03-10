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
package com.infinities.skyport.cache.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.util.NamingConstraints;
import org.dasein.cloud.util.ResourceNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.async.service.AsyncAdminServices;
import com.infinities.skyport.async.service.AsyncCIServices;
import com.infinities.skyport.async.service.AsyncDataCenterServices;
import com.infinities.skyport.async.service.AsyncIdentityServices;
import com.infinities.skyport.async.service.AsyncPlatformServices;
import com.infinities.skyport.cache.CachedServiceProvider;
import com.infinities.skyport.cache.impl.service.concurrent.CachedComputeServicesImplLazyInitializer;
import com.infinities.skyport.cache.impl.service.concurrent.CachedNetworkServicesImplLazyInitializer;
import com.infinities.skyport.cache.impl.service.concurrent.CachedStorageServicesImplLazyInitializer;
import com.infinities.skyport.cache.service.CachedComputeServices;
import com.infinities.skyport.cache.service.CachedNetworkServices;
import com.infinities.skyport.cache.service.CachedStorageServices;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.distributed.util.DistributedUtil;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FirstLevelDispatcher;

public class CachedServiceProviderImpl implements CachedServiceProvider {

	private Logger logger = LoggerFactory.getLogger(CachedServiceProviderImpl.class);
	public static final String COLLECTING = "collecting data, please try later";
	public static final long INITIAL_DELAY = 1;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final AsyncServiceProvider inner;

	private DistributedObjectFactory objectFactory;
	protected CachedComputeServicesImplLazyInitializer cachedComputeServicesLazyInitializer;
	protected CachedNetworkServicesImplLazyInitializer cachedNetworkServicesLazyInitializer;
	protected CachedStorageServicesImplLazyInitializer cachedStorageServicesLazyInitializer;
	
	public CachedServiceProviderImpl(ConfigurationHome home, AsyncServiceProvider inner,
			ListeningScheduledExecutorService scheduler, ListeningExecutorService worker) throws Exception {
		this.inner = inner;
		String distributedKey = DistributedUtil.generateDistributedName(inner.getConfiguration());
		objectFactory = DistributedUtil.getDistributedObjectFactory(distributedKey);
		FirstLevelDispatcher dispatcher =
				new EventBusFirstLevelDispatcher(new AsyncEventBus("eventbus-" + distributedKey, worker));
		cachedComputeServicesLazyInitializer =
				new CachedComputeServicesImplLazyInitializer(home, inner, inner.getConfiguration(), scheduler, worker,
						dispatcher, objectFactory);
		cachedNetworkServicesLazyInitializer =
				new CachedNetworkServicesImplLazyInitializer(home, inner, inner.getConfiguration(), scheduler, worker,
						dispatcher, objectFactory);
		cachedStorageServicesLazyInitializer =
				new CachedStorageServicesImplLazyInitializer(home, inner, inner.getConfiguration(), scheduler, worker,
						dispatcher, objectFactory);
	}

	@Override
	public void close() throws ConcurrentException {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				this.cachedComputeServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.cachedNetworkServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.cachedStorageServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.getObjectFactory().close();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			inner.close();
			logger.debug("close CacheDriver");
		}
	}

	protected DistributedObjectFactory getObjectFactory() {
		return objectFactory;
	}

	@Override
	public String testContext() {
		return inner.testContext();
	}

	@Override
	public boolean isConnected() {
		return inner.isConnected();
	}

	@Override
	public boolean hasAdminServices() {
		return inner.hasAdminServices();
	}

	@Override
	public boolean hasCIServices() {
		return inner.hasCIServices();
	}

	@Override
	public boolean hasComputeServices() {
		return inner.hasComputeServices();
	}

	@Override
	public boolean hasIdentityServices() {
		return inner.hasIdentityServices();
	}

	@Override
	public boolean hasNetworkServices() {
		return inner.hasNetworkServices();
	}

	@Override
	public boolean hasPlatformServices() {
		return inner.hasPlatformServices();
	}

	@Override
	public boolean hasStorageServices() {
		return inner.hasStorageServices();
	}

	@Override
	public ProviderContext getContext() {
		return inner.getContext();
	}

	@Override
	public ContextRequirements getContextRequirements() {
		return inner.getContextRequirements();
	}

	@Override
	public String findUniqueName(String baseName, NamingConstraints constraints, ResourceNamespace namespace)
			throws CloudException, InternalException {
		return inner.findUniqueName(baseName, constraints, namespace);
	}

	@Override
	public void initialize() throws Exception {
		if (isInitialized.compareAndSet(false, true)) {
			inner.initialize();
			this.cachedComputeServicesLazyInitializer.get().initialize();
		} else {
			throw new IllegalStateException("object have been initialized");
		}
	}

	@Override
	public CachedStorageServices getStorageServices() throws ConcurrentException {
		return cachedStorageServicesLazyInitializer.get();
	}

	@Override
	public AsyncAdminServices getAdminServices() throws ConcurrentException {
		return inner.getAdminServices();
	}

	@Override
	public AsyncDataCenterServices getDataCenterServices() throws ConcurrentException {
		return inner.getDataCenterServices();
	}

	@Override
	public AsyncCIServices getCIServices() throws ConcurrentException {
		return inner.getCIServices();
	}

	@Override
	public CachedComputeServices getComputeServices() throws ConcurrentException {
		return cachedComputeServicesLazyInitializer.get();
	}

	@Override
	public AsyncIdentityServices getIdentityServices() throws ConcurrentException {
		return inner.getIdentityServices();
	}

	@Override
	public CachedNetworkServices getNetworkServices() throws ConcurrentException {
		return cachedNetworkServicesLazyInitializer.get();
	}

	@Override
	public AsyncPlatformServices getPlatformServices() throws ConcurrentException {
		return inner.getPlatformServices();
	}

	@Override
	public Configuration getConfiguration() {
		return inner.getConfiguration();
	}

	public AsyncServiceProvider getInner() {
		return inner;
	}

}
