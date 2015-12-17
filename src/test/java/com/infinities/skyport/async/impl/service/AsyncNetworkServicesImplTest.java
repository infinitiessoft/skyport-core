package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.network.NetworkServices;
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
import com.infinities.skyport.model.configuration.service.NetworkConfiguration;

public class AsyncNetworkServicesImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DistributedThreadPool pools;
	private NetworkConfiguration configuration;
	private NetworkServices services;
	private String configurationId = "configurationId";
	private ServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		pools = context.mock(DistributedThreadPool.class);
		provider = context.mock(ServiceProvider.class);
		services = context.mock(NetworkServices.class);

		configuration = new NetworkConfiguration();

	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testAsyncNetworkServicesImpl() throws ConcurrentException {
		context.checking(new Expectations() {

			{
				exactly(1).of(provider).getNetworkServices();
				will(returnValue(services));

				exactly(1).of(services).hasDnsSupport();
				will(returnValue(true));
				exactly(1).of(services).hasFirewallSupport();
				will(returnValue(true));
				exactly(1).of(services).hasIpAddressSupport();
				will(returnValue(true));
				exactly(1).of(services).hasLoadBalancerSupport();
				will(returnValue(true));
				exactly(1).of(services).hasNetworkFirewallSupport();
				will(returnValue(true));
				exactly(1).of(services).hasVlanSupport();
				will(returnValue(true));
				exactly(1).of(services).hasVpnSupport();
				will(returnValue(true));
			}
		});
		new AsyncNetworkServicesImpl(configurationId, provider, configuration, pools);
	}
}
