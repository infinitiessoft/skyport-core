package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;

public class VmCallable implements Callable<Iterable<VirtualMachine>> {

	private VirtualMachineSupport support;


	public VmCallable(VirtualMachineSupport support) {
		this.support = support;
	}

	@Override
	public Iterable<VirtualMachine> call() throws Exception {
		return support.listVirtualMachines();
	}

}
