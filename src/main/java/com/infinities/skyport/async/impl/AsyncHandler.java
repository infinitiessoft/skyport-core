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
package com.infinities.skyport.async.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ObjectArrays;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.util.concurrent.ListenableFuture;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncResult;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.distributed.DistributedExecutor;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.entity.TaskEvent;
import com.infinities.skyport.entity.TaskEventLog;
import com.infinities.skyport.entity.TaskEventLog.Status;
import com.infinities.skyport.jpa.EntityManagerHelper;
import com.infinities.skyport.jpa.impl.TaskEventHome;
import com.infinities.skyport.jpa.impl.TaskEventLogHome;
import com.infinities.skyport.model.FunctionConfiguration;
import com.infinities.skyport.model.PoolSize;
import com.infinities.skyport.model.ThrowableWrapper;
import com.infinities.skyport.service.jpa.ITaskEventHome;
import com.infinities.skyport.service.jpa.ITaskEventLogHome;

public class AsyncHandler extends AbstractInvocationHandler {

	private static final Logger logger = LoggerFactory.getLogger(AsyncHandler.class);

	private final ServiceProvider inner;
	private final String configurationId;
	private final TaskType taskType;
	private final DistributedThreadPool pools;
	private final Object configuration;

	protected ITaskEventHome taskEventHome = new TaskEventHome();
	protected ITaskEventLogHome taskEventLogHome = new TaskEventLogHome();


	public AsyncHandler(String configurationId, TaskType taskType, ServiceProvider inner, DistributedThreadPool pools,
			Object configuration) {
		this.configurationId = configurationId;
		this.taskType = taskType;
		this.inner = inner;
		this.pools = pools;
		this.configuration = configuration;
	}

	@Override
	protected Object handleInvocation(Object proxy, Method method, final Object[] args) throws Throwable {
		Object support = TaskType.getSupport(taskType, inner);
		if ("getSupport".equals(method.getName()) && args.length == 0) {
			return support;
		}
		final Method innerMethod = AsyncHandler.getMethod(support, method.getName(), method.getParameterTypes());
		PoolSize poolSize = PoolSize.SHORT;
		String methodName = method.getName();

		if (!AsyncResult.class.equals(method.getReturnType())) {
			return innerMethod.invoke(inner, args);
		}

		TaskEvent event = TaskEvent.getInitializedEvent(null, method.getName(), configurationId);
		taskEventHome.persist(event);
		TaskEventLog log = new TaskEventLog(event, new Date(), Status.Initiazing, "Task is initialized", null);
		taskEventLogHome.persist(log);
		try {
			EntityManagerHelper.commitAndClose();
		} catch (Exception ex) {
			logger.warn("commit log failed, please check the database.", ex);
			throw ex;
		}

		// get pool size the method use.
		try {
			FunctionConfiguration functionConfiguration =
					(FunctionConfiguration) PropertyUtils.getProperty(configuration, methodName);
			poolSize = functionConfiguration.getPoolSize();
		} catch (Exception e) {
			throwCause(e, false);
			throw new AssertionError("can't get here");
		}

		DistributedExecutor pool = pools.getThreadPool(poolSize);

		// //save job;

		AsyncTaskImpl task = new AsyncTaskImpl();
		task.setArgs(args);
		task.setMethodName(method.getName());
		task.setServiceProvider(inner);
		task.setTaskType(taskType);
		task.setParameterTypes(method.getParameterTypes());

		try {
			ListenableFuture<Object> future = pool.submit(task);
			AsyncResult<Object> asyncResult = new AsyncResult<Object>(future);
			return asyncResult;
		} catch (Exception e) {
			try {
				ThrowableWrapper wrapper = new ThrowableWrapper(e);
				commitLog(Status.Fail, event.getId(), "Task has been rejected", wrapper);
				logger.error("encounter exception while schedule a task", e);
			} catch (Exception ex) {
				logger.warn("unexpected exception in callback obReject event", ex);
			}

			throw e;
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

	protected void commitLog(Status status, long eventid, String msg, Serializable detail) {
		try {
			TaskEvent event = taskEventHome.findById(eventid);
			TaskEventLog log = new TaskEventLog(event, new Date(), status, msg, detail);
			taskEventLogHome.persist(log);
			logger.debug("commit log: {}, {}", new Object[] { status, eventid });
		} finally {
			try {
				EntityManagerHelper.commitAndClose();
			} catch (Exception e) {
				logger.error("commit log fail for event: {}", eventid);
				logger.warn("commit log failed, please check the database.", e);
			}
		}
	}

	protected static Method getMethod(Object support, String methodName, Class<?>[] parameterTypes)
			throws NoSuchMethodException, SecurityException {
		// Class<?>[] parameterTypes = null;
		//
		// if (args != null) {
		// parameterTypes = new Class<?>[args.length];
		// for (int i = 0; i < args.length; i++) {
		// parameterTypes[i] = args[i].getClass();
		// }
		// }
		try {
			return support.getClass().getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			logger.error("class:{}, method:{}, args:{}", new Object[] { support.getClass(), methodName, parameterTypes });
			throw e;
		}
	}
}
