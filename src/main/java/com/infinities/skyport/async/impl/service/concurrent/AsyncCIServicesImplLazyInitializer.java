package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.impl.service.AsyncCIServicesImpl;
import com.infinities.skyport.async.service.AsyncCIServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.CIConfiguration;

public class AsyncCIServicesImplLazyInitializer extends LazyInitializer<AsyncCIServices> {

	private String configurationId;
	private ServiceProvider inner;
	private CIConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncCIServicesImplLazyInitializer(String configurationId, ServiceProvider inner, CIConfiguration configuration,
			DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncCIServices initialize() throws ConcurrentException {
		try {
			if (inner.hasCIServices()) {
				AsyncCIServices services = new AsyncCIServicesImpl(configurationId, inner, configuration, threadPools);
				services.initialize();

				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;

	}

}
