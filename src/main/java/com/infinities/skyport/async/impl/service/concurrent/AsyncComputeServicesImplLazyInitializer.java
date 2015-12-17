package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.impl.service.AsyncComputeServicesImpl;
import com.infinities.skyport.async.service.AsyncComputeServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.ComputeConfiguration;

public class AsyncComputeServicesImplLazyInitializer extends LazyInitializer<AsyncComputeServices> {

	private String configurationId;
	private ServiceProvider inner;
	private ComputeConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncComputeServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			ComputeConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncComputeServices initialize() throws ConcurrentException {
		try {
			if (inner.hasComputeServices()) {
				AsyncComputeServices services =
						new AsyncComputeServicesImpl(configurationId, inner, configuration, threadPools);
				services.initialize();
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}

		return null;
	}

}
