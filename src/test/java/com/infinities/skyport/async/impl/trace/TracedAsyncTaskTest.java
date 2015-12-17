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
//package com.infinities.skyport.async.impl.trace;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.UUID;
//
//import javax.persistence.EntityManager;
//import javax.persistence.EntityManagerFactory;
//import javax.persistence.EntityTransaction;
//
//import org.jmock.Expectations;
//import org.jmock.Mockery;
//import org.jmock.integration.junit4.JUnit4Mockery;
//import org.jmock.lib.concurrent.Synchroniser;
//import org.jmock.lib.legacy.ClassImposteriser;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import com.infinities.skyport.async.AsyncDriver;
//import com.infinities.skyport.async.command.AsyncCommandFactory.CommandEnum;
//import com.infinities.skyport.distributed.DistributedObjectFactory.Delegate;
//import com.infinities.skyport.distributed.util.DistributedUtil;
//import com.infinities.skyport.entity.Job;
//import com.infinities.skyport.entity.TaskEvent;
//import com.infinities.skyport.entity.TaskEventLog;
//import com.infinities.skyport.entity.TaskEventLog.Status;
//import com.infinities.skyport.jpa.EntityManagerHelper;
//
//public class TracedAsyncTaskTest {
//
//	protected Mockery context = new JUnit4Mockery() {
//
//		{
//			setThreadingPolicy(new Synchroniser());
//			setImposteriser(ClassImposteriser.INSTANCE);
//		}
//	};
//	private TracedAsyncTask<Boolean> task;
//	private TaskEvent event;
//	private String key;
//	private String jobid;
//	private EntityManager entityManager;
//	private EntityTransaction transaction;
//	private EntityManagerFactory efactory;
//	private AsyncDriver driver;
//	private long time;
//	private Job job;
//
//
//	@Before
//	public void setUp() throws Exception {
//		driver = context.mock(AsyncDriver.class);
//		event = new TaskEvent();
//		event.setId(1L);
//		key = UUID.randomUUID().toString();
//		jobid = UUID.randomUUID().toString();
//		time = System.currentTimeMillis();
//		task = new TracedAsyncTask<Boolean>(CommandEnum.FlushCache, key, event.getEventid(), jobid, driver, time);
//		task.init();
//
//		job = new Job();
//		job.setId(jobid);
//
//		efactory = context.mock(EntityManagerFactory.class);
//		entityManager = context.mock(EntityManager.class);
//		transaction = context.mock(EntityTransaction.class);
//		EntityManagerHelper.threadLocal.set(entityManager);
//		EntityManagerHelper.factoryLocal.set(efactory);
//		context.checking(new Expectations() {
//
//			{
//				allowing(efactory).isOpen();
//				will(returnValue(true));
//				allowing(efactory).createEntityManager();
//				will(returnValue(entityManager));
//				allowing(entityManager).isOpen();
//				will(returnValue(true));
//				allowing(entityManager).getTransaction();
//				will(returnValue(transaction));
//				allowing(transaction).isActive();
//				will(returnValue(true));
//				allowing(transaction).commit();
//				allowing(entityManager).persist(with(any(TaskEvent.class)));
//				allowing(entityManager).persist(with(any(TaskEventLog.class)));
//				allowing(entityManager).merge(with(any(TaskEvent.class)));
//				will(returnValue(event));
//				allowing(entityManager).find(TaskEvent.class, 1L);
//				will(returnValue(event));
//				allowing(entityManager).close();
//				allowing(entityManager).find(Job.class, jobid);
//				will(returnValue(job));
//				allowing(entityManager).merge(with(any(Job.class)));
//				will(returnValue(job));
//				allowing(entityManager).remove(with(any(Job.class)));
//			}
//		});
//		DistributedUtil.defaultDelegate = Delegate.disabled;
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		context.assertIsSatisfied();
//		EntityManagerHelper.threadLocal.remove();
//		EntityManagerHelper.factoryLocal.remove();
//	}
//
//	@Test
//	public void testOnSuccess() throws Exception {
//		context.checking(new Expectations() {
//
//			{
//				oneOf(driver).flushCache();
//
//			}
//		});
//
//		boolean ret = task.call();
//		assertEquals(true, ret);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testOnFail() throws Exception {
//		context.checking(new Expectations() {
//
//			{
//				oneOf(driver).flushCache();
//				will(throwException(new IllegalStateException("you cannot withdraw nothing!")));
//
//			}
//		});
//
//		boolean ret = task.call();
//		assertEquals(true, ret);
//	}
//
//	@Test
//	public void testGetCmd() {
//		assertEquals(CommandEnum.FlushCache, task.getCmd());
//	}
//
//	@Test
//	public void testGetEventid() {
//		assertEquals(event.getId().longValue(), task.getEventid());
//	}
//
//	@Test
//	public void testTerminate() {
//		task.init();
//		task.terminate();
//	}
//
//	@Test
//	public void testRemoveJob() {
//		task.init();
//		task.removeJob();
//	}
//
//	@Test
//	public void testInit() {
//		task.init();
//	}
//
//	@Test
//	public void testGetJobId() {
//		assertEquals(job.getId(), task.getJobId());
//	}
//
//	@Test
//	public void testGetCreatedAt() {
//		assertEquals(time, task.getCreatedAt());
//	}
//
//	@Test
//	public void testIsStarted() {
//		assertEquals(job.isStart(), task.isStarted());
//	}
//
//	@Test
//	public void testCommitLog() {
//		task.commitLog(Status.Success, event.getId(), "msg", "detail");
//	}
// }
