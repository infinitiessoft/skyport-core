package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.network.Subnet;

import com.infinities.skyport.network.SkyportVLANSupport;

public class SubnetCallable implements Callable<Iterable<Subnet>>{

	private SkyportVLANSupport support;
	
	public SubnetCallable(SkyportVLANSupport support) {
		this.support = support;
	}

	@Override
	public Iterable<Subnet> call() throws Exception {
		return support.listSubnets(null);
	}
}
