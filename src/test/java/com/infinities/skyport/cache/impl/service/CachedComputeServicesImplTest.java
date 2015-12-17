package com.infinities.skyport.cache.impl.service;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.service.AsyncComputeServices;
import com.infinities.skyport.async.service.compute.AsyncMachineImageSupport;
import com.infinities.skyport.cache.impl.service.compute.CachedMachineImageSupportImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.proxy.MachineImageProxy;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FirstLevelDispatcher;

public class CachedComputeServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private CachedComputeServicesImpl service;
	private ConfigurationHome home;
	private AsyncComputeServices inner;
	private Configuration configuration;
	private ListeningScheduledExecutorService scheduler;
	private ListeningExecutorService worker;
	private FirstLevelDispatcher dispatcher;
	private DistributedObjectFactory objectFactory;
	private DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;

	private AsyncMachineImageSupport support;
	private DistributedCache<String, MachineImageProxy> cache;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		home = context.mock(ConfigurationHome.class);
		inner = context.mock(AsyncComputeServices.class);
		configuration = new Configuration();
		configuration.setId("id");
		configuration.setCloudName("cloudName");
		configuration.setStatus(true);
		scheduler = context.mock(ListeningScheduledExecutorService.class);
		worker = context.mock(ListeningExecutorService.class);
		dispatcher = context.mock(FirstLevelDispatcher.class);
		objectFactory = context.mock(DistributedObjectFactory.class);
		typeMap = context.mock(DistributedMap.class);

		support = context.mock(AsyncMachineImageSupport.class);
		cache = context.mock(DistributedCache.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(inner).hasImageSupport();
				will(returnValue(true));
				exactly(1).of(inner).getImageSupport();
				will(returnValue(support));
				exactly(1).of(home).addLifeCycleListener(with(any(CachedMachineImageSupportImpl.class)));
				exactly(1).of(objectFactory).getCache(with(any(String.class)), with(any(IllegalStateException.class)));
				will(returnValue(cache));
			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(objectFactory).getMap("compute_quartz");
				will(returnValue(typeMap));

				exactly(1).of(typeMap).isEmpty();
				will(returnValue(false));

				exactly(1).of(inner).hasVirtualMachineSupport();
				will(returnValue(false));

				exactly(1).of(inner).hasVolumeSupport();
				will(returnValue(false));

				exactly(1).of(inner).hasSnapshotSupport();
				will(returnValue(false));

				exactly(1).of(inner).initialize();

				exactly(1).of(worker).execute(with(any(Runnable.class)));
			}
		});
		service = new CachedComputeServicesImpl(home, inner, configuration, scheduler, worker, dispatcher, objectFactory);
		service.initialize();
	}

	@After
	public void tearDown() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(inner).close();
			}
		});
		service.close();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFlushCacheWithTypeNotExist() {
		service.flushCache(ComputeQuartzType.VirtualMachine);
	}

}
