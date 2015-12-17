package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.storage.StorageServices;
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
import com.infinities.skyport.model.configuration.service.StorageConfiguration;

public class AsyncStorageServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DistributedThreadPool pools;
	private StorageConfiguration configuration;
	private StorageServices services;
	private String configurationId = "configurationId";
	private ServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		pools = context.mock(DistributedThreadPool.class);
		provider = context.mock(ServiceProvider.class);
		services = context.mock(StorageServices.class);
		configuration = new StorageConfiguration();

	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testAsyncStorageServicesImpl() throws ConcurrentException {
		context.checking(new Expectations() {

			{

				exactly(1).of(provider).getStorageServices();
				will(returnValue(services));

				exactly(1).of(services).hasOfflineStorageSupport();
				will(returnValue(true));

				exactly(1).of(services).hasOnlineStorageSupport();
				will(returnValue(true));
			}
		});
		new AsyncStorageServicesImpl(configurationId, provider, configuration, pools);
	}
}
