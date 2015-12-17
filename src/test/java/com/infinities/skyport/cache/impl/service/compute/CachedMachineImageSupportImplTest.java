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
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageSupport;
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
import com.infinities.skyport.async.service.compute.AsyncMachineImageSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedMachineImageSupport.CachedMachineImageListener;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.proxy.MachineImageProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzService;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.machineimage.MachineImageFailureEvent;
import com.infinities.skyport.service.event.compute.machineimage.MachineImageRefreshedEvent;

public class CachedMachineImageSupportImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private CachedMachineImageSupportImpl support;
	private AsyncMachineImageSupport inner;
	private MachineImageSupport original;
	private Configuration configuration;
	private QuartzService<ComputeQuartzType> quartz;
	private DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private FirstLevelDispatcher dispatcher;
	private DistributedObjectFactory objectFactory;
	private DistributedCache<String, MachineImageProxy> cache;
	private ScheduledFuture<Iterable<MachineImage>> future;
	private List<MachineImage> images;
	private MachineImage image;
	private ListenableFuture<Iterable<MachineImage>> result;
	private AsyncResult<Iterable<MachineImage>> asyncResult;
	private ConfigurationHome configurationHome;


	@SuppressWarnings({ "unchecked", "deprecation" })
	@Before
	public void setUp() throws Exception {
		configurationHome = context.mock(ConfigurationHome.class);
		inner = context.mock(AsyncMachineImageSupport.class);
		original = context.mock(MachineImageSupport.class);
		configuration = new Configuration();
		configuration.setId("id");
		configuration.setCloudName("cloudName");
		configuration.setStatus(true);
		configuration.getComputeConfiguration().getMachineImageConfiguration().getListImages().getDelay().setNumber(1L);
		quartz = context.mock(QuartzService.class);
		typeMap = context.mock(DistributedMap.class);
		dispatcher = context.mock(FirstLevelDispatcher.class);
		objectFactory = context.mock(DistributedObjectFactory.class);
		cache = context.mock(DistributedCache.class);
		future = context.mock(ScheduledFuture.class);
		result = context.mock(ListenableFuture.class);
		asyncResult = new AsyncResult<Iterable<MachineImage>>(result);

		images = new ArrayList<MachineImage>();
		image = new MachineImage();
		image.setProviderMachineImageId("providerMachineImageId");
		image.setName("name");
		images.add(image);

		context.checking(new Expectations() {

			{
				exactly(1).of(configurationHome).addLifeCycleListener(with(any(CachedMachineImageSupportImpl.class)));
				exactly(1).of(inner).getSupport();
				will(returnValue(original));
				exactly(1).of(future).get();
				exactly(1).of(objectFactory).getCache(with(any(String.class)), with(any(IllegalStateException.class)));
				will(returnValue(cache));

			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(quartz).schedule(with(any(ComputeQuartzType.class)), with(any(QuartzConfiguration.class)));
				will(new CustomAction("check argument") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						ComputeQuartzType type = (ComputeQuartzType) invocation.getParameter(0);
						QuartzConfiguration<Iterable<MachineImage>> config =
								(QuartzConfiguration<Iterable<MachineImage>>) invocation.getParameter(1);
						Assert.assertEquals(ComputeQuartzType.MachineImage, type);
						Assert.assertEquals("Image:" + configuration.getCloudName(), config.getName());
						Assert.assertEquals(CachedServiceProviderImpl.INITIAL_DELAY, config.getInitialDelay());
						Assert.assertEquals(configuration.getComputeConfiguration().getMachineImageConfiguration()
								.getListImages().getDelay(), config.getTime());
						Assert.assertEquals(1, config.getCallbacks().size());
						FutureCallback<Iterable<MachineImage>> callback = config.getCallbacks().iterator().next();
						Callable<Iterable<MachineImage>> callable = config.getCallable();
						testCallback(callback);
						testCallable(callable);
						return future;
					}

				});
			}
		});
		support =
				new CachedMachineImageSupportImpl(configurationHome, inner, configuration, quartz, typeMap, dispatcher,
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
	public void testListImages() throws CloudException, InternalException, InterruptedException, ExecutionException {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(images));
			}
		});
		AsyncResult<Iterable<MachineImage>> result = support.listImages(ImageFilterOptions.getInstance());
		Iterable<MachineImage> iterable = result.get();
		Iterator<MachineImage> iterator = iterable.iterator();
		Assert.assertEquals(image, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void testListImagesWithOptions() throws CloudException, InternalException, InterruptedException,
			ExecutionException {
		final ImageFilterOptions options = ImageFilterOptions.getInstance(true);
		context.checking(new Expectations() {

			{
				exactly(1).of(inner).listImages(options);
				will(returnValue(asyncResult));

				exactly(1).of(result).get();
				will(returnValue(images));
			}
		});
		AsyncResult<Iterable<MachineImage>> result = support.listImages(options);
		Iterable<MachineImage> iterable = result.get();
		Iterator<MachineImage> iterator = iterable.iterator();
		Assert.assertEquals(image, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRefreshImageCache() {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, MachineImageProxy> imageMap =
								(Map<String, MachineImageProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, imageMap.size());
						MachineImageProxy proxy = imageMap.get(image.getProviderMachineImageId());
						Assert.assertEquals(image, proxy.getMachineImage());
						return null;
					}

				});
			}
		});
		support.refreshImageCache(images);
	}

	@Test
	public void testAddMachineImageListener() {
		final CachedMachineImageListener service = context.mock(CachedMachineImageListener.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).addListener(service);
				exactly(1).of(dispatcher).removeListener(service);

			}
		});
		support.addMachineImageListener(service);
	}

	@Test
	public void testRemoveMachineImageListener() {
		CachedMachineImageListener service = context.mock(CachedMachineImageListener.class);
		support.removeMachineImageListener(service);
	}

	private void testCallback(FutureCallback<Iterable<MachineImage>> callback) {
		testCallbackFail(callback);
		testCallbackSuccess(callback);

	}

	@SuppressWarnings("unchecked")
	private void testCallbackSuccess(FutureCallback<Iterable<MachineImage>> callback) {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(images));
				exactly(1).of(cache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, MachineImageProxy> imageMap =
								(Map<String, MachineImageProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, imageMap.size());
						MachineImageProxy proxy = imageMap.get(image.getProviderMachineImageId());
						Assert.assertEquals(image, proxy.getMachineImage());
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).fireRefreshedEvent(with(any(MachineImageRefreshedEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						MachineImageRefreshedEvent event = (MachineImageRefreshedEvent) invocation.getParameter(0);
						Collection<MachineImage> collection = event.getNewEntries();
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(1, collection.size());
						Assert.assertEquals(image, collection.iterator().next());
						return null;
					}

				});
			}
		});
		callback.onSuccess(images);
	}

	private void testCallbackFail(FutureCallback<Iterable<MachineImage>> callback) {
		final Exception e = new Exception("exception on purpose");
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).reload(e);
				exactly(1).of(dispatcher).fireFaiureEvent(with(any(FailureEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						MachineImageFailureEvent event = (MachineImageFailureEvent) invocation.getParameter(0);
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(e, event.getThrowable());
						return null;
					}

				});
			}
		});
		callback.onFailure(e);
	}

	private void testCallable(Callable<Iterable<MachineImage>> callable) throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(original).listImages(with(any(ImageFilterOptions.class)));
				will(new CustomAction("check callable") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						ImageFilterOptions options = (ImageFilterOptions) invocation.getParameter(0);
						Assert.assertFalse(options.isMatchesAny());
						return images;
					}

				});
			}
		});
		Iterable<MachineImage> iterable = callable.call();
		Iterator<MachineImage> iterator = iterable.iterator();
		Assert.assertEquals(image, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

}
