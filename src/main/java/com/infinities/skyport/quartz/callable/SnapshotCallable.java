package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotSupport;

public class SnapshotCallable implements Callable<Iterable<Snapshot>> {

	private SnapshotSupport support;


	public SnapshotCallable(SnapshotSupport support) {
		this.support = support;
	}

	@Override
	public Iterable<Snapshot> call() throws Exception {
		return support.listSnapshots();
	}

}
