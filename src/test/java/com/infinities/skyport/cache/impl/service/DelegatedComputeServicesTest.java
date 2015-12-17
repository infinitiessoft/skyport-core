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
package com.infinities.skyport.cache.impl.service;

import static org.junit.Assert.assertTrue;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.async.service.AsyncComputeServices;
import com.infinities.skyport.async.service.compute.AsyncMachineImageSupport;
import com.infinities.skyport.async.service.compute.AsyncSnapshotSupport;
import com.infinities.skyport.async.service.compute.AsyncVirtualMachineSupport;
import com.infinities.skyport.async.service.compute.AsyncVolumeSupport;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedMachineImageSupport.CachedMachineImageListener;
import com.infinities.skyport.cache.service.compute.CachedSnapshotSupport.CachedSnapshotListener;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport.CachedVirtualMachineListener;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport.CachedVirtualMachineProductListener;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport.CachedVolumeListener;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport.CachedVolumeProductListener;

public class DelegatedComputeServicesTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private AsyncComputeServices inner;
	private DelegatedComputeServices services;

	private AsyncVirtualMachineSupport virtualMachineSupport;
	private AsyncMachineImageSupport machineImageSupport;
	private AsyncSnapshotSupport snapshotSupport;
	private AsyncVolumeSupport volumeSupport;


	@Before
	public void setUp() throws Exception {
		inner = context.mock(AsyncComputeServices.class);
		virtualMachineSupport = context.mock(AsyncVirtualMachineSupport.class);
		machineImageSupport = context.mock(AsyncMachineImageSupport.class);
		volumeSupport = context.mock(AsyncVolumeSupport.class);
		snapshotSupport = context.mock(AsyncSnapshotSupport.class);

		context.checking(new Expectations() {

			{
				exactly(1).of(inner).getVirtualMachineSupport();
				will(returnValue(virtualMachineSupport));

				exactly(1).of(inner).hasVirtualMachineSupport();
				will(returnValue(true));

				exactly(1).of(inner).getImageSupport();
				will(returnValue(machineImageSupport));

				exactly(1).of(inner).hasImageSupport();
				will(returnValue(true));

				exactly(1).of(inner).getSnapshotSupport();
				will(returnValue(snapshotSupport));

				exactly(1).of(inner).hasSnapshotSupport();
				will(returnValue(true));

				exactly(1).of(inner).getVolumeSupport();
				will(returnValue(volumeSupport));

				exactly(1).of(inner).hasVolumeSupport();
				will(returnValue(true));
			}
		});
		services = new DelegatedComputeServices(inner);
		services.initialize();

	}

	@After
	public void tearDown() throws Exception {
		services.close();
		context.assertIsSatisfied();
	}

	@Test
	public void testVirtualMachineSupport() throws CloudException, InternalException {
		context.checking(new Expectations() {

			{
				exactly(1).of(virtualMachineSupport).isSubscribed();
				will(returnValue(true));

			}
		});
		assertTrue(services.getVirtualMachineSupport().isSubscribed());
	}

	@Test
	public void testMachineImageSupport() throws CloudException, InternalException {
		context.checking(new Expectations() {

			{
				exactly(1).of(machineImageSupport).isSubscribed();
				will(returnValue(true));

			}
		});
		assertTrue(services.getImageSupport().isSubscribed());
	}

	@Test
	public void testSnapshotSupport() throws CloudException, InternalException {
		context.checking(new Expectations() {

			{
				exactly(1).of(snapshotSupport).isSubscribed();
				will(returnValue(true));

			}
		});
		assertTrue(services.getSnapshotSupport().isSubscribed());
	}

	@Test
	public void testVolumeSupport() throws CloudException, InternalException {
		context.checking(new Expectations() {

			{
				exactly(1).of(volumeSupport).isSubscribed();
				will(returnValue(true));

			}
		});
		assertTrue(services.getVolumeSupport().isSubscribed());
	}

	@Test(expected = IllegalStateException.class)
	public void testAddVirtualMachineListener() {
		CachedVirtualMachineListener listener = context.mock(CachedVirtualMachineListener.class);
		services.getVirtualMachineSupport().addVirtualMachineListener(listener);
	}

	@Test(expected = IllegalStateException.class)
	public void testAddVirtualMachineProductListener() {
		CachedVirtualMachineProductListener listener = context.mock(CachedVirtualMachineProductListener.class);
		services.getVirtualMachineSupport().addVirtualMachineProductListener(listener);
	}

	@Test(expected = IllegalStateException.class)
	public void testAddMachineImageListener() {
		CachedMachineImageListener service = context.mock(CachedMachineImageListener.class);
		services.getImageSupport().addMachineImageListener(service);
	}

	@Test(expected = IllegalStateException.class)
	public void testAddSnapshotListener() {
		CachedSnapshotListener service = context.mock(CachedSnapshotListener.class);
		services.getSnapshotSupport().addSnapshotListener(service);
	}

	@Test(expected = IllegalStateException.class)
	public void testAddVolumeListener() {
		CachedVolumeListener service = context.mock(CachedVolumeListener.class);
		services.getVolumeSupport().addVolumeListener(service);
	}

	@Test(expected = IllegalStateException.class)
	public void testAddVolumeProductListener() {
		CachedVolumeProductListener service = context.mock(CachedVolumeProductListener.class);
		services.getVolumeSupport().addVolumeProductListener(service);
	}

	@Test(expected = IllegalStateException.class)
	public void testFlushCache() {
		services.flushCache(ComputeQuartzType.VirtualMachine);
	}

}
