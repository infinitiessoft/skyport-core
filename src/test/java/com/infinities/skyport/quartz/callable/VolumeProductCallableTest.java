package com.infinities.skyport.quartz.callable;

import static org.junit.Assert.assertEquals;

import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VolumeProductCallableTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private VolumeProductCallable callable;
	private VolumeSupport support;


	@Before
	public void setUp() throws Exception {
		support = context.mock(VolumeSupport.class);
		callable = new VolumeProductCallable(support);
	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCall() throws Exception {
		final Iterable<VolumeProduct> products = context.mock(Iterable.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(support).listVolumeProducts();
				will(returnValue(products));

			}
		});
		Iterable<VolumeProduct> ret = callable.call();
		assertEquals(ret, products);
	}
}
