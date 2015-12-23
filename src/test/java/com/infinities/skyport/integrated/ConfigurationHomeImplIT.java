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
package com.infinities.skyport.integrated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.dasein.cloud.compute.VMFilterOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.infinities.skyport.async.impl.AsyncServiceProviderImpl;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport.CachedVirtualMachineListener;
import com.infinities.skyport.distributed.impl.local.LocalExecutor;
import com.infinities.skyport.mock.MockServiceProvider;
import com.infinities.skyport.model.PoolSize;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.registrar.ConfigurationHomeImpl;
import com.infinities.skyport.registrar.DriverHomeImpl;
import com.infinities.skyport.registrar.ProfileHomeImpl;
import com.infinities.skyport.service.DriverHome;
import com.infinities.skyport.service.event.compute.virtualmachine.VirtualMachineFailureEvent;
import com.infinities.skyport.service.event.compute.virtualmachine.VirtualMachineRefreshedEvent;
import com.infinities.skyport.timeout.TimedServiceProvider;

public class ConfigurationHomeImplIT {

	private DriverHome driverHome;
	private ConfigurationHomeImpl home;
	private ProfileHomeImpl profileHome;
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	private File tempFile;


	@Before
	public void setUp() throws Exception {
		driverHome = new DriverHomeImpl();
		driverHome.initialize();
		home = new ConfigurationHomeImpl();
		home.setDriverHome(driverHome);

		testFolder.create();
		tempFile = testFolder.newFile("test.xml");
		profileHome = new ProfileHomeImpl();
		profileHome.setFile(tempFile);
		profileHome.setSaveFile(tempFile);
		home.setProfileHome(profileHome);
		home.initialize();
	}

	@After
	public void tearDown() throws Exception {
		home.close();
	}

	// check if objectfactory have been close in DelegateServiceProvider
	@Test
	public void testNoCacheableToCacheable() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setCloudName("MyCloud");
		configuration.setAccount("Account");
		configuration.setEndpoint("Endpoint");
		configuration.setProviderName("Demo");
		configuration.setProviderClass(MockServiceProvider.class.getName());
		configuration.setRegionId("Taiwan");
		configuration.getProperties().setProperty("apiKeys_SHARED", "apiKeys_SHARED");
		configuration.getProperties().setProperty("apiKeys_SECRET", "apiKeys_SECRET");
		configuration.getProperties().setProperty("x509_SHARED", "x509_SHARED");
		configuration.getProperties().setProperty("x509_SECRET", "x509_SECRET");
		home.persist(configuration);

		Thread.sleep(1000L);
		configuration.setCacheable(true);
		home.merge(configuration.getId(), configuration);
		Thread.sleep(5000L);
		VMFilterOptions options = VMFilterOptions.getInstance("test");
		home.findById(configuration.getId()).getComputeServices().getVirtualMachineSupport().listVirtualMachines(options);
	}

	// check if objectfactory have been close in DelegateServiceProvider
	@Test
	public void testChangeDelay() throws Exception {
		final AtomicInteger counter = new AtomicInteger(0);
		Configuration configuration = new Configuration();
		configuration.setCloudName("MyCloud");
		configuration.setAccount("Account");
		configuration.setEndpoint("Endpoint");
		configuration.setProviderName("Demo");
		configuration.setProviderClass(MockServiceProvider.class.getName());
		configuration.setRegionId("Taiwan");
		configuration.getProperties().setProperty("apiKeys_SHARED", "apiKeys_SHARED");
		configuration.getProperties().setProperty("apiKeys_SECRET", "apiKeys_SECRET");
		configuration.getProperties().setProperty("x509_SHARED", "x509_SHARED");
		configuration.getProperties().setProperty("x509_SECRET", "x509_SECRET");
		configuration.setCacheable(true);
		configuration.getComputeConfiguration().getVirtualMachineConfiguration().getListVirtualMachines().getDelay()
				.setNumber(5L);
		home.persist(configuration);
		home.findById(configuration.getId()).getComputeServices().getVirtualMachineSupport()
				.addVirtualMachineListener(new CachedVirtualMachineListener() {

					@Override
					public void onEntitiesRefreshed(VirtualMachineRefreshedEvent e) throws Exception {
						counter.incrementAndGet();
					}

					@Override
					public void onFailure(VirtualMachineFailureEvent e) {

					}

				});

		Thread.sleep(7000L);
		configuration.getComputeConfiguration().getVirtualMachineConfiguration().getListVirtualMachines().getDelay()
				.setNumber(1L);
		home.merge(configuration.getId(), configuration);
		Thread.sleep(10000L);
		assertTrue(counter.get() > 3);
		assertEquals(10, counter.get());

	}

	@Test
	public void testChangeThreadPoolCore() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setCloudName("MyCloud");
		configuration.setAccount("Account");
		configuration.setEndpoint("Endpoint");
		configuration.setProviderName("Demo");
		configuration.setProviderClass(MockServiceProvider.class.getName());
		configuration.setRegionId("Taiwan");
		configuration.getProperties().setProperty("apiKeys_SHARED", "apiKeys_SHARED");
		configuration.getProperties().setProperty("apiKeys_SECRET", "apiKeys_SECRET");
		configuration.getProperties().setProperty("x509_SHARED", "x509_SHARED");
		configuration.getProperties().setProperty("x509_SECRET", "x509_SECRET");
		configuration.setCacheable(true);
		home.persist(configuration);
		configuration.getLongPoolConfig().setCoreSize(9);
		home.merge(configuration.getId(), configuration);
		CachedServiceProviderImpl provider = (CachedServiceProviderImpl) home.merge(configuration.getId(), configuration);
		AsyncServiceProviderImpl asyncProvider = (AsyncServiceProviderImpl) provider.getInner();
		LocalExecutor executor = (LocalExecutor) asyncProvider.getThreadPools().getThreadPool(PoolSize.LONG);
		assertEquals(9, executor.getThreadPool().getCorePoolSize());
	}

	@Test
	public void testChangeTimeout() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setCloudName("MyCloud");
		configuration.setAccount("Account");
		configuration.setEndpoint("Endpoint");
		configuration.setProviderName("Demo");
		configuration.setProviderClass(MockServiceProvider.class.getName());
		configuration.setRegionId("Taiwan");
		configuration.getProperties().setProperty("apiKeys_SHARED", "apiKeys_SHARED");
		configuration.getProperties().setProperty("apiKeys_SECRET", "apiKeys_SECRET");
		configuration.getProperties().setProperty("x509_SHARED", "x509_SHARED");
		configuration.getProperties().setProperty("x509_SECRET", "x509_SECRET");
		configuration.setCacheable(true);
		home.persist(configuration);
		configuration.getComputeConfiguration().getVirtualMachineConfiguration().getListVirtualMachines().getTimeout()
				.setNumber(5);
		home.merge(configuration.getId(), configuration);
		CachedServiceProviderImpl provider = (CachedServiceProviderImpl) home.merge(configuration.getId(), configuration);
		AsyncServiceProviderImpl asyncProvider = (AsyncServiceProviderImpl) provider.getInner();
		TimedServiceProvider timedServiceProvider = (TimedServiceProvider) asyncProvider.getInner();
		long timeout =
				timedServiceProvider.getConfiguration().getComputeConfiguration().getVirtualMachineConfiguration()
						.getListVirtualMachines().getTimeout().getNumber();
		assertEquals(5, timeout);

	}
}
