package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeSupport;

public class VolumeCallable implements Callable<Iterable<Volume>> {

	private VolumeSupport support;


	public VolumeCallable(VolumeSupport support) {
		this.support = support;
	}

	@Override
	public Iterable<Volume> call() throws Exception {
		return support.listVolumes();
	}

}
