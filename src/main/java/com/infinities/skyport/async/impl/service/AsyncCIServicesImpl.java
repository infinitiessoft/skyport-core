package com.infinities.skyport.async.impl.service;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.ci.CIServices;

import com.google.common.reflect.Reflection;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.impl.AsyncHandler;
import com.infinities.skyport.async.service.AsyncCIServices;
import com.infinities.skyport.async.service.ci.AsyncConvergedHttpLoadBalancerSupport;
import com.infinities.skyport.async.service.ci.AsyncConvergedInfrastructureSupport;
import com.infinities.skyport.async.service.ci.AsyncTopologySupport;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.model.configuration.service.CIConfiguration;

public class AsyncCIServicesImpl implements AsyncCIServices {

	private final CIServices inner;
	private AsyncConvergedInfrastructureSupport asyncConvergedInfrastructureSupport;
	private AsyncConvergedHttpLoadBalancerSupport asyncConvergedHttpLoadBalancerSupport;
	private AsyncTopologySupport asyncTopologySupport;


	public AsyncCIServicesImpl(String configurationId, ServiceProvider inner, CIConfiguration configuration,
			DistributedThreadPool threadPools) throws ConcurrentException {
		this.inner = inner.getCIServices();
		if (this.inner.hasConvergedInfrastructureSupport()) {
			this.asyncConvergedInfrastructureSupport =
					Reflection.newProxy(AsyncConvergedInfrastructureSupport.class,
							new AsyncHandler(configurationId, TaskType.ConvergedInfrastructureSupport, inner, threadPools,
									configuration.getConvergedInfrastructureConfiguration()));
		}
		if (this.inner.hasConvergedHttpLoadBalancerSupport()) {
			this.asyncConvergedHttpLoadBalancerSupport =
					Reflection.newProxy(AsyncConvergedHttpLoadBalancerSupport.class,
							new AsyncHandler(configurationId, TaskType.ConvergedHttpLoadBalancerSupport, inner, threadPools,
									configuration.getConvergedHttpLoadBalancerConfiguration()));
		}
		if (this.inner.hasTopologySupport()) {
			this.asyncTopologySupport =
					Reflection.newProxy(AsyncTopologySupport.class, new AsyncHandler(configurationId,
							TaskType.TopologySupport, inner, threadPools, configuration.getTopologyConfiguration()));
		}
	}

	@Override
	public boolean hasConvergedInfrastructureSupport() {
		return inner.hasConvergedInfrastructureSupport();
	}

	@Override
	public AsyncConvergedInfrastructureSupport getConvergedInfrastructureSupport() {
		return asyncConvergedInfrastructureSupport;
	}

	@Override
	public boolean hasConvergedHttpLoadBalancerSupport() {
		return inner.hasConvergedHttpLoadBalancerSupport();
	}

	@Override
	public AsyncConvergedHttpLoadBalancerSupport getConvergedHttpLoadBalancerSupport() {
		return asyncConvergedHttpLoadBalancerSupport;
	}

	@Override
	public boolean hasTopologySupport() {
		return inner.hasTopologySupport();
	}

	@Override
	public AsyncTopologySupport getTopologySupport() {
		return asyncTopologySupport;
	}

	@Override
	public void initialize() throws Exception {

	}

	@Override
	public void close() {

	}

}
