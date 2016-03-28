package com.infinities.skyport.cache.impl.service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import com.google.common.collect.ObjectArrays;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.infinities.skyport.async.service.AsyncNetworkServices;
import com.infinities.skyport.async.service.network.AsyncDNSSupport;
import com.infinities.skyport.async.service.network.AsyncFirewallSupport;
import com.infinities.skyport.async.service.network.AsyncIpAddressSupport;
import com.infinities.skyport.async.service.network.AsyncNetworkFirewallSupport;
import com.infinities.skyport.async.service.network.AsyncVpnSupport;
import com.infinities.skyport.cache.service.CachedNetworkServices;
import com.infinities.skyport.cache.service.network.CachedLoadBalancerSupport;
import com.infinities.skyport.cache.service.network.CachedVLANSupport;

public class DelegatedNetworkServices implements CachedNetworkServices{

	private static final Set<String> LISTENERS;
	private AsyncNetworkServices inner;
	private CachedVLANSupport delegatedVLANSupport;
	private CachedLoadBalancerSupport delegatedLoadBalancerSupport;

	static {
		Set<String> listeners = new HashSet<String>();
		listeners.add("addVLANListener");
		listeners.add("removeVLANListener");
		listeners.add("addSubnetListener");
		listeners.add("removeSubnetListener");
		listeners.add("addLoadBalancerListener");
		listeners.add("removeLoadBalancerListener");
		LISTENERS = Collections.unmodifiableSet(listeners);
	}
	
	public DelegatedNetworkServices(AsyncNetworkServices inner) {
		this.inner = inner;
		if (inner.hasVlanSupport()) {
			this.delegatedVLANSupport =
					Reflection.newProxy(CachedVLANSupport.class,
							new DelegatedHandler(inner.getVlanSupport()));
		}
		if (inner.hasLoadBalancerSupport()) {
			this.delegatedLoadBalancerSupport =
					Reflection.newProxy(CachedLoadBalancerSupport.class,
							new DelegatedHandler(inner.getLoadBalancerSupport()));
		}
	}
	
	@Override
	public boolean hasDnsSupport() {
		return inner.hasDnsSupport();
	}

	@Override
	public boolean hasFirewallSupport() {
		return inner.hasFirewallSupport();
	}

	@Override
	public boolean hasIpAddressSupport() {
		return inner.hasIpAddressSupport();
	}

	@Override
	public boolean hasLoadBalancerSupport() {
		return inner.hasLoadBalancerSupport();
	}

	@Override
	public boolean hasNetworkFirewallSupport() {
		return inner.hasNetworkFirewallSupport();
	}

	@Override
	public boolean hasVlanSupport() {
		return inner.hasVlanSupport();
	}

	@Override
	public boolean hasVpnSupport() {
		return inner.hasVpnSupport();
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
	public void initialize() throws Exception {
		
	}

	@Override
	public void close() {
		
	}

	@Override
	public AsyncDNSSupport getDnsSupport() {
		return inner.getDnsSupport();
	}

	@Override
	public AsyncFirewallSupport getFirewallSupport() {
		return inner.getFirewallSupport();
	}

	@Override
	public AsyncIpAddressSupport getIpAddressSupport() {
		return inner.getIpAddressSupport();
	}

	@Override
	public CachedLoadBalancerSupport getLoadBalancerSupport() {
		return this.delegatedLoadBalancerSupport;
	}

	@Override
	public AsyncNetworkFirewallSupport getNetworkFirewallSupport() {
		return null;
	}

	@Override
	public CachedVLANSupport getVlanSupport() {
		return this.delegatedVLANSupport;
	}

	@Override
	public AsyncVpnSupport getVpnSupport() {
		return null;
	}

	@Override
	public ScheduledFuture<?> flushCache(NetworkQuartzType type) {
		throw new IllegalStateException("cache disabled");
	}

}
