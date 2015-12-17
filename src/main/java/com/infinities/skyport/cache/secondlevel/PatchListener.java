package com.infinities.skyport.cache.secondlevel;

import com.google.common.eventbus.Subscribe;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.SuccessEvent;

public interface PatchListener<T> {

	@Subscribe
	void onChanged(SuccessEvent<T> e);

	@Subscribe
	void onFailure(FailureEvent<T> e);
}
