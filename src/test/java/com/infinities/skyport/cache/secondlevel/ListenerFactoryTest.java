package com.infinities.skyport.cache.secondlevel;

import java.util.Map;

import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeProduct;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ListenerFactoryTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetVolumeListener() throws Exception {
		PatchListener<Volume> inner = context.mock(PatchListener.class);
		Map<String, Volume> cache = context.mock(Map.class);
		ListenerFactory.getVolumeListener(inner, cache);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetMachineImageListener() {
		PatchListener<MachineImage> inner = context.mock(PatchListener.class);
		Map<String, MachineImage> cache = context.mock(Map.class);
		ListenerFactory.getMachineImageListener(inner, cache);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetVolumeProductListener() {
		PatchListener<VolumeProduct> inner = context.mock(PatchListener.class);
		Map<String, VolumeProduct> cache = context.mock(Map.class);
		ListenerFactory.getVolumeProductListener(inner, cache);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetVirtualMachineListener() {
		PatchListener<VirtualMachine> inner = context.mock(PatchListener.class);
		Map<String, VirtualMachine> cache = context.mock(Map.class);
		ListenerFactory.getVirtualMachineListener(inner, cache);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetVirtualMachineProductListener() {
		PatchListener<VirtualMachineProduct> inner = context.mock(PatchListener.class);
		Map<String, VirtualMachineProduct> cache = context.mock(Map.class);
		ListenerFactory.getVirtualMachineProductListener(inner, cache);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetSnapshotListener() {
		PatchListener<Snapshot> inner = context.mock(PatchListener.class);
		Map<String, Snapshot> cache = context.mock(Map.class);
		ListenerFactory.getSnapshotListener(inner, cache);
	}

}
