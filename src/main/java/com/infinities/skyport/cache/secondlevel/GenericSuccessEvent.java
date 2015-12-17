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
