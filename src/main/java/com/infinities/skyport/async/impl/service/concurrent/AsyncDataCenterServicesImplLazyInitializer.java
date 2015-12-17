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

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncDataCenterServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.DataCenterConfiguration;

public class AsyncDataCenterServicesImplLazyInitializer extends LazyInitializer<AsyncDataCenterServices> {

	private String configurationId;
	private ServiceProvider inner;
	private DataCenterConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncDataCenterServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			DataCenterConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncDataCenterServices initialize() throws ConcurrentException {
		try {
			if (inner.getDataCenterServices() != null) {
				AsyncDataCenterServices services =
						Reflection.newProxy(AsyncDataCenterServices.class, new AsyncHandler(configurationId,
								TaskType.DataCenterServices, inner, threadPools, configuration));
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;
	}
}
