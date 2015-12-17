package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.impl.service.AsyncAdminServicesImpl;
import com.infinities.skyport.async.service.AsyncAdminServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.AdminConfiguration;

public class AsyncAdminServicesImplLazyInitializer extends LazyInitializer<AsyncAdminServices> {

	private String configurationId;
	private ServiceProvider inner;
	private AdminConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncAdminServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			AdminConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncAdminServices initialize() throws ConcurrentException {
		try {
			if (inner.hasAdminServices()) {
				AsyncAdminServices services = new AsyncAdminServicesImpl(configurationId, inner, configuration, threadPools);
				services.initialize();
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;
	}

}
