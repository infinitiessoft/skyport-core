package com.infinities.skyport.registrar;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.Module;
import com.infinities.skyport.Skyport;

public class ModuleHomeImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private ModuleHomeImpl home;
	private Skyport skyport;
	private SortedMap<String, Module> map;
	private Module module;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		skyport = context.mock(Skyport.class);
		module = context.mock(Module.class);
		map = context.mock(SortedMap.class);
		home = new ModuleHomeImpl(skyport);
		home.initialize();
		home.registeredModules = map;
	}

	@After
	public void tearDown() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(map).clear();

			}
		});
		home.close();
		context.assertIsSatisfied();
	}

	@Test
	public void testPersist() throws Exception {
		final Set<Module> modules = new HashSet<Module>();
		modules.add(module);
		context.checking(new Expectations() {

			{
				exactly(1).of(module).getAlias();
				will(returnValue("alias"));

				exactly(1).of(map).containsKey("alias");
				will(returnValue(false));

				exactly(1).of(module).initialize(skyport);
				exactly(1).of(module).close();

				exactly(1).of(map).put("alias", module);
				will(returnValue(null));

				exactly(1).of(map).values();
				will(returnValue(modules));

			}
		});
		home.persist(module);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPersistWithDuplicateAlias() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(module).getAlias();
				will(returnValue("alias"));

				exactly(1).of(map).containsKey("alias");
				will(returnValue(true));

				exactly(1).of(map).values();
				will(returnValue(new HashSet<Module>()));

			}
		});
		home.persist(module);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPersistWithNullAlias() throws Exception {
		context.checking(new Expectations() {

			{
				exactly(1).of(module).getAlias();
				will(returnValue(null));

				exactly(1).of(map).values();
				will(returnValue(new HashSet<Module>()));

			}
		});
		home.persist(module);
	}

	@Test
	public void testFindAll() {
		context.checking(new Expectations() {

			{

				exactly(2).of(map).size();
				will(returnValue(5));

				exactly(1).of(map).values();
				will(returnValue(new HashSet<Module>()));
			}
		});
		SortedMap<String, Module> ret = home.findAll();
		assertEquals(map.size(), ret.size());
	}

	@Test
	public void testFindByName() throws InstantiationException, IllegalAccessException {
		context.checking(new Expectations() {

			{

				exactly(1).of(map).get("name");
				will(returnValue(module));

				exactly(1).of(map).values();
				will(returnValue(new HashSet<Module>()));
			}
		});
		Module ret = home.findByName("name");
		assertEquals(module, ret);
	}

}
