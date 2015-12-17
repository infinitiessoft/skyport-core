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
//package com.infinities.skyport.ui;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import javax.swing.tree.DefaultMutableTreeNode;
//
//import org.jmock.Expectations;
//import org.jmock.Mockery;
//import org.jmock.integration.junit4.JUnit4Mockery;
//import org.jmock.lib.concurrent.Synchroniser;
//import org.jmock.lib.legacy.ClassImposteriser;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import com.infinities.skyport.annotation.Cmd;
//import com.infinities.skyport.async.AsyncDriver;
//import com.infinities.skyport.model.AccessConfig;
//import com.infinities.skyport.model.AccessConfig.Delay;
//import com.infinities.skyport.model.AccessConfig.Timeout;
//import com.infinities.skyport.model.PoolConfig;
//
//public class TestingPanelTest {
//
//	protected Mockery context = new JUnit4Mockery() {
//
//		{
//			setThreadingPolicy(new Synchroniser());
//			setImposteriser(ClassImposteriser.INSTANCE);
//		}
//	};
//	private TestingPanel panel;
//	// private DriverHome driverHome;
//	// private AccessConfigHome accessConfigHome;
//	private AsyncDriver asyncDriver;
//	private AccessConfig accessconfig;
//	private final String name = "name";
//	private final String desc = "desc";
//	private final String driver = "driver";
//	private final String server = "server";
//	private final String username = "username";
//	private final String password = "password";
//	private final String domain = "domain";
//	private final String protocol = "http";
//	private final String ip = "localhost";
//	private final String port = "8080";
//	private final Delay delay = new Delay(2);
//	private final Timeout timeout = new Timeout(2);
//
//
//	@Before
//	public void setUp() throws Exception {
//		// driverHome = context.mock(DriverHome.class);
//		// accessConfigHome = context.mock(AccessConfigHome.class);
//		panel = new TestingPanel(protocol, ip, port);
//		asyncDriver = context.mock(AsyncDriver.class);
//		accessconfig = new AccessConfig();
//		this.accessconfig.setDelay(delay);
//		this.accessconfig.setDescription(desc);
//		this.accessconfig.setDomain(domain);
//		this.accessconfig.setDriver(driver);
//		this.accessconfig.setId("0L");
//		// this.accessconfig.setJks(jks);
//		this.accessconfig.setLongPoolConfig(new PoolConfig(1, 1, 120, 1));
//		this.accessconfig.setMediumPoolConfig(new PoolConfig(1, 1, 120, 1));
//		this.accessconfig.setModifiedDate(new Date().toString());
//		this.accessconfig.setName(name);
//		this.accessconfig.setPassword(password);
//		this.accessconfig.setServer(server);
//		this.accessconfig.setShortPoolConfig(new PoolConfig(1, 1, 120, 1));
//		this.accessconfig.setStatus(true);
//		this.accessconfig.setTimeout(timeout);
//		this.accessconfig.setUsername(username);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		context.assertIsSatisfied();
//	}
//
//	@Test
//	public void testPersist() {
//		final List<Cmd> cmds = new ArrayList<Cmd>();
//		context.checking(new Expectations() {
//
//			{
//
//				exactly(3).of(asyncDriver).getAccessConfig();
//				will(returnValue(accessconfig));
//
//				exactly(1).of(asyncDriver).getCmds();
//				will(returnValue(cmds));
//			}
//		});
//		panel.activate();
//		assertEquals(0, panel.getTopNode().getChildCount());
//		panel.persist(asyncDriver);
//		assertEquals(1, panel.getTopNode().getChildCount());
//	}
//
//	@Test
//	public void testMerge() {
//		final List<Cmd> cmds = new ArrayList<Cmd>();
//		context.checking(new Expectations() {
//
//			{
//
//				exactly(5).of(asyncDriver).getAccessConfig();
//				will(returnValue(accessconfig));
//
//				exactly(2).of(asyncDriver).getCmds();
//				will(returnValue(cmds));
//			}
//		});
//		panel.activate();
//		assertEquals(0, panel.getTopNode().getChildCount());
//		panel.persist(asyncDriver);
//		String oldName = String.valueOf(((DefaultMutableTreeNode) panel.getTopNode().getFirstChild()).getUserObject());
//		assertEquals(accessconfig.getName() + ":0L", oldName);
//		accessconfig.setName("test");
//		assertEquals(1, panel.getTopNode().getChildCount());
//		panel.merge(asyncDriver);
//		assertEquals(1, panel.getTopNode().getChildCount());
//		String name = String.valueOf(((DefaultMutableTreeNode) panel.getTopNode().getFirstChild()).getUserObject());
//		assertEquals("test:0L", name);
//	}
//
//	@Test
//	public void testRemove() {
//		final List<Cmd> cmds = new ArrayList<Cmd>();
//		context.checking(new Expectations() {
//
//			{
//
//				exactly(4).of(asyncDriver).getAccessConfig();
//				will(returnValue(accessconfig));
//
//				exactly(1).of(asyncDriver).getCmds();
//				will(returnValue(cmds));
//			}
//		});
//		panel.activate();
//		assertEquals(0, panel.getTopNode().getChildCount());
//		panel.persist(asyncDriver);
//		assertEquals(1, panel.getTopNode().getChildCount());
//		panel.remove(asyncDriver);
//		assertEquals(0, panel.getTopNode().getChildCount());
//	}
//
//	@Test
//	public void testClear() {
//		final List<Cmd> cmds = new ArrayList<Cmd>();
//		context.checking(new Expectations() {
//
//			{
//
//				exactly(3).of(asyncDriver).getAccessConfig();
//				will(returnValue(accessconfig));
//
//				exactly(1).of(asyncDriver).getCmds();
//				will(returnValue(cmds));
//			}
//		});
//		panel.activate();
//		assertEquals(0, panel.getTopNode().getChildCount());
//		panel.persist(asyncDriver);
//		assertEquals(1, panel.getTopNode().getChildCount());
//		panel.clear();
//		assertEquals(0, panel.getTopNode().getChildCount());
//	}
//
// }
