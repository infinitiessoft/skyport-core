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

import java.util.Map;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.ServiceProvider;

public class DriverHomeImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DriverHomeImpl home;
	private ServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		home = new DriverHomeImpl();
		provider = context.mock(ServiceProvider.class);
		home.initialize();
	}

	@After
	public void tearDown() throws Exception {
		home.close();
		context.assertIsSatisfied();
	}

	@Test
	public void testPersist() throws Exception {
		home.persist(provider);
	}

	@Test
	public void testFindAll() {
		Map<String, Class<? extends ServiceProvider>> map = home.findAll();
		assertEquals(1, map.size());
	}

	@Test
	public void testFindByName() {
		String className = "com.infinities.skyport.MockServiceProvider";
		Class<? extends ServiceProvider> serverProvider = home.findByName(className);
		assertEquals(className, serverProvider.getName());
	}

}
