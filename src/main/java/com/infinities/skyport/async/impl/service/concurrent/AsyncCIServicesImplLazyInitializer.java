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
