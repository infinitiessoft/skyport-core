package com.infinities.skyport.registrar;

import com.infinities.skyport.service.ConfigurationHome;

public class ConfigurationHomeFactory {

	private static final ConfigurationHome instance = new ConfigurationHomeImpl();
	public static String ACCESS_CONFIG_FILE = "accessconfig.xml";


	private ConfigurationHomeFactory() {

	}

	public static ConfigurationHome getInstance() {
		return instance;
	}

}
