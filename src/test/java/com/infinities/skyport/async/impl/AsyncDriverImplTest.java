//package com.infinities.skyport.async.impl;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
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
//import com.google.common.util.concurrent.ListeningExecutorService;
//import com.google.common.util.concurrent.ListeningScheduledExecutorService;
//import com.infinities.skyport.annotation.Cmd;
//import com.infinities.skyport.async.command.AsyncCommandFactory.CommandEnum;
//import com.infinities.skyport.compute.ComputeDriver;
//import com.infinities.skyport.compute.command.AddDiskCommand;
//import com.infinities.skyport.compute.command.AddNetworkAdapterCommand;
//import com.infinities.skyport.compute.command.CreateVmCommand;
//import com.infinities.skyport.compute.command.RemoveDiskCommand;
//import com.infinities.skyport.compute.command.RemoveNetworkAdapterCommand;
//import com.infinities.skyport.compute.command.RemoveVmCommand;
//import com.infinities.skyport.compute.command.RestartVmCommand;
//import com.infinities.skyport.compute.command.StartVmCommand;
//import com.infinities.skyport.compute.command.StopVmCommand;
//import com.infinities.skyport.compute.command.UpdateVmCommand;
//import com.infinities.skyport.compute.entity.Disk;
//import com.infinities.skyport.compute.entity.NetworkAdapter;
//import com.infinities.skyport.compute.entity.Vm;
//import com.infinities.skyport.distributed.DistributedAtomicLong;
//import com.infinities.skyport.distributed.DistributedObjectFactory.Delegate;
//import com.infinities.skyport.distributed.impl.local.LocalInstance;
//import com.infinities.skyport.distributed.util.DistributedUtil;
//import com.infinities.skyport.entity.Job;
//import com.infinities.skyport.entity.TaskEvent;
//import com.infinities.skyport.entity.TaskEventLog;
//import com.infinities.skyport.jpa.EntityManagerHelper;
//import com.infinities.skyport.model.AccessConfig;
//import com.infinities.skyport.model.AccessConfig.Delay;
//import com.infinities.skyport.model.AccessConfig.Timeout;
//import com.infinities.skyport.model.PoolConfig;
//import com.infinities.skyport.model.PoolSize;
//import com.infinities.skyport.proxy.VmProxy;
//import com.infinities.skyport.service.event.FirstLevelDispatcher;
//import com.infinities.skyport.service.jpa.ITaskEventHome;
//import com.infinities.skyport.service.jpa.ITaskEventLogHome;
//
//public class AsyncDriverImplTest {
//
//	protected AsyncDriverImpl asyncDriver;
//	protected Mockery context = new JUnit4Mockery() {
//
//		{
//			setThreadingPolicy(new Synchroniser());
//			setImposteriser(ClassImposteriser.INSTANCE);
//		}
//	};
//	protected ComputeDriver computeDriver;
//	protected AccessConfig accessconfig;
//	protected final String name = "name";
//	protected final String desc = "desc";
//	protected final String driver = "driver";
//	protected final String server = "server";
//	protected final String username = "username";
//	protected final String password = "password";
//	protected final String domain = "domain";
//	// protected String jks = "jks";
//	protected ListeningScheduledExecutorService scheduler;
//	protected ListeningExecutorService worker;
//	protected ScheduledFuture<?> future;
//	protected FirstLevelDispatcher dispatcher;
//	// protected TaskEvent event;
//	protected Vm vm;
//	protected DistributedAtomicLong atomicLong;
//	protected VmProxy proxy;
//	protected Map<String, VmProxy> proxys;
//	protected ITaskEventHome taskEventHomeService;
//	protected ITaskEventLogHome taskEventLogHomeService;
//	protected EntityManager entityManager;
//	protected EntityTransaction transaction;
//	protected TaskEvent taskEvent;
//	protected EntityManagerFactory factory;
//	protected Job job;
//
//
//	public void setDelegate() {
//		DistributedUtil.defaultDelegate = Delegate.disabled;
//	}
//
//	@Before
//	public void setUp() throws Exception {
//		setDelegate();
//		factory = context.mock(EntityManagerFactory.class);
//		entityManager = context.mock(EntityManager.class);
//		transaction = context.mock(EntityTransaction.class);
//		taskEventHomeService = context.mock(ITaskEventHome.class);
//		taskEventLogHomeService = context.mock(ITaskEventLogHome.class);
//		scheduler = context.mock(ListeningScheduledExecutorService.class);
//		worker = context.mock(ListeningExecutorService.class);
//		computeDriver = context.mock(ComputeDriver.class);
//		future = context.mock(ScheduledFuture.class);
//		dispatcher = context.mock(FirstLevelDispatcher.class);
//		accessconfig = new AccessConfig();
//		this.accessconfig.setDelay(new Delay(2));
//		this.accessconfig.setDescription(desc);
//		this.accessconfig.setDomain(domain);
//		this.accessconfig.setDriver(driver);
//		this.accessconfig.setId("0L");
//		// this.accessconfig.setJks(jks);
//		this.accessconfig.setLongPoolConfig(new PoolConfig(1, 1, 120, 1));
//		this.accessconfig.setMediumPoolConfig(new PoolConfig(1, 1, 120, 1));
//		this.accessconfig.setModifiedDate(new Date().toString());
//		this.accessconfig.setName(name);
//		this.accessconfig.setPassword(password);
//		this.accessconfig.setServer(server);
//		this.accessconfig.setShortPoolConfig(new PoolConfig(1, 1, 120, 1));
//		this.accessconfig.setStatus(true);
//		this.accessconfig.setTimeout(new Timeout(2));
//		this.accessconfig.setUsername(username);
//		// event = new TaskEvent();
//		job = new Job();
//		job.setId("0");
//
//		vm = new Vm();
//		vm.setVmid("vmid");
//		vm.setName("name");
//		vm.setConfigid("1L");
//		atomicLong = context.mock(DistributedAtomicLong.class);
//		proxy = new VmProxy(vm, atomicLong, "distributedKey");
//		proxys = new HashMap<String, VmProxy>();
//		proxys.put("vmid", proxy);
//
//		taskEvent = new TaskEvent();
//		taskEvent.setId(0L);
//
//		asyncDriver = new AsyncDriverImpl(computeDriver, accessconfig, scheduler, worker, dispatcher);
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
//		// asyncDriver.taskEventHomeService = taskEventHomeService;
//		// asyncDriver.taskEventLogHomeService = taskEventLogHomeService;
//
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		// EntityManagerHelper.commitAndClose();
//		// context.assertIsSatisfied();
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).close();
//				exactly(8).of(future).cancel(true);
//				will(returnValue(true));
//			}
//		});
//		asyncDriver.close();
//		EntityManagerHelper.threadLocal.remove();
//		EntityManagerHelper.factoryLocal.remove();
//		LocalInstance.instances.clear();
//	}
//
//	@Test
//	public void testClose() throws Exception {
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).close();
//				exactly(8).of(future).cancel(true);
//				will(returnValue(true));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.close();
//	}
//
//	@Test
//	public void testInitialize() throws Exception {
//		final List<Cmd> cmds = new ArrayList<Cmd>();
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).initialize();
//				// will(returnValue(driver));
//				oneOf(computeDriver).getCmds();
//				will(returnValue(cmds));
//				exactly(8).of(scheduler).schedule(with(any(Thread.class)), with(any(Long.class)), with(any(TimeUnit.class)));
//				will(returnValue(future));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		asyncDriver.initialize();
//		asyncDriver.refreshDate.set(1);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRefreshVmCacheWithoutInitialized() {
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		Set<Vm> vms = new HashSet<Vm>();
//		asyncDriver.refreshVmCache(vms);
//	}
//
//	@Test
//	public void testRefreshVmCache() throws Exception {
//		final List<String> keys = new ArrayList<String>();
//		keys.add("test1");
//		keys.add("test2");
//		taskEvent = new TaskEvent();
//
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
//		String key = DistributedUtil.generateDistributedName(driver, "test", server, username, password, domain);
//		final String key1 = key + "_" + PoolSize.LONG;
//		final String key2 = key + "_" + PoolSize.MEDIUM;
//		final String key3 = key + "_" + PoolSize.SHORT;
//
//		final Job job = new Job();
//		job.setId("123");
//		job.setArgs("test");
//		job.setCmd(CommandEnum.AddDisk.name());
//		job.setCreatedAt(1L);
//		job.setDistributedKey("key1");
//		job.setEventid(0L);
//		job.setExecutorKey("key2");
//		job.setStart(false);
//		job.setUuid("uuid");
//
//		final Job job2 = new Job();
//		job2.setId("1234");
//		job2.setArgs("test");
//		job2.setCmd(CommandEnum.AddDisk.name());
//		job2.setCreatedAt(1L);
//		job2.setDistributedKey("key1");
//		job2.setEventid(0L);
//		job2.setExecutorKey("key2");
//		job2.setStart(true);
//		job2.setUuid("uuid");
//		jobs.add(job2);
//
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(entityManager).getCriteriaBuilder();
//				will(returnValue(cb));
//				exactly(3).of(cb).createQuery(Job.class);
//				will(returnValue(cq));
//				exactly(3).of(cq).from(Job.class);
//				will(returnValue(root));
//				exactly(3).of(cq).select(root);
//				exactly(3).of(entityManager).createQuery(cq);
//				will(returnValue(q));
//				exactly(3).of(root).get("executorKey");
//				will(returnValue(path));
//				exactly(3).of(cb).equal(path, key1);
//				will(returnValue(predicate));
//				exactly(3).of(cb).equal(path, key2);
//				will(returnValue(predicate));
//				exactly(3).of(cb).equal(path, key3);
//				will(returnValue(predicate));
//				exactly(3).of(cq).where(predicate);
//				exactly(3).of(q).getResultList();
//				will(returnValue(jobs));
//				allowing(entityManager).find(Job.class, "123");
//				will(returnValue(job));
//				allowing(entityManager).find(Job.class, "1234");
//				will(returnValue(job2));
//			}
//		});
//		testInitialize();
//		Set<Vm> vms = new HashSet<Vm>();
//		asyncDriver.refreshVmCache(vms);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testUpdateVmAsyncWithoutInitialized() throws Exception {
//		asyncDriver.updateVmAsync(taskEvent, new Vm());
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testUpdateVmAsyncWithIllegalCache() throws Exception {
//		final UpdateVmCommand cmd = context.mock(UpdateVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).updateVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.updateVmAsync(taskEvent, vm);
//	}
//
//	@Test
//	public void testUpdateVmAsync() throws Exception {
//		final TaskEvent event = taskEvent;
//		final UpdateVmCommand cmd = context.mock(UpdateVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).updateVm();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(vm);
//				will(returnValue(vm));
//
//				oneOf(taskEventHomeService).persist(event);
//				oneOf(taskEventLogHomeService).persist(with(any(TaskEventLog.class)));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.updateVmAsync(event, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRemoveVmAsyncWithoutInitialized() throws Exception {
//		asyncDriver.removeVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRemoveVmAsyncWithIllegalCache() throws Exception {
//		final RemoveVmCommand cmd = context.mock(RemoveVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//
//			}
//		});
//		testInitialize();
//		asyncDriver.removeVmAsync(taskEvent, vm);
//	}
//
//	@Test
//	public void testRemoveVmAsync() throws Exception {
//		final RemoveVmCommand cmd = context.mock(RemoveVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.removeVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testStartVmAsyncWithoutInitialized() throws Exception {
//		asyncDriver.startVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testStartVmAsyncWithIllegalCache() throws Exception {
//		final StartVmCommand cmd = context.mock(StartVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).startVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.startVmAsync(taskEvent, vm);
//	}
//
//	@Test
//	public void testStartVmAsync() throws Exception {
//		final StartVmCommand cmd = context.mock(StartVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).startVm();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(vm);
//				will(returnValue(vm));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.startVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRestartVmAsyncWithoutInitialized() throws Exception {
//		asyncDriver.restartVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRestartVmAsyncWithIllegalCache() throws Exception {
//		final RestartVmCommand cmd = context.mock(RestartVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).restartVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.restartVmAsync(taskEvent, vm);
//	}
//
//	@Test
//	public void testRestartVmAsync() throws Exception {
//		final RestartVmCommand cmd = context.mock(RestartVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).restartVm();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(vm);
//				will(returnValue(vm));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.restartVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testStopVmAsyncWithoutInitialized() throws Exception {
//		asyncDriver.stopVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testStopVmAsyncWithIllegalCache() throws Exception {
//		final StopVmCommand cmd = context.mock(StopVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).stopVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//			}
//		});
//		testInitialize();
//		asyncDriver.stopVmAsync(taskEvent, vm);
//	}
//
//	@Test
//	public void testStopVmAsync() throws Exception {
//		final StopVmCommand cmd = context.mock(StopVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).stopVm();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(vm);
//				will(returnValue(vm));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.stopVmAsync(taskEvent, vm);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testAddDiskAsyncWithoutInitialized() throws Exception {
//		Disk disk = new Disk();
//		disk.setVmId("vmid");
//		asyncDriver.addDiskAsync(taskEvent, disk);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testAddDiskAsyncWithIllegalCache() throws Exception {
//		final AddDiskCommand cmd = context.mock(AddDiskCommand.class);
//		Disk disk = new Disk();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).addDisk();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.addDiskAsync(taskEvent, disk);
//	}
//
//	@Test
//	public void testAddDiskAsync() throws Exception {
//		final AddDiskCommand cmd = context.mock(AddDiskCommand.class);
//		final Disk disk = new Disk();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).addDisk();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(disk);
//				will(returnValue(disk));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.addDiskAsync(taskEvent, disk);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRemoveDiskAsyncWithoutInitialized() throws Exception {
//		Disk disk = new Disk();
//		disk.setVmId("vmid");
//		asyncDriver.removeDiskAsync(taskEvent, disk);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRemoveDiskAsyncWithIllegalCache() throws Exception {
//		final RemoveDiskCommand cmd = context.mock(RemoveDiskCommand.class);
//		Disk disk = new Disk();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeDisk();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.removeDiskAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Test
//	public void testRemoveDiskAsync() throws Exception {
//		final RemoveDiskCommand cmd = context.mock(RemoveDiskCommand.class);
//		final Disk disk = new Disk();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeDisk();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(disk);
//				will(returnValue(disk));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.removeDiskAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testAddNetworkAdapterAsyncWithoutInitialized() throws Exception {
//		NetworkAdapter nic = new NetworkAdapter();
//		nic.setVmId("vmid");
//		asyncDriver.addNetworkAdapterAsync(taskEvent, nic);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testAddNetworkAdapterAsyncWithIllegalCache() throws Exception {
//		final AddNetworkAdapterCommand cmd = context.mock(AddNetworkAdapterCommand.class);
//		NetworkAdapter disk = new NetworkAdapter();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).addNetworkAdapter();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.addNetworkAdapterAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Test
//	public void testAddNetworkAdapterAsync() throws Exception {
//		final AddNetworkAdapterCommand cmd = context.mock(AddNetworkAdapterCommand.class);
//		final NetworkAdapter disk = new NetworkAdapter();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).addNetworkAdapter();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(disk);
//				will(returnValue(disk));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.addNetworkAdapterAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRemoveNetworkAdapterAsyncWithoutInitialized() throws Exception {
//		NetworkAdapter nic = new NetworkAdapter();
//		nic.setVmId("vmid");
//		asyncDriver.removeNetworkAdapterAsync(taskEvent, nic);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testRemoveNetworkAdapterAsyncWithIllegalCache() throws Exception {
//		final RemoveNetworkAdapterCommand cmd = context.mock(RemoveNetworkAdapterCommand.class);
//		NetworkAdapter disk = new NetworkAdapter();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeNetworkAdapter();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.removeNetworkAdapterAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Test
//	public void testRemoveNetworkAdapterAsync() throws Exception {
//		final RemoveNetworkAdapterCommand cmd = context.mock(RemoveNetworkAdapterCommand.class);
//		final NetworkAdapter disk = new NetworkAdapter();
//		disk.setVmId("vmid");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeNetworkAdapter();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(disk);
//				will(returnValue(disk));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.removeNetworkAdapterAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testCreateVmAsyncWithoutInitialized() throws Exception {
//
//		asyncDriver.createVmAsync(taskEvent, vm);
//	}
//
//	@Test
//	public void testCreateVmAsync() throws Exception {
//		final CreateVmCommand cmd = context.mock(CreateVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).createVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.createVmAsync(taskEvent, vm);
//	}
//
//	@Test
//	public void testFlushCacheAsync() throws Exception {
//		context.checking(new Expectations() {
//
//			{
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.flushCacheAsync(taskEvent);
//	}
//
// }
