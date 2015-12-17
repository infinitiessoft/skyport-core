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
package com.infinities.skyport.cache.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.cache.impl.service.CachedComputeServicesImpl;
import com.infinities.skyport.cache.service.CachedComputeServices;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FirstLevelDispatcher;

public class CachedComputeServicesImplLazyInitializer extends LazyInitializer<CachedComputeServices> {

	private ConfigurationHome home;
	private AsyncServiceProvider inner;
	private Configuration configuration;
	private DistributedObjectFactory objectFactory;
	private ListeningScheduledExecutorService scheduler;
	private ListeningExecutorService worker;
	private FirstLevelDispatcher dispatcher;


	public CachedComputeServicesImplLazyInitializer(ConfigurationHome home, AsyncServiceProvider inner,
			Configuration configuration, ListeningScheduledExecutorService scheduler, ListeningExecutorService worker,
			FirstLevelDispatcher dispatcher, DistributedObjectFactory objectFactory) {
		super();
		this.home = home;
		this.inner = inner;
		this.configuration = configuration;
		this.objectFactory = objectFactory;
		this.scheduler = scheduler;
		this.worker = worker;
		this.dispatcher = dispatcher;
	}

	@Override
	protected CachedComputeServices initialize() throws ConcurrentException {
		try {
			if (inner.hasComputeServices()) {
				CachedComputeServices services =
						new CachedComputeServicesImpl(home, inner.getComputeServices(), configuration, scheduler, worker,
								dispatcher, objectFactory);
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}

		return null;
	}

}
