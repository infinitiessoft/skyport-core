package com.infinities.skyport.cache.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.cache.CachedServiceProvider;
import com.infinities.skyport.service.ConfigurationHome;

public class CachedServiceProviderFactory {

	public CachedServiceProviderFactory() {
	}

	public CachedServiceProvider getInstance(ConfigurationHome home, AsyncServiceProvider serviceProvider,
			ListeningScheduledExecutorService scheduler, ListeningExecutorService worker) throws Exception {
		CachedServiceProviderImpl cachedServiceProvider =
				new CachedServiceProviderImpl(home, serviceProvider, scheduler, worker);
		return cachedServiceProvider;
	}
}
