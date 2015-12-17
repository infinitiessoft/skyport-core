package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.impl.service.AsyncStorageServicesImpl;
import com.infinities.skyport.async.service.AsyncStorageServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.StorageConfiguration;

public class AsyncStorageServicesImplLazyInitializer extends LazyInitializer<AsyncStorageServices> {

	private String configurationId;
	private ServiceProvider inner;
	private StorageConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncStorageServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			StorageConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncStorageServices initialize() throws ConcurrentException {
		try {
			if (inner.hasStorageServices()) {
				AsyncStorageServices services =
						new AsyncStorageServicesImpl(configurationId, inner, configuration, threadPools);
				services.initialize();
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;

	}

}
