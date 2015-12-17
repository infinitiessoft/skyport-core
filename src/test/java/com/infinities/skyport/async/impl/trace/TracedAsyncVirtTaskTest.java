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
//import com.infinities.skyport.compute.IVm;
//import com.infinities.skyport.compute.command.CreateVmCommand;
//import com.infinities.skyport.compute.entity.Vm;
//import com.infinities.skyport.distributed.DistributedAtomicLong;
//import com.infinities.skyport.distributed.DistributedCache;
//import com.infinities.skyport.distributed.DistributedCondition;
//import com.infinities.skyport.distributed.DistributedLock;
//import com.infinities.skyport.distributed.DistributedMap;
//import com.infinities.skyport.distributed.DistributedObjectFactory;
//import com.infinities.skyport.distributed.DistributedObjectFactory.Delegate;
//import com.infinities.skyport.distributed.impl.local.LocalInstance;
//import com.infinities.skyport.distributed.util.DistributedUtil;
//import com.infinities.skyport.entity.Job;
//import com.infinities.skyport.entity.TaskEvent;
//import com.infinities.skyport.entity.TaskEventLog;
//import com.infinities.skyport.jpa.EntityManagerHelper;
//import com.infinities.skyport.proxy.VmProxy;
//
//public class TracedAsyncVirtTaskTest {
//
//	protected Mockery context = new JUnit4Mockery() {
//
//		{
//			setThreadingPolicy(new Synchroniser());
//			setImposteriser(ClassImposteriser.INSTANCE);
//		}
//	};
//	private TracedAsyncVirtTask<Vm, Vm> task;
//	private TaskEvent event;
//	private String uuid;
//	private String key;
//	private String jobid;
//	private DistributedObjectFactory factory;
//	private DistributedAtomicLong vmRefreshDate;
//	private DistributedLock vmLock;
//	private DistributedCondition isVmsRefresh;
//	private DistributedMap<String, String> creatingVmMap;
//	private DistributedCache<String, VmProxy> vmCache;
//	private VmProxy proxy;
//	protected DistributedAtomicLong atomicLong;
//	private Vm vm;
//
//	private EntityManager entityManager;
//	private EntityTransaction transaction;
//	private EntityManagerFactory efactory;
//	private AsyncDriver driver;
//	private long time;
//	private Job job;
//
//
//	@SuppressWarnings("unchecked")
//	@Before
//	public void setUp() throws Exception {
//		driver = context.mock(AsyncDriver.class);
//		vm = new Vm();
//		event = new TaskEvent();
//		event.setId(1L);
//		uuid = UUID.randomUUID().toString();
//		key = UUID.randomUUID().toString();
//		jobid = UUID.randomUUID().toString();
//		time = System.currentTimeMillis();
//		task = new TracedAsyncVirtTask<Vm, Vm>(CommandEnum.CreateVm, vm, key, event.getEventid(), uuid, jobid, driver, time);
//		task.init();
//		factory = context.mock(DistributedObjectFactory.class);
//
//		vmRefreshDate = context.mock(DistributedAtomicLong.class, "vmRefreshDate");
//		vmLock = context.mock(DistributedLock.class);
//		isVmsRefresh = context.mock(DistributedCondition.class);
//		creatingVmMap = context.mock(DistributedMap.class);
//		vmCache = context.mock(DistributedCache.class);
//
//		Vm vm = new Vm();
//		vm.setVmid(uuid);
//		atomicLong = context.mock(DistributedAtomicLong.class, "vmLock");
//		proxy = new VmProxy(vm, atomicLong, "distributedKey");
//
//		job = new Job();
//		job.setId("0");
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
//		LocalInstance.instances.put(key, factory);
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
//		final CreateVmCommand command = new CreateVmCommand() {
//
//			@Override
//			public IVm execute(Vm args) throws Exception {
//				return vm;
//			}
//
//		};
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(factory).getAtomicLong("vmRefreshDate");
//				will(returnValue(vmRefreshDate));
//				oneOf(vmRefreshDate).get();
//				will(returnValue(1L));
//				oneOf(factory).getLock("vmLock");
//				will(returnValue(vmLock));
//				oneOf(vmLock).newCondition("isVmsRefresh");
//				will(returnValue(isVmsRefresh));
//				oneOf(vmLock).lock();
//				oneOf(vmRefreshDate).get();
//				will(returnValue(2L));
//				oneOf(vmLock).unlock();
//				oneOf(factory).getMap("creatingVmMap");
//				will(returnValue(creatingVmMap));
//				oneOf(creatingVmMap).lock(uuid);
//				oneOf(creatingVmMap).remove(uuid);
//				will(returnValue(null));
//				oneOf(creatingVmMap).unlock(uuid);
//				oneOf(factory).getCache("vmCache");
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(uuid);
//				will(returnValue(proxy)); // unlock
//				allowing(atomicLong).compareAndSet(1L, 0L);
//				will(returnValue(true));
//				exactly(2).of(isVmsRefresh).await();
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(driver).createVm();
//				will(returnValue(command));
//
//			}
//		});
//		Vm ret = task.call();
//		assertEquals(vm, ret);
//		// task.onSuccess("good");
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testOnFailure() throws Exception {
//		final CreateVmCommand command = new CreateVmCommand() {
//
//			@Override
//			public IVm execute(Vm args) throws Exception {
//				throw new IllegalStateException("test");
//			}
//
//		};
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(factory).getAtomicLong("vmRefreshDate");
//				will(returnValue(vmRefreshDate));
//				oneOf(vmRefreshDate).get();
//				will(returnValue(1L));
//				oneOf(factory).getLock("vmLock");
//				will(returnValue(vmLock));
//				oneOf(vmLock).newCondition("isVmsRefresh");
//				will(returnValue(isVmsRefresh));
//				oneOf(vmLock).lock();
//				oneOf(vmRefreshDate).get();
//				will(returnValue(2L));
//				oneOf(vmLock).unlock();
//				oneOf(factory).getMap("creatingVmMap");
//				will(returnValue(creatingVmMap));
//				oneOf(creatingVmMap).lock(uuid);
//				oneOf(creatingVmMap).remove(uuid);
//				will(returnValue(null));
//				oneOf(creatingVmMap).unlock(uuid);
//				oneOf(factory).getCache("vmCache");
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(uuid);
//				will(returnValue(proxy));
//				exactly(2).of(isVmsRefresh).await();
//				allowing(atomicLong).compareAndSet(1L, 0L);
//				will(returnValue(true));
//			}
//		});
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(driver).createVm();
//				will(returnValue(command));
//			}
//		});
//		task.call();
//	}
//
//	@Test
//	public void testLazyUnlockWithNotNew() throws InterruptedException {
//		context.checking(new Expectations() {
//
//			{
//				oneOf(factory).getAtomicLong("vmRefreshDate");
//				will(returnValue(vmRefreshDate));
//				oneOf(vmRefreshDate).get();
//				will(returnValue(1L));
//				oneOf(factory).getLock("vmLock");
//				will(returnValue(vmLock));
//				oneOf(vmLock).newCondition("isVmsRefresh");
//				will(returnValue(isVmsRefresh));
//				oneOf(vmLock).lock();
//				oneOf(vmRefreshDate).get();
//				will(returnValue(2L));
//				oneOf(vmLock).unlock();
//				oneOf(factory).getMap("creatingVmMap");
//				will(returnValue(creatingVmMap));
//				oneOf(creatingVmMap).lock(uuid);
//				oneOf(creatingVmMap).remove(uuid);
//				will(returnValue(null));
//				oneOf(creatingVmMap).unlock(uuid);
//				oneOf(factory).getCache("vmCache");
//				will(returnValue(vmCache));
//				oneOf(vmCache).get(uuid);
//				will(returnValue(proxy));
//				exactly(2).of(isVmsRefresh).await();
//				allowing(atomicLong).compareAndSet(1L, 0L);
//				will(returnValue(true));
//			}
//		});
//		TracedAsyncVirtTask.lazyUnlock(factory, uuid);
//	}
//
//	@Test
//	public void testLazyUnlockWithNew() throws InterruptedException {
//		context.checking(new Expectations() {
//
//			{
//				oneOf(factory).getAtomicLong("vmRefreshDate");
//				will(returnValue(vmRefreshDate));
//				oneOf(vmRefreshDate).get();
//				will(returnValue(1L));
//				oneOf(factory).getLock("vmLock");
//				will(returnValue(vmLock));
//				oneOf(vmLock).newCondition("isVmsRefresh");
//				will(returnValue(isVmsRefresh));
//				oneOf(vmLock).lock();
//				oneOf(vmRefreshDate).get();
//				will(returnValue(2L));
//				oneOf(vmLock).unlock();
//				oneOf(factory).getMap("creatingVmMap");
//				will(returnValue(creatingVmMap));
//				oneOf(creatingVmMap).lock(uuid);
//				oneOf(creatingVmMap).remove(uuid);
//				will(returnValue("newVm"));
//				oneOf(creatingVmMap).unlock(uuid);
//				oneOf(factory).getCache("vmCache");
//				will(returnValue(vmCache));
//				oneOf(vmCache).get("newVm");
//				will(returnValue(proxy));
//				exactly(2).of(isVmsRefresh).await();
//				allowing(atomicLong).compareAndSet(1L, 0L);
//				will(returnValue(true));
//			}
//		});
//		TracedAsyncVirtTask.lazyUnlock(factory, uuid);
//	}
//
// }
