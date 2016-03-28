package com.infinities.skyport.cache.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.cache.impl.service.CachedStorageServicesImpl;
import com.infinities.skyport.cache.service.CachedStorageServices;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FirstLevelDispatcher;

public class CachedStorageServicesImplLazyInitializer extends LazyInitializer<CachedStorageServices>{

	private ConfigurationHome home;
	private AsyncServiceProvider inner;
	private Configuration configuration;
	private DistributedObjectFactory objectFactory;
	private ListeningScheduledExecutorService scheduler;
	private ListeningExecutorService worker;
	private FirstLevelDispatcher dispatcher;


	public CachedStorageServicesImplLazyInitializer(ConfigurationHome home, AsyncServiceProvider inner,
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
	protected CachedStorageServices initialize() throws ConcurrentException {
		try {
			if (inner.hasStorageServices()) {
				CachedStorageServices services =
						new CachedStorageServicesImpl(home, inner.getStorageServices(), configuration, scheduler, worker,
								dispatcher, objectFactory);
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}

		return null;
	}

}
