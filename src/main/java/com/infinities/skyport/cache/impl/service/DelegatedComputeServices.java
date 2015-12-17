package com.infinities.skyport.cache.impl.service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import com.google.common.collect.ObjectArrays;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.infinities.skyport.async.service.AsyncComputeServices;
import com.infinities.skyport.async.service.compute.AsyncAffinityGroupSupport;
import com.infinities.skyport.async.service.compute.AsyncAutoScalingSupport;
import com.infinities.skyport.cache.service.CachedComputeServices;
import com.infinities.skyport.cache.service.compute.CachedMachineImageSupport;
import com.infinities.skyport.cache.service.compute.CachedSnapshotSupport;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport;

public class DelegatedComputeServices implements CachedComputeServices {

	private static final Set<String> LISTENERS;
	private AsyncComputeServices inner;
	private CachedVirtualMachineSupport delegatedVirtualMachineSupport;
	private CachedMachineImageSupport delegatedMachineImageSupport;
	private CachedSnapshotSupport delegatedSnapshotSupport;
	private CachedVolumeSupport delegatedVolumeSupport;

	static {
		Set<String> listeners = new HashSet<String>();
		listeners.add("addVirtualMachineListener");
		listeners.add("removeVirtualMachineListener");
		listeners.add("addVirtualMachineProductListener");
		listeners.add("removeVirtualMachineProductListener");
		listeners.add("addMachineImageListener");
		listeners.add("removeMachineImageListener");
		listeners.add("addSnapshotListener");
		listeners.add("removeSnapshotListener");
		listeners.add("addVolumeListener");
		listeners.add("removeVolumeListener");
		listeners.add("addVolumeProductListener");
		listeners.add("removeVolumeProductListener");
		LISTENERS = Collections.unmodifiableSet(listeners);

	}


	public DelegatedComputeServices(AsyncComputeServices inner) {
		this.inner = inner;
		if (inner.hasVirtualMachineSupport()) {
			this.delegatedVirtualMachineSupport =
					Reflection.newProxy(CachedVirtualMachineSupport.class,
							new DelegatedHandler(inner.getVirtualMachineSupport()));
		}
		if (inner.hasImageSupport()) {
			this.delegatedMachineImageSupport =
					Reflection.newProxy(CachedMachineImageSupport.class, new DelegatedHandler(inner.getImageSupport()));
		}
		if (inner.hasSnapshotSupport()) {
			this.delegatedSnapshotSupport =
					Reflection.newProxy(CachedSnapshotSupport.class, new DelegatedHandler(inner.getSnapshotSupport()));
		}
		if (inner.hasVolumeSupport()) {
			this.delegatedVolumeSupport =
					Reflection.newProxy(CachedVolumeSupport.class, new DelegatedHandler(inner.getVolumeSupport()));
		}
	}

	@Override
	public AsyncAffinityGroupSupport getAffinityGroupSupport() {
		return inner.getAffinityGroupSupport();
	}

	@Override
	public AsyncAutoScalingSupport getAutoScalingSupport() {
		return inner.getAutoScalingSupport();
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
	public CachedVirtualMachineSupport getVirtualMachineSupport() {
		return this.delegatedVirtualMachineSupport;
	}

	@Override
	public CachedMachineImageSupport getImageSupport() {
		return this.delegatedMachineImageSupport;
	}

	@Override
	public CachedSnapshotSupport getSnapshotSupport() {
		return this.delegatedSnapshotSupport;
	}

	@Override
	public CachedVolumeSupport getVolumeSupport() {
		return this.delegatedVolumeSupport;
	}


	private class DelegatedHandler extends AbstractInvocationHandler {

		private final Object inner;


		public DelegatedHandler(Object inner) {
			this.inner = inner;
		}

		@Override
		protected Object handleInvocation(Object proxy, Method method, final Object[] args) throws Throwable {
			if (LISTENERS.contains(method.getName())) {
				throwCause(new IllegalStateException("listener is disabled"), false);
			}

			// get pool size the method use.
			try {
				return method.invoke(inner, args);
			} catch (Exception e) {
				throwCause(e, false);
				throw new AssertionError("can't get here");
			}
		}

		private Exception throwCause(Exception e, boolean combineStackTraces) throws Exception {
			Throwable cause = e.getCause();
			if (cause == null) {
				throw e;
			}
			if (combineStackTraces) {
				StackTraceElement[] combined =
						ObjectArrays.concat(cause.getStackTrace(), e.getStackTrace(), StackTraceElement.class);
				cause.setStackTrace(combined);
			}
			if (cause instanceof Exception) {
				throw (Exception) cause;
			}
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			// The cause is a weird kind of Throwable, so throw the outer
			// exception.
			throw e;
		}

	}


	@Override
	public ScheduledFuture<?> flushCache(ComputeQuartzType type) {
		throw new IllegalStateException("cache disabled");
	}

	@Override
	public void initialize() throws Exception {
	}

	@Override
	public void close() {

	}

}
