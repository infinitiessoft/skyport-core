//package com.infinities.skyport.async.impl;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import org.jmock.Expectations;
//import org.junit.Test;
//
//import com.infinities.skyport.annotation.Cmd;
//import com.infinities.skyport.compute.command.AddDiskCommand;
//import com.infinities.skyport.compute.command.AddNetworkAdapterCommand;
//import com.infinities.skyport.compute.command.RemoveDiskCommand;
//import com.infinities.skyport.compute.command.RemoveNetworkAdapterCommand;
//import com.infinities.skyport.compute.command.RemoveVmCommand;
//import com.infinities.skyport.compute.command.RestartVmCommand;
//import com.infinities.skyport.compute.command.StartVmCommand;
//import com.infinities.skyport.compute.command.StopVmCommand;
//import com.infinities.skyport.compute.command.UpdateVmCommand;
//import com.infinities.skyport.compute.entity.Disk;
//import com.infinities.skyport.compute.entity.NetworkAdapter;
//import com.infinities.skyport.distributed.DistributedObjectFactory.Delegate;
//import com.infinities.skyport.distributed.util.DistributedUtil;
//import com.infinities.skyport.entity.TaskEvent;
//import com.infinities.skyport.entity.TaskEventLog;
//
//public class AsyncDriverImplByHazelcastTest extends AsyncDriverImplTest {
//
//	@Override
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		asyncDriver.initialize();
//		asyncDriver.refreshDate.set(1);
//	}
//
//	@Override
//	@Test
//	public void testRefreshVmCache() throws Exception {
//		context.checking(new Expectations() {
//
//			{
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		super.testRefreshVmCache();
//	}
//
//	@Override
//	@Test(expected = IllegalStateException.class)
//	public void testUpdateVmAsyncWithIllegalCache() throws Exception {
//		context.checking(new Expectations() {
//
//			{
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		super.testUpdateVmAsyncWithIllegalCache();
//	}
//
//	@Override
//	@Test
//	public void testUpdateVmAsync() throws Exception {
//		vm.setVmid("vmid1");
//		proxys.put(vm.getVmid(), proxy);
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
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
//	@Override
//	@Test(expected = IllegalStateException.class)
//	public void testRemoveVmAsyncWithIllegalCache() throws Exception {
//		final RemoveVmCommand cmd = context.mock(RemoveVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.removeVmAsync(taskEvent, vm);
//	}
//
//	@Override
//	@Test
//	public void testRemoveVmAsync() throws Exception {
//		vm.setVmid("vmid2");
//		proxys.put(vm.getVmid(), proxy);
//		final RemoveVmCommand cmd = context.mock(RemoveVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
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
//	@Override
//	@Test(expected = IllegalStateException.class)
//	public void testStartVmAsyncWithIllegalCache() throws Exception {
//		final StartVmCommand cmd = context.mock(StartVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).startVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.startVmAsync(taskEvent, vm);
//	}
//
//	@Override
//	@Test
//	public void testStartVmAsync() throws Exception {
//		vm.setVmid("vmid3");
//		proxys.put(vm.getVmid(), proxy);
//		final StartVmCommand cmd = context.mock(StartVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).startVm();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(vm);
//				will(returnValue(vm));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//				
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.startVmAsync(taskEvent, vm);
//	}
//	
//	@Override
//	@Test(expected = IllegalStateException.class)
//	public void testRestartVmAsyncWithIllegalCache() throws Exception {
//		final RestartVmCommand cmd = context.mock(RestartVmCommand.class);
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).restartVm();
//				will(returnValue(cmd));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.restartVmAsync(taskEvent, vm);
//	}
//	
//	@Override
//	@Test
//	public void testRestartVmAsync() throws Exception {
//		vm.setVmid("vmid4");
//		proxys.put(vm.getVmid(), proxy);
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//				
//				allowing(atomicLong).compareAndSet(0L, 1L);
//				will(returnValue(true));
//			}
//		});
//		testInitialize();
//		asyncDriver.getVmCache().reload(proxys);
//		asyncDriver.restartVmAsync(taskEvent, vm);
//	}
//
//	@Override
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//
//			}
//		});
//		testInitialize();
//		asyncDriver.stopVmAsync(taskEvent, vm);
//	}
//
//	@Override
//	@Test
//	public void testStopVmAsync() throws Exception {
//		vm.setVmid("vmid5");
//		proxys.put(vm.getVmid(), proxy);
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
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
//	@Override
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.addDiskAsync(taskEvent, disk);
//	}
//
//	@Override
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.removeDiskAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Override
//	@Test
//	public void testRemoveDiskAsync() throws Exception {
//		vm.setVmid("vmid6");
//		proxys.put(vm.getVmid(), proxy);
//		final RemoveDiskCommand cmd = context.mock(RemoveDiskCommand.class);
//		final Disk disk = new Disk();
//		disk.setVmId("vmid6");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeDisk();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(disk);
//				will(returnValue(disk));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
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
//	@Override
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.addNetworkAdapterAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Override
//	@Test
//	public void testAddNetworkAdapterAsync() throws Exception {
//		vm.setVmid("vmid7");
//		proxys.put(vm.getVmid(), proxy);
//		final AddNetworkAdapterCommand cmd = context.mock(AddNetworkAdapterCommand.class);
//		final NetworkAdapter disk = new NetworkAdapter();
//		disk.setVmId("vmid7");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).addNetworkAdapter();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(disk);
//				will(returnValue(disk));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
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
//	@Override
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
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
//			}
//		});
//		testInitialize();
//		asyncDriver.removeNetworkAdapterAsync(taskEvent, disk);
//		// EntityManagerHelper.commitAndClose();
//	}
//
//	@Override
//	@Test
//	public void testRemoveNetworkAdapterAsync() throws Exception {
//		vm.setVmid("vmid8");
//		proxys.put(vm.getVmid(), proxy);
//		final RemoveNetworkAdapterCommand cmd = context.mock(RemoveNetworkAdapterCommand.class);
//		final NetworkAdapter disk = new NetworkAdapter();
//		disk.setVmId("vmid8");
//		context.checking(new Expectations() {
//
//			{
//				oneOf(computeDriver).removeNetworkAdapter();
//				will(returnValue(cmd));
//
//				oneOf(cmd).execute(disk);
//				will(returnValue(disk));
//				exactly(1).of(worker).execute(with(any(Runnable.class)));
//				exactly(3).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)),
//						with(any(TimeUnit.class)));
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
//	@Override
//	public void setDelegate() {
//		DistributedUtil.defaultDelegate = Delegate.hazelcast;
//	}
// }
