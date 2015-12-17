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
package com.infinities.skyport.cache.secondlevel;

import java.util.Comparator;
import java.util.Map;

import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeProduct;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.cache.service.compute.CachedMachineImageSupport.CachedMachineImageListener;
import com.infinities.skyport.cache.service.compute.CachedSnapshotSupport.CachedSnapshotListener;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport.CachedVirtualMachineListener;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport.CachedVirtualMachineProductListener;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport.CachedVolumeListener;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport.CachedVolumeProductListener;
import com.infinities.skyport.compute.entity.comparator.MachineImageComparator;
import com.infinities.skyport.compute.entity.comparator.SnapshotComparator;
import com.infinities.skyport.compute.entity.comparator.VirtualMachineComparator;
import com.infinities.skyport.compute.entity.comparator.VirtualMachineProductComparator;
import com.infinities.skyport.compute.entity.comparator.VolumeComparator;
import com.infinities.skyport.compute.entity.comparator.VolumeProductComparator;
import com.infinities.skyport.compute.entity.patch.MachineImagePatchBuilder;
import com.infinities.skyport.compute.entity.patch.PatchBuilder;
import com.infinities.skyport.compute.entity.patch.SnapshotPatchBuilder;
import com.infinities.skyport.compute.entity.patch.VirtualMachinePatchBuilder;
import com.infinities.skyport.compute.entity.patch.VirtualMachineProductPatchBuilder;
import com.infinities.skyport.compute.entity.patch.VolumePatchBuilder;
import com.infinities.skyport.compute.entity.patch.VolumeProductPatchBuilder;

public class ListenerFactory {

	private ListenerFactory() {

	}

	public static CachedVolumeListener getVolumeListener(PatchListener<Volume> inner, Map<String, Volume> cache) {
		String field = "providerSnapshotId";
		return getProxy(CachedVolumeListener.class, field, new VolumeComparator(), new VolumePatchBuilder(), inner, cache);
	}

	public static CachedMachineImageListener getMachineImageListener(PatchListener<MachineImage> inner,
			Map<String, MachineImage> cache) {
		String field = "providerMachineImageId";
		return getProxy(CachedMachineImageListener.class, field, new MachineImageComparator(),
				new MachineImagePatchBuilder(), inner, cache);
	}

	public static CachedVolumeProductListener getVolumeProductListener(PatchListener<VolumeProduct> inner,
			Map<String, VolumeProduct> cache) {
		String field = "providerProductId";
		return getProxy(CachedVolumeProductListener.class, field, new VolumeProductComparator(),
				new VolumeProductPatchBuilder(), inner, cache);
	}

	public static CachedVirtualMachineListener getVirtualMachineListener(PatchListener<VirtualMachine> inner,
			Map<String, VirtualMachine> cache) {
		String field = "providerVirtualMachineId";
		return getProxy(CachedVirtualMachineListener.class, field, new VirtualMachineComparator(),
				new VirtualMachinePatchBuilder(), inner, cache);
	}

	public static CachedVirtualMachineProductListener getVirtualMachineProductListener(
			PatchListener<VirtualMachineProduct> inner, Map<String, VirtualMachineProduct> cache) {
		String field = "providerProductId";
		return getProxy(CachedVirtualMachineProductListener.class, field, new VirtualMachineProductComparator(),
				new VirtualMachineProductPatchBuilder(), inner, cache);
	}

	public static CachedSnapshotListener getSnapshotListener(PatchListener<Snapshot> inner, Map<String, Snapshot> cache) {
		String field = "providerSnapshotId";
		return getProxy(CachedSnapshotListener.class, field, new SnapshotComparator(), new SnapshotPatchBuilder(), inner,
				cache);
	}

	private static <T, E> T getProxy(Class<T> intf, String idProperty, Comparator<E> comparator,
			PatchBuilder<E> patchBuilder, PatchListener<E> inner, Map<String, E> cache) {
		return Reflection.newProxy(intf, new PatchHandler<E>(idProperty, comparator, patchBuilder, inner, cache));
	}

}
