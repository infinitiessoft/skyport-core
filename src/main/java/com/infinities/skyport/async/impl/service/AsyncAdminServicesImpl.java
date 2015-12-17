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
import org.dasein.cloud.admin.AdminServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncAdminServices;
import com.infinities.skyport.async.service.admin.AsyncAccountSupport;
import com.infinities.skyport.async.service.admin.AsyncBillingSupport;
import com.infinities.skyport.async.service.admin.AsyncPrepaymentSupport;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.AdminConfiguration;

public class AsyncAdminServicesImpl implements AsyncAdminServices {

	private final Logger logger = LoggerFactory.getLogger(AsyncAdminServicesImpl.class);
	private final ServiceProvider inner;
	private AsyncPrepaymentSupport asyncPrepaymentSupport;
	private AsyncAccountSupport asyncAccountSupport;
	private AsyncBillingSupport asyncBillingSupport;

	private AdminConfiguration configuration;
	private String configurationId;
	private DistributedThreadPool threadPools;


	public AsyncAdminServicesImpl(String configurationId, ServiceProvider inner, AdminConfiguration configuration,
			DistributedThreadPool threadPools) {
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	public AsyncPrepaymentSupport getPrepaymentSupport() {
		return asyncPrepaymentSupport;
	}

	@Override
	public boolean hasPrepaymentSupport() {
		try {
			return inner.getAdminServices().hasPrepaymentSupport();
		} catch (ConcurrentException e) {
			logger.warn("get AdminService failed", e);
			return false;
		}
	}

	@Override
	public AsyncAccountSupport getAccountSupport() {
		return asyncAccountSupport;
	}

	@Override
	public boolean hasAccountSupport() {
		try {
			return inner.getAdminServices().hasAccountSupport();
		} catch (ConcurrentException e) {
			logger.warn("get AdminService failed", e);
			return false;
		}
	}

	@Override
	public AsyncBillingSupport getBillingSupport() {
		return asyncBillingSupport;
	}

	@Override
	public boolean hasBillingSupport() {
		try {
			return inner.getAdminServices().hasBillingSupport();
		} catch (ConcurrentException e) {
			logger.warn("get AdminService failed", e);
			return false;
		}
	}

	@Override
	public void initialize() throws Exception {
		AdminServices admin = inner.getAdminServices();
		if (admin.hasPrepaymentSupport()) {
			this.asyncPrepaymentSupport =
					Reflection.newProxy(AsyncPrepaymentSupport.class, new AsyncHandler(configurationId,
							TaskType.PrepaymentSupport, inner, threadPools, configuration.getPrepaymentConfiguration()));
		}
		if (admin.hasAccountSupport()) {
			this.asyncAccountSupport =
					Reflection.newProxy(AsyncAccountSupport.class, new AsyncHandler(configurationId,
							TaskType.AccountSupport, inner, threadPools, configuration.getAccountConfiguration()));
		}
		if (admin.hasBillingSupport()) {
			this.asyncBillingSupport =
					Reflection.newProxy(AsyncBillingSupport.class, new AsyncHandler(configurationId,
							TaskType.BillingSupport, inner, threadPools, configuration.getBillingConfiguration()));
		}
	}

	@Override
	public void close() {

	}

}
