package com.infinities.skyport.cache.secondlevel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.builder.DiffResult;

import com.google.common.collect.Sets;
import com.google.common.reflect.AbstractInvocationHandler;
import com.infinities.skyport.compute.entity.patch.PatchBuilder;
import com.infinities.skyport.service.event.Event.Type;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.RefreshedEvent;
import com.infinities.skyport.service.event.SuccessEvent;

public class PatchHandler<T> extends AbstractInvocationHandler {

	private String idProperty;
	private Map<String, T> cache;
	private Comparator<T> comparator;
	private PatchBuilder<T> patchBuilder;
	private PatchListener<T> inner;


	public PatchHandler(String idProperty, Comparator<T> comparator, PatchBuilder<T> patchBuilder, PatchListener<T> inner,
			Map<String, T> cache) {
		this.idProperty = idProperty;
		this.comparator = comparator;
		this.patchBuilder = patchBuilder;
		this.inner = inner;
		this.cache = cache;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		if ("onEntitiesRefreshed".equals(method.getName())) {
			RefreshedEvent<T> event = (RefreshedEvent<T>) args[0];
			onEntitiesRefreshed(event);
		} else if ("onFailure".equals(method.getName())) {
			FailureEvent<T> event = (FailureEvent<T>) args[0];
			onFailure(event);
		} else {
			return method.invoke(proxy, args);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void onEntitiesRefreshed(RefreshedEvent<T> e) throws Exception {
		Collection<T> refreshedCaches = e.getNewEntries();
		Set<T> productsAdded = new HashSet<T>();
		Set<T> productsRemoved = new HashSet<T>();
		Set<T> productsChanged = new HashSet<T>();
		Map<String, T> newCache = new HashMap<String, T>();
		Map<String, DiffResult> patchs = new HashMap<String, DiffResult>();

		for (T newProduct : refreshedCaches) {
			T product = (T) BeanUtils.cloneBean(newProduct);
			String id = BeanUtils.getProperty(product, idProperty);
			newCache.put(id, product);
			T oldProduct = cache.get(id);
			if (oldProduct == null) {
				productsAdded.add(product);
				continue;
			}

			int compare = comparator.compare(oldProduct, product);

			if (compare != 0) {
				DiffResult patch = patchBuilder.diff(oldProduct, product);
				patchs.put(id, patch);
				productsChanged.add(product);
			}
		}

		Set<String> idsRemoved = Sets.difference(cache.keySet(), newCache.keySet());
		for (String id : idsRemoved) {
			T product = cache.get(id);
			productsRemoved.add(product);
		}

		cache.clear();
		cache.putAll(newCache);

		if (!productsRemoved.isEmpty()) {
			GenericSuccessEvent<T> event = new GenericSuccessEvent<T>(Type.REMOVED, e.getConfigid(), productsRemoved);
			fireEvent(event);

		}

		if (!productsAdded.isEmpty()) {
			GenericSuccessEvent<T> event = new GenericSuccessEvent<T>(Type.ADDED, e.getConfigid(), productsAdded);
			fireEvent(event);

		}

		if (!productsChanged.isEmpty()) {
			GenericSuccessEvent<T> event = new GenericSuccessEvent<T>(Type.MODIFIED, e.getConfigid(), productsChanged);
			event.getPatchs().putAll(patchs);
			fireEvent(event);
		}

		GenericSuccessEvent<T> event =
				new GenericSuccessEvent<T>(Type.REFRESHED, e.getConfigid(), new ArrayList<T>(newCache.values()));
		fireEvent(event);
	}

	private void onFailure(FailureEvent<T> e) {
		fireFaiureEvent(e);
	}

	private void fireEvent(SuccessEvent<T> e) {
		inner.onChanged(e);
	}

	private void fireFaiureEvent(FailureEvent<T> e) {
		inner.onFailure(e);
	}

}
