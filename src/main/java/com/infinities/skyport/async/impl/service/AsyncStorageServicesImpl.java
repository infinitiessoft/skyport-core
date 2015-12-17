/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.storage.StorageServices;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncStorageServices;
import com.infinities.skyport.async.service.storage.AsyncBlobStoreSupport;
import com.infinities.skyport.async.service.storage.AsyncOfflineStoreSupport;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.StorageConfiguration;

public class AsyncStorageServicesImpl implements AsyncStorageServices {

	private final StorageServices inner;
	private AsyncOfflineStoreSupport asyncOfflineStoreSupport;
	private AsyncBlobStoreSupport asyncBlobStoreSupport;


	public AsyncStorageServicesImpl(String configurationId, ServiceProvider inner, StorageConfiguration configuration,
			DistributedThreadPool threadPools) throws ConcurrentException {
		this.inner = inner.getStorageServices();
		if (this.inner.hasOfflineStorageSupport()) {
			this.asyncOfflineStoreSupport =
					Reflection.newProxy(AsyncOfflineStoreSupport.class, new AsyncHandler(configurationId,
							TaskType.OfflineStoreSupport, inner, threadPools, configuration.getOfflineStoreConfiguration()));
		}
		if (this.inner.hasOnlineStorageSupport()) {
			this.asyncBlobStoreSupport =
					Reflection.newProxy(AsyncBlobStoreSupport.class, new AsyncHandler(configurationId,
							TaskType.BlobStoreSupport, inner, threadPools, configuration.getOnlineStorageConfiguration()));
		}
	}

	@Override
	public AsyncOfflineStoreSupport getOfflineStorageSupport() {
		return asyncOfflineStoreSupport;
	}

	@Override
	public boolean hasOfflineStorageSupport() {
		return inner.hasOfflineStorageSupport();
	}

	@Override
	public AsyncBlobStoreSupport getOnlineStorageSupport() {
		return asyncBlobStoreSupport;
	}

	@Override
	public boolean hasOnlineStorageSupport() {
		return inner.hasOnlineStorageSupport();
	}

	@Override
	public void initialize() throws Exception {

	}

	@Override
	public void close() {

	}

}
