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
import org.dasein.cloud.network.NetworkServices;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncNetworkServices;
import com.infinities.skyport.async.service.network.AsyncDNSSupport;
import com.infinities.skyport.async.service.network.AsyncFirewallSupport;
import com.infinities.skyport.async.service.network.AsyncIpAddressSupport;
import com.infinities.skyport.async.service.network.AsyncLoadBalancerSupport;
import com.infinities.skyport.async.service.network.AsyncNetworkFirewallSupport;
import com.infinities.skyport.async.service.network.AsyncVLANSupport;
import com.infinities.skyport.async.service.network.AsyncVpnSupport;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.NetworkConfiguration;

public class AsyncNetworkServicesImpl implements AsyncNetworkServices {

	private final NetworkServices inner;
	private AsyncDNSSupport asyncDNSSupport;
	private AsyncFirewallSupport asyncFirewallSupport;
	private AsyncIpAddressSupport asyncIpAddressSupport;
	private AsyncLoadBalancerSupport asyncLoadBalancerSupport;
	private AsyncNetworkFirewallSupport asyncNetworkFirewallSupport;
	private AsyncVLANSupport asyncVLANSupport;
	private AsyncVpnSupport asyncVpnSupport;


	public AsyncNetworkServicesImpl(String configurationId, ServiceProvider inner, NetworkConfiguration configuration,
			DistributedThreadPool threadPools) throws ConcurrentException {
		this.inner = inner.getNetworkServices();
		if (this.inner.hasDnsSupport()) {
			this.asyncDNSSupport =
					Reflection.newProxy(AsyncDNSSupport.class, new AsyncHandler(configurationId, TaskType.DNSSupport, inner,
							threadPools, configuration.getdNSConfiguration()));
		}
		if (this.inner.hasFirewallSupport()) {
			this.asyncFirewallSupport =
					Reflection.newProxy(AsyncFirewallSupport.class, new AsyncHandler(configurationId,
							TaskType.FirewallSupport, inner, threadPools, configuration.getFirewallConfiguration()));
		}
		if (this.inner.hasIpAddressSupport()) {
			this.asyncIpAddressSupport =
					Reflection.newProxy(AsyncIpAddressSupport.class, new AsyncHandler(configurationId,
							TaskType.IpAddressSupport, inner, threadPools, configuration.getIpAddressConfiguration()));
		}
		if (this.inner.hasLoadBalancerSupport()) {
			this.asyncLoadBalancerSupport =
					Reflection.newProxy(AsyncLoadBalancerSupport.class, new AsyncHandler(configurationId,
							TaskType.LoadBalancerSupport, inner, threadPools, configuration.getLoadBalancerConfiguration()));
		}
		if (this.inner.hasNetworkFirewallSupport()) {
			this.asyncNetworkFirewallSupport =
					Reflection.newProxy(AsyncNetworkFirewallSupport.class,
							new AsyncHandler(configurationId, TaskType.NetworkFirewallSupport, inner, threadPools,
									configuration.getNetworkFirewallConfiguration()));
		}
		if (this.inner.hasVlanSupport()) {
			this.asyncVLANSupport =
					Reflection.newProxy(AsyncVLANSupport.class, new AsyncHandler(configurationId, TaskType.VLANSupport,
							inner, threadPools, configuration.getvLANConfiguration()));
		}
		if (this.inner.hasVpnSupport()) {
			this.asyncVpnSupport =
					Reflection.newProxy(AsyncVpnSupport.class, new AsyncHandler(configurationId, TaskType.VpnSupport, inner,
							threadPools, configuration.getVpnConfiguration()));
		}

	}

	@Override
	public AsyncDNSSupport getDnsSupport() {
		return asyncDNSSupport;
	}

	@Override
	public AsyncFirewallSupport getFirewallSupport() {
		return asyncFirewallSupport;
	}

	@Override
	public AsyncIpAddressSupport getIpAddressSupport() {
		return asyncIpAddressSupport;
	}

	@Override
	public AsyncLoadBalancerSupport getLoadBalancerSupport() {
		return asyncLoadBalancerSupport;
	}

	@Override
	public AsyncNetworkFirewallSupport getNetworkFirewallSupport() {
		return asyncNetworkFirewallSupport;
	}

	@Override
	public AsyncVLANSupport getVlanSupport() {
		return asyncVLANSupport;
	}

	@Override
	public AsyncVpnSupport getVpnSupport() {
		return asyncVpnSupport;
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

	@Override
	public void initialize() throws Exception {

	}

	@Override
	public void close() {

	}

}
