package com.infinities.skyport.quartz;

import java.util.Collection;
import java.util.EnumMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

public class QuartzServiceImpl<K extends Enum<K>> implements QuartzService<K> {

	private static final Logger logger = LoggerFactory.getLogger(QuartzServiceImpl.class);
	private final ListeningScheduledExecutorService scheduler;
	private final ListeningExecutorService worker;
	private final AtomicBoolean isShutdown = new AtomicBoolean(false);
	private volatile EnumMap<K, ScheduledFuture<?>> taskMap;
	private final EnumMap<K, QuartzConfiguration<?>> map;


	public QuartzServiceImpl(Class<K> c, ListeningScheduledExecutorService scheduler, ListeningExecutorService worker) {
		map = new EnumMap<K, QuartzConfiguration<?>>(c);
		this.scheduler = scheduler;
		this.worker = worker;
		taskMap = new EnumMap<K, ScheduledFuture<?>>(c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.quartz.QuartzServuce#schedule(K,
	 * com.infinities.skyport.quartz.QuartzConfiguration)
	 */
	@Override
	public <T> ScheduledFuture<?> schedule(K k, QuartzConfiguration<T> configuration) {
		ScheduledFuture<?> task = execute(k, configuration, configuration.getInitialDelay(), true);
		return task;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.quartz.QuartzServuce#executeOnce(K)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> ScheduledFuture<?> executeOnce(K k) {
		if (!map.containsKey(k)) {
			throw new IllegalArgumentException("quartz :" + k + " is not existed");
		}
		QuartzConfiguration<T> configuration = (QuartzConfiguration<T>) map.get(k);
		ScheduledTask<T> outer = new ScheduledTask<T>(k, configuration, false, worker);
		ScheduledFuture<?> task = scheduler.schedule(outer, configuration.getInitialDelay(), TimeUnit.SECONDS);
		return task;
	}

	private <T> ScheduledFuture<?> execute(K k, QuartzConfiguration<T> configuration, long delay, boolean periodic) {
		if (map.containsKey(k)) {
			throw new IllegalArgumentException("duplicate quartz :" + k);
		}
		ScheduledTask<T> outer = new ScheduledTask<T>(k, configuration, periodic, worker);
		ScheduledFuture<?> task = scheduler.schedule(outer, delay, configuration.getTime().getUnit());
		map.put(k, configuration);
		taskMap.put(k, task);
		return task;
	}

	private <T> FutureCallback<T> rescheduledCallback(final K k, final QuartzConfiguration<T> configuration,
			final boolean periodic, final ListeningExecutorService worker) {
		return new FutureCallback<T>() {

			@Override
			public void onSuccess(T result) {
				reschedule(k, configuration, periodic, worker);
			}

			@Override
			public void onFailure(Throwable t) {
				reschedule(k, configuration, periodic, worker);
			}
		};
	}

	private <T> void
			reschedule(K k, QuartzConfiguration<T> configuration, boolean periodic, ListeningExecutorService worker) {
		if (!isShutdown.get()) {
			synchronized (taskMap) {
				if (periodic) {
					ScheduledTask<T> outer = new ScheduledTask<T>(k, configuration, periodic, worker);
					taskMap.put(k, scheduler.schedule(outer, configuration.getTime().getNumber(), configuration.getTime()
							.getUnit()));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.quartz.QuartzServuce#close()
	 */
	@Override
	public synchronized void close() {
		isShutdown.set(true);
		for (ScheduledFuture<?> task : taskMap.values()) {
			task.cancel(true);
		}
		taskMap.clear();
		map.clear();
	}


	class ScheduledTask<T> extends Thread {

		private K k;
		private QuartzConfiguration<T> configuration;
		private final Callable<T> inner;
		private final boolean periodic;
		private final Collection<FutureCallback<T>> callbacks;
		private final ListeningExecutorService worker;
		private ListenableFuture<T> mainTask;


		private ScheduledTask(K k, QuartzConfiguration<T> configuration, boolean periodic, ListeningExecutorService worker) {
			super(configuration.getName());
			this.k = k;
			this.inner = configuration.getCallable();
			this.periodic = periodic;
			this.callbacks = configuration.getCallbacks();
			this.worker = worker;
			this.configuration = configuration;
		}

		@Override
		public void run() {
			if (configuration.isEnable()) {
				try {
					// maintask
					mainTask = worker.submit(inner);
					// timeout
					Futures.addCallback(mainTask, rescheduledCallback(k, configuration, periodic, worker), worker);
					for (FutureCallback<T> callback : callbacks) {
						Futures.addCallback(mainTask, callback, worker);
					}
				} catch (Exception e) {
					logger.warn("encounter exception when schedule task ", e);
					if (mainTask != null) {
						mainTask.cancel(true);
					}
				}
			} else {
				if (periodic) {
					reschedule(k, configuration, periodic, worker);
				}
			}
		}
	}

	// public void setDelay(K k, long delay) {
	// map.get(k).setDelay(delay);
	// }

}
