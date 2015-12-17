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
