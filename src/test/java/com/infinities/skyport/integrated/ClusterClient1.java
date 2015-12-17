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
package com.infinities.skyport.integrated;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;
import com.infinities.skyport.Main;

public class ClusterClient1 {

	private static final Logger logger = LoggerFactory.getLogger(ClusterClient1.class);


	public static void main(String[] args) {
		URL url = Thread.currentThread().getContextClassLoader().getResource("server_test1.xml");
		URL url2 = Thread.currentThread().getContextClassLoader().getResource("hazelcast_test.xml");
		URL url3 = Thread.currentThread().getContextClassLoader().getResource("websockify_test1.xml");
		String[] args1 = new String[] { "-server", url.getPath(), "-key", "test1", "-cluster", "hazelcast", "-hazelcast",
				url2.getPath(), "-websockify", url3.getPath() };
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
