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

import com.google.common.eventbus.Subscribe;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.SuccessEvent;

public interface PatchListener<T> {

	@Subscribe
	void onChanged(SuccessEvent<T> e);

	@Subscribe
	void onFailure(FailureEvent<T> e);
}
