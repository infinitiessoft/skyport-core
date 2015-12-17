package com.infinities.skyport.registrar;

import org.glassfish.hk2.api.Factory;

import com.infinities.skyport.service.ConfigurationHome;

public class ConfigurationHomeH2Factory implements Factory<ConfigurationHome> {

	@Override
	public ConfigurationHome provide() {
		return ConfigurationHomeFactory.getInstance();
	}

	@Override
	public void dispose(ConfigurationHome instance) {

	}
}
