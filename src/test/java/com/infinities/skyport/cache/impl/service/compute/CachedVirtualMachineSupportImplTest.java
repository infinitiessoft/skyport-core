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
package com.infinities.skyport.cache.impl.service.compute;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.service.compute.AsyncVirtualMachineSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport.CachedVirtualMachineListener;
import com.infinities.skyport.cache.service.compute.CachedVirtualMachineSupport.CachedVirtualMachineProductListener;
import com.infinities.skyport.distributed.DistributedAtomicLong;
import com.infinities.skyport.distributed.DistributedAtomicReference;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedCondition;
import com.infinities.skyport.distributed.DistributedLock;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.proxy.VirtualMachineProductProxy;
import com.infinities.skyport.proxy.VirtualMachineProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.virtualmachine.VirtualMachineFailureEvent;
import com.infinities.skyport.service.event.compute.virtualmachine.VirtualMachineRefreshedEvent;
import com.infinities.skyport.service.event.compute.virtualmachineproduct.VirtualMachineProductFailureEvent;
import com.infinities.skyport.service.event.compute.virtualmachineproduct.VirtualMachineProductRefreshedEvent;

public class CachedVirtualMachineSupportImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};

	private CachedVirtualMachineSupportImpl support;
	private AsyncVirtualMachineSupport inner;
	private VirtualMachineSupport original;
	private Configuration configuration;
	private FirstLevelDispatcher dispatcher;
	private DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private DistributedObjectFactory objectFactory;
	private QuartzServiceImpl<ComputeQuartzType> quartz;
	private DistributedCache<String, VirtualMachineProxy> cache;
	private ScheduledFuture<Iterable<VirtualMachine>> future;
	private List<VirtualMachineProxy> proxyList;
	private List<VirtualMachine> vms;
	private VirtualMachine vm;
	private ListenableFuture<VirtualMachine> result;
	private AsyncResult<VirtualMachine> asyncResult;

	private DistributedAtomicReference<Boolean> heartbeat;
	private DistributedLock lock;
	private DistributedCondition isVmsRefresh;
	private DistributedAtomicLong refreshDate;
	private DistributedMap<String, String> creatingVmMap;
	private DistributedAtomicLong mockLong;

	private DistributedCache<String, VirtualMachineProductProxy> productCache;
	private ScheduledFuture<Iterable<VirtualMachineProduct>> productFuture;
	private List<VirtualMachineProduct> products;
	private VirtualMachineProduct product;
	private ListenableFuture<Iterable<VirtualMachineProduct>> productResult;
	private AsyncResult<Iterable<VirtualMachineProduct>> asyncProductResult;
	private ConfigurationHome configurationHome;


	@SuppressWarnings({ "unchecked" })
	@Before
	public void setUp() throws InterruptedException, ExecutionException {
		configurationHome = context.mock(ConfigurationHome.class);
		inner = context.mock(AsyncVirtualMachineSupport.class);
		original = context.mock(VirtualMachineSupport.class);
		configuration = new Configuration();
		configuration.setId("id");
		configuration.setCloudName("cloudName");
		configuration.setStatus(true);
		configuration.getComputeConfiguration().getVirtualMachineConfiguration().getListVirtualMachines().getDelay()
				.setNumber(1L);
		quartz = context.mock(QuartzServiceImpl.class);
		typeMap = context.mock(DistributedMap.class, "typeMap");
		dispatcher = context.mock(FirstLevelDispatcher.class);
		objectFactory = context.mock(DistributedObjectFactory.class);
		cache = context.mock(DistributedCache.class, "vmCache");
		future = context.mock(ScheduledFuture.class, "future");

		heartbeat = context.mock(DistributedAtomicReference.class);
		lock = context.mock(DistributedLock.class);
		isVmsRefresh = context.mock(DistributedCondition.class);
		refreshDate = context.mock(DistributedAtomicLong.class);
		creatingVmMap = context.mock(DistributedMap.class, "creatingVmMap");
		mockLong = context.mock(DistributedAtomicLong.class, "mockLong");

		productCache = context.mock(DistributedCache.class, "vmProductCache");
		productFuture = context.mock(ScheduledFuture.class, "productFuture");

		vms = new ArrayList<VirtualMachine>();
		proxyList = new ArrayList<VirtualMachineProxy>();
		vm = new VirtualMachine();
		vm.setProviderVirtualMachineId("providerVirtualMachineId");
		vm.setName("name");
		vms.add(vm);

		result = context.mock(ListenableFuture.class, "result");
		asyncResult = new AsyncResult<VirtualMachine>(result);
		productResult = context.mock(ListenableFuture.class, "productResult");
		asyncProductResult = new AsyncResult<Iterable<VirtualMachineProduct>>(productResult);

		products = new ArrayList<VirtualMachineProduct>();
		product = new VirtualMachineProduct();
		product.setProviderProductId("providerProductId");
		product.setName("name");
		products.add(product);

		context.checking(new Expectations() {

			{
				exactly(1).of(configurationHome).addLifeCycleListener(with(any(CachedVirtualMachineSupportImpl.class)));
				exactly(1).of(inner).getSupport();
				will(returnValue(original));
				exactly(1).of(future).get();
				exactly(1).of(objectFactory).getCache(with(any(String.class)), with(any(IllegalStateException.class)));
				will(returnValue(cache));
				exactly(1).of(objectFactory).getAtomicReference(with(any(String.class)));
				will(returnValue(heartbeat));
				exactly(1).of(heartbeat).set(false);
				exactly(1).of(objectFactory).getMap(with(any(String.class)));
				will(returnValue(creatingVmMap));
				exactly(1).of(objectFactory).getLock(with(any(String.class)));
				will(returnValue(lock));
				exactly(1).of(lock).newCondition(with(any(String.class)));
				will(returnValue(isVmsRefresh));
				exactly(1).of(objectFactory).getAtomicLong(with(any(String.class)));
				will(returnValue(refreshDate));
			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(quartz).schedule(with(any(ComputeQuartzType.class)), with(any(QuartzConfiguration.class)));
				will(new CustomAction("check argument") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						ComputeQuartzType type = (ComputeQuartzType) invocation.getParameter(0);
						System.out.println("Type : " + type);
						QuartzConfiguration<Iterable<VirtualMachine>> config =
								(QuartzConfiguration<Iterable<VirtualMachine>>) invocation.getParameter(1);
						Assert.assertEquals(ComputeQuartzType.VirtualMachine, type);
						Assert.assertEquals("Vm:" + configuration.getCloudName(), config.getName());
						Assert.assertEquals(CachedServiceProviderImpl.INITIAL_DELAY, config.getInitialDelay());
						Assert.assertEquals(configuration.getComputeConfiguration().getVirtualMachineConfiguration()
								.getListVirtualMachines().getDelay(), config.getTime());
						Assert.assertEquals(1, config.getCallbacks().size());
						FutureCallback<Iterable<VirtualMachine>> callback = config.getCallbacks().iterator().next();
						Callable<Iterable<VirtualMachine>> callable = config.getCallable();
						testCallback(callback);
						testCallable(callable);
						return future;
					}
				});
			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(inner).getSupport();
				will(returnValue(original));
				exactly(1).of(productFuture).get();
				exactly(1).of(objectFactory).getCache(with(any(String.class)), with(any(IllegalStateException.class)));
				will(returnValue(productCache));
			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(quartz).schedule(with(any(ComputeQuartzType.class)), with(any(QuartzConfiguration.class)));
				will(new CustomAction("check argument") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						ComputeQuartzType type = (ComputeQuartzType) invocation.getParameter(0);
						System.out.println("Type : " + type);
						QuartzConfiguration<Iterable<VirtualMachineProduct>> config =
								(QuartzConfiguration<Iterable<VirtualMachineProduct>>) invocation.getParameter(1);
						Assert.assertEquals(ComputeQuartzType.VirtualMachineProduct, type);
						Assert.assertEquals("VmProduct:" + configuration.getCloudName(), config.getName());
						Assert.assertEquals(CachedServiceProviderImpl.INITIAL_DELAY, config.getInitialDelay());
						Assert.assertEquals(configuration.getComputeConfiguration().getVirtualMachineConfiguration()
								.getListAllProducts().getDelay(), config.getTime());
						Assert.assertEquals(1, config.getCallbacks().size());
						FutureCallback<Iterable<VirtualMachineProduct>> callback = config.getCallbacks().iterator().next();
						Callable<Iterable<VirtualMachineProduct>> callable = config.getCallable();
						testProductCallback(callback);
						testProductCallable(callable);
						return productFuture;
					}
				});
			}
		});

		support =
				new CachedVirtualMachineSupportImpl(configurationHome, inner, configuration, quartz, typeMap, dispatcher,
						objectFactory);
		support.initialize();
	}

	@After
	public void teartDown() {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).destroy();
				exactly(1).of(productCache).destroy();
			}
		});
		support.close();
		context.assertIsSatisfied();
	}

	@Test
	public void testAlterVirtualMachineProduct() throws InternalException, CloudException, InterruptedException,
			ExecutionException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).alterVirtualMachineProduct(with(any(String.class)), with(any(String.class)));
				will(returnValue(asyncResult));

				exactly(1).of(result).addListener(with(any(Runnable.class)), with(any(Executor.class)));
				exactly(1).of(result).get();
				will(returnValue(vm));
			}
		});

		AsyncResult<VirtualMachine> result =
				support.alterVirtualMachineProduct(vm.getProviderVirtualMachineId(), product.getProviderProductId());
		Assert.assertEquals(vm.getProductId(), result.get().getProductId());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAlterVirtualMachineSize() throws InternalException, CloudException, InterruptedException,
			ExecutionException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		final AsyncResult<VirtualMachine> ret = context.mock(AsyncResult.class, "VirtualMachine");
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).alterVirtualMachineSize(with(any(String.class)), with(any(String.class)),
						with(any(String.class)));
				will(returnValue(ret));
				exactly(1).of(ret).addListener(with(any(Runnable.class)), with(any(Executor.class)));
			}
		});

		AsyncResult<VirtualMachine> result = support.alterVirtualMachineSize(vm.getProviderVirtualMachineId(), "1", "512");
		assertEquals(ret, result);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAlterVirtualMachineFirewalls() throws InternalException, CloudException, InterruptedException,
			ExecutionException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		final String[] firewalls = { "firewalls" };
		final AsyncResult<VirtualMachine> ret = context.mock(AsyncResult.class, "VirtualMachine");
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).alterVirtualMachineFirewalls(with(any(String.class)), with(any(String[].class)));
				will(returnValue(ret));
				exactly(1).of(ret).addListener(with(any(Runnable.class)), with(any(Executor.class)));
			}
		});

		AsyncResult<VirtualMachine> result =
				support.alterVirtualMachineFirewalls(vm.getProviderVirtualMachineId(), firewalls);
		assertEquals(ret, result);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testClone() throws InternalException, CloudException, InterruptedException, ExecutionException {
		final AsyncResult<VirtualMachine> ret = context.mock(AsyncResult.class, "VirtualMachine");
		context.checking(new Expectations() {

			{
				exactly(1).of(creatingVmMap).set(with(any(String.class)), with(any(String.class)));
				exactly(1).of(inner).clone(with(any(String.class)), with(any(String.class)), with(any(String.class)),
						with(any(String.class)), with(any(Boolean.class)), with(any(String.class)));
				will(returnValue(ret));
				exactly(2).of(ret).addListener(with(any(Runnable.class)), with(any(Executor.class)));
			}
		});

		AsyncResult<VirtualMachine> result = support.clone("vmId", "intoDcId", "name", "description", true, "firewallIds");
		assertEquals(ret, result);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLaunch() throws CloudException, InternalException, InterruptedException, ExecutionException {
		final VMLaunchOptions options =
				VMLaunchOptions.getInstance("withStandardProductId", "usingMachineImageId", "havingFriendlyName",
						"withDescription");
		final AsyncResult<VirtualMachine> ret = context.mock(AsyncResult.class, "VirtualMachine");
		context.checking(new Expectations() {

			{
				exactly(1).of(creatingVmMap).set(with(any(String.class)), with(any(String.class)));
				exactly(1).of(inner).launch(with(any(VMLaunchOptions.class)));
				will(returnValue(ret));
				exactly(1).of(ret).addListener(with(any(Runnable.class)), with(any(Executor.class)));
			}
		});
		AsyncResult<VirtualMachine> result = support.launch(options);
		assertEquals(ret, result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLaunchMany() throws CloudException, InternalException, InterruptedException, ExecutionException {
		final VMLaunchOptions options =
				VMLaunchOptions.getInstance("withStandardProductId", "usingMachineImageId", "havingFriendlyName",
						"withDescription");
		final AsyncResult<Iterable<String>> ret = context.mock(AsyncResult.class, "Iterable<String>");
		context.checking(new Expectations() {

			{
				exactly(1).of(creatingVmMap).set(with(any(String.class)), with(any(String.class)));
				exactly(1).of(inner).launchMany(with(options), with(any(Integer.class)));
				will(returnValue(ret));

			}
		});
		AsyncResult<Iterable<String>> result = support.launchMany(options, 1);
		assertEquals(ret, result);
	}

	@Test
	public void testPause() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).pause(vm.getProviderVirtualMachineId());
			}
		});
		support.pause(vm.getProviderVirtualMachineId());
	}

	@Test
	public void testReboot() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).reboot(vm.getProviderVirtualMachineId());
			}
		});
		support.reboot(vm.getProviderVirtualMachineId());
	}

	@Test
	public void testResume() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).resume(vm.getProviderVirtualMachineId());
			}
		});
		support.resume(vm.getProviderVirtualMachineId());
	}

	@Test
	public void testStart() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).start(vm.getProviderVirtualMachineId());
			}
		});
		support.start(vm.getProviderVirtualMachineId());
	}

	@Test
	public void testStop() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).stop(vm.getProviderVirtualMachineId());
			}
		});
		support.stop(vm.getProviderVirtualMachineId());
	}

	@Test
	public void testStopWithForce() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).stop(with(vm.getProviderVirtualMachineId()), with(any(Boolean.class)));
			}
		});
		support.stop(vm.getProviderVirtualMachineId(), true);
	}

	@Test
	public void testSuspend() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).suspend(vm.getProviderVirtualMachineId());
			}
		});
		support.suspend(vm.getProviderVirtualMachineId());
	}

	@Test
	public void testTerminate() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).terminate(vm.getProviderVirtualMachineId());
			}
		});
		support.terminate(vm.getProviderVirtualMachineId());
	}

	@Test
	public void testTerminateWithExplanation() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).terminate(vm.getProviderVirtualMachineId(), "explanation");
			}
		});
		support.terminate(vm.getProviderVirtualMachineId(), "explanation");
	}

	@Test
	public void testUnpause() throws CloudException, InternalException {
		final VirtualMachineProxy proxy = context.mock(VirtualMachineProxy.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
				will(returnValue(proxy));
				exactly(1).of(proxy).lock();
				will(onConsecutiveCalls(returnValue(true),
						throwException(new IllegalStateException("vm is lock: " + vm.getProviderVirtualMachineId()))));
				exactly(1).of(inner).unpause(vm.getProviderVirtualMachineId());
			}
		});
		support.unpause(vm.getProviderVirtualMachineId());
	}

	// @Test
	// public void testAlterVirtualMachineProduct() throws InternalException,
	// CloudException, InterruptedException, ExecutionException {
	// final VirtualMachineProxy proxy =
	// context.mock(VirtualMachineProxy.class);
	// context.checking(new Expectations() {
	// {
	// exactly(1).of(cache).get(vm.getProviderVirtualMachineId());
	// will(returnValue(proxy));
	// exactly(1).of(proxy).lock();
	// will(onConsecutiveCalls(returnValue(true), throwException(new
	// IllegalStateException("vm is lock: " +
	// vm.getProviderVirtualMachineId()))));
	// exactly(1).of(inner).alterVirtualMachineProduct(with(any(String.class)),
	// with(any(String.class)));
	// will(returnValue(asyncResult));
	// }
	// });
	//
	// AsyncResult<VirtualMachine> result =
	// support.alterVirtualMachineProduct(vm.getProviderVirtualMachineId(),
	// product.getProviderProductId());
	// }

	// @Test
	// public void testLaunch() throws CloudException, InternalException,
	// InterruptedException, ExecutionException {
	// final VMLaunchOptions options =
	// VMLaunchOptions.getInstance("withStandardProductId",
	// "usingMachineImageId", "havingFriendlyName", "withDescription");
	// context.checking(new Expectations() {
	// {
	// exactly(1).of(creatingVmMap).set(with(any(String.class)),
	// with(options.getFriendlyName()));
	// exactly(1).of(support).launch(options);
	// will(returnValue(result));
	// }
	// });
	//
	// }

	@Test
	public void testListVirtualMachines() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(vms));
			}
		});
		AsyncResult<Iterable<VirtualMachine>> result = support.listVirtualMachines();
		Iterable<VirtualMachine> iterable = result.get();
		Iterator<VirtualMachine> iterator = iterable.iterator();
		Assert.assertEquals(vm, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void testListVirtualMachineProducts() throws InternalException, CloudException, InterruptedException,
			ExecutionException {
		context.checking(new Expectations() {

			{
				exactly(1).of(productCache).values();
				will(returnValue(products));
			}
		});
		AsyncResult<Iterable<VirtualMachineProduct>> result =
				support.listProducts(VirtualMachineProductFilterOptions.getInstance());
		Iterable<VirtualMachineProduct> iterable = result.get();
		Iterator<VirtualMachineProduct> iterator = iterable.iterator();
		Assert.assertEquals(product, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void testListVirtualMachineProductsWithOptions() throws InternalException, CloudException, InterruptedException,
			ExecutionException {
		final VirtualMachineProductFilterOptions options = VirtualMachineProductFilterOptions.getInstance(true);
		context.checking(new Expectations() {

			{
				exactly(1).of(inner).listProducts(options);
				will(returnValue(asyncProductResult));

				exactly(1).of(productResult).get();
				will(returnValue(products));
			}
		});
		AsyncResult<Iterable<VirtualMachineProduct>> result = support.listProducts(options);
		Iterable<VirtualMachineProduct> iterable = result.get();
		Iterator<VirtualMachineProduct> iterator = iterable.iterator();
		Assert.assertEquals(product, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void testAddVirtualMachineListener() throws InternalException, CloudException, InterruptedException,
			ExecutionException {
		final CachedVirtualMachineListener service = context.mock(CachedVirtualMachineListener.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).addListener(service);
				exactly(1).of(dispatcher).removeListener(service);

			}
		});
		support.addVirtualMachineListener(service);
	}

	@Test
	public void testRemoveVirtualMachineListener() {
		CachedVirtualMachineListener service = context.mock(CachedVirtualMachineListener.class);
		support.removeVirtualMachineListener(service);
	}

	@Test
	public void testAddVirtualMachineProductListener() throws InternalException, CloudException, InterruptedException,
			ExecutionException {
		final CachedVirtualMachineProductListener service = context.mock(CachedVirtualMachineProductListener.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).addListener(service);
				exactly(1).of(dispatcher).removeListener(service);

			}
		});
		support.addVirtualMachineProductListener(service);
	}

	@Test
	public void testRemoveVirtualMachineProductListener() {
		CachedVirtualMachineProductListener service = context.mock(CachedVirtualMachineProductListener.class);
		support.removeVirtualMachineProductListener(service);
	}

	private void testCallback(FutureCallback<Iterable<VirtualMachine>> callback) throws InterruptedException,
			ExecutionException {
		testCallbackFail(callback);
		testCallbackSuccess(callback);
	}

	private void testCallbackSuccess(FutureCallback<Iterable<VirtualMachine>> callback) throws InterruptedException,
			ExecutionException {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(proxyList));
				exactly(1).of(heartbeat).set(true);
				exactly(1).of(objectFactory).getAtomicLong("vm_providerVirtualMachineId");
				will(returnValue(mockLong));
				exactly(1).of(cache).set(with(any(String.class)), with(any(VirtualMachineProxy.class)));
				will(new CustomAction("check all event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						String id = (String) invocation.getParameter(0);
						VirtualMachineProxy proxy = (VirtualMachineProxy) invocation.getParameter(1);
						Assert.assertEquals(id, vm.getProviderVirtualMachineId());
						Assert.assertEquals(proxy.getVirtualMachine(), vm);
						return null;
					}
				});

				exactly(1).of(lock).lock();
				exactly(1).of(cache).refresh();
				exactly(1).of(refreshDate).set(with(any(Long.class)));
				exactly(1).of(isVmsRefresh).signalAll();
				exactly(1).of(lock).unlock();
			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(vms));
				exactly(1).of(dispatcher).fireRefreshedEvent(with(any(VirtualMachineRefreshedEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VirtualMachineRefreshedEvent event = (VirtualMachineRefreshedEvent) invocation.getParameter(0);
						Collection<VirtualMachine> collection = event.getNewEntries();
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(1, collection.size());
						Assert.assertEquals(vm, collection.iterator().next());
						return null;
					}

				});
			}
		});
		callback.onSuccess(vms);
	}

	private void testCallbackFail(FutureCallback<Iterable<VirtualMachine>> callback) {
		final Exception e = new Exception("exception on purpose");
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).reload(e);
				exactly(1).of(heartbeat).set(false);
				exactly(1).of(dispatcher).fireFaiureEvent(with(any(FailureEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VirtualMachineFailureEvent event = (VirtualMachineFailureEvent) invocation.getParameter(0);
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(e, event.getThrowable());
						return null;
					}
				});
			}
		});
		callback.onFailure(e);
	}

	private void testCallable(Callable<Iterable<VirtualMachine>> callable) throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(original).listVirtualMachines();
				will(returnValue(vms));
			}
		});
		Iterable<VirtualMachine> iterable = callable.call();
		Iterator<VirtualMachine> iterator = iterable.iterator();
		Assert.assertEquals(vm, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	private void testProductCallback(FutureCallback<Iterable<VirtualMachineProduct>> callback) throws InterruptedException,
			ExecutionException {
		testProductCallbackFail(callback);
		testProductCallbackSuccess(callback);
	}

	@SuppressWarnings("unchecked")
	private void testProductCallbackSuccess(FutureCallback<Iterable<VirtualMachineProduct>> callback)
			throws InterruptedException, ExecutionException {
		context.checking(new Expectations() {

			{
				exactly(1).of(productCache).values();
				will(returnValue(products));
				exactly(1).of(heartbeat).set(true);
				exactly(1).of(productCache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, VirtualMachineProductProxy> productMap =
								(Map<String, VirtualMachineProductProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, productMap.size());
						VirtualMachineProductProxy proxy = productMap.get(product.getProviderProductId());
						Assert.assertEquals(product, proxy.getProduct());
						return null;
					}

				});
			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).fireRefreshedEvent(with(any(VirtualMachineProductRefreshedEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VirtualMachineProductRefreshedEvent event =
								(VirtualMachineProductRefreshedEvent) invocation.getParameter(0);
						Collection<VirtualMachineProduct> collection = event.getNewEntries();
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(1, collection.size());
						Assert.assertEquals(product, collection.iterator().next());
						return null;
					}

				});
			}
		});

		callback.onSuccess(products);
	}

	private void testProductCallbackFail(FutureCallback<Iterable<VirtualMachineProduct>> callback) {
		final Exception e = new Exception("exception on purpose");
		context.checking(new Expectations() {

			{
				exactly(1).of(productCache).reload(e);
				exactly(1).of(dispatcher).fireFaiureEvent(with(any(FailureEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VirtualMachineProductFailureEvent event =
								(VirtualMachineProductFailureEvent) invocation.getParameter(0);
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(e, event.getThrowable());
						return null;
					}
				});
			}
		});
		callback.onFailure(e);
	}

	private void testProductCallable(Callable<Iterable<VirtualMachineProduct>> callable) throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(original).listAllProducts();
				will(returnValue(products));
			}
		});
		Iterable<VirtualMachineProduct> iterable = callable.call();
		Iterator<VirtualMachineProduct> iterator = iterable.iterator();
		Assert.assertEquals(product, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}
}
