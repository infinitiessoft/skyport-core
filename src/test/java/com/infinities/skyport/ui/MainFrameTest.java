package com.infinities.skyport.ui;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationHome;
import com.infinities.skyport.service.DriverHome;

public class MainFrameTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private MainFrame frame;
	private DriverHome driverHome;
	private ConfigurationHome configurationHome;
	private Configuration configuration;
	private final String protocol = "http";
	private final String ip = "localhost";
	private final String port = "8080";


	@Before
	public void setUp() throws Exception {
		driverHome = context.mock(DriverHome.class);
		configurationHome = context.mock(ConfigurationHome.class);
		frame = new MainFrame(driverHome, configurationHome, protocol, ip, port);
		configuration = new Configuration();
		this.configuration.setId("id");
		this.configuration.setCloudName("cloudName");
		this.configuration.setModifiedDate(Calendar.getInstance());
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testPersist() throws InterruptedException {
		context.checking(new Expectations() {

			{
				exactly(1).of(configurationHome).addLifeCycleListener(frame);

			}
		});
		frame.activate();
		frame.persist(configuration);
		assertEquals(1, frame.tableModel.getRowCount());
	}

	@Test
	public void testRemove() throws InterruptedException {
		context.checking(new Expectations() {

			{

				exactly(1).of(configurationHome).addLifeCycleListener(frame);

			}
		});

		frame.activate();
		frame.persist(configuration);
		assertEquals(1, frame.tableModel.getRowCount());
		frame.remove(configuration);
		assertEquals(0, frame.tableModel.getRowCount());
	}

	@Test
	public void testMerge() throws InterruptedException {
		context.checking(new Expectations() {

			{

				exactly(1).of(configurationHome).addLifeCycleListener(frame);

			}
		});

		frame.activate();
		frame.persist(configuration);
		assertEquals(1, frame.tableModel.getRowCount());
		String oldName = String.valueOf(frame.tableModel.getValueAt(0, 2));
		assertEquals(oldName, configuration.getCloudName());
		configuration.setCloudName("test");
		frame.merge(configuration);
		assertEquals(1, frame.tableModel.getRowCount());
		assertEquals("test", configuration.getCloudName());
	}

	@Test
	public void testClear() throws InterruptedException {
		context.checking(new Expectations() {

			{

				exactly(1).of(configurationHome).addLifeCycleListener(frame);

			}
		});

		frame.activate();
		frame.persist(configuration);
		assertEquals(1, frame.tableModel.getRowCount());
		frame.clear();
		assertEquals(0, frame.tableModel.getRowCount());
	}

}
