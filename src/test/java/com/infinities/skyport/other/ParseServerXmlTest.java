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
package com.infinities.skyport.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.model.webserver.Connector;
import com.infinities.skyport.model.webserver.Server;
import com.infinities.skyport.model.webserver.Service;
import com.infinities.skyport.util.XMLUtil;

public class ParseServerXmlTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("server.xml");
		File file = new File(url.getPath());
		Server server = XMLUtil.convertValue(file, Server.class);
		assertEquals(1, server.getServices().size());
		for (Service service : server.getServices()) {
			assertEquals(1, service.getConnectors().size());
			for (Connector connector : service.getConnectors()) {
				assertEquals("HTTP/1.1", connector.getProtocol());
				assertEquals("8085", connector.getPort());
				assertFalse(connector.isEnableSSL());
				assertTrue(connector.isDefaultConnector());
			}
		}
	}

}
