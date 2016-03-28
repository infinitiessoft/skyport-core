package com.infinities.skyport.quartz.callable;

import java.util.concurrent.Callable;

import org.dasein.cloud.storage.Blob;

import com.infinities.skyport.storage.SkyportBlobStoreSupport;

public class BlobCallable implements Callable<Iterable<Blob>>{

	private SkyportBlobStoreSupport support;

	public BlobCallable(SkyportBlobStoreSupport support) {
		this.support = support;
	}
	
	@Override
	public Iterable<Blob> call() throws Exception {
		return support.list(null);
	}

}
