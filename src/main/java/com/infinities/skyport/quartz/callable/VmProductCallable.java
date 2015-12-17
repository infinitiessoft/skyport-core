package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;

public class VmProductCallable implements Callable<Iterable<VirtualMachineProduct>> {

	private VirtualMachineSupport support;


	public VmProductCallable(VirtualMachineSupport support) {
		this.support = support;
	}

	@Override
	public Iterable<VirtualMachineProduct> call() throws Exception {
		return support.listAllProducts();
	}

}
