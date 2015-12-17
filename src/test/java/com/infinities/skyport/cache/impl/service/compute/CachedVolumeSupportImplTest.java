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
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.compute.VolumeType;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
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
import com.infinities.skyport.async.service.compute.AsyncVolumeSupport;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.cache.service.CachedComputeServices.ComputeQuartzType;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport.CachedVolumeListener;
import com.infinities.skyport.cache.service.compute.CachedVolumeSupport.CachedVolumeProductListener;
import com.infinities.skyport.distributed.DistributedAtomicLong;
import com.infinities.skyport.distributed.DistributedCache;
import com.infinities.skyport.distributed.DistributedMap;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.proxy.VolumeProductProxy;
import com.infinities.skyport.proxy.VolumeProxy;
import com.infinities.skyport.quartz.QuartzConfiguration;
import com.infinities.skyport.quartz.QuartzServiceImpl;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.compute.volume.VolumeFailureEvent;
import com.infinities.skyport.service.event.compute.volume.VolumeRefreshedEvent;
import com.infinities.skyport.service.event.compute.volumeproduct.VolumeProductFailureEvent;
import com.infinities.skyport.service.event.compute.volumeproduct.VolumeProductRefreshedEvent;

public class CachedVolumeSupportImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};

	private CachedVolumeSupportImpl support;
	private AsyncVolumeSupport inner;
	private VolumeSupport original;
	private Configuration configuration;
	private QuartzServiceImpl<ComputeQuartzType> quartz;
	private DistributedMap<ComputeQuartzType, ComputeQuartzType> typeMap;
	private FirstLevelDispatcher dispatcher;
	private DistributedObjectFactory objectFactory;
	private DistributedCache<String, VolumeProxy> cache;
	private ScheduledFuture<Iterable<Volume>> future;
	private List<Volume> volumes;
	private Volume volume;

	private DistributedCache<String, VolumeProductProxy> productCache;
	private ScheduledFuture<Iterable<VolumeProductProxy>> productFuture;
	private List<VolumeProductProxy> products;
	private VolumeProduct product;
	private VolumeProductProxy productProxy;
	private DistributedAtomicLong mockLong;
	private ConfigurationHome configurationHome;


	@SuppressWarnings({ "unchecked" })
	@Before
	public void setUp() throws Exception {
		configurationHome = context.mock(ConfigurationHome.class);
		inner = context.mock(AsyncVolumeSupport.class);
		original = context.mock(VolumeSupport.class);
		configuration = new Configuration();
		configuration.setId("id");
		configuration.setCloudName("cloudName");
		configuration.setStatus(true);
		configuration.getComputeConfiguration().getVolumeConfiguration().getListVolumes().getDelay().setNumber(1L);
		quartz = context.mock(QuartzServiceImpl.class);
		typeMap = context.mock(DistributedMap.class);
		dispatcher = context.mock(FirstLevelDispatcher.class);
		objectFactory = context.mock(DistributedObjectFactory.class);
		cache = context.mock(DistributedCache.class, "volumeCache");
		future = context.mock(ScheduledFuture.class, "future");
		productCache = context.mock(DistributedCache.class, "volumeProductCache");
		productFuture = context.mock(ScheduledFuture.class, "productFuture");
		mockLong = context.mock(DistributedAtomicLong.class);

		volumes = new ArrayList<Volume>();
		volume = new Volume();
		volume.setProviderVolumeId("providerVolumeId");
		volume.setName("name");
		volumes.add(volume);

		products = new ArrayList<VolumeProductProxy>();
		product =
				VolumeProduct.getInstance("id", "name", "description", VolumeType.SSD, new Storage<Gigabyte>(5,
						new Gigabyte()), "currency", 5, 5, new Float(5), new Float(5));
		productProxy = new VolumeProductProxy(product, "id", "cloudName");
		products.add(productProxy);

		context.checking(new Expectations() {

			{
				exactly(1).of(configurationHome).addLifeCycleListener(with(any(CachedVolumeSupportImpl.class)));
				exactly(1).of(inner).getSupport();
				will(returnValue(original));
				exactly(1).of(future).get();
				exactly(1).of(objectFactory).getCache(with(any(String.class)), with(any(IllegalStateException.class)));
				will(returnValue(cache));
			}
		});

		context.checking(new Expectations() {

			{
				exactly(1).of(objectFactory).getAtomicLong("volume_providerVolumeId");
				will(returnValue(mockLong));
				exactly(1).of(quartz).schedule(with(any(ComputeQuartzType.class)), with(any(QuartzConfiguration.class)));
				will(new CustomAction("check argument") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						ComputeQuartzType type = (ComputeQuartzType) invocation.getParameter(0);
						QuartzConfiguration<Iterable<Volume>> config =
								(QuartzConfiguration<Iterable<Volume>>) invocation.getParameter(1);
						Assert.assertEquals(ComputeQuartzType.Volume, type);
						Assert.assertEquals("Volume:" + configuration.getCloudName(), config.getName());
						Assert.assertEquals(CachedServiceProviderImpl.INITIAL_DELAY, config.getInitialDelay());
						Assert.assertEquals(configuration.getComputeConfiguration().getVolumeConfiguration()
								.getListVolumes().getDelay(), config.getTime());
						Assert.assertEquals(1, config.getCallbacks().size());
						FutureCallback<Iterable<Volume>> callback = config.getCallbacks().iterator().next();
						Callable<Iterable<Volume>> callable = config.getCallable();
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
						QuartzConfiguration<Iterable<VolumeProduct>> config =
								(QuartzConfiguration<Iterable<VolumeProduct>>) invocation.getParameter(1);
						Assert.assertEquals(ComputeQuartzType.VolumeProduct, type);
						Assert.assertEquals("Volume Product:" + configuration.getCloudName(), config.getName());
						Assert.assertEquals(CachedServiceProviderImpl.INITIAL_DELAY, config.getInitialDelay());
						Assert.assertEquals(configuration.getComputeConfiguration().getVolumeConfiguration()
								.getListVolumeProducts().getDelay(), config.getTime());
						Assert.assertEquals(1, config.getCallbacks().size());
						FutureCallback<Iterable<VolumeProduct>> callback = config.getCallbacks().iterator().next();
						Callable<Iterable<VolumeProduct>> callable = config.getCallable();
						testProductCallback(callback);
						testProductCallable(callable);
						return productFuture;
					}
				});
			}
		});

		support =
				new CachedVolumeSupportImpl(configurationHome, inner, configuration, quartz, typeMap, dispatcher,
						objectFactory);
		support.initialize();

	}

	@After
	public void tearDown() throws Exception {
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
	public void testListVolumes() throws InternalException, CloudException, InterruptedException, ExecutionException {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(volumes));
			}
		});
		AsyncResult<Iterable<Volume>> result = support.listVolumes();
		Iterable<Volume> iterable = result.get();
		Iterator<Volume> iterator = iterable.iterator();
		Assert.assertEquals(volume, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void testListVolumeProducts() throws InternalException, CloudException, InterruptedException, ExecutionException {
		context.checking(new Expectations() {

			{
				exactly(1).of(productCache).values();
				will(returnValue(products));
			}
		});
		AsyncResult<Iterable<VolumeProduct>> result = support.listVolumeProducts();
		Iterable<VolumeProduct> iterable = result.get();
		Iterator<VolumeProduct> iterator = iterable.iterator();
		Assert.assertEquals(product, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRefreshVolumeCache() {
		context.checking(new Expectations() {

			{
				exactly(1).of(objectFactory).getAtomicLong("volume_providerVolumeId");
				will(returnValue(mockLong));
				exactly(1).of(cache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, VolumeProxy> volumeMap = (Map<String, VolumeProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, volumeMap.size());
						VolumeProxy proxy = volumeMap.get(volume.getProviderVolumeId());
						Assert.assertEquals(volume, proxy.getVolume());
						return null;
					}
				});
			}
		});
		support.refreshVolumeCache(volumes);
	}

	@Test
	public void testAddVolumeListener() {
		final CachedVolumeListener service = context.mock(CachedVolumeListener.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).addListener(service);
				exactly(1).of(dispatcher).removeListener(service);

			}
		});
		support.addVolumeListener(service);
	}

	@Test
	public void testRemoveVolumeListener() {
		CachedVolumeListener service = context.mock(CachedVolumeListener.class);
		support.removeVolumeListener(service);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRefreshVolumeProductCache() {
		context.checking(new Expectations() {

			{
				exactly(1).of(productCache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, VolumeProductProxy> productMap =
								(Map<String, VolumeProductProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, productMap.size());
						VolumeProductProxy proxy = productMap.get(product.getProviderProductId());
						Assert.assertEquals(product, proxy.getProduct());
						return null;
					}
				});
			}
		});
		List<VolumeProduct> list = new ArrayList<VolumeProduct>();
		list.add(product);
		support.refreshVolumeProductCache(list);
	}

	@Test
	public void testAddVolumeProductListener() {
		final CachedVolumeProductListener service = context.mock(CachedVolumeProductListener.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).addListener(service);
				exactly(1).of(dispatcher).removeListener(service);

			}
		});
		support.addVolumeProductListener(service);
	}

	@Test
	public void testRemoveVolumeProductListener() {
		CachedVolumeProductListener service = context.mock(CachedVolumeProductListener.class);
		support.removeVolumeProductListener(service);
	}

	private void testCallback(FutureCallback<Iterable<Volume>> callback) {
		testCallbackFail(callback);
		testCallbackSuccess(callback);
	}

	@SuppressWarnings("unchecked")
	private void testCallbackSuccess(FutureCallback<Iterable<Volume>> callback) {
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).values();
				will(returnValue(volumes));
				exactly(1).of(cache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, VolumeProxy> volumeMap = (Map<String, VolumeProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, volumeMap.size());
						VolumeProxy proxy = volumeMap.get(volume.getProviderVolumeId());
						Assert.assertEquals(volume, proxy.getVolume());
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).fireRefreshedEvent(with(any(VolumeRefreshedEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VolumeRefreshedEvent event = (VolumeRefreshedEvent) invocation.getParameter(0);
						Collection<Volume> collection = event.getNewEntries();
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(1, collection.size());
						Assert.assertEquals(volume, collection.iterator().next());
						return null;
					}

				});
			}
		});
		callback.onSuccess(volumes);
	}

	private void testCallbackFail(FutureCallback<Iterable<Volume>> callback) {
		final Exception e = new Exception("exception on purpose");
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).reload(e);
				exactly(1).of(dispatcher).fireFaiureEvent(with(any(FailureEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VolumeFailureEvent event = (VolumeFailureEvent) invocation.getParameter(0);
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(e, event.getThrowable());
						return null;
					}
				});
			}
		});
		callback.onFailure(e);
	}

	private void testCallable(Callable<Iterable<Volume>> callable) throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(original).listVolumes();
				will(returnValue(volumes));
			}
		});
		Iterable<Volume> iterable = callable.call();
		Iterator<Volume> iterator = iterable.iterator();
		Assert.assertEquals(volume, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	private void testProductCallback(FutureCallback<Iterable<VolumeProduct>> callback) {
		testProductCallbackFail(callback);
		testProductCallbackSuccess(callback);
	}

	@SuppressWarnings("unchecked")
	private void testProductCallbackSuccess(FutureCallback<Iterable<VolumeProduct>> callback) {
		context.checking(new Expectations() {

			{
				exactly(1).of(productCache).values();
				will(returnValue(products));
				exactly(1).of(productCache).reload(with(any(Map.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, VolumeProductProxy> productMap =
								(Map<String, VolumeProductProxy>) invocation.getParameter(0);
						Assert.assertEquals(1, productMap.size());
						VolumeProductProxy proxy = productMap.get(product.getProviderProductId());
						Assert.assertEquals(product, proxy.getProduct());
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(dispatcher).fireRefreshedEvent(with(any(VolumeProductRefreshedEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VolumeProductRefreshedEvent event = (VolumeProductRefreshedEvent) invocation.getParameter(0);
						Collection<VolumeProduct> collection = event.getNewEntries();
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(1, collection.size());
						Assert.assertEquals(product, collection.iterator().next());
						return null;
					}

				});
			}
		});
		List<VolumeProduct> list = new ArrayList<VolumeProduct>();
		list.add(product);
		callback.onSuccess(list);
	}

	private void testProductCallbackFail(FutureCallback<Iterable<VolumeProduct>> callback) {
		final Exception e = new Exception("exception on purpose");
		context.checking(new Expectations() {

			{
				exactly(1).of(productCache).reload(e);
				exactly(1).of(dispatcher).fireFaiureEvent(with(any(FailureEvent.class)));
				will(new CustomAction("check fail event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						VolumeProductFailureEvent event = (VolumeProductFailureEvent) invocation.getParameter(0);
						Assert.assertEquals(configuration.getId(), event.getConfigid());
						Assert.assertEquals(e, event.getThrowable());
						return null;
					}
				});
			}
		});
		callback.onFailure(e);
	}

	private void testProductCallable(Callable<Iterable<VolumeProduct>> callable) throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(original).listVolumeProducts();
				will(returnValue(products));
			}
		});
		Iterable<VolumeProduct> iterable = callable.call();
		Iterator<VolumeProduct> iterator = iterable.iterator();
		Assert.assertEquals(productProxy, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}
}
