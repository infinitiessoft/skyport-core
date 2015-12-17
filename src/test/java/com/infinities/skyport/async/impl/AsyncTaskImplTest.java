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

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.entity.TaskEvent;
import com.infinities.skyport.entity.TaskEventLog;
import com.infinities.skyport.jpa.EntityManagerHelper;
import com.infinities.skyport.service.jpa.ITaskEventHome;
import com.infinities.skyport.service.jpa.ITaskEventLogHome;

public class AsyncTaskImplTest {

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
	private AsyncTaskImpl task;
	private ServiceProvider provider;
	private TaskType type;
	private String methodName = "alterVirtualMachineProduct";
	private String virtualMachineId = "virtualMachineId";
	private String productId = "productId";
	private String configurationId = "configurationId";
	private ComputeServices computeServices;
	private VirtualMachineSupport virtualMachineSupport;


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
		task = new AsyncTaskImpl();
		type = TaskType.VirtualMachineSupport;
		task.setServiceProvider(provider);
		task.setTaskType(type);
		task.setMethodName(methodName);
		task.setArgs(new Object[] { virtualMachineId, productId });
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testCall() throws Exception {
		final TaskEvent event = TaskEvent.getInitializedEvent(null, methodName, configurationId);
		event.setId(0L);
		EntityManagerHelper.threadLocal.set(entityManager);
		EntityManagerHelper.factoryLocal.set(factory);
		final VirtualMachine vm = new VirtualMachine();
		vm.setProviderVirtualMachineId(virtualMachineId);
		context.checking(new Expectations() {

			{
				exactly(9).of(entityManager).isOpen();
				will(returnValue(true));
				exactly(8).of(entityManager).getTransaction();
				will(returnValue(transaction));
				exactly(2).of(transaction).isActive();
				will(returnValue(true));
				exactly(2).of(transaction).commit();
				exactly(2).of(entityManager).close();
				exactly(1).of(factory).isOpen();
				will(returnValue(true));
				exactly(1).of(factory).createEntityManager();
				will(returnValue(entityManager));
				exactly(1).of(provider).getComputeServices();
				will(returnValue(computeServices));
				exactly(1).of(computeServices).getVirtualMachineSupport();
				will(returnValue(virtualMachineSupport));
				exactly(1).of(virtualMachineSupport).alterVirtualMachineProduct(virtualMachineId, productId);
				will(returnValue(vm));
				exactly(2).of(taskEventHome).findById(event.getId());
				will(returnValue(event));
				exactly(2).of(taskEventLogHome).persist(with(any(TaskEventLog.class)));
			}
		});
		task.taskEventHome = taskEventHome;
		task.taskEventLogHome = taskEventLogHome;

		VirtualMachine ret = (VirtualMachine) task.call();
		assertEquals(vm, ret);
	}
}
