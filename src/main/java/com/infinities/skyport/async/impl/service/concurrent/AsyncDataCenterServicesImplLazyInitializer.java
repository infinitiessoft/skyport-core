package com.infinities.skyport.async.impl.service.concurrent;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncDataCenterServices;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.DataCenterConfiguration;

public class AsyncDataCenterServicesImplLazyInitializer extends LazyInitializer<AsyncDataCenterServices> {

	private String configurationId;
	private ServiceProvider inner;
	private DataCenterConfiguration configuration;
	private DistributedThreadPool threadPools;


	public AsyncDataCenterServicesImplLazyInitializer(String configurationId, ServiceProvider inner,
			DataCenterConfiguration configuration, DistributedThreadPool threadPools) {
		super();
		this.inner = inner;
		this.configuration = configuration;
		this.configurationId = configurationId;
		this.threadPools = threadPools;
	}

	@Override
	protected AsyncDataCenterServices initialize() throws ConcurrentException {
		try {
			if (inner.getDataCenterServices() != null) {
				AsyncDataCenterServices services =
						Reflection.newProxy(AsyncDataCenterServices.class, new AsyncHandler(configurationId,
								TaskType.DataCenterServices, inner, threadPools, configuration));
				return services;
			}
		} catch (Exception e) {
			throw new ConcurrentException(e);
		}
		return null;
	}
}
