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

import com.infinities.skyport.model.console.ConnectorSet;
import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.util.XMLUtil;

public class ParseWebsockifyXmlTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource("websockify.xml");
		File file = new File(url.getPath());
		ConnectorSet set = XMLUtil.convertValue(file, ConnectorSet.class);
		assertEquals(3, set.getConnectors().size());
		for (ServerConfiguration configuration : set.getConnectors()) {
			if ("127.0.0.2".equals(configuration.getIp())) {
				assertFalse(configuration.isEnableSSL());
				assertFalse(configuration.isRequireSSL());
			} else {
				assertTrue(configuration.isEnableSSL());
				assertTrue(configuration.isRequireSSL());
			}

		}
	}

}
