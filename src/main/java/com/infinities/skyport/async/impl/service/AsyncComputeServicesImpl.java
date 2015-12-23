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

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncComputeServices;
import com.infinities.skyport.async.service.compute.AsyncAffinityGroupSupport;
import com.infinities.skyport.async.service.compute.AsyncAutoScalingSupport;
import com.infinities.skyport.async.service.compute.AsyncMachineImageSupport;
import com.infinities.skyport.async.service.compute.AsyncSnapshotSupport;
import com.infinities.skyport.async.service.compute.AsyncVirtualMachineSupport;
import com.infinities.skyport.async.service.compute.AsyncVolumeSupport;
import com.infinities.skyport.compute.SkyportComputeServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.ComputeConfiguration;

public class AsyncComputeServicesImpl implements AsyncComputeServices {

	// private static final Logger logger =
	// LoggerFactory.getLogger(AsyncComputeServicesImpl.class);
	private final SkyportComputeServices inner;
	private AsyncVirtualMachineSupport asyncVirtualMachineSupport;
	private AsyncAffinityGroupSupport asyncAffinityGroupSupport;
	private AsyncMachineImageSupport asyncMachineImageSupport;
	private AsyncSnapshotSupport asyncSnapshotSupport;
	private AsyncVolumeSupport asyncVolumeSupport;
	private AsyncAutoScalingSupport asyncAutoScalingSupport;


	public AsyncComputeServicesImpl(String configurationId, ServiceProvider inner, ComputeConfiguration configuration,
			DistributedThreadPool threadPools) throws ConcurrentException {
		this.inner = inner.getSkyportComputeServices();
		if (this.inner.hasAffinityGroupSupport()) {
			this.asyncAffinityGroupSupport = Reflection.newProxy(
					AsyncAffinityGroupSupport.class,
					new AsyncHandler(configurationId, TaskType.AffinityGroupSupport, inner, threadPools, configuration
							.getAffinityGroupConfiguration()));
		}
		if (this.inner.hasVirtualMachineSupport()) {
			this.asyncVirtualMachineSupport = Reflection.newProxy(
					AsyncVirtualMachineSupport.class,
					new AsyncHandler(configurationId, TaskType.VirtualMachineSupport, inner, threadPools, configuration
							.getVirtualMachineConfiguration()));
		}
		if (this.inner.hasImageSupport()) {
			this.asyncMachineImageSupport = Reflection.newProxy(
					AsyncMachineImageSupport.class,
					new AsyncHandler(configurationId, TaskType.MachineImageSupport, inner, threadPools, configuration
							.getMachineImageConfiguration()));
		}
		if (this.inner.hasSnapshotSupport()) {
			this.asyncSnapshotSupport = Reflection.newProxy(AsyncSnapshotSupport.class, new AsyncHandler(configurationId,
					TaskType.SnapshotSupport, inner, threadPools, configuration.getSnapshotConfiguration()));
		}
		if (this.inner.hasVolumeSupport()) {
			this.asyncVolumeSupport = Reflection.newProxy(AsyncVolumeSupport.class, new AsyncHandler(configurationId,
					TaskType.VolumeSupport, inner, threadPools, configuration.getSnapshotConfiguration()));
		}
		if (this.inner.hasAutoScalingSupport()) {
			this.asyncAutoScalingSupport = Reflection.newProxy(
					AsyncAutoScalingSupport.class,
					new AsyncHandler(configurationId, TaskType.AutoScalingSupport, inner, threadPools, configuration
							.getAutoScalingConfiguration()));
		}

	}

	@Override
	public AsyncAffinityGroupSupport getAffinityGroupSupport() {
		return asyncAffinityGroupSupport;
	}

	@Override
	public AsyncAutoScalingSupport getAutoScalingSupport() {
		return asyncAutoScalingSupport;
	}

	@Override
	public AsyncMachineImageSupport getImageSupport() {
		return asyncMachineImageSupport;
	}

	@Override
	public AsyncSnapshotSupport getSnapshotSupport() {
		return asyncSnapshotSupport;
	}

	@Override
	public AsyncVirtualMachineSupport getVirtualMachineSupport() {
		return asyncVirtualMachineSupport;
	}

	@Override
	public AsyncVolumeSupport getVolumeSupport() {
		return asyncVolumeSupport;
	}

	@Override
	public boolean hasAffinityGroupSupport() {
		return inner.hasAffinityGroupSupport();
	}

	@Override
	public boolean hasAutoScalingSupport() {
		return inner.hasAutoScalingSupport();
	}

	@Override
	public boolean hasImageSupport() {
		return inner.hasImageSupport();
	}

	@Override
	public boolean hasSnapshotSupport() {
		return inner.hasSnapshotSupport();
	}

	@Override
	public boolean hasVirtualMachineSupport() {
		return inner.hasVirtualMachineSupport();
	}

	@Override
	public boolean hasVolumeSupport() {
		return inner.hasVolumeSupport();
	}

	@Override
	public void initialize() throws Exception {
	}

	@Override
	public void close() {

	}

}
