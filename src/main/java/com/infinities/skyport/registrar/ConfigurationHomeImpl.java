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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.ServiceProviderBuilder;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.async.impl.AsyncServiceProviderFactory;
import com.infinities.skyport.cache.CachedServiceProvider;
import com.infinities.skyport.cache.impl.CachedServiceProviderFactory;
import com.infinities.skyport.cache.impl.DelegatedServiceProviderFactory;
import com.infinities.skyport.distributed.impl.hazelcast.hazeltask.core.concurrent.NamedThreadFactory;
import com.infinities.skyport.model.Profile;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.DriverHome;
import com.infinities.skyport.service.ProfileHome;
import com.infinities.skyport.timeout.TimedServiceProviderFactory;

public class ConfigurationHomeImpl implements ConfigurationHome {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(ConfigurationHomeImpl.class);
	private DriverHome driverHome;
	private ProfileHome profileHome;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	protected List<ConfigurationLifeCycleListener> listeners = Lists.newCopyOnWriteArrayList();
	protected Map<String, CachedServiceProvider> registeredServiceProviders = Maps.newLinkedHashMap();
	protected AsyncServiceProviderFactory asyncServiceProviderFactory = new AsyncServiceProviderFactory();
	protected TimedServiceProviderFactory timedServiceProviderFactory = new TimedServiceProviderFactory();
	protected CachedServiceProviderFactory cachedServiceProviderFactory = new CachedServiceProviderFactory();
	protected DelegatedServiceProviderFactory delegatedServiceProviderFactory = new DelegatedServiceProviderFactory();
	protected File file; // for testing
	protected File saveFile; // for testing
	protected ListeningScheduledExecutorService scheduler;
	protected ListeningExecutorService worker;


	public ConfigurationHomeImpl() {
	}

	@Override
	public synchronized void persist(Configuration transientInstance) throws Exception {
		if (!isInitialized.get()) {
			throw new IllegalStateException("ConfigurationHome has not been initialized yet");
		}

		checkParameters(transientInstance);
		if (Strings.isNullOrEmpty(transientInstance.getId())) {
			transientInstance.setId(UUID.randomUUID().toString());
		}
		checkConfigurationId(transientInstance);
		Profile profile = getProfile();

		if (driverHome.findByName(transientInstance.getProviderClass()) != null) {
			transientInstance.setModifiedDate(Calendar.getInstance());
			transientInstance.setStatus(true);
			Configuration clone = transientInstance.clone();
			CachedServiceProvider driver = setUpCachedServiceProvider(clone);
			profile.getConfigurations().add(clone);
			registeredServiceProviders.put(clone.getId(), driver);

			for (ConfigurationLifeCycleListener service : listeners) {
				try {
					service.persist(clone.clone());
				} catch (Exception e) {
					logger.error("persist exception", e);
				}
			}
			profileHome.save();
			logger.debug("persist Configuration: {}, {}", new Object[] { clone.getId(), clone.getCloudName() });
		} else {
			throw new IllegalArgumentException("cannot deregister accessconfig, no driver:"
					+ transientInstance.getProviderClass() + " has been set");
		}
	}

	@Override
	public synchronized void remove(String id) throws Exception {
		if (!isInitialized.get()) {
			throw new IllegalStateException("object has not been initialized yet");
		}

		checkNotNull(id);

		CachedServiceProvider serviceProvider = registeredServiceProviders.remove(id);

		Profile profile = getProfile();
		if (serviceProvider != null) {
			Configuration configuration = serviceProvider.getConfiguration();
			serviceProvider.close();
			profile.getConfigurations().remove(configuration);

			for (ConfigurationLifeCycleListener service : listeners) {
				try {
					service.remove(configuration.clone());
				} catch (Exception e) {
					logger.error("remove exception", e);
				}
			}
			profileHome.save();
			logger.debug("remove Configuration: {}, {}",
					new Object[] { configuration.getId(), configuration.getCloudName() });
		} else {
			throw new IllegalArgumentException("cannot deregister accessconfig, no key:" + id + " has been set");
		}
	}

