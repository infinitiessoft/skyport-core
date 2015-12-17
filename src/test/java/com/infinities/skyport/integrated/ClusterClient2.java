package com.infinities.skyport.integrated;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;
import com.infinities.skyport.Main;

public class ClusterClient2 {

	private static final Logger logger = LoggerFactory.getLogger(ClusterClient2.class);


	public static void main(String[] args) {
		URL url = Thread.currentThread().getContextClassLoader().getResource("server_test2.xml");
		URL url2 = Thread.currentThread().getContextClassLoader().getResource("hazelcast_test.xml");
		URL url3 = Thread.currentThread().getContextClassLoader().getResource("websockify_test2.xml");
		URL url4 = Thread.currentThread().getContextClassLoader().getResource("accessconfig.xml");
		String[] args1 = new String[] { "-server", url.getPath(), "-key", "test1", "-cluster", "hazelcast", "-hazelcast",
				url2.getPath(), "-websockify", url3.getPath(), "-accessconfig", url4.getPath() };
		Main main = null;
		try {
			main = new Main(args1);
			logger.trace("initialize skyport");
			main.initialize();
		} catch (Throwable e) {
			logger.error("Encounter error when initialize skyport.", e);
			try {
				if (main != null) {
					Closeables.close(main, true);
				}
			} catch (IOException e1) {
				logger.error("Encounter error when close skyport.", e1);
			}
		}
	}

}
