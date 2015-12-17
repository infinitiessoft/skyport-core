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
import org.dasein.cloud.platform.PlatformServices;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncPlatformServices;
import com.infinities.skyport.async.service.platform.AsyncCDNSupport;
import com.infinities.skyport.async.service.platform.AsyncDataWarehouseSupport;
import com.infinities.skyport.async.service.platform.AsyncKeyValueDatabaseSupport;
import com.infinities.skyport.async.service.platform.AsyncMQSupport;
import com.infinities.skyport.async.service.platform.AsyncMonitoringSupport;
import com.infinities.skyport.async.service.platform.AsyncPushNotificationSupport;
import com.infinities.skyport.async.service.platform.AsyncRelationalDatabaseSupport;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.PlatformConfiguration;

public class AsyncPlatformServicesImpl implements AsyncPlatformServices {

	private final PlatformServices inner;
	private AsyncCDNSupport asyncCDNSupport;
	private AsyncDataWarehouseSupport asyncDataWarehouseSupport;
	private AsyncKeyValueDatabaseSupport asyncKeyValueDatabaseSupport;
	private AsyncMQSupport asyncMQSupport;
	private AsyncPushNotificationSupport asyncPushNotificationSupport;
	private AsyncRelationalDatabaseSupport asyncRelationalDatabaseSupport;
	private AsyncMonitoringSupport asyncMonitoringSupport;


	public AsyncPlatformServicesImpl(String configurationId, ServiceProvider inner, PlatformConfiguration configuration,
			DistributedThreadPool threadPools) throws ConcurrentException {
		this.inner = inner.getPlatformServices();
		if (this.inner.hasCDNSupport()) {
			this.asyncCDNSupport =
					Reflection.newProxy(AsyncCDNSupport.class, new AsyncHandler(configurationId, TaskType.CDNSupport, inner,
							threadPools, configuration.getcDNConfiguration()));
		}
		if (this.inner.hasDataWarehouseSupport()) {
			this.asyncDataWarehouseSupport =
					Reflection.newProxy(AsyncDataWarehouseSupport.class,
							new AsyncHandler(configurationId, TaskType.DataWarehouseSupport, inner, threadPools,
									configuration.getDataWarehouseConfiguration()));
		}
		if (this.inner.hasKeyValueDatabaseSupport()) {
			this.asyncKeyValueDatabaseSupport =
					Reflection.newProxy(AsyncKeyValueDatabaseSupport.class,
							new AsyncHandler(configurationId, TaskType.KeyValueDatabaseSupport, inner, threadPools,
									configuration.getKeyValueDatabaseConfiguration()));
		}
		if (this.inner.hasMessageQueueSupport()) {
			this.asyncMQSupport =
					Reflection.newProxy(AsyncMQSupport.class, new AsyncHandler(configurationId, TaskType.MQSupport, inner,
							threadPools, configuration.getMessageQueueConfiguration()));
		}
		if (this.inner.hasPushNotificationSupport()) {
			this.asyncPushNotificationSupport =
					Reflection.newProxy(AsyncPushNotificationSupport.class,
							new AsyncHandler(configurationId, TaskType.PushNotificationSupport, inner, threadPools,
									configuration.getPushNotificationConfiguration()));
		}
		if (this.inner.hasRelationalDatabaseSupport()) {
			this.asyncRelationalDatabaseSupport =
					Reflection.newProxy(AsyncRelationalDatabaseSupport.class,
							new AsyncHandler(configurationId, TaskType.RelationalDatabaseSupport, inner, threadPools,
									configuration.getRelationalDatabaseConfiguration()));
		}
		if (this.inner.hasMonitoringSupport()) {
			this.asyncMonitoringSupport =
					Reflection.newProxy(AsyncMonitoringSupport.class, new AsyncHandler(configurationId,
							TaskType.MonitoringSupport, inner, threadPools, configuration.getMonitoringConfiguration()));
		}
	}

	@Override
	public AsyncCDNSupport getCDNSupport() {
		return asyncCDNSupport;
	}

	@Override
	public AsyncDataWarehouseSupport getDataWarehouseSupport() {
		return asyncDataWarehouseSupport;
	}

	@Override
	public AsyncKeyValueDatabaseSupport getKeyValueDatabaseSupport() {
		return asyncKeyValueDatabaseSupport;
	}

	@Override
	public AsyncMQSupport getMessageQueueSupport() {
		return asyncMQSupport;
	}

	@Override
	public AsyncPushNotificationSupport getPushNotificationSupport() {
		return asyncPushNotificationSupport;
	}

	@Override
	public AsyncRelationalDatabaseSupport getRelationalDatabaseSupport() {
		return asyncRelationalDatabaseSupport;
	}

	@Override
	public AsyncMonitoringSupport getMonitoringSupport() {
		return asyncMonitoringSupport;
	}

	@Override
	public boolean hasCDNSupport() {
		return inner.hasCDNSupport();
	}

	@Override
	public boolean hasDataWarehouseSupport() {
		return inner.hasDataWarehouseSupport();
	}

	@Override
	public boolean hasKeyValueDatabaseSupport() {
		return inner.hasKeyValueDatabaseSupport();
	}

	@Override
	public boolean hasMessageQueueSupport() {
		return inner.hasMessageQueueSupport();
	}

	@Override
	public boolean hasPushNotificationSupport() {
		return inner.hasPushNotificationSupport();
	}

	@Override
	public boolean hasRelationalDatabaseSupport() {
		return inner.hasRelationalDatabaseSupport();
	}

	@Override
	public boolean hasMonitoringSupport() {
		return inner.hasMonitoringSupport();
	}

	@Override
	public void initialize() throws Exception {

	}

	@Override
	public void close() {

	}

}
