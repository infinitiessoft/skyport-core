package com.infinities.skyport.registrar;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.ServiceProvider;

public class DriverHomeImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private DriverHomeImpl home;
	private ServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		home = new DriverHomeImpl();
		provider = context.mock(ServiceProvider.class);
		home.initialize();
	}

	@After
	public void tearDown() throws Exception {
		home.close();
		context.assertIsSatisfied();
	}

	@Test
	public void testPersist() throws Exception {
		home.persist(provider);
	}

	@Test
	public void testFindAll() {
		Map<String, Class<? extends ServiceProvider>> map = home.findAll();
		assertEquals(18, map.size());
	}

	@Test
	public void testFindByName() {
		String className = "com.infinities.skyport.VcloudServiceProvider";
		Class<? extends ServiceProvider> serverProvider = home.findByName(className);
		assertEquals(className, serverProvider.getName());
	}

}
