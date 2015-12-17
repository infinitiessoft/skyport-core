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
package com.infinities.skyport.cache.secondlevel;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.DiffResult;

import com.infinities.skyport.service.event.SuccessEvent;

public class GenericSuccessEvent<T> implements SuccessEvent<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Date date;
	private final Type type;
	private final String configid;
	private final Collection<T> entries;
	private final Map<String, DiffResult> patchs = new HashMap<String, DiffResult>(0);


	public GenericSuccessEvent(Type type, String configid, Collection<T> entries) {
		this.date = new Date();
		this.type = type;
		this.entries = entries;
		this.configid = configid;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Collection<T> getEntries() {
		return entries;
	}

	@Override
	public Map<String, DiffResult> getPatchs() {
		return patchs;
	}

	@Override
	public String getConfigid() {
		return configid;
	}

}