	@Override
	public synchronized CachedServiceProvider merge(String id, Configuration detachedInstance) throws Exception {
		if (!isInitialized.get()) {
			throw new IllegalStateException("object has not been initialized yet");
		}

		checkNotNull(id);
		Configuration clone = detachedInstance.clone();
		clone.setId(id);

		CachedServiceProvider serviceProvider = this.findById(id);

		if (serviceProvider == null) {
			throw new IllegalArgumentException("cannot merge configuration, no key:" + id + " has been set");
		}

		Configuration original = serviceProvider.getConfiguration();

		checkParameters(clone);

		boolean isDriverDataModified = false;
		logger.debug("original Configuration status: {}, updated Configuration status: {}",
				new Object[] { original.getStatus(), clone.getStatus() });
		if (!original.getProviderName().equals(clone.getProviderName())
				|| !original.getCloudName().equals(clone.getCloudName())
				|| !original.getEndpoint().equals(clone.getEndpoint())
				|| !original.getRegionId().equals(clone.getRegionId()) || !original.getAccount().equals(clone.getAccount())
				|| !original.getProperties().equals(clone.getProperties())
				|| original.getCacheable() != clone.getCacheable() || original.getTimeoutable() != clone.getTimeoutable()) {
			isDriverDataModified = true;
		}

		if (original.getShortPoolConfig().getQueueCapacity() != clone.getShortPoolConfig().getQueueCapacity()
				|| original.getMediumPoolConfig().getQueueCapacity() != clone.getMediumPoolConfig().getQueueCapacity()
				|| original.getLongPoolConfig().getQueueCapacity() != clone.getLongPoolConfig().getQueueCapacity()) {
			isDriverDataModified = true;
		}

		boolean boot = false;
		// active configuration
		if (!original.getStatus() && clone.getStatus()) {
			boot = true;
		}

		// inactive configuration then close ServiceProvider and build a new
		// one;
		if (original.getStatus() && !clone.getStatus()) {
			isDriverDataModified = true;
		}

		if (isDriverDataModified) {
			if (original.getStatus() && clone.getStatus()) {
				throw new IllegalStateException(
						"There are Severe changes in configuration, ServiceProvider must be inactive first.");
			}
			serviceProvider.close();
			Profile profile = getProfile();
			int index = profile.getConfigurations().indexOf(original);
			if (profile.getConfigurations().remove(original)) {
				clone.setModifiedDate(Calendar.getInstance());
				detachedInstance.setModifiedDate(clone.getModifiedDate());
				CachedServiceProvider newServiceProvider = setUpCachedServiceProvider(clone);
				serviceProvider = registeredServiceProviders.put(clone.getId(), newServiceProvider);
				profile.getConfigurations().add(index, clone);
				for (ConfigurationLifeCycleListener service : listeners) {
					try {
						service.heavyMerge(newServiceProvider.getConfiguration().clone());
					} catch (Exception e) {
						logger.error("merge exception", e);
					}
				}
				profileHome.save();
				logger.debug("heavy merging Configuration: {}", clone.getCloudName());
				return serviceProvider;
			}
		} else {
			Profile profile = getProfile();
			int index = profile.getConfigurations().indexOf(clone);
			if (profile.getConfigurations().remove(clone)) {
				clone.setModifiedDate(Calendar.getInstance());
				detachedInstance.setModifiedDate(clone.getModifiedDate());

				if (boot) {
					serviceProvider.initialize();
					logger.debug("Service Provider:{} initialize", clone.getCloudName());
				}

				registeredServiceProviders.put(clone.getId(), serviceProvider);
				profile.getConfigurations().add(index, clone.clone());
				for (ConfigurationLifeCycleListener service : listeners) {
					try {
						service.lightMerge(clone.clone());
					} catch (Exception e) {
						logger.error("merge exception", e);
					}
				}

				profileHome.save();
				logger.debug("light merging Configuration: {}", clone.getCloudName());
				return serviceProvider;
			}
		}

		throw new IllegalArgumentException("cannot merge Configuration, no key:" + detachedInstance.getId()
				+ " has been set");
	}

	private Profile getProfile() {
		Profile profile = profileHome.get();
		return profile;
	}

	private CachedServiceProvider setUpCachedServiceProvider(Configuration configuration) throws Exception {
		Configuration clone = configuration.clone();
		Class<? extends ServiceProvider> cls = driverHome.findByName(clone.getProviderClass());
		checkNotNull(cls, "invalid Service Provider");

		ServiceProvider serviceProvider = ServiceProviderBuilder.build(cls, clone);
		if (clone.getTimeoutable()) {
			serviceProvider = timedServiceProviderFactory.getInstance(this, serviceProvider, clone, worker);
		}
		AsyncServiceProvider asyncServiceProvider =
				asyncServiceProviderFactory.getInstance(this, serviceProvider, clone, scheduler);

		CachedServiceProvider cachedServiceProvider = null;

		if (clone.getCacheable()) {
			cachedServiceProvider = cachedServiceProviderFactory.getInstance(this, asyncServiceProvider, scheduler, worker);
		} else {
			cachedServiceProvider = delegatedServiceProviderFactory.getInstance(asyncServiceProvider, scheduler, worker);
		}

		if (clone.getStatus()) {
			cachedServiceProvider.initialize();
			logger.debug("Service Provider:{} initialize", clone.getCloudName());
		}

		return cachedServiceProvider;
	}

	private void checkParameters(Configuration transientInstance) {
		checkNotNull(transientInstance);
		checkNotNull(transientInstance.getCloudName());
		checkNotNull(transientInstance.getAccount());
		checkNotNull(transientInstance.getEndpoint());
		checkNotNull(transientInstance.getProviderName());
		checkNotNull(transientInstance.getProviderClass());
		checkNotNull(transientInstance.getRegionId());

		// check no duplicate name in registered drivers
		for (Entry<String, CachedServiceProvider> entry : this.registeredServiceProviders.entrySet()) {
			String id = entry.getKey();
			logger.debug("check configuration id:{}", id);
			if (transientInstance.getId() == null || !id.equals(transientInstance.getId())) {
				Configuration configuration = entry.getValue().getConfiguration();
				checkArgument(!configuration.getCloudName().equals(transientInstance.getCloudName()), "duplicate name");
			}
		}

	}

