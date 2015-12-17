package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.identity.IdentityServices;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncIdentityServices;
import com.infinities.skyport.async.service.identity.AsyncIdentityAndAccessSupport;
import com.infinities.skyport.async.service.identity.AsyncShellKeySupport;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.IdentityConfiguration;

public class AsyncIdentityServicesImpl implements AsyncIdentityServices {

	private final IdentityServices inner;
	private AsyncIdentityAndAccessSupport asyncIdentityAndAccessSupport;
	private AsyncShellKeySupport asyncShellKeySupport;


	public AsyncIdentityServicesImpl(String configurationId, ServiceProvider inner, IdentityConfiguration configuration,
			DistributedThreadPool threadPools) throws ConcurrentException {
		this.inner = inner.getIdentityServices();
		if (this.inner.hasIdentityAndAccessSupport()) {
			this.asyncIdentityAndAccessSupport =
					Reflection.newProxy(AsyncIdentityAndAccessSupport.class,
							new AsyncHandler(configurationId, TaskType.IdentityAndAccessSupport, inner, threadPools,
									configuration.getIdentityAndAccessConfiguration()));
		}
		if (this.inner.hasShellKeySupport()) {
			this.asyncShellKeySupport =
					Reflection.newProxy(AsyncShellKeySupport.class, new AsyncHandler(configurationId,
							TaskType.ShellKeySupport, inner, threadPools, configuration.getShellKeyConfiguration()));
		}
	}

	@Override
	public AsyncIdentityAndAccessSupport getIdentityAndAccessSupport() {
		return asyncIdentityAndAccessSupport;
	}

	@Override
	public AsyncShellKeySupport getShellKeySupport() {
		return asyncShellKeySupport;
	}

	@Override
	public boolean hasIdentityAndAccessSupport() {
		return inner.hasIdentityAndAccessSupport();
	}

	@Override
	public boolean hasShellKeySupport() {
		return inner.hasShellKeySupport();
	}

	@Override
	public void initialize() throws Exception {

	}

	@Override
	public void close() {

	}

}
