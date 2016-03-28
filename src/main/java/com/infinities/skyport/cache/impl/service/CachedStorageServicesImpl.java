package com.infinities.skyport.cache.impl.service;

import java.util.concurrent.ScheduledFuture;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.service.AsyncStorageServices;
import com.infinities.skyport.async.service.storage.AsyncOfflineStoreSupport;
import com.infinities.skyport.cache.impl.service.storage.CachedBlobStoreSupportImpl;
import com.infinities.skyport.cache.service.CachedStorageServices;
import com.infinities.skyport.cache.service.storage.CachedBlobStoreSupport;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FirstLevelDispatcher;

public class CachedStorageServicesImpl implements CachedStorageServices{

	private final AsyncStorageServices inner;
	private final ListeningExecutorService worker;
	private final DistributedObjectFactory objectFactory;
	private CachedBlobStoreSupportImpl cachedBlobStoreSupport;
	private final QuartzServiceImpl<StorageQuartzType> quartzService;


	public CachedStorageServicesImpl(ConfigurationHome home, AsyncStorageServices inner, Configuration configuration,
			ListeningScheduledExecutorService scheduler, ListeningExecutorService worker, FirstLevelDispatcher dispatcher,
			DistributedObjectFactory objectFactory) {
		this.inner = inner;
		this.worker = worker;
		this.objectFactory = objectFactory;
		DistributedMap<StorageQuartzType, StorageQuartzType> typeMap = getObjectFactory().getMap("storage_quartz");
		if (typeMap.isEmpty()) {
			for (StorageQuartzType type : StorageQuartzType.values()) {
				typeMap.put(type, type);
			}
		}
		this.quartzService = new QuartzServiceImpl<StorageQuartzType>(StorageQuartzType.class, scheduler, worker);
		if (inner.hasOnlineStorageSupport()) {
			this.cachedBlobStoreSupport =
					new CachedBlobStoreSupportImpl(home, inner.getOnlineStorageSupport(), configuration,
							quartzService, typeMap, dispatcher, objectFactory);
		}
	}
	
	public DistributedObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public ListeningExecutorService getWorker() {
		return worker;
	}
	
	@Override
	public boolean hasOfflineStorageSupport() {
		return inner.hasOfflineStorageSupport();
	}

	@Override
	public boolean hasOnlineStorageSupport() {
		return inner.hasOnlineStorageSupport();
	}

	@Override
	public void initialize() throws Exception {
		inner.initialize();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				if (cachedBlobStoreSupport != null) {
					cachedBlobStoreSupport.initialize();
				}
			}

		};
		getWorker().execute(r);
	}

	@Override
	public void close() {
		if (cachedBlobStoreSupport != null) {
			cachedBlobStoreSupport.close();
		}
	}

	@Override
	public AsyncOfflineStoreSupport getOfflineStorageSupport() {
		return inner.getOfflineStorageSupport();
	}

	@Override
	public CachedBlobStoreSupport getOnlineStorageSupport() {
		return cachedBlobStoreSupport;
	}

	@Override
	public ScheduledFuture<?> flushCache(StorageQuartzType type) {
		return quartzService.executeOnce(type);
	}

}
