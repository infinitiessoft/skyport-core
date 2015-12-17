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
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import javax.persistence.EntityManager;
//import javax.persistence.EntityManagerFactory;
//import javax.persistence.EntityTransaction;
//import javax.persistence.TypedQuery;
//import javax.persistence.criteria.CriteriaBuilder;
//import javax.persistence.criteria.CriteriaQuery;
//import javax.persistence.criteria.Path;
//import javax.persistence.criteria.Predicate;
//import javax.persistence.criteria.Root;
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
//import com.infinities.skyport.async.AsyncVirtTask;
//import com.infinities.skyport.async.command.AsyncCommandFactory.CommandEnum;
//import com.infinities.skyport.cache.ICache;
//import com.infinities.skyport.compute.entity.Vm;
//import com.infinities.skyport.distributed.DistributedAtomicLong;
//import com.infinities.skyport.distributed.DistributedExecutor;
//import com.infinities.skyport.distributed.DistributedMap;
//import com.infinities.skyport.entity.Job;
//import com.infinities.skyport.entity.TaskEvent;
//import com.infinities.skyport.entity.TaskEventLog;
//import com.infinities.skyport.jpa.EntityManagerHelper;
//import com.infinities.skyport.proxy.VmProxy;
//
//public class TracedAsyncTaskStoreTest {
//
//	private TracedAsyncTaskStore taskStore;
//	protected Mockery context = new JUnit4Mockery() {
//
//		{
//			setThreadingPolicy(new Synchroniser());
//			setImposteriser(ClassImposteriser.INSTANCE);
//		}
//	};
//	private AsyncDriver driver;
//	private EntityManager entityManager;
//	private EntityTransaction transaction;
//	private TaskEvent taskEvent;
//	private EntityManagerFactory factory;
//	private Job job, job2, job3;
//	private Vm vm;
//	protected DistributedAtomicLong atomicLong;
//	private VmProxy proxy;
//	private Map<String, VmProxy> proxys;
//	private ICache<String, VmProxy> vmCache;
//
//
//	@SuppressWarnings("unchecked")
//	@Before
//	public void setUp() throws Exception {
//		vmCache = context.mock(ICache.class);
//		driver = context.mock(AsyncDriver.class);
//		factory = context.mock(EntityManagerFactory.class);
//		entityManager = context.mock(EntityManager.class);
//		transaction = context.mock(EntityTransaction.class);
//		taskStore = new TracedAsyncTaskStore(driver);
//		vm = new Vm();
//		vm.setVmid("vmid");
//		vm.setName("name");
//		atomicLong = context.mock(DistributedAtomicLong.class);
//		proxy = new VmProxy(vm, atomicLong, "distributedKey");
//		proxys = new HashMap<String, VmProxy>();
//		proxys.put("vmid", proxy);
//
//		job = new Job();
//		job.setId("123");
//		job.setArgs(vm);
//		job.setCmd(CommandEnum.AddDisk.name());
//		job.setCreatedAt(1L);
//		job.setDistributedKey("key1");
//		job.setEventid(0L);
//		job.setExecutorKey("key2");
//		job.setStart(false);
//		job.setUuid("vmid");
//
//		job2 = new Job();
//		job2.setId("1234");
//		job2.setArgs(vm);
//		job2.setCmd(CommandEnum.CreateVm.name());
//		job2.setCreatedAt(1L);
//		job2.setDistributedKey("key1");
//		job2.setEventid(0L);
//		job2.setExecutorKey("key2");
//		job2.setStart(true);
//		job2.setUuid("vmid2");
//
//		job3 = new Job();
//		job3.setId("12345");
//		job3.setArgs(true);
//		job3.setCmd(CommandEnum.FlushCache.name());
//		job3.setCreatedAt(1L);
//		job3.setDistributedKey("key1");
//		job3.setEventid(0L);
//		job3.setExecutorKey("key2");
//		job3.setStart(false);
//		job3.setUuid(null);
//		taskEvent = new TaskEvent();
//
//		EntityManagerHelper.threadLocal.set(entityManager);
//		EntityManagerHelper.factoryLocal.set(factory);
//		context.checking(new Expectations() {
//
//			{
//				allowing(entityManager).isOpen();
//				will(returnValue(true));
//				allowing(entityManager).getTransaction();
//				will(returnValue(transaction));
//				allowing(transaction).isActive();
//				will(returnValue(true));
//				allowing(transaction).commit();
//				allowing(entityManager).persist(taskEvent);
//				allowing(entityManager).persist(with(any(TaskEventLog.class)));
//				allowing(entityManager).merge(taskEvent);
//				will(returnValue(taskEvent));
//				allowing(entityManager).find(TaskEvent.class, 0L);
//				will(returnValue(taskEvent));
//				allowing(entityManager).persist(with(any(Job.class)));
//				allowing(entityManager).find(Job.class, "0");
//				will(returnValue(job));
//				allowing(entityManager).merge(with(any(Job.class)));
//				will(returnValue(job));
//				allowing(entityManager).remove(with(any(Job.class)));
//				allowing(entityManager).close();
//				allowing(factory).isOpen();
//				will(returnValue(true));
//				allowing(factory).createEntityManager();
//				will(returnValue(entityManager));
//			}
//		});
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		EntityManagerHelper.threadLocal.remove();
//		EntityManagerHelper.factoryLocal.remove();
//		context.assertIsSatisfied();
//	}
//
//	@Test
//	public void testLoadTerminated() {
//		context.checking(new Expectations() {
//
//			{
//				exactly(2).of(entityManager).find(Job.class, job.getId());
//				will(returnValue(job));
//				exactly(1).of(driver).getVmCache();
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(job.getUuid());
//				will(returnValue(proxy));
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(false));
//			}
//		});
//		AsyncVirtTask<Serializable, Serializable> task = taskStore.load(job.getId());
//		assertEquals(job.getId(), task.getJobId());
//		assertEquals(vm, task.getArgs());
//		assertEquals(job.getCmd(), task.getCmd().name());
//		assertEquals(job.getUuid(), task.getUuid());
//	}
//
//	@Test(expected = NullPointerException.class)
//	public void testLoadJobNotFound() {
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job.getId());
//				will(returnValue(null));
//			}
//		});
//		taskStore.load(job.getId());
//	}
//
//	@Test
//	public void testLoadCreateVmStarted() {
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job2.getId());
//				will(returnValue(job2));
//				// exactly(4).of(driver).getVmCache();
//				// will(returnValue(vmCache));
//				// oneOf(vmCache).tryLock(job2.getUuid());
//				// will(returnValue(true));
//				// oneOf(vmCache).get(job2.getUuid());
//				// will(returnValue(proxy));
//				// oneOf(vmCache).set(job2.getUuid(), proxy);
//				// oneOf(vmCache).unlock(job2.getUuid());
//			}
//		});
//		AsyncVirtTask<Serializable, Serializable> task = taskStore.load(job2.getId());
//		assertEquals(job2.getId(), task.getJobId());
//		assertEquals(vm, task.getArgs());
//		assertEquals(job2.getCmd(), task.getCmd().name());
//		assertEquals(job2.getUuid(), task.getUuid());
//	}
//
//	@Test
//	public void testLoadCreateVm() {
//		@SuppressWarnings("unchecked")
//		final DistributedMap<String, String> newvm = context.mock(DistributedMap.class);
//		job2.setStart(false);
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job2.getId());
//				will(returnValue(job2));
//				exactly(1).of(driver).getNewVm();
//				will(returnValue(newvm));
//				// exactly(4).of(driver).getVmCache();
//				// will(returnValue(vmCache));
//				// oneOf(vmCache).tryLock(job2.getUuid());
//				// will(returnValue(true));
//				// oneOf(vmCache).get(job2.getUuid());
//				// will(returnValue(proxy));
//				// oneOf(vmCache).set(job2.getUuid(), proxy);
//				// oneOf(vmCache).unlock(job2.getUuid());
//
//				oneOf(newvm).set(job2.getUuid(), vm.getName());
//			}
//		});
//		AsyncVirtTask<Serializable, Serializable> task = taskStore.load(job2.getId());
//		assertEquals(job2.getId(), task.getJobId());
//		assertEquals(vm, task.getArgs());
//		assertEquals(job2.getCmd(), task.getCmd().name());
//		assertEquals(job2.getUuid(), task.getUuid());
//	}
//
//	@Test
//	public void testLoadAddDisk() {
//		job.setStart(false);
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job.getId());
//				will(returnValue(job));
//				exactly(1).of(driver).getVmCache();
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(job.getUuid());
//				will(returnValue(proxy));
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		AsyncVirtTask<Serializable, Serializable> task = taskStore.load(job.getId());
//		assertEquals(job.getId(), task.getJobId());
//		assertEquals(vm, task.getArgs());
//		assertEquals(job.getCmd(), task.getCmd().name());
//		assertEquals(job.getUuid(), task.getUuid());
//	}
//
//	@Test(expected = IllegalArgumentException.class)
//	public void testLoadFlushCache() {
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job3.getId());
//				will(returnValue(job3));
//
//			}
//		});
//		AsyncVirtTask<Serializable, Serializable> task = taskStore.load(job3.getId());
//		assertEquals(job3.getId(), task.getJobId());
//		assertEquals(true, task.getArgs());
//		assertEquals(job3.getCmd(), task.getCmd().name());
//		assertEquals(job3.getUuid(), task.getUuid());
//	}
//
//	@Test
//	public void testLoadAllString() {
//		@SuppressWarnings("unchecked")
//		final DistributedMap<String, String> newvm = context.mock(DistributedMap.class);
//
//		job2.setStart(false);
//		final CriteriaBuilder cb = context.mock(CriteriaBuilder.class);
//		@SuppressWarnings("unchecked")
//		final CriteriaQuery<Job> cq = context.mock(CriteriaQuery.class);
//		@SuppressWarnings("unchecked")
//		final Root<Job> root = context.mock(Root.class);
//		@SuppressWarnings("unchecked")
//		final TypedQuery<Job> q = context.mock(TypedQuery.class);
//
//		final Path<?> path = context.mock(Path.class);
//		final Predicate predicate = context.mock(Predicate.class);
//
//		final List<Job> jobs = new ArrayList<Job>();
//		jobs.add(job);
//		jobs.add(job2);
//		jobs.add(job3);
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(entityManager).getCriteriaBuilder();
//				will(returnValue(cb));
//				oneOf(cb).createQuery(Job.class);
//				will(returnValue(cq));
//				oneOf(cq).from(Job.class);
//				will(returnValue(root));
//				oneOf(cq).select(root);
//				oneOf(entityManager).createQuery(cq);
//				will(returnValue(q));
//				oneOf(root).get("executorKey");
//				will(returnValue(path));
//				oneOf(cb).equal(path, "executorKey");
//				will(returnValue(predicate));
//				oneOf(cq).where(predicate);
//				oneOf(q).getResultList();
//				will(returnValue(jobs));
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(driver).getVmCache();
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(job.getUuid());
//				will(returnValue(proxy));
//				exactly(1).of(driver).getNewVm();
//				will(returnValue(newvm));
//				oneOf(newvm).set(job2.getUuid(), vm.getName());
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job3.getId());
//				will(returnValue(job3));
//			}
//		});
//		taskStore.loadAll("executorKey");
//	}
//
//	@Test
//	public void testLoadAllCollectionOfString() {
//		final Set<String> keys = new HashSet<String>();
//		keys.add(job.getId());
//		keys.add(job2.getId());
//
//		@SuppressWarnings("unchecked")
//		final DistributedMap<String, String> newvm = context.mock(DistributedMap.class);
//
//		job2.setStart(false);
//		final CriteriaBuilder cb = context.mock(CriteriaBuilder.class);
//		@SuppressWarnings("unchecked")
//		final CriteriaQuery<Job> cq = context.mock(CriteriaQuery.class);
//		@SuppressWarnings("unchecked")
//		final Root<Job> root = context.mock(Root.class);
//		@SuppressWarnings("unchecked")
//		final TypedQuery<Job> q = context.mock(TypedQuery.class);
//		final Predicate predicate = context.mock(Predicate.class);
//		@SuppressWarnings("unchecked")
//		final Path<String> param = context.mock(Path.class);
//
//		final List<Job> jobs = new ArrayList<Job>();
//		jobs.add(job);
//		jobs.add(job2);
//		jobs.add(job3);
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(entityManager).getCriteriaBuilder();
//				will(returnValue(cb));
//				oneOf(cb).createQuery(Job.class);
//				will(returnValue(cq));
//				oneOf(cq).from(Job.class);
//				will(returnValue(root));
//				oneOf(cq).select(root);
//				oneOf(entityManager).createQuery(cq);
//				will(returnValue(q));
//				oneOf(root).get("id");
//				will(returnValue(param));
//				oneOf(param).in(keys);
//				will(returnValue(predicate));
//				oneOf(cq).where(predicate);
//				oneOf(q).getResultList();
//				will(returnValue(jobs));
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(driver).getVmCache();
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(job.getUuid());
//				will(returnValue(proxy));
//				exactly(1).of(driver).getNewVm();
//				will(returnValue(newvm));
//				oneOf(newvm).set(job2.getUuid(), vm.getName());
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job3.getId());
//				will(returnValue(job3));
//			}
//		});
//		taskStore.loadAll(keys);
//	}
//
//	@Test
//	public void testDelete() {
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job.getId());
//				will(returnValue(job));
//			}
//		});
//		taskStore.delete(job.getId());
//	}
//
//	@Test
//	public void testDeleteAll() {
//		Set<String> set = new HashSet<String>();
//		set.add(job.getId());
//		set.add(job2.getId());
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job.getId());
//				will(returnValue(job));
//
//				exactly(1).of(entityManager).find(Job.class, job2.getId());
//				will(returnValue(job2));
//			}
//		});
//		taskStore.deleteAll(set);
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void testRecoverTask() {
//		final DistributedExecutor executor = context.mock(DistributedExecutor.class);
//		final DistributedMap<String, String> newvm = context.mock(DistributedMap.class);
//
//		job2.setStart(false);
//		final CriteriaBuilder cb = context.mock(CriteriaBuilder.class);
//		final CriteriaQuery<Job> cq = context.mock(CriteriaQuery.class);
//		final Root<Job> root = context.mock(Root.class);
//		final TypedQuery<Job> q = context.mock(TypedQuery.class);
//		final Path<?> path = context.mock(Path.class);
//		final Predicate predicate = context.mock(Predicate.class);
//		final List<Job> jobs = new ArrayList<Job>();
//		jobs.add(job);
//		jobs.add(job2);
//		jobs.add(job3);
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(entityManager).getCriteriaBuilder();
//				will(returnValue(cb));
//				oneOf(cb).createQuery(Job.class);
//				will(returnValue(cq));
//				oneOf(cq).from(Job.class);
//				will(returnValue(root));
//				oneOf(cq).select(root);
//				oneOf(entityManager).createQuery(cq);
//				will(returnValue(q));
//				oneOf(root).get("executorKey");
//				will(returnValue(path));
//				oneOf(cb).equal(path, "executorKey");
//				will(returnValue(predicate));
//				oneOf(cq).where(predicate);
//				oneOf(q).getResultList();
//				will(returnValue(jobs));
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(driver).getVmCache();
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(job.getUuid());
//				will(returnValue(proxy));
//				exactly(1).of(driver).getNewVm();
//				will(returnValue(newvm));
//				oneOf(newvm).set(job2.getUuid(), vm.getName());
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//
//				exactly(2).of(executor).submit(with(any(TracedAsyncVirtTask.class)));
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(entityManager).find(Job.class, job3.getId());
//				will(returnValue(job3));
//			}
//		});
//		taskStore.recoverTask("executorKey", executor);
//	}
//
// }
