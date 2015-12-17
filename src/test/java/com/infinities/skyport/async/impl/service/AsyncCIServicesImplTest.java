package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.ci.CIServices;
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
import com.infinities.skyport.model.configuration.service.CIConfiguration;

public class AsyncCIServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};

	private DistributedThreadPool pools;
	private CIConfiguration configuration;
	private CIServices services;
	private String configurationId = "configurationId";
	private ServiceProvider provider;


	@Before
	public void setUp() {
		pools = context.mock(DistributedThreadPool.class);
		provider = context.mock(ServiceProvider.class);
		services = context.mock(CIServices.class);
		configuration = new CIConfiguration();
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testAsyncCIServicesImpl() throws ConcurrentException {
		context.checking(new Expectations() {

			{
				exactly(1).of(provider).getCIServices();
				will(returnValue(services));

				exactly(1).of(services).hasConvergedHttpLoadBalancerSupport();
				will(returnValue(true));
				exactly(1).of(services).hasConvergedInfrastructureSupport();
				will(returnValue(true));
				exactly(1).of(services).hasTopologySupport();
				will(returnValue(true));
			}
		});
		new AsyncCIServicesImpl(configurationId, provider, configuration, pools);
	}
}
