package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.identity.IdentityServices;
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
import com.infinities.skyport.model.configuration.service.IdentityConfiguration;

public class AsyncIdentityServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DistributedThreadPool pools;
	private IdentityConfiguration configuration;
	private IdentityServices services;
	private String configurationId = "configurationId";
	private ServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		pools = context.mock(DistributedThreadPool.class);
		provider = context.mock(ServiceProvider.class);
		services = context.mock(IdentityServices.class);

		configuration = new IdentityConfiguration();
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testAsyncIdentityServicesImpl() throws ConcurrentException {
		context.checking(new Expectations() {

			{
				exactly(1).of(provider).getIdentityServices();
				will(returnValue(services));

				exactly(1).of(services).hasIdentityAndAccessSupport();
				will(returnValue(true));
				exactly(1).of(services).hasShellKeySupport();
				will(returnValue(true));
			}
		});
		new AsyncIdentityServicesImpl(configurationId, provider, configuration, pools);
	}
}
