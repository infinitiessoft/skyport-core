package com.infinities.skyport.cache.impl.service.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dasein.cloud.storage.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.infinities.skyport.async.service.storage.AsyncBlobStoreSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedStorageServices.StorageQuartzType;
import com.infinities.skyport.cache.service.storage.CachedBlobStoreSupport;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.Time;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.model.configuration.storage.OnlineStorageConfiguration;
import com.infinities.skyport.model.storage.BlobStoreSupportProxy;
import com.infinities.skyport.proxy.storage.BlobProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.quartz.QuartzConfiguration.Precondition;
import com.infinities.skyport.quartz.callable.BlobCallable;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.storage.BlobFailureEvent;
import com.infinities.skyport.service.event.storage.BlobRefreshedEvent;

public class CachedBlobStoreSupportImpl extends BlobStoreSupportProxy
		implements CachedBlobStoreSupport, ConfigurationLifeCycleListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(CachedBlobStoreSupportImpl.class);
	private AsyncBlobStoreSupport inner;
	private Configuration configuration;
	private final DistributedObjectFactory objectFactory;
	private final FirstLevelDispatcher dispatcher;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final DistributedMap<StorageQuartzType, StorageQuartzType> typeMap;
	private final DistributedCache<String, BlobProxy> blobCache;
	private final QuartzServiceImpl<StorageQuartzType> quartzService;
	private final Map<StorageQuartzType, QuartzConfiguration<?>> quartzs = Maps.newEnumMap(StorageQuartzType.class);
	private final List<CachedBlobListener> blobListeners = new ArrayList<CachedBlobListener>();

	public CachedBlobStoreSupportImpl(ConfigurationHome home, AsyncBlobStoreSupport inner, Configuration configuration,
			QuartzServiceImpl<StorageQuartzType> quartz, DistributedMap<StorageQuartzType, StorageQuartzType> typeMap,
			FirstLevelDispatcher dispatcher, DistributedObjectFactory objectFactory) {
		super(inner);
		home.addLifeCycleListener(this);
		this.inner = inner;
		this.configuration = configuration;
		this.dispatcher = dispatcher;
		this.objectFactory = objectFactory;
		this.typeMap = typeMap;
		this.quartzService = quartz;
		this.blobCache = objectFactory.getCache("blobCache",
				new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
	}

	public synchronized void initialize() {
		if (isInitialized.compareAndSet(false, true)) {
			// super.initialize();
			setUpSchedule(configuration.getStorageConfiguration().getOnlineStorageConfiguration());
			logger.info("initialize cache");
		} else {
			throw new IllegalStateException("object has been initialized");
		}
	}

	public synchronized void close() {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				blobCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			for (Iterator<CachedBlobListener> it = blobListeners.iterator(); it.hasNext();) {
				CachedBlobListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
		}
	}

	private void setUpSchedule(OnlineStorageConfiguration blobStoreConfiguration) {
		if (configuration.getStatus()) {
			Time blobTime = blobStoreConfiguration.getList().getDelay();

			Precondition blobCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(StorageQuartzType.BLOB);
				}

			};
			QuartzConfiguration<Iterable<Blob>> blobConfig = new QuartzConfiguration.Builder<Iterable<Blob>>(
					new BlobCallable(inner.getSupport()), blobCondition).addCallback(getBlobCallback()).delay(blobTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("Blob:" + configuration.getCloudName()).build();
			quartzs.put(StorageQuartzType.BLOB, blobConfig);

			fireQuartz();
		}
	}

	private FutureCallback<Iterable<Blob>> getBlobCallback() {
		return new FutureCallback<Iterable<Blob>>() {

			@Override
			public void onSuccess(Iterable<Blob> result) {
				List<Blob> blobList = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} blob success: {}", new Object[] { name, blobList.size() });
				refreshBlobCache(result);

				List<Blob> values = Lists.newArrayList();
				for (BlobProxy proxy : blobCache.values()) {
					values.add(proxy.getBlob());
				}

				dispatcher.fireRefreshedEvent(new BlobRefreshedEvent(values, id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} blob failed", name);
				logger.warn("list blob failed", t);
				blobCache.reload(t);
				dispatcher.fireFaiureEvent(new BlobFailureEvent(id, t));
			}

		};
	}

	protected void refreshBlobCache(Iterable<Blob> result) {
		Map<String, BlobProxy> blobMap = new HashMap<String, BlobProxy>();
		Iterator<Blob> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			Blob blob = iterator.next();
			BlobProxy proxy = new BlobProxy(blob, configName, configId);
			blobMap.put(blob.getObjectName(), proxy);
		}
		this.blobCache.reload(blobMap);
	}

	private void fireQuartz() {
		for (Entry<StorageQuartzType, QuartzConfiguration<?>> quartz : quartzs.entrySet()) {
			try {
				quartzService.schedule(quartz.getKey(), quartz.getValue()).get();
			} catch (Exception e) {
				logger.warn("data collecting failed", e);
			}
		}
	}

	protected DistributedObjectFactory getObjectFactory() {
		return objectFactory;
	}

	@Override
	public void persist(Configuration configuration) {
		// ignore
	}

	@Override
	public void lightMerge(Configuration configuration) {
		if (configuration.getId().equals(this.configuration.getId())) {
			// change listVirtualMachines delay
			Time blobDelay = configuration.getStorageConfiguration().getOnlineStorageConfiguration().getList()
					.getDelay();
			if (quartzs.containsKey(StorageQuartzType.BLOB)) {
				Time oldDelay = quartzs.get(StorageQuartzType.BLOB).getTime();
				if (!oldDelay.equals(blobDelay)) {
					quartzs.get(StorageQuartzType.BLOB).setTime(blobDelay);
				}
			}
		}
	}

	@Override
	public void heavyMerge(Configuration configuration) {
		// ignore
	}

	@Override
	public void remove(Configuration configuration) {
		// ignore
	}

	@Override
	public void clear() {
		// ignore
	}

	@Override
	public void addBlobListener(CachedBlobListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!blobListeners.contains(service)) {
			dispatcher.addListener(service);
			blobListeners.add(service);
		}
	}

	@Override
	public void removeBlobListener(CachedBlobListener service) {
		if (blobListeners.contains(service)) {
			dispatcher.removeListener(service);
			blobListeners.remove(service);
		}
	}

}
