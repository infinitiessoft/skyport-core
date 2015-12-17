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
