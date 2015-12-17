package com.infinities.skyport.cache.impl.service.compute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.service.compute.AsyncSnapshotSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedSnapshotSupport;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.Time;
import com.infinities.skyport.model.compute.SnapshotSupportProxy;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.model.configuration.compute.SnapshotConfiguration;
import com.infinities.skyport.proxy.SnapshotProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzConfiguration.Precondition;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.quartz.callable.SnapshotCallable;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.snapshot.SnapshotFailureEvent;
import com.infinities.skyport.service.event.compute.snapshot.SnapshotRefreshedEvent;

public class CachedSnapshotSupportImpl extends SnapshotSupportProxy implements CachedSnapshotSupport,
		ConfigurationLifeCycleListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(CachedSnapshotSupportImpl.class);
	private final AsyncSnapshotSupport inner;
	private final Configuration configuration;
	private final DistributedObjectFactory objectFactory;
	private final FirstLevelDispatcher dispatcher;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private final DistributedCache<String, SnapshotProxy> snapshotCache;
	private final QuartzServiceImpl<ComputeQuartzType> quartzService;
	private final Map<ComputeQuartzType, QuartzConfiguration<?>> quartzs = Maps.newEnumMap(ComputeQuartzType.class);
	private final List<CachedSnapshotListener> snapshotListeners = new ArrayList<CachedSnapshotListener>();


	public CachedSnapshotSupportImpl(ConfigurationHome home, AsyncSnapshotSupport inner, Configuration configuration,
			QuartzServiceImpl<ComputeQuartzType> quartz, DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap,
			FirstLevelDispatcher dispatcher, DistributedObjectFactory objectFactory) {
		super(inner);
		home.addLifeCycleListener(this);
		this.inner = inner;
		this.configuration = configuration;
		this.quartzService = quartz;
		this.typeMap = typeMap;
		this.dispatcher = dispatcher;
		this.objectFactory = objectFactory;
		snapshotCache =
				objectFactory.getCache("snapshotCache", new IllegalStateException(CachedServiceProviderImpl.COLLECTING));
	}

	public synchronized void initialize() {
		if (isInitialized.compareAndSet(false, true)) {
			// super.initialize();
			setUpSchedule(configuration.getComputeConfiguration().getSnapshotConfiguration());
			logger.info("initialize cache");
		} else {
			throw new IllegalStateException("object has been initialized");
		}
	}

	public synchronized void close() {
		if (isInitialized.compareAndSet(true, false)) {
			try {
				snapshotCache.destroy();
			} catch (Exception e) {
				logger.warn("ignore", e);
			}
			for (Iterator<CachedSnapshotListener> it = snapshotListeners.iterator(); it.hasNext();) {
				CachedSnapshotListener listener = it.next();
				try {
					dispatcher.removeListener(listener);
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
				it.remove();
			}
		}

	}

	private void setUpSchedule(SnapshotConfiguration snapshotConfiguration) {
		if (configuration.getStatus()) {
			Time snapshotTime = snapshotConfiguration.getListSnapshots().getDelay();

			Precondition snapshotCondition = new Precondition() {

				@Override
				public boolean check() {
					return typeMap.containsKey(ComputeQuartzType.Snapshot);
				}

			};
			QuartzConfiguration<Iterable<Snapshot>> snapshotConfig =
					new QuartzConfiguration.Builder<Iterable<Snapshot>>(new SnapshotCallable(inner.getSupport()),
							snapshotCondition).addCallback(getSnapshotCallback()).delay(snapshotTime)
							.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY)
							.name("Snapshot:" + configuration.getCloudName()).build();
			quartzs.put(ComputeQuartzType.Snapshot, snapshotConfig);

			fireQuartz();
		}
	}

	private FutureCallback<Iterable<Snapshot>> getSnapshotCallback() {
		return new FutureCallback<Iterable<Snapshot>>() {

			@Override
			public void onSuccess(Iterable<Snapshot> result) {
				List<Snapshot> snapshotList = Lists.newArrayList(result);
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.debug("list {} snapshot success: {}", new Object[] { name, snapshotList.size() });
				refreshSnapshotCache(result);
				dispatcher.fireRefreshedEvent(new SnapshotRefreshedEvent(snapshotCache.values(), id));
			}

			@Override
			public void onFailure(Throwable t) {
				String name = configuration.getCloudName();
				String id = configuration.getId();
				logger.warn("list {} snapshot failed", name);
				logger.warn("list snapshot failed", t);
				snapshotCache.reload(t);
				dispatcher.fireFaiureEvent(new SnapshotFailureEvent(id, t));
			}

		};
	}

	protected void refreshSnapshotCache(Iterable<Snapshot> result) {
		Map<String, SnapshotProxy> snapshotMap = new HashMap<String, SnapshotProxy>();
		Iterator<Snapshot> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configName = configuration.getCloudName();
			String configId = configuration.getId();
			Snapshot snapshot = iterator.next();
			SnapshotProxy proxy =
					new SnapshotProxy(snapshot, configName, configId, this.getObjectFactory().getAtomicLong(
							"snapshot_" + snapshot.getProviderSnapshotId()));
			snapshotMap.put(snapshot.getProviderSnapshotId(), proxy);
		}
		this.snapshotCache.reload(snapshotMap);
	}

	private void fireQuartz() {
		for (Entry<ComputeQuartzType, QuartzConfiguration<?>> quartz : quartzs.entrySet()) {
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
	public AsyncResult<Iterable<Snapshot>> listSnapshots() throws InternalException, CloudException {
		Iterable<Snapshot> list = new ArrayList<Snapshot>(snapshotCache.values());
		ListenableFuture<Iterable<Snapshot>> future = Futures.immediateFuture(list);
		AsyncResult<Iterable<Snapshot>> ret = new AsyncResult<Iterable<Snapshot>>(future);
		return ret;
	}

	@Override
	public synchronized void addSnapshotListener(CachedSnapshotListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("service provider has not been initialized yet");
		}
		Preconditions.checkNotNull(service);
		if (!snapshotListeners.contains(service)) {
			dispatcher.addListener(service);
			snapshotListeners.add(service);
		}
	}

	@Override
	public synchronized void removeSnapshotListener(CachedSnapshotListener service) {
		if (snapshotListeners.contains(service)) {
			dispatcher.removeListener(service);
			snapshotListeners.remove(service);
		}
	}

	@Override
	public void persist(Configuration configuration) {
		// ignore
	}

	@Override
	public synchronized void lightMerge(Configuration configuration) {
		if (configuration.getId().equals(this.configuration.getId())) {
			// change listSnapshots delay
			Time delay = configuration.getComputeConfiguration().getSnapshotConfiguration().getListSnapshots().getDelay();
			if (quartzs.containsKey(ComputeQuartzType.Snapshot)) {
				Time oldDelay = quartzs.get(ComputeQuartzType.Snapshot).getTime();
				if (!oldDelay.equals(delay)) {
					quartzs.get(ComputeQuartzType.Snapshot).setTime(delay);
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

}
