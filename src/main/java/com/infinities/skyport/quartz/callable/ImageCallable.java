package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageSupport;

public class ImageCallable implements Callable<Iterable<MachineImage>> {

	private MachineImageSupport support;


	public ImageCallable(MachineImageSupport support) {
		this.support = support;
	}

	@Override
	public Iterable<MachineImage> call() throws Exception {
		ImageFilterOptions options = ImageFilterOptions.getInstance();
		return support.listImages(options);
	}
}
