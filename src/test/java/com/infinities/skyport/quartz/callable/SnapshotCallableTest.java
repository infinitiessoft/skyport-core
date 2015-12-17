package com.infinities.skyport.quartz.callable;

import static org.junit.Assert.assertEquals;

import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SnapshotCallableTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private SnapshotCallable callable;
	private SnapshotSupport support;


	@Before
	public void setUp() throws Exception {
		support = context.mock(SnapshotSupport.class);
		callable = new SnapshotCallable(support);
	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCall() throws Exception {
		final Iterable<Snapshot> snapshots = context.mock(Iterable.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(support).listSnapshots();
				will(returnValue(snapshots));
			}
		});
		Iterable<Snapshot> ret = callable.call();
		assertEquals(ret, snapshots);
	}
}
