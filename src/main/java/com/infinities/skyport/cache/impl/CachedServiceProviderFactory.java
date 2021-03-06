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
package com.infinities.skyport.cache.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.cache.CachedServiceProvider;
import com.infinities.skyport.service.ConfigurationHome;

public class CachedServiceProviderFactory implements ICachedServiceProviderFactory {

	public CachedServiceProviderFactory() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.infinities.skyport.cache.impl.ICachedServiceProviderFactory#getInstance
	 * (com.infinities.skyport.service.ConfigurationHome,
	 * com.infinities.skyport.async.AsyncServiceProvider,
	 * com.google.common.util.concurrent.ListeningScheduledExecutorService,
	 * com.google.common.util.concurrent.ListeningExecutorService)
	 */
	@Override
	public CachedServiceProvider getInstance(ConfigurationHome home, AsyncServiceProvider serviceProvider,
			ListeningScheduledExecutorService scheduler, ListeningExecutorService worker) throws Exception {
		CachedServiceProviderImpl cachedServiceProvider = new CachedServiceProviderImpl(home, serviceProvider, scheduler,
				worker);
		return cachedServiceProvider;
	}
}
