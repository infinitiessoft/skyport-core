package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.compute.ComputeServices;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.ComputeConfiguration;

public class AsyncComputeServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DistributedThreadPool pools;
	private ComputeConfiguration configuration;
	private ComputeServices services;
	private String configurationId = "configurationId";
	private ServiceProvider provider;


	@Before
	public void setUp() {
		pools = context.mock(DistributedThreadPool.class);
		provider = context.mock(ServiceProvider.class);
		services = context.mock(ComputeServices.class);
		configuration = new ComputeConfiguration();
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testAsyncComputeServicesImpl() throws ConcurrentException {
		context.checking(new Expectations() {

			{
				exactly(1).of(provider).getComputeServices();
				will(returnValue(services));

				exactly(1).of(services).hasAffinityGroupSupport();
				will(returnValue(true));
				exactly(1).of(services).hasAutoScalingSupport();
				will(returnValue(true));
				exactly(1).of(services).hasImageSupport();
				will(returnValue(true));
				exactly(1).of(services).hasSnapshotSupport();
				will(returnValue(true));
				exactly(1).of(services).hasVirtualMachineSupport();
				will(returnValue(true));
				exactly(1).of(services).hasVolumeSupport();
				will(returnValue(true));
			}
		});
		new AsyncComputeServicesImpl(configurationId, provider, configuration, pools);
	}
}
