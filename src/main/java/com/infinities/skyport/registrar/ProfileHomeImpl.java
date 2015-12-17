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
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.model.Profile;
import com.infinities.skyport.service.ProfileHome;
import com.infinities.skyport.util.XMLUtil;

public class ProfileHomeImpl implements ProfileHome {

	private static final Logger logger = LoggerFactory.getLogger(ProfileHomeImpl.class);
	protected File file; // for testing
	protected File saveFile; // for testing
	protected Profile profile;


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.registrar.ProfileHome#get()
	 */
	@Override
	public synchronized Profile get() {
		if (profile != null) {
			return profile;
		}
		profile = new Profile();
		try {
			profile = XMLUtil.convertValue(getFile(), Profile.class);
			logger.info("parse xml success, configs: {}", new Object[] { profile.getConfigurations().size() });
		} catch (JAXBException e) {
			logger.warn("parse xml failed: " + getFile() + ", it might be old style", e);
		} catch (FileNotFoundException e) {
			logger.warn("Configuration file: {} not found", getFile());
		} catch (Exception e) {
			logger.warn("unexpected error from parse xml: {}", e);
		}

		return profile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.infinities.skyport.registrar.ProfileHome#saveProfile(com.infinities
	 * .skyport.model.Profile)
	 */
	@Override
	public synchronized void save() throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(Profile.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		File file = saveFile != null ? saveFile : getFile();
		m.marshal(get(), file);
		logger.debug("save file: {}", file.getAbsoluteFile());
	}

	private File getFile() {
		if (file == null) {
			String fileName = ConfigurationHomeFactory.ACCESS_CONFIG_FILE;
			file = new File(fileName);
		}
		logger.debug("accessconfig FileName: {}", file.getAbsolutePath());

		return file;
	}

	public void setSaveFile(File saveFile) {
		this.saveFile = saveFile;
	}

	public void setFile(File file) {
		this.file = file;
	}

}
