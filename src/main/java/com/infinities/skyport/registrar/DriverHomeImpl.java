package com.infinities.skyport.registrar;

import java.util.Collections;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.service.DriverHome;

public class DriverHomeImpl implements DriverHome {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected SortedMap<String, Class<? extends ServiceProvider>> registered =
			new TreeMap<String, Class<? extends ServiceProvider>>();

	private static final Logger logger = LoggerFactory.getLogger(DriverHomeImpl.class);


	protected synchronized void persist(ServiceProvider transientInstance) throws Exception {
		if (transientInstance == null) {
			throw new NullPointerException("provider cannot be null");
		}

		String providerClass = transientInstance.getClass().getName();

		if (registered.containsKey(providerClass)) {
			throw new IllegalStateException("key:" + providerClass + " have been set");
		}

		registered.put(providerClass, transientInstance.getClass());
		logger.info("Register Service Provider: {}", providerClass);
	}

	@Override
	public synchronized void initialize() {
		logger.info("Loading initial service providers");
		try {
			ServiceLoader<ServiceProvider> serviceLoader = ServiceLoader.load(ServiceProvider.class);
			int num = 0;
			for (ServiceProvider service : serviceLoader) {
				persist(service);
				logger.warn("Loading done by the java.lang.reflect.Class: {}", service.getClass());
				num++;
			}

			logger.info("Total service providers: {}", num);
		} catch (Throwable t) {
			logger.error("Error while loading providers", t);
		}
	}

	@Override
	public synchronized void close() {
		registered.clear();
	}

	@Override
	public SortedMap<String, Class<? extends ServiceProvider>> findAll() {
		return Collections.unmodifiableSortedMap(registered);
	}

	@Override
	public Class<? extends ServiceProvider> findByName(String name) {
		Class<? extends ServiceProvider> driver = registered.get(name);
		if (driver == null) {
			return null;
		}
		return registered.get(name);
	}
}
