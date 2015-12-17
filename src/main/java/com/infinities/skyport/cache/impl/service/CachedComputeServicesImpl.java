package com.infinities.skyport.cache.impl.service;

import java.util.concurrent.ScheduledFuture;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.service.AsyncComputeServices;
import com.infinities.skyport.async.service.compute.AsyncAffinityGroupSupport;
import com.infinities.skyport.async.service.compute.AsyncAutoScalingSupport;
import com.infinities.skyport.cache.impl.service.compute.CachedMachineImageSupportImpl;
import com.infinities.skyport.cache.impl.service.compute.CachedSnapshotSupportImpl;
import com.infinities.skyport.cache.impl.service.compute.CachedVirtualMachineSupportImpl;
import com.infinities.skyport.cache.impl.service.compute.CachedVolumeSupportImpl;
import com.infinities.skyport.cache.service.CachedComputeServices;
import com.infinities.skyport.cache.service.compute.CachedMachineImageSupport;
import com.infinities.skyport.cache.service.compute.CachedSnapshotSupport;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FirstLevelDispatcher;

public class CachedComputeServicesImpl implements CachedComputeServices {

	private final AsyncComputeServices inner;
	private final ListeningExecutorService worker;
	private final DistributedObjectFactory objectFactory;
	private CachedVirtualMachineSupportImpl cachedVirtualMachineSupport;
	private CachedMachineImageSupportImpl cachedMachineImageSupport;
	private CachedSnapshotSupportImpl cachedSnapshotSupport;
	private CachedVolumeSupportImpl cachedVolumeSupport;
	private final QuartzServiceImpl<ComputeQuartzType> quartzService;


	public CachedComputeServicesImpl(ConfigurationHome home, AsyncComputeServices inner, Configuration configuration,
			ListeningScheduledExecutorService scheduler, ListeningExecutorService worker, FirstLevelDispatcher dispatcher,
			DistributedObjectFactory objectFactory) {
		this.inner = inner;
		this.worker = worker;
		this.objectFactory = objectFactory;
		DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap = getObjectFactory().getMap("compute_quartz");
		if (typeMap.isEmpty()) {
			for (ComputeQuartzType type : ComputeQuartzType.values()) {
				typeMap.put(type, type);
			}
		}
		this.quartzService = new QuartzServiceImpl<ComputeQuartzType>(ComputeQuartzType.class, scheduler, worker);
		if (inner.hasVirtualMachineSupport()) {
			this.cachedVirtualMachineSupport =
					new CachedVirtualMachineSupportImpl(home, inner.getVirtualMachineSupport(), configuration,
							quartzService, typeMap, dispatcher, objectFactory);
		}
		if (inner.hasImageSupport()) {
			this.cachedMachineImageSupport =
					new CachedMachineImageSupportImpl(home, inner.getImageSupport(), configuration, quartzService, typeMap,
							dispatcher, objectFactory);
		}
		if (inner.hasSnapshotSupport()) {
			this.cachedSnapshotSupport =
					new CachedSnapshotSupportImpl(home, inner.getSnapshotSupport(), configuration, quartzService, typeMap,
							dispatcher, objectFactory);
		}
		if (inner.hasVolumeSupport()) {
			this.cachedVolumeSupport =
					new CachedVolumeSupportImpl(home, inner.getVolumeSupport(), configuration, quartzService, typeMap,
							dispatcher, objectFactory);
		}

	}

	@Override
	public AsyncAffinityGroupSupport getAffinityGroupSupport() {
		return inner.getAffinityGroupSupport();
	}

	@Override
	public AsyncAutoScalingSupport getAutoScalingSupport() {
		return inner.getAutoScalingSupport();
	}

	@Override
	public CachedMachineImageSupport getImageSupport() {
		return cachedMachineImageSupport;
	}

	@Override
	public CachedSnapshotSupport getSnapshotSupport() {
		return cachedSnapshotSupport;
	}

	@Override
	public CachedVirtualMachineSupport getVirtualMachineSupport() {
		return cachedVirtualMachineSupport;
	}

	@Override
	public CachedVolumeSupport getVolumeSupport() {
		return cachedVolumeSupport;
	}

	@Override
	public boolean hasAffinityGroupSupport() {
		return inner.hasAffinityGroupSupport();
	}

	@Override
	public boolean hasAutoScalingSupport() {
		return inner.hasAutoScalingSupport();
	}

	@Override
	public boolean hasImageSupport() {
		return inner.hasImageSupport();
	}

	@Override
	public boolean hasSnapshotSupport() {
		return inner.hasSnapshotSupport();
	}

	@Override
	public boolean hasVirtualMachineSupport() {
		return inner.hasVirtualMachineSupport();
	}

	@Override
	public boolean hasVolumeSupport() {
		return inner.hasVolumeSupport();
	}

	public DistributedObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public ListeningExecutorService getWorker() {
		return worker;
	}

	@Override
	public void initialize() throws Exception {
		inner.initialize();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				if (cachedVirtualMachineSupport != null) {
					cachedVirtualMachineSupport.initialize();
				}
				if (cachedMachineImageSupport != null) {
					cachedMachineImageSupport.initialize();
				}
				if (cachedSnapshotSupport != null) {
					cachedSnapshotSupport.initialize();
				}
				if (cachedVolumeSupport != null) {
					cachedVolumeSupport.initialize();
				}
			}

		};
		getWorker().execute(r);

	}

	@Override
	public void close() {
		if (cachedVirtualMachineSupport != null) {
			cachedVirtualMachineSupport.close();
		}
		if (cachedMachineImageSupport != null) {
			cachedMachineImageSupport.close();
		}
		if (cachedSnapshotSupport != null) {
			cachedSnapshotSupport.close();
		}
		if (cachedVolumeSupport != null) {
			cachedVolumeSupport.close();
		}
		quartzService.close();
		inner.close();
	}

	@Override
	public ScheduledFuture<?> flushCache(ComputeQuartzType type) {
		return quartzService.executeOnce(type);
	}
}
