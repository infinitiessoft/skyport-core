package com.infinities.skyport.async.impl;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationHome;

public class AsyncServiceProviderFactory {

	public AsyncServiceProvider getInstance(ConfigurationHome configurationHome, ServiceProvider provider,
			Configuration configuration, ListeningScheduledExecutorService scheduler) throws Exception {
		AsyncServiceProviderImpl proxy = new AsyncServiceProviderImpl(provider, configuration, scheduler);
		configurationHome.addLifeCycleListener(proxy);
		return proxy;
	}
}
