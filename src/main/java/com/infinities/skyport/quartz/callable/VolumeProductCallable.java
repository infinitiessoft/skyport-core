package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeSupport;

public class VolumeProductCallable implements Callable<Iterable<VolumeProduct>> {

	private VolumeSupport support;


	public VolumeProductCallable(VolumeSupport support) {
		this.support = support;
	}

	@Override
	public Iterable<VolumeProduct> call() throws Exception {
		return support.listVolumeProducts();
	}

}
