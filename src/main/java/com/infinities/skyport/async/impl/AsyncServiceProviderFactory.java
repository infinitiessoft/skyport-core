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
package com.infinities.skyport.async.impl;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationHome;

public class AsyncServiceProviderFactory implements IAsyncServiceProviderFactory {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.infinities.skyport.async.impl.IAsyncServiceProviderFactory#getInstance
	 * (com.infinities.skyport.service.ConfigurationHome,
	 * com.infinities.skyport.ServiceProvider,
	 * com.infinities.skyport.model.configuration.Configuration,
	 * com.google.common.util.concurrent.ListeningScheduledExecutorService)
	 */
	@Override
	public AsyncServiceProvider getInstance(ConfigurationHome configurationHome, ServiceProvider provider,
			Configuration configuration, ListeningScheduledExecutorService scheduler) throws Exception {
		AsyncServiceProviderImpl proxy = new AsyncServiceProviderImpl(provider, configuration, scheduler);
		configurationHome.addLifeCycleListener(proxy);
		return proxy;
	}
}
