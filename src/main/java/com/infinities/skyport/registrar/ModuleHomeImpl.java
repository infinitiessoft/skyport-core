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

import java.util.Collections;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.infinities.skyport.Module;
import com.infinities.skyport.Skyport;
import com.infinities.skyport.service.ModuleHome;

public class ModuleHomeImpl implements ModuleHome {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(ModuleHomeImpl.class);
	protected SortedMap<String, Module> registeredModules = new TreeMap<String, Module>();
	private final Skyport skyport;


	public ModuleHomeImpl(Skyport skyport) {
		this.skyport = skyport;
	}

	protected synchronized void persist(Module transientInstance) throws Exception {
		if (transientInstance == null) {
			throw new NullPointerException("driver cannot be null");
		}

		String alias = transientInstance.getAlias();

		if (Strings.isNullOrEmpty(alias)) {
			throw new IllegalArgumentException("name cannot be null");
		}

		if (registeredModules.containsKey(alias)) {
			throw new IllegalArgumentException("key:" + alias + " have been set");
		}

		transientInstance.initialize(skyport);
		registeredModules.put(alias, transientInstance);
		logger.info("Register Module: {}", alias);
	}

	@Override
	public synchronized void initialize() {
		logger.info("Loading initial modules");
		ServiceLoader<Module> serviceLoader = ServiceLoader.load(Module.class);
		int num = 0;
		for (Module module : serviceLoader) {
			try {
				persist(module);
				logger.warn("Loading done by the java.lang.reflect.Class: {}", module.getClass());
				num++;
			} catch (Throwable t) {
				logger.error("Error while loading modules", t);
			}
		}

		logger.info("Total modules: {}", num);
	}

	@Override
	public synchronized void close() {
		for (Module module : registeredModules.values()) {
			try {
				module.close();
				logger.warn("Loading done by the java.lang.reflect.Class: {}", module.getClass());
			} catch (Throwable t) {
				logger.error("Error while loading modules", t);
			}
		}
		registeredModules.clear();
	}

	@Override
	public SortedMap<String, Module> findAll() {
		return Collections.unmodifiableSortedMap(registeredModules);
	}

	@Override
	public Module findByName(String name) throws InstantiationException, IllegalAccessException {
		Module module = registeredModules.get(name);
		return module;
	}
}
