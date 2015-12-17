package com.infinities.skyport.quartz.callable;

import static org.junit.Assert.assertEquals;

import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VmProductCallableTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private VmProductCallable callable;
	private VirtualMachineSupport support;


	@Before
	public void setUp() throws Exception {
		support = context.mock(VirtualMachineSupport.class);
		callable = new VmProductCallable(support);
	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCall() throws Exception {
		final Iterable<VirtualMachineProduct> products = context.mock(Iterable.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(support).listAllProducts();
				will(returnValue(products));

			}
		});
		Iterable<VirtualMachineProduct> ret = callable.call();
		assertEquals(ret, products);
	}
}
