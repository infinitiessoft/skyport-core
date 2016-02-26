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
package com.infinities.skyport.registrar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.ContextRequirements.Field;
import org.dasein.cloud.ContextRequirements.FieldType;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.admin.AdminServices;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.platform.PlatformServices;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.async.impl.IAsyncServiceProviderFactory;
import com.infinities.skyport.cache.CachedServiceProvider;
import com.infinities.skyport.cache.impl.ICachedServiceProviderFactory;
import com.infinities.skyport.compute.SkyportComputeServices;
import com.infinities.skyport.dc.SkyportDataCenterServices;
import com.infinities.skyport.model.Profile;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.network.SkyportNetworkServices;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.DriverHome;
import com.infinities.skyport.service.ProfileHome;

public class ConfigurationHomeImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DriverHome driverHome;
	private ProfileHome profileHome;
	private ConfigurationHomeImpl home;
	private Profile profile;

	private Map<String, CachedServiceProvider> map;
	private CachedServiceProvider serviceProvider;
	private AsyncServiceProvider asyncServiceProvider;
	private Configuration configuration = new Configuration();

	private IAsyncServiceProviderFactory asyncFactory;
	private ICachedServiceProviderFactory factory;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		configuration.setCloudName("MyCloud");
		configuration.setAccount("Account");
		configuration.setEndpoint("Endpoint");
		configuration.setProviderName("Demo");
		configuration.setProviderClass("Mock");
		configuration.setRegionId("Taiwan");
		configuration.getProperties().setProperty("token", "MyCloudToken");

		profile = new Profile();

		profileHome = context.mock(ProfileHome.class);
		driverHome = context.mock(DriverHome.class);
		map = context.mock(Map.class);
		serviceProvider = context.mock(CachedServiceProvider.class);
		factory = context.mock(ICachedServiceProviderFactory.class);
		asyncFactory = context.mock(IAsyncServiceProviderFactory.class);
		asyncServiceProvider = context.mock(AsyncServiceProvider.class);
		home = new ConfigurationHomeImpl();
		home.setDriverHome(driverHome);
		home.setProfileHome(profileHome);
		home.registeredServiceProviders = map;
		home.cachedServiceProviderFactory = factory;
		home.delegatedServiceProviderFactory = factory;
		home.asyncServiceProviderFactory = asyncFactory;
		context.checking(new Expectations() {

			{
				exactly(1).of(profileHome).get();
				will(returnValue(profile));

			}
		});
		home.initialize();
	}

	@After
	public void tearDown() throws Exception {
		final List<CachedServiceProvider> providers = new ArrayList<CachedServiceProvider>();
		providers.add(serviceProvider);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).values();
				will(returnValue(providers));
				exactly(1).of(serviceProvider).close();
				exactly(1).of(map).clear();
				exactly(1).of(profileHome).save();

			}
		});
		home.close();
		context.assertIsSatisfied();
	}

	@Test
	public void testPersist() throws Exception {
		final Set<String> keys = new HashSet<String>();
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		final List<CachedServiceProvider> values = new ArrayList<CachedServiceProvider>();

		context.checking(new Expectations() {

			{
				exactly(2).of(driverHome).findByName(configuration.getProviderClass());
				will(returnValue(MockServiceProvider.class));
				exactly(1).of(asyncFactory).getInstance(with(any(ConfigurationHome.class)),
						with(any(ServiceProvider.class)), with(any(Configuration.class)),
						with(any(ListeningScheduledExecutorService.class)));
				will(returnValue(asyncServiceProvider));
				exactly(1).of(factory).getInstance(with(any(ConfigurationHome.class)),
						with(any(AsyncServiceProvider.class)), with(any(ListeningScheduledExecutorService.class)),
						with(any(ListeningExecutorService.class)));
				will(returnValue(serviceProvider));
				exactly(1).of(serviceProvider).initialize();
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(map).keySet();
				will(returnValue(keys));
				exactly(1).of(map).values();
				will(returnValue(values));
				exactly(1).of(profileHome).get();
				will(returnValue(profile));
				exactly(1).of(profileHome).save();
				exactly(1).of(map).put("id", serviceProvider);
			}
		});
		configuration.setId("id");
		home.persist(configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPersistWithDuplicateName() throws Exception {
		// final String providerClass = "Mock";
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		context.checking(new Expectations() {

			{
				exactly(1).of(map).entrySet();
				will(returnValue(set));

				exactly(1).of(provider).getConfiguration();
				will(returnValue(configuration));
			}
		});
		home.persist(configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPersistWithDuplicateId() throws Exception {
		final Configuration newConfig = configuration.clone();
		configuration.setId("id");
		newConfig.setCloudName("new");
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final Set<String> keys = new HashSet<String>();
		keys.add("id");

		context.checking(new Expectations() {

			{
				exactly(1).of(map).entrySet();
				will(returnValue(set));

				exactly(1).of(map).keySet();
				will(returnValue(keys));

			}
		});
		home.persist(configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemoveWithConfigNotExist() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(map).remove("id");
				will(returnValue(null));

				exactly(1).of(profileHome).get();
				will(returnValue(profile));
			}
		});
		home.remove("id");
	}

	@Test
	public void testRemove() throws Exception {
		final Configuration configuration = new Configuration();
		context.checking(new Expectations() {

			{
				exactly(1).of(map).remove("id");
				will(returnValue(serviceProvider));

				exactly(1).of(serviceProvider).getConfiguration();
				will(returnValue(configuration));

				exactly(1).of(serviceProvider).close();

				exactly(1).of(profileHome).get();
				will(returnValue(profile));

				exactly(1).of(profileHome).save();
			}
		});
		home.registeredServiceProviders = map;
		home.remove("id");
	}

	@Test
	public void testHeavyMerge() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final List<CachedServiceProvider> values = new ArrayList<CachedServiceProvider>();
		values.add(provider);
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final Profile profile = new Profile();
		final Configuration oldConfiguration = new Configuration();
		oldConfiguration.setId("id");
		oldConfiguration.setCloudName("OldMyCloud");
		oldConfiguration.setAccount("Account");
		oldConfiguration.setEndpoint("Endpoint");
		oldConfiguration.setProviderName("Demo");
		oldConfiguration.setProviderClass("Mock");
		oldConfiguration.setRegionId("Taiwan");
		oldConfiguration.getProperties().setProperty("token", "MyCloudToken");
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).values();
				will(returnValue(values));
				exactly(2).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
				exactly(1).of(driverHome).findByName("Mock");
				will(returnValue(MockServiceProvider.class));
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(map).put(with(any(String.class)), with(any(CachedServiceProvider.class)));
				will(returnValue(null));
				exactly(1).of(provider).close();
				exactly(1).of(profileHome).get();
				will(returnValue(profile));
				exactly(1).of(profileHome).save();
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(asyncFactory).getInstance(with(any(ConfigurationHome.class)),
						with(any(ServiceProvider.class)), with(any(Configuration.class)),
						with(any(ListeningScheduledExecutorService.class)));
				will(returnValue(asyncServiceProvider));
				exactly(1).of(factory).getInstance(with(any(ConfigurationHome.class)),
						with(any(AsyncServiceProvider.class)), with(any(ListeningScheduledExecutorService.class)),
						with(any(ListeningExecutorService.class)));
				will(returnValue(serviceProvider));
				exactly(1).of(serviceProvider).getConfiguration();
				will(returnValue(configuration));
			}
		});
		home.merge("id", configuration);
	}

	@Test
	public void testLightMerge() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final Profile profile = new Profile();
		final Configuration oldConfiguration = new Configuration();
		oldConfiguration.setId("id");
		oldConfiguration.setCloudName("MyCloud");
		oldConfiguration.setAccount("Account");
		oldConfiguration.setEndpoint("Endpoint");
		oldConfiguration.setProviderName("Demo");
		oldConfiguration.setProviderClass("Mock");
		oldConfiguration.setRegionId("Taiwan");
		oldConfiguration.getProperties().setProperty("token", "MyCloudToken");
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(provider));

				exactly(1).of(map).entrySet();
				will(returnValue(set));

				exactly(1).of(map).put(with(any(String.class)), with(any(CachedServiceProvider.class)));
				will(returnValue(null));

				exactly(1).of(profileHome).get();
				will(returnValue(profile));

				exactly(1).of(profileHome).save();

				exactly(1).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
			}
		});
		home.merge("id", configuration);
	}

	@Test
	public void testActiveConfiguration() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final Profile profile = new Profile();
		configuration.setId("id");
		final Configuration oldConfiguration = configuration.clone();
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(1).of(provider).initialize();
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(map).put(with(any(String.class)), with(any(CachedServiceProvider.class)));
				will(returnValue(null));
				exactly(1).of(profileHome).get();
				will(returnValue(profile));
				exactly(1).of(profileHome).save();
				exactly(1).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
			}
		});
		configuration.setStatus(true);
		home.merge("id", configuration);
	}

	@Test
	public void testInactiveConfiguration() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final List<CachedServiceProvider> list = new ArrayList<CachedServiceProvider>();
		list.add(provider);
		final Profile profile = new Profile();
		configuration.setId("id");
		final Configuration oldConfiguration = configuration.clone();
		profile.getConfigurations().add(oldConfiguration);
		oldConfiguration.setStatus(true);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(map).put(with(any(String.class)), with(any(CachedServiceProvider.class)));
				will(returnValue(null));
				exactly(1).of(profileHome).get();
				will(returnValue(profile));
				exactly(1).of(profileHome).save();
				exactly(1).of(provider).close();
				exactly(1).of(driverHome).findByName(configuration.getProviderClass());
				will(returnValue(MockServiceProvider.class));
				exactly(1).of(map).values();
				will(returnValue(list));
				exactly(2).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(asyncFactory).getInstance(with(any(ConfigurationHome.class)),
						with(any(ServiceProvider.class)), with(any(Configuration.class)),
						with(any(ListeningScheduledExecutorService.class)));
				will(returnValue(asyncServiceProvider));
				exactly(1).of(factory).getInstance(with(any(ConfigurationHome.class)),
						with(any(AsyncServiceProvider.class)), with(any(ListeningScheduledExecutorService.class)),
						with(any(ListeningExecutorService.class)));
				will(returnValue(serviceProvider));
				exactly(1).of(serviceProvider).getConfiguration();
				will(returnValue(configuration));
			}
		});
		configuration.setStatus(false);
		home.merge("id", configuration);
	}

	@Test
	public void testActiveAndHeavyMergeConfiguration() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final List<CachedServiceProvider> list = new ArrayList<CachedServiceProvider>();
		list.add(provider);
		final Profile profile = new Profile();
		configuration.setId("id");
		final Configuration oldConfiguration = configuration.clone();
		oldConfiguration.setId("id");
		oldConfiguration.setCloudName("MyCloud2");
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(driverHome).findByName(configuration.getProviderClass());
				will(returnValue(MockServiceProvider.class));
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(2).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
				exactly(1).of(provider).close();
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(map).values();
				will(returnValue(list));
				exactly(1).of(map).put(with(any(String.class)), with(any(CachedServiceProvider.class)));
				will(returnValue(null));
				exactly(1).of(profileHome).get();
				will(returnValue(profile));
				exactly(1).of(profileHome).save();
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(asyncFactory).getInstance(with(any(ConfigurationHome.class)),
						with(any(ServiceProvider.class)), with(any(Configuration.class)),
						with(any(ListeningScheduledExecutorService.class)));
				will(returnValue(asyncServiceProvider));
				exactly(1).of(factory).getInstance(with(any(ConfigurationHome.class)),
						with(any(AsyncServiceProvider.class)), with(any(ListeningScheduledExecutorService.class)),
						with(any(ListeningExecutorService.class)));
				will(returnValue(serviceProvider));
				exactly(1).of(serviceProvider).getConfiguration();
				will(returnValue(configuration));
				exactly(1).of(serviceProvider).initialize();
			}
		});
		configuration.setStatus(true);
		home.merge("id", configuration);
	}

	@Test
	public void testInActiveAndHeavyMergeConfiguration() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final List<CachedServiceProvider> list = new ArrayList<CachedServiceProvider>();
		list.add(provider);
		final Profile profile = new Profile();
		configuration.setId("id");
		final Configuration oldConfiguration = configuration.clone();
		oldConfiguration.setId("id");
		oldConfiguration.setCloudName("MyCloud2");
		oldConfiguration.setStatus(true);
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(driverHome).findByName(configuration.getProviderClass());
				will(returnValue(MockServiceProvider.class));
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(2).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
				exactly(1).of(provider).close();
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(map).values();
				will(returnValue(list));
				exactly(1).of(map).put(with(any(String.class)), with(any(CachedServiceProvider.class)));
				will(returnValue(null));
				exactly(1).of(profileHome).get();
				will(returnValue(profile));
				exactly(1).of(profileHome).save();
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(asyncFactory).getInstance(with(any(ConfigurationHome.class)),
						with(any(ServiceProvider.class)), with(any(Configuration.class)),
						with(any(ListeningScheduledExecutorService.class)));
				will(returnValue(asyncServiceProvider));
				exactly(1).of(factory).getInstance(with(any(ConfigurationHome.class)),
						with(any(AsyncServiceProvider.class)), with(any(ListeningScheduledExecutorService.class)),
						with(any(ListeningExecutorService.class)));
				will(returnValue(serviceProvider));
				exactly(1).of(serviceProvider).getConfiguration();
				will(returnValue(configuration));
			}
		});
		home.merge("id", configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMergeWithDuplicateName() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider1");
		final CachedServiceProvider provider2 = context.mock(CachedServiceProvider.class, "provider2");
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		set.add(Maps.immutableEntry("id2", provider2));
		final Profile profile = new Profile();
		final Configuration oldConfiguration = new Configuration();
		oldConfiguration.setId("id");
		oldConfiguration.setCloudName("MyCloud2");
		oldConfiguration.setAccount("Account");
		oldConfiguration.setEndpoint("Endpoint");
		oldConfiguration.setProviderName("Demo");
		oldConfiguration.setProviderClass("Mock");
		oldConfiguration.setRegionId("Taiwan");
		oldConfiguration.getProperties().setProperty("token", "MyCloudToken");
		profile.getConfigurations().add(oldConfiguration);
		final Configuration oldConfiguration2 = new Configuration();
		oldConfiguration2.setId("id2");
		oldConfiguration2.setCloudName("MyCloud");
		profile.getConfigurations().add(oldConfiguration2);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).entrySet();
				will(returnValue(set));

				exactly(1).of(map).get("id");
				will(returnValue(provider));

				exactly(1).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));

				exactly(1).of(provider2).getConfiguration();
				will(returnValue(oldConfiguration2));

			}
		});
		home.merge("id", configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMergeWitConfigNotFound() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(null));
			}
		});
		home.merge("id", configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMergeWithProviderNotFound() throws Exception {
		final Profile profile = new Profile();
		final Configuration oldConfiguration = new Configuration();
		oldConfiguration.setId("id");
		oldConfiguration.setCloudName("MyCloud2");
		oldConfiguration.setAccount("Account");
		oldConfiguration.setEndpoint("Endpoint");
		oldConfiguration.setProviderName("Demo");
		oldConfiguration.setProviderClass("Mock");
		oldConfiguration.setRegionId("Taiwan");
		oldConfiguration.getProperties().setProperty("token", "MyCloudToken");
		profile.getConfigurations().add(oldConfiguration);
		final Configuration oldConfiguration2 = new Configuration();
		oldConfiguration2.setId("id2");
		oldConfiguration2.setCloudName("MyCloud");
		profile.getConfigurations().add(oldConfiguration2);
		context.checking(new Expectations() {

			{

				exactly(1).of(map).get("id");
				will(returnValue(null));

			}
		});
		home.merge("id", configuration);
	}

	@Test(expected = IllegalStateException.class)
	public void testHeavyMergeWithoutInactiveProvider() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final List<CachedServiceProvider> values = new ArrayList<CachedServiceProvider>();
		values.add(provider);
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final Profile profile = new Profile();
		configuration.setStatus(true);
		final Configuration oldConfiguration = new Configuration();
		oldConfiguration.setId("id");
		oldConfiguration.setCloudName("OldMyCloud");
		oldConfiguration.setAccount("Account");
		oldConfiguration.setEndpoint("Endpoint");
		oldConfiguration.setProviderName("Demo");
		oldConfiguration.setProviderClass("Mock");
		oldConfiguration.setRegionId("Taiwan");
		oldConfiguration.getProperties().setProperty("token", "MyCloudToken");
		oldConfiguration.setStatus(true);
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
			}
		});

		home.merge("id", configuration);
	}

	@Test(expected = IllegalStateException.class)
	public void testChangeQueueCapacityWithoutInactiveProvider() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final List<CachedServiceProvider> values = new ArrayList<CachedServiceProvider>();
		values.add(provider);
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final Profile profile = new Profile();
		configuration.setStatus(true);
		final Configuration oldConfiguration = configuration.clone();
		oldConfiguration.setId("id");
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(provider).getConfiguration();
				will(returnValue(oldConfiguration));
			}
		});
		configuration.getLongPoolConfig().setQueueCapacity(1);
		home.merge("id", configuration);
	}

	@Test
	public void testChangeThreadCoreWithoutInactiveProvider() throws Exception {
		final CachedServiceProvider provider = context.mock(CachedServiceProvider.class, "provider2");
		final List<CachedServiceProvider> values = new ArrayList<CachedServiceProvider>();
		values.add(provider);
		final Set<Entry<String, CachedServiceProvider>> set = new HashSet<Entry<String, CachedServiceProvider>>();
		set.add(Maps.immutableEntry("id", provider));
		final Profile profile = new Profile();
		configuration.setStatus(true);
		final Configuration oldConfiguration = configuration.clone();
		oldConfiguration.setId("id");
		profile.getConfigurations().add(oldConfiguration);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(provider));
				exactly(1).of(map).entrySet();
				will(returnValue(set));
				exactly(1).of(profileHome).get();
				will(returnValue(profile));
				exactly(1).of(map).put(with(any(String.class)), with(any(CachedServiceProvider.class)));
				exactly(1).of(profileHome).save();
				exactly(1).of(provider).getConfiguration();
				will(returnValue(configuration));

			}
		});
		configuration.getLongPoolConfig().setCoreSize(100);
		home.merge("id", configuration);
	}

	@Test
	public void testAddLifeCycleListener() {
		final ConfigurationLifeCycleListener service = context.mock(ConfigurationLifeCycleListener.class);
		final Set<CachedServiceProvider> set = new HashSet<CachedServiceProvider>();
		set.add(serviceProvider);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).values();
				will(returnValue(set));

				exactly(1).of(serviceProvider).getConfiguration();
				will(returnValue(configuration));

				exactly(1).of(service).persist(configuration);

			}
		});
		home.addLifeCycleListener(service);
	}

	@Test
	public void testRemoveLifeCycleListener() {
		final ConfigurationLifeCycleListener service = context.mock(ConfigurationLifeCycleListener.class);
		home.removeLifeCycleListener(service);
	}

	@Test
	public void testFindById() {
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(serviceProvider));
			}
		});
		CachedServiceProvider ret = home.findById("id");
		assertEquals(ret, serviceProvider);
	}

	@Test
	public void testFindByIdNotFound() {
		context.checking(new Expectations() {

			{
				exactly(1).of(map).get("id");
				will(returnValue(null));
			}
		});
		CachedServiceProvider ret = home.findById("id");
		assertNull(ret);
	}

	@Test
	public void testFindByName() {
		final Set<CachedServiceProvider> set = new HashSet<CachedServiceProvider>();
		set.add(serviceProvider);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).values();
				will(returnValue(set));
				exactly(1).of(serviceProvider).getConfiguration();
				will(returnValue(configuration));
			}
		});
		CachedServiceProvider ret = home.findByName("MyCloud");
		assertEquals(ret, serviceProvider);
	}

	@Test
	public void testFindByNameNotFound() {
		final Set<CachedServiceProvider> set = new HashSet<CachedServiceProvider>();
		context.checking(new Expectations() {

			{
				exactly(1).of(map).values();
				will(returnValue(set));
			}
		});
		CachedServiceProvider ret = home.findByName("MyCloud");
		assertNull(ret);
	}

	@Test
	public void testFindAll() {
		final Set<CachedServiceProvider> set = new HashSet<CachedServiceProvider>();
		set.add(serviceProvider);
		context.checking(new Expectations() {

			{
				exactly(1).of(map).values();
				will(returnValue(set));

			}
		});
		Collection<CachedServiceProvider> collection = home.findAll();
		assertEquals(set.size(), collection.size());
		assertEquals(set.iterator().next(), collection.iterator().next());
	}

	@Test
	public void testTestContext() throws ClassNotFoundException, IllegalAccessException, InstantiationException,
			UnsupportedEncodingException, CloudException, InternalException {
		context.checking(new Expectations() {

			{
				exactly(1).of(driverHome).findByName("Mock");
				will(returnValue(MockServiceProvider.class));

			}
		});
		String ret = home.testContext(configuration);
		assertEquals("testContext", ret);
	}


	public static class MockServiceProvider extends CloudProvider implements ServiceProvider {

		private ContextRequirements contextRequirements = new ContextRequirements(new Field("token", "MyCloudToken",
				FieldType.TOKEN));

		protected Mockery context = new JUnit4Mockery() {

			{
				setThreadingPolicy(new Synchroniser());
				setImposteriser(ClassImposteriser.INSTANCE);
			}
		};

		private SkyportComputeServices computeServices = context.mock(SkyportComputeServices.class);


		public MockServiceProvider() {
			context.checking(new Expectations() {

				{
					allowing(computeServices).hasAffinityGroupSupport();
					will(returnValue(false));

					allowing(computeServices).hasAutoScalingSupport();
					will(returnValue(false));

					allowing(computeServices).hasImageSupport();
					will(returnValue(false));

					allowing(computeServices).hasSnapshotSupport();
					will(returnValue(false));

					allowing(computeServices).hasVirtualMachineSupport();
					will(returnValue(false));

					allowing(computeServices).hasVolumeSupport();
					will(returnValue(false));
				}
			});
		}

		@Override
		public void initialize() {

		}

		@Override
		public AdminServices getAdminServices() {
			return null;
		}

		@Override
		public ContextRequirements getContextRequirements() {
			return contextRequirements;
		}

		@Override
		public String getCloudName() {
			return null;
		}

		@Override
		public SkyportDataCenterServices getSkyportDataCenterServices() {
			return null;
		}

		@Override
		public CIServices getCIServices() {
			return null;
		}

		@Override
		public SkyportComputeServices getSkyportComputeServices() {
			return computeServices;
		}

		@Override
		public IdentityServices getIdentityServices() {
			return null;
		}

		@Override
		public SkyportNetworkServices getSkyportNetworkServices() {
			return null;
		}

		@Override
		public PlatformServices getPlatformServices() {
			return null;
		}

		@Override
		public String getProviderName() {
			return null;
		}

		@Override
		public String testContext() {
			return "testContext";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.dasein.cloud.CloudProvider#getComputeServices()
		 */
		@Override
		public ComputeServices getComputeServices() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.dasein.cloud.CloudProvider#getDataCenterServices()
		 */
		@Override
		public DataCenterServices getDataCenterServices() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.dasein.cloud.CloudProvider#getNetworkServices()
		 */
		@Override
		public NetworkServices getNetworkServices() {
			return null;
		}

	}

}
