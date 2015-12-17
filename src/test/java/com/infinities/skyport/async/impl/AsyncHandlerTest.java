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
package com.infinities.skyport.async.impl;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.service.compute.AsyncVirtualMachineSupport;
import com.infinities.skyport.distributed.DistributedExecutor;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.entity.TaskEvent;
import com.infinities.skyport.entity.TaskEventLog;
import com.infinities.skyport.entity.TaskEventLog.Status;
import com.infinities.skyport.jpa.EntityManagerHelper;
import com.infinities.skyport.model.FunctionConfiguration;
import com.infinities.skyport.model.PoolSize;
import com.infinities.skyport.service.jpa.ITaskEventHome;
import com.infinities.skyport.service.jpa.ITaskEventLogHome;

public class AsyncHandlerTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	protected ITaskEventHome taskEventHome;
	protected ITaskEventLogHome taskEventLogHome;
	protected EntityManager entityManager;
	protected EntityTransaction transaction;
	protected EntityManagerFactory factory;
	private AsyncHandler handler;
	private String configurationId;
	private TaskType taskType;
	private ServiceProvider provider;
	private ComputeServices computeServices;
	private VirtualMachineSupport virtualMachineSupport;
	private DistributedThreadPool pools;
	private DistributedExecutor executor;
	private Config config;

	private ListenableFuture<Object> future;
	private AsyncVirtualMachineSupport proxy;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		factory = context.mock(EntityManagerFactory.class);
		entityManager = context.mock(EntityManager.class);
		transaction = context.mock(EntityTransaction.class);
		taskEventHome = context.mock(ITaskEventHome.class);
		taskEventLogHome = context.mock(ITaskEventLogHome.class);
		provider = context.mock(ServiceProvider.class);
		computeServices = context.mock(ComputeServices.class);
		virtualMachineSupport = context.mock(VirtualMachineSupport.class);
		pools = context.mock(DistributedThreadPool.class);
		executor = context.mock(DistributedExecutor.class);
		future = context.mock(ListenableFuture.class);
		proxy = context.mock(AsyncVirtualMachineSupport.class);

		configurationId = "configurationId";
		taskType = TaskType.VirtualMachineSupport;
		config = new Config();
		handler = new AsyncHandler(configurationId, taskType, provider, pools, config);

		EntityManagerHelper.threadLocal.set(entityManager);
		EntityManagerHelper.factoryLocal.set(factory);

		handler.taskEventHome = taskEventHome;
		handler.taskEventLogHome = taskEventLogHome;

	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleInvocationObjectMethodObjectArray() throws Throwable {
		String methodName = "alterVirtualMachineProduct";
		final TaskEvent event = TaskEvent.getInitializedEvent(null, methodName, configurationId);
		final TaskEventLog log = new TaskEventLog(event, new Date(), Status.Initiazing, "Task is initialized", null);
		final String virtualMachineId = "virtualMachineId";
		final String productId = "productId";
		final VirtualMachine vm = new VirtualMachine();
		vm.setProviderVirtualMachineId(virtualMachineId);
		context.checking(new Expectations() {

			{
				exactly(5).of(entityManager).isOpen();
				will(returnValue(true));
				exactly(4).of(entityManager).getTransaction();
				will(returnValue(transaction));
				exactly(1).of(transaction).isActive();
				will(returnValue(true));
				exactly(1).of(transaction).commit();
				exactly(1).of(entityManager).close();
			}
		});
		context.checking(new Expectations() {

			{
				oneOf(provider).getComputeServices();
				will(returnValue(computeServices));

				oneOf(computeServices).getVirtualMachineSupport();
				will(returnValue(virtualMachineSupport));

				oneOf(taskEventHome).persist(event);

				oneOf(taskEventLogHome).persist(log);

				oneOf(pools).getThreadPool(PoolSize.SHORT);
				will(returnValue(executor));

				oneOf(executor).submit(with(any(AsyncTaskImpl.class)));
				will(returnValue(future));

				oneOf(future).get();
				will(returnValue(vm));

			}
		});

		Object proxy = new Object();
		Method method = AsyncVirtualMachineSupport.class.getMethod(methodName, String.class, String.class);
		Object[] args = new Object[] { virtualMachineId, productId };
		AsyncResult<VirtualMachine> ret = (AsyncResult<VirtualMachine>) handler.handleInvocation(proxy, method, args);
		assertEquals(vm, ret.get());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInvoke() throws Throwable {
		String methodName = "alterVirtualMachineProduct";
		final TaskEvent event = TaskEvent.getInitializedEvent(null, methodName, configurationId);
		final TaskEventLog log = new TaskEventLog(event, new Date(), Status.Initiazing, "Task is initialized", null);
		final String virtualMachineId = "virtualMachineId";
		final String productId = "productId";
		final VirtualMachine vm = new VirtualMachine();
		vm.setProviderVirtualMachineId(virtualMachineId);
		context.checking(new Expectations() {

			{
				exactly(5).of(entityManager).isOpen();
				will(returnValue(true));
				exactly(4).of(entityManager).getTransaction();
				will(returnValue(transaction));
				exactly(1).of(transaction).isActive();
				will(returnValue(true));
				exactly(1).of(transaction).commit();
				exactly(1).of(entityManager).close();
			}
		});
		context.checking(new Expectations() {

			{
				oneOf(provider).getComputeServices();
				will(returnValue(computeServices));

				oneOf(computeServices).getVirtualMachineSupport();
				will(returnValue(virtualMachineSupport));

				oneOf(taskEventHome).persist(event);

				oneOf(taskEventLogHome).persist(log);

				oneOf(pools).getThreadPool(PoolSize.SHORT);
				will(returnValue(executor));

				oneOf(executor).submit(with(any(AsyncTaskImpl.class)));
				will(returnValue(future));

				oneOf(future).get();
				will(returnValue(vm));

			}
		});

		Method method = AsyncVirtualMachineSupport.class.getMethod(methodName, String.class, String.class);
		Object[] args = new Object[] { virtualMachineId, productId };
		AsyncResult<VirtualMachine> ret = (AsyncResult<VirtualMachine>) handler.invoke(proxy, method, args);
		assertEquals(vm, ret.get());
	}

	@Test
	public void testGetSupport() throws Throwable {
		String methodName = "getSupport";
		context.checking(new Expectations() {

			{
				oneOf(provider).getComputeServices();
				will(returnValue(computeServices));

				oneOf(computeServices).getVirtualMachineSupport();
				will(returnValue(virtualMachineSupport));

			}
		});

		Method method = AsyncVirtualMachineSupport.class.getMethod(methodName);
		Object[] args = new Object[] {};
		VirtualMachineSupport ret = (VirtualMachineSupport) handler.handleInvocation(proxy, method, args);
		assertEquals(virtualMachineSupport, ret);
	}


	public class Config {

		private FunctionConfiguration alterVirtualMachineProduct = new FunctionConfiguration();


		public FunctionConfiguration getAlterVirtualMachineProduct() {
			return alterVirtualMachineProduct;
		}

		public void setAlterVirtualMachineProduct(FunctionConfiguration alterVirtualMachineProduct) {
			this.alterVirtualMachineProduct = alterVirtualMachineProduct;
		}

	}

}
