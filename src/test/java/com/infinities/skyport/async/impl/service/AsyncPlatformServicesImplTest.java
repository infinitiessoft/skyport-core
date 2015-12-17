package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.platform.PlatformServices;
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
import com.infinities.skyport.model.configuration.service.PlatformConfiguration;

public class AsyncPlatformServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DistributedThreadPool pools;
	private PlatformConfiguration configuration;
	private PlatformServices services;
	private String configurationId = "configurationId";
	private ServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		pools = context.mock(DistributedThreadPool.class);
		provider = context.mock(ServiceProvider.class);
		services = context.mock(PlatformServices.class);

		configuration = new PlatformConfiguration();
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testAsyncPlatformServicesImpl() throws ConcurrentException {
		context.checking(new Expectations() {

			{
				exactly(1).of(provider).getPlatformServices();
				will(returnValue(services));

				exactly(1).of(services).hasCDNSupport();
				will(returnValue(true));
				exactly(1).of(services).hasDataWarehouseSupport();
				will(returnValue(true));
				exactly(1).of(services).hasKeyValueDatabaseSupport();
				will(returnValue(true));
				exactly(1).of(services).hasMessageQueueSupport();
				will(returnValue(true));
				exactly(1).of(services).hasPushNotificationSupport();
				will(returnValue(true));
				exactly(1).of(services).hasRelationalDatabaseSupport();
				will(returnValue(true));
				exactly(1).of(services).hasMonitoringSupport();
				will(returnValue(true));
			}
		});

		new AsyncPlatformServicesImpl(configurationId, provider, configuration, pools);
	}
}
