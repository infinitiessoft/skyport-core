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
