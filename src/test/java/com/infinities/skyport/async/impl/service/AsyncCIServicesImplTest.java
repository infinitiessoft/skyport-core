/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
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
