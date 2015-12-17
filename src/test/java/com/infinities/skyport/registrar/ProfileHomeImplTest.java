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
package com.infinities.skyport.registrar;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.bind.JAXBException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.infinities.skyport.model.Profile;
import com.infinities.skyport.model.configuration.Configuration;

public class ProfileHomeImplTest {

	private ProfileHomeImpl home;
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	private File tempFile;


	@Before
	public void setUp() throws Exception {
		testFolder.create();
		tempFile = testFolder.newFile("test.xml");
		home = new ProfileHomeImpl();
		// home.file = tempFile;
		// home.saveFile = tempFile;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGet() throws URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("accessconfig_profileHomeImplTest.xml");
		File file = new File(url.toURI().getSchemeSpecificPart());
		home.file = file;
		home.get();
	}

	@Test
	public void testGetWithBlankFile() throws URISyntaxException {
		home.file = tempFile;
		home.get();
	}

	@Test
	public void testSave() throws JAXBException {
		Profile profile = new Profile();
		Configuration configuration = new Configuration();
		configuration.setCloudName("MyCloud");
		configuration.setAccount("Account");
		configuration.setEndpoint("Endpoint");
		configuration.setProviderName("Demo");
		configuration.setProviderClass("Mock");
		configuration.setRegionId("Taiwan");
		configuration.getProperties().setProperty("token", "MyCloudToken");
		profile.getConfigurations().add(configuration);
		home.profile = profile;
		home.saveFile = tempFile;
		home.save();
	}

}
