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
package com.infinities.skyport.cache.impl;

import com.google.common.eventbus.EventBus;
import com.infinities.skyport.service.EntityListener;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.FirstLevelDispatcher;
import com.infinities.skyport.service.event.RefreshedEvent;

public class EventBusFirstLevelDispatcher implements FirstLevelDispatcher {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final EventBus eventBus;


	public EventBusFirstLevelDispatcher(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public synchronized void fireRefreshedEvent(RefreshedEvent<?> e) {
		eventBus.post(e);
	}

	@Override
	public synchronized void fireFaiureEvent(FailureEvent<?> e) {
		eventBus.post(e);
	}

	@Override
	public void addListener(EntityListener l) {
		eventBus.register(l);
	}

	@Override
	public void removeListener(EntityListener l) {
		eventBus.unregister(l);
	}

}
