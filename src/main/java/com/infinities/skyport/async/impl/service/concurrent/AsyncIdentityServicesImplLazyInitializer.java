package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.impl.service.AsyncIdentityServicesImpl;
import com.infinities.skyport.async.service.AsyncIdentityServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.IdentityConfiguration;

public class AsyncIdentityServicesImplLazyInitializer extends LazyInitializer<AsyncIdentityServices> {

	private String configurationId;
	private ServiceProvider inner;
	private IdentityConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncIdentityServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			IdentityConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncIdentityServices initialize() throws ConcurrentException {
		try {
			if (inner.hasIdentityServices()) {
				AsyncIdentityServices services =
						new AsyncIdentityServicesImpl(configurationId, inner, configuration, threadPools);
				services.initialize();
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;
	}

}