	private void checkConfigurationId(Configuration transientInstance) {
		logger.debug("check configuration id:{}", transientInstance.getId());
		checkArgument(!this.registeredServiceProviders.keySet().contains(transientInstance.getId()), "duplicate id");
	}

	@Override
	public synchronized void addLifeCycleListener(ConfigurationLifeCycleListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("object has not been initialized yet");
		}

		// logger.debug("clear ProxyService");
		// service.clear();
		// logger.debug("inject AccessConfig size: {}",
		// registeredServiceProviders.size());
		for (CachedServiceProvider driver : registeredServiceProviders.values()) {
			Configuration configuration = driver.getConfiguration();
			logger.debug("persist listener: {}", configuration.getCloudName());
			service.persist(configuration);
		}
		this.listeners.add(service);
	}

	@Override
	public synchronized void removeLifeCycleListener(ConfigurationLifeCycleListener service) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("object has not been initialized yet");
		}

		this.listeners.remove(service);
	}

	@Override
	public synchronized void initialize() throws Exception {
		if (driverHome == null) {
			throw new NullPointerException("driverHome cannot be null");
		}
		if (isInitialized.compareAndSet(false, true)) {
			logger.debug("AccessConfigHomeService Activate");
			logger.debug("AccessConfigHomeService load AccessConfigs");
			this.worker =
					MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new NamedThreadFactory("ordinary",
							"Cache-Worker")));
			this.scheduler =
					MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(
							"ordinary", "Scheduler")));
			// dispatcher = new EventBusSecondLevelDispatcher(new
			// AsyncEventBus("eventbus", worker));
			Profile profile = profileHome.get();

			for (Configuration configuration : profile.getConfigurations()) {
				if (!this.registeredServiceProviders.containsKey(configuration.getId())) {
					try {
						CachedServiceProvider asyncDriver = setUpCachedServiceProvider(configuration);
						for (ConfigurationLifeCycleListener service : listeners) {
							service.persist(asyncDriver.getConfiguration());
						}
						this.registeredServiceProviders.put(asyncDriver.getConfiguration().getId(), asyncDriver);
						logger.info("setup Service Provider success, config: {}",
								new Object[] { configuration.getCloudName() });
					} catch (Exception e) {
						logger.error("setup Service Provider failed: {}", configuration.getCloudName(), e);
					}
				}

			}
		} else {
			throw new IllegalStateException("object has been initialized");
		}
	}

	@Override
	public synchronized void close() throws JAXBException {
		if (isInitialized.compareAndSet(true, false)) {
			profileHome.save();

			for (CachedServiceProvider driver : registeredServiceProviders.values()) {
				try {
					driver.close();
				} catch (Exception e) {
					logger.error("unexpected exception when close driver", e);
				}
			}
			shutdownExecutor(scheduler);
			logger.debug("shutdown scheduler");
			shutdownExecutor(worker);
			logger.debug("shutdown worker");
			this.registeredServiceProviders.clear();
			logger.debug("ConfigurationHome close");
		}
	}

	private void shutdownExecutor(ExecutorService executor) {
		try {
			if (executor != null && !executor.isShutdown()) {
				executor.shutdown();
				executor.awaitTermination(10, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			logger.error("interrupted when shudown executor", e);
		}

	}

	@Override
	public CachedServiceProvider findById(String id) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("object has not been initialized yet");
		}

		CachedServiceProvider ret = registeredServiceProviders.get(id);
		return ret;
	}

	@Override
	public CachedServiceProvider findByName(String name) {
		if (!isInitialized.get()) {
			throw new IllegalStateException("object has not been initialized yet");
		}
		checkNotNull(name);
		for (CachedServiceProvider driver : registeredServiceProviders.values()) {
			if (driver.getConfiguration().getCloudName().equals(name)) {
				return driver;
			}
		}
		return null;
	}

	@Override
	public Collection<CachedServiceProvider> findAll() {
		if (!isInitialized.get()) {
			throw new IllegalStateException("object has not been initialized yet");
		}
		return Collections.unmodifiableCollection(registeredServiceProviders.values());
	}

	@Override
	public void setDriverHome(DriverHome driverHome) {
		this.driverHome = driverHome;
	}

	@Override
	public String testContext(Configuration configuration) throws ClassNotFoundException, IllegalAccessException,
			InstantiationException, UnsupportedEncodingException, CloudException, InternalException {
		Class<? extends ServiceProvider> cls = driverHome.findByName(configuration.getProviderClass());
		if (cls == null) {
			throw new IllegalArgumentException("invalid ProviderClass");
		}
		ServiceProvider serviceProvider = ServiceProviderBuilder.build(cls, configuration);
		return serviceProvider.testContext();
	}

	@Override
	public void setProfileHome(ProfileHome profileHome) {
		this.profileHome = profileHome;
	}

}
