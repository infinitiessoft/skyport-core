package com.infinities.skyport.cache.impl.service;

import java.util.concurrent.ScheduledFuture;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.async.service.AsyncNetworkServices;
import com.infinities.skyport.async.service.network.AsyncDNSSupport;
import com.infinities.skyport.async.service.network.AsyncFirewallSupport;
import com.infinities.skyport.async.service.network.AsyncIpAddressSupport;
import com.infinities.skyport.async.service.network.AsyncNetworkFirewallSupport;
import com.infinities.skyport.async.service.network.AsyncVpnSupport;
import com.infinities.skyport.cache.impl.service.network.CachedLoadBalancerSupportImpl;
import com.infinities.skyport.cache.impl.service.network.CachedVLANSupportImpl;
import com.infinities.skyport.cache.service.CachedNetworkServices;
import com.infinities.skyport.cache.service.network.CachedLoadBalancerSupport;
import com.infinities.skyport.cache.service.network.CachedVLANSupport;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FirstLevelDispatcher;

public class CachedNetworkServicesImpl implements CachedNetworkServices{
	
	private final AsyncNetworkServices inner;
	private final ListeningExecutorService worker;
	private final DistributedObjectFactory objectFactory;
	private CachedVLANSupportImpl cachedVLANSupport;
	private CachedLoadBalancerSupportImpl cachedLoadBalancerSuport;
	private final QuartzServiceImpl<NetworkQuartzType> quartzService;


	public CachedNetworkServicesImpl(ConfigurationHome home, AsyncNetworkServices inner, Configuration configuration,
			ListeningScheduledExecutorService scheduler, ListeningExecutorService worker, FirstLevelDispatcher dispatcher,
			DistributedObjectFactory objectFactory) {
		this.inner = inner;
		this.worker = worker;
		this.objectFactory = objectFactory;
		DistributedMap<NetworkQuartzType, NetworkQuartzType> typeMap = getObjectFactory().getMap("network_quartz");
		if (typeMap.isEmpty()) {
			for (NetworkQuartzType type : NetworkQuartzType.values()) {
				typeMap.put(type, type);
			}
		}
		this.quartzService = new QuartzServiceImpl<NetworkQuartzType>(NetworkQuartzType.class, scheduler, worker);
		if (inner.hasVlanSupport()) {
			this.cachedVLANSupport =
					new CachedVLANSupportImpl(home, inner.getVlanSupport(), configuration,
							quartzService, typeMap, dispatcher, objectFactory);
		}
		if (inner.hasLoadBalancerSupport()) {
			this.cachedLoadBalancerSuport =
					new CachedLoadBalancerSupportImpl(home, inner.getLoadBalancerSupport(), configuration,
							quartzService, typeMap, dispatcher, objectFactory);
		}

	}
	
	public DistributedObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public ListeningExecutorService getWorker() {
		return worker;
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
		inner.initialize();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				if (cachedVLANSupport != null) {
					cachedVLANSupport.initialize();
				}
				if (cachedLoadBalancerSuport != null) {
					cachedLoadBalancerSuport.initialize();
				}
			}

		};
		getWorker().execute(r);
	}

	@Override
	public void close() {
		if (cachedVLANSupport != null) {
			cachedVLANSupport.close();
		}
		if (cachedLoadBalancerSuport != null) {
			cachedLoadBalancerSuport.close();
		}
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
		return cachedLoadBalancerSuport;
	}

	@Override
	public AsyncNetworkFirewallSupport getNetworkFirewallSupport() {
		return inner.getNetworkFirewallSupport();
	}

	@Override
	public CachedVLANSupport getVlanSupport() {
		return cachedVLANSupport;
	}

	@Override
	public AsyncVpnSupport getVpnSupport() {
		return inner.getVpnSupport();
	}

	@Override
	public ScheduledFuture<?> flushCache(NetworkQuartzType type) {
		return quartzService.executeOnce(type);
	}

}
