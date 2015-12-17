package com.infinities.skyport.quartz.callable;

import static org.junit.Assert.assertEquals;

import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ImageCallableTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private ImageCallable callable;
	private MachineImageSupport support;


	@Before
	public void setUp() throws Exception {
		support = context.mock(MachineImageSupport.class);
		callable = new ImageCallable(support);
	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCall() throws Exception {
		final Iterable<MachineImage> images = context.mock(Iterable.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(support).listImages(with(any(ImageFilterOptions.class)));
				will(returnValue(images));
			}
		});
		Iterable<MachineImage> ret = callable.call();
		assertEquals(ret, images);
	}
}
