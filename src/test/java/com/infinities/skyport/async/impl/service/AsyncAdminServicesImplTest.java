package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.admin.AdminServices;
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
import com.infinities.skyport.model.configuration.service.AdminConfiguration;

public class AsyncAdminServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DistributedThreadPool pools;
	private AdminConfiguration configuration;
	private AdminServices services;
	private String configurationId = "configurationId";
	private ServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		pools = context.mock(DistributedThreadPool.class);
		provider = context.mock(ServiceProvider.class);
		services = context.mock(AdminServices.class);
		configuration = new AdminConfiguration();

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAsyncAdminServicesImpl() throws ConcurrentException {
		context.checking(new Expectations() {

			{
				exactly(1).of(provider).getAdminServices();
				will(returnValue(services));

				exactly(1).of(services).hasPrepaymentSupport();
				will(returnValue(true));
				exactly(1).of(services).hasAccountSupport();
				will(returnValue(true));
				exactly(1).of(services).hasBillingSupport();
				will(returnValue(true));
			}
		});
		new AsyncAdminServicesImpl(configurationId, provider, configuration, pools);
	}

}
