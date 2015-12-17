package com.infinities.skyport.quartz.callable;

import static org.junit.Assert.assertEquals;

import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VolumeCallableTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private VolumeCallable callable;
	private VolumeSupport support;


	@Before
	public void setUp() throws Exception {
		support = context.mock(VolumeSupport.class);
		callable = new VolumeCallable(support);
	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCall() throws Exception {
		final Iterable<Volume> volumes = context.mock(Iterable.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(support).listVolumes();
				will(returnValue(volumes));

			}
		});
		Iterable<Volume> ret = callable.call();
		assertEquals(ret, volumes);
	}
}
