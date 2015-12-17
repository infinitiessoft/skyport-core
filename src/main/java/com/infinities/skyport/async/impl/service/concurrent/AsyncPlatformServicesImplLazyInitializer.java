package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.impl.service.AsyncPlatformServicesImpl;
import com.infinities.skyport.async.service.AsyncPlatformServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.PlatformConfiguration;

public class AsyncPlatformServicesImplLazyInitializer extends LazyInitializer<AsyncPlatformServices> {

	private String configurationId;
	private ServiceProvider inner;
	private PlatformConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncPlatformServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			PlatformConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncPlatformServices initialize() throws ConcurrentException {
		try {
			if (inner.hasPlatformServices()) {
				AsyncPlatformServices services =
						new AsyncPlatformServicesImpl(configurationId, inner, configuration, threadPools);
				services.initialize();
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;

	}

}
