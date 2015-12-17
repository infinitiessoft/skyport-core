package com.infinities.skyport.cache.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.cache.CachedServiceProvider;

public class DelegatedServiceProviderFactory {

	public DelegatedServiceProviderFactory() {
	}

	public CachedServiceProvider getInstance(AsyncServiceProvider serviceProvider,
			ListeningScheduledExecutorService scheduler, ListeningExecutorService worker) throws Exception {
		CachedServiceProvider cachedServiceProvider = new DelegatedServiceProvider(serviceProvider);
		return cachedServiceProvider;
	}
}
