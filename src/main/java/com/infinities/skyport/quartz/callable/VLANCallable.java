package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.network.VLAN;
import com.infinities.skyport.network.SkyportVLANSupport;

public class VLANCallable implements Callable<Iterable<VLAN>> {

	private SkyportVLANSupport support;


	public VLANCallable(SkyportVLANSupport skyportVLANSupport) {
		this.support = skyportVLANSupport;
	}

	@Override
	public Iterable<VLAN> call() throws Exception {
		return support.listVlans();
	}

}
