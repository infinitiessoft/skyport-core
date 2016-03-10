package com.infinities.skyport.cache.impl.service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import com.google.common.collect.ObjectArrays;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.infinities.skyport.async.service.AsyncStorageServices;
import com.infinities.skyport.async.service.storage.AsyncOfflineStoreSupport;
import com.infinities.skyport.cache.service.CachedStorageServices;
import com.infinities.skyport.cache.service.storage.CachedBlobStoreSupport;

public class DelegatedStorageServices implements CachedStorageServices{

	private static final Set<String> LISTENERS;
	private AsyncStorageServices inner;
	private CachedBlobStoreSupport delegatedBlobStoreSupport;

	static {
		Set<String> listeners = new HashSet<String>();
		listeners.add("addBlobListener");
		listeners.add("removeBlobListener");
		LISTENERS = Collections.unmodifiableSet(listeners);
	}
	
	public DelegatedStorageServices(AsyncStorageServices inner) {
		this.inner = inner;
		if (inner.hasOnlineStorageSupport()) {
			this.delegatedBlobStoreSupport =
					Reflection.newProxy(CachedBlobStoreSupport.class,
							new DelegatedHandler(inner.getOnlineStorageSupport()));
		}
	}
	
	@Override
	public boolean hasOfflineStorageSupport() {
		return inner.hasOfflineStorageSupport();
	}

	@Override
	public boolean hasOnlineStorageSupport() {
		return inner.hasOnlineStorageSupport();
	}

	@Override
	public void initialize() throws Exception {
		
	}

	@Override
	public void close() {
		
	}

	@Override
	public AsyncOfflineStoreSupport getOfflineStorageSupport() {
		return inner.getOfflineStorageSupport();
	}

	@Override
	public CachedBlobStoreSupport getOnlineStorageSupport() {
		return this.delegatedBlobStoreSupport;
	}

	@Override
	public ScheduledFuture<?> flushCache(StorageQuartzType type) {
		throw new IllegalStateException("cache disabled");
	}
	
	private class DelegatedHandler extends AbstractInvocationHandler {

		private final Object inner;


		public DelegatedHandler(Object inner) {
			this.inner = inner;
		}

		@Override
		protected Object handleInvocation(Object proxy, Method method, final Object[] args) throws Throwable {
			if (LISTENERS.contains(method.getName())) {
				throwCause(new IllegalStateException("listener is disabled"), false);
			}

			// get pool size the method use.
			try {
				return method.invoke(inner, args);
			} catch (Exception e) {
				throwCause(e, false);
				throw new AssertionError("can't get here");
			}
		}

		private Exception throwCause(Exception e, boolean combineStackTraces) throws Exception {
			Throwable cause = e.getCause();
			if (cause == null) {
				throw e;
			}
			if (combineStackTraces) {
				StackTraceElement[] combined =
						ObjectArrays.concat(cause.getStackTrace(), e.getStackTrace(), StackTraceElement.class);
				cause.setStackTrace(combined);
			}
			if (cause instanceof Exception) {
				throw (Exception) cause;
			}
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			// The cause is a weird kind of Throwable, so throw the outer
			// exception.
			throw e;
		}

	}

}
