package com.infinities.skyport.quartz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.util.concurrent.FutureCallback;
import com.infinities.skyport.model.Time;

public class QuartzConfiguration<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Callable<T> callable;
	private long initialDelay;
	private Time time;
	private List<FutureCallback<T>> callbacks = new ArrayList<FutureCallback<T>>();
	private String name;
	private Precondition condition;


	public QuartzConfiguration(Callable<T> callable, long initialDelay, List<FutureCallback<T>> callbacks, String name,
			Time time, Precondition condition) {
		super();
		this.callable = callable;
		this.initialDelay = initialDelay;
		this.callbacks = callbacks;
		this.name = name;
		this.setTime(time);
		this.condition = condition;
	}

	public Callable<T> getCallable() {
		return callable;
	}

	public void setCallable(Callable<T> callable) {
		this.callable = callable;
	}

	public long getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}

	public List<FutureCallback<T>> getCallbacks() {
		return callbacks;
	}

	public void setCallbacks(List<FutureCallback<T>> callbacks) {
		this.callbacks = callbacks;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addCallback(FutureCallback<T> callback) {
		this.callbacks.add(callback);
	}

	public boolean isEnable() {
		return condition.check();
	}

	public Precondition getCondition() {
		return condition;
	}

	public void setCondition(Precondition condition) {
		this.condition = condition;
	}

	public Time getTime() {
		return time;
	}

	public void setTime(Time time) {
		this.time = time;
	}


	public interface Precondition {

		boolean check();
	}

	public static class Builder<T> {

		private Callable<T> callable;
		private long initialDelay = 1;
		private Time time;
		private List<FutureCallback<T>> callbacks = new ArrayList<FutureCallback<T>>();
		private String name;
		private Precondition condition;


		public Builder(Callable<T> callable, Precondition condition) {
			this.condition = condition;
			this.callable = callable;
		}

		public Builder<T> name(final String name) {
			this.name = name;
			return this;
		}

		public Builder<T> initialDelay(final long initialDelay) {
			this.initialDelay = initialDelay;
			return this;
		}

		public Builder<T> delay(final Time time) {
			this.time = time;
			return this;
		}

		public Builder<T> addCallback(final FutureCallback<T> callback) {
			this.callbacks.add(callback);
			return this;
		}

		public QuartzConfiguration<T> build() {
			return new QuartzConfiguration<T>(callable, initialDelay, callbacks, name, time, condition);
		}
	}
}
