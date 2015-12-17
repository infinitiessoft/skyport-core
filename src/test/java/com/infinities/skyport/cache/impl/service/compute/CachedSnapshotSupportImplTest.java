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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotSupport;
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
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.service.compute.AsyncSnapshotSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedSnapshotSupport.CachedSnapshotListener;
import com.infinities.skyport.distributed.DistributedAtomicLong;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.proxy.SnapshotProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.snapshot.SnapshotFailureEvent;
import com.infinities.skyport.service.event.compute.snapshot.SnapshotRefreshedEvent;

public class CachedSnapshotSupportImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};

	private CachedSnapshotSupportImpl support;
	private AsyncSnapshotSupport inner;
	private SnapshotSupport original;
	private Configuration configuration;
	private QuartzServiceImpl<ComputeQuartzType> quartz;
	private DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private FirstLevelDispatcher dispatcher;
	private DistributedObjectFactory objectFactory;
	private DistributedCache<String, SnapshotProxy> cache;
	private ScheduledFuture<Iterable<Snapshot>> future;
	private List<Snapshot> snapshots;
	private Snapshot snapshot;
	private DistributedAtomicLong mockLong;
	private ConfigurationHome configurationHome;


	@SuppressWarnings({ "unchecked" })
	@Before
	public void setUp() throws Exception {
		configurationHome = context.mock(ConfigurationHome.class);
		inner = context.mock(AsyncSnapshotSupport.class);
		original = context.mock(SnapshotSupport.class);
		configuration = new Configuration();
		configuration.setId("id");
		configuration.setCloudName("cloudName");
		configuration.setStatus(true);
		configuration.getComputeConfiguration().getSnapshotConfiguration().getListSnapshots().getDelay().setNumber(1L);
		quartz = context.mock(QuartzServiceImpl.class);
		typeMap = context.mock(DistributedMap.class);
		dispatcher = context.mock(FirstLevelDispatcher.class);
		objectFactory = context.mock(DistributedObjectFactory.class);
		cache = context.mock(DistributedCache.class);
		future = context.mock(ScheduledFuture.class);
		mockLong = context.mock(DistributedAtomicLong.class);

		snapshots = new ArrayList<Snapshot>();
		snapshot = new Snapshot();
		snapshot.setProviderSnapshotId("providerSnapshotId");
		snapshot.setName("name");
		snapshots.add(snapshot);

		context.checking(new Expectations() {

			{
				exactly(1).of(configurationHome).addLifeCycleListener(with(any(CachedSnapshotSupportImpl.class)));
				exactly(1).of(inner).getSupport();
				will(returnValue(original));
				exactly(1).of(future).get();
				exactly(1).of(objectFactory).getCache(with(any(String.class)), with(any(IllegalStateException.class)));
				will(returnValue(cache));

			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(objectFactory).getAtomicLong("snapshot_providerSnapshotId");
				will(returnValue(mockLong));
				exactly(1).of(quartz).schedule(with(any(ComputeQuartzType.class)), with(any(QuartzConfiguration.class)));
				will(new CustomAction("check argument") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						ComputeQuartzType type = (ComputeQuartzType) invocation.getParameter(0);
						QuartzConfiguration<Iterable<Snapshot>> config =
								(QuartzConfiguration<Iterable<Snapshot>>) invocation.getParameter(1);
						Assert.assertEquals(ComputeQuartzType.Snapshot, type);
						Assert.assertEquals("Snapshot:" + configuration.getCloudName(), config.getName());
						Assert.assertEquals(CachedServiceProviderImpl.INITIAL_DELAY, config.getInitialDelay());
						Assert.assertEquals(configuration.getComputeConfiguration().getSnapshotConfiguration()
								.getListSnapshots().getDelay(), config.getTime());
						Assert.assertEquals(1, config.getCallbacks().size());
						FutureCallback<Iterable<Snapshot>> callback = config.getCallbacks().iterator().next();
						Callable<Iterable<Snapshot>> callable = config.getCallable();
						testCallback(callback);
						testCallable(callable);
						return future;
					}

				});
			}
		});

		support =
				new CachedSnapshotSupportImpl(configurationHome, inner, configuration, quartz, typeMap, dispatcher,
						objectFactory);
		support.initialize();
	}

	@After
	public void tearDown() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).destroy();
			}
		});
		support.close();
		context.assertIsSatisfied();
	}

	@Test
	public void testListSnapshots() throws InternalException, CloudException, InterruptedException, ExecutionException {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(snapshots));
			}
		});
		AsyncResult<Iterable<Snapshot>> result = support.listSnapshots();
		Iterable<Snapshot> iterable = result.get();
		Iterator<Snapshot> iterator = iterable.iterator();
		Assert.assertEquals(snapshot, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRefreshSnapshotCache() {
		context.checking(new Expectations() {

			{
				exactly(1).of(objectFactory).getAtomicLong("snapshot_providerSnapshotId");
				will(returnValue(mockLong));
				exactly(1).of(cache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, SnapshotProxy> snapshotMap = (Map<String, SnapshotProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, snapshotMap.size());
						SnapshotProxy proxy = snapshotMap.get(snapshot.getProviderSnapshotId());
						Assert.assertEquals(snapshot, proxy.getSnapshot());
						return null;
					}
				});
			}
		});
		support.refreshSnapshotCache(snapshots);
	}

	@Test
	public void testAddSnapshotListener() {
		final CachedSnapshotListener service = context.mock(CachedSnapshotListener.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).addListener(service);
				exactly(1).of(dispatcher).removeListener(service);

			}
		});
		support.addSnapshotListener(service);
	}

	@Test
	public void testRemoveSnapshotListener() {
		CachedSnapshotListener service = context.mock(CachedSnapshotListener.class);
		support.removeSnapshotListener(service);
	}

	private void testCallback(FutureCallback<Iterable<Snapshot>> callback) {
		testCallbackFail(callback);
		testCallbackSuccess(callback);

	}

	@SuppressWarnings("unchecked")
	private void testCallbackSuccess(FutureCallback<Iterable<Snapshot>> callback) {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(snapshots));
				exactly(1).of(cache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, SnapshotProxy> snapshotMap = (Map<String, SnapshotProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, snapshotMap.size());
						SnapshotProxy proxy = snapshotMap.get(snapshot.getProviderSnapshotId());
						Assert.assertEquals(snapshot, proxy.getSnapshot());
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).fireRefreshedEvent(with(any(SnapshotRefreshedEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						SnapshotRefreshedEvent event = (SnapshotRefreshedEvent) invocation.getParameter(0);
						Collection<Snapshot> collection = event.getNewEntries();
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(1, collection.size());
						Assert.assertEquals(snapshot, collection.iterator().next());
						return null;
					}

				});
			}
		});
		callback.onSuccess(snapshots);
	}

	private void testCallbackFail(FutureCallback<Iterable<Snapshot>> callback) {
		final Exception e = new Exception("exception on purpose");
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).reload(e);
				exactly(1).of(dispatcher).fireFaiureEvent(with(any(FailureEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						SnapshotFailureEvent event = (SnapshotFailureEvent) invocation.getParameter(0);
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(e, event.getThrowable());
						return null;
					}
				});
			}
		});
		callback.onFailure(e);
	}

	private void testCallable(Callable<Iterable<Snapshot>> callable) throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(original).listSnapshots();
				will(returnValue(snapshots));
			}
		});
		Iterable<Snapshot> iterable = callable.call();
		Iterator<Snapshot> iterator = iterable.iterator();
		Assert.assertEquals(snapshot, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}
}
