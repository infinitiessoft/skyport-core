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

import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.async.service.AsyncAdminServices;
import com.infinities.skyport.async.service.AsyncCIServices;
import com.infinities.skyport.async.service.AsyncDataCenterServices;
import com.infinities.skyport.async.service.AsyncIdentityServices;
import com.infinities.skyport.async.service.AsyncNetworkServices;
import com.infinities.skyport.async.service.AsyncPlatformServices;
import com.infinities.skyport.async.service.AsyncStorageServices;
import com.infinities.skyport.cache.CachedServiceProvider;
import com.infinities.skyport.cache.impl.service.DelegatedComputeServices;
import com.infinities.skyport.cache.service.CachedComputeServices;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.distributed.util.DistributedUtil;
import com.infinities.skyport.model.configuration.Configuration;

public class DelegatedServiceProvider implements CachedServiceProvider {

	private static final Logger logger = LoggerFactory.getLogger(DelegatedServiceProvider.class);
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final AsyncServiceProvider inner;
	private CachedComputeServices delegatedComputeServices;


	public DelegatedServiceProvider(AsyncServiceProvider inner) throws ConcurrentException {
		this.inner = inner;
		if (inner.hasComputeServices()) {
			this.delegatedComputeServices = new DelegatedComputeServices(inner.getComputeServices());
		}
	}

	@Override
	public void initialize() throws Exception {
		if (isInitialized.compareAndSet(false, true)) {
			inner.initialize();
			if (delegatedComputeServices != null) {
				delegatedComputeServices.initialize();
			}
		}
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
	public void close() throws ConcurrentException {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				String distributedKey = DistributedUtil.generateDistributedName(inner.getConfiguration());
				DistributedObjectFactory objectFactory = DistributedUtil.getDistributedObjectFactory(distributedKey);
				objectFactory.close();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			inner.close();
			logger.debug("close CacheDriver");
		}
	}

	@Override
	public AsyncStorageServices getStorageServices() throws ConcurrentException {
		return inner.getStorageServices();
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
	public AsyncIdentityServices getIdentityServices() throws ConcurrentException {
		return inner.getIdentityServices();
	}

	@Override
	public AsyncNetworkServices getNetworkServices() throws ConcurrentException {
		return inner.getNetworkServices();
	}

	@Override
	public AsyncPlatformServices getPlatformServices() throws ConcurrentException {
		return inner.getPlatformServices();
	}

	@Override
	public CachedComputeServices getComputeServices() {
		return delegatedComputeServices;
	}

	@Override
	public Configuration getConfiguration() {
		return inner.getConfiguration();
	}

}
