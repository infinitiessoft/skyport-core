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
