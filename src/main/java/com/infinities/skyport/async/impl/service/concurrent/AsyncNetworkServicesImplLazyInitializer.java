package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.impl.service.AsyncNetworkServicesImpl;
import com.infinities.skyport.async.service.AsyncNetworkServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.NetworkConfiguration;

public class AsyncNetworkServicesImplLazyInitializer extends LazyInitializer<AsyncNetworkServices> {

	private String configurationId;
	private ServiceProvider inner;
	private NetworkConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncNetworkServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			NetworkConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncNetworkServices initialize() throws ConcurrentException {
		try {
			if (inner.hasNetworkServices()) {
				AsyncNetworkServices services =
						new AsyncNetworkServicesImpl(configurationId, inner, configuration, threadPools);
				services.initialize();
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;
	}

}
