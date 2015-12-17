package com.infinities.skyport.async.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider.TaskType;
import com.infinities.skyport.async.AsyncTask;
import com.infinities.skyport.entity.TaskEvent;
import com.infinities.skyport.entity.TaskEventLog;
import com.infinities.skyport.entity.TaskEventLog.Status;
import com.infinities.skyport.exception.TaskCancelledException;
import com.infinities.skyport.jpa.EntityManagerHelper;
import com.infinities.skyport.jpa.impl.TaskEventHome;
import com.infinities.skyport.jpa.impl.TaskEventLogHome;
import com.infinities.skyport.model.ThrowableWrapper;
import com.infinities.skyport.service.jpa.ITaskEventHome;
import com.infinities.skyport.service.jpa.ITaskEventLogHome;
import com.infinities.skyport.util.ExceptionUtil;

public class AsyncTaskImpl implements AsyncTask<Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(AsyncTaskImpl.class);
	private long eventid;
	private TaskType taskType;
	private String methodName;
	private Object[] args;
	private transient ServiceProvider serviceProvider;
	protected transient ITaskEventHome taskEventHome;
	protected transient ITaskEventLogHome taskEventLogHome;


	public AsyncTaskImpl() {
		taskEventHome = new TaskEventHome();
		taskEventLogHome = new TaskEventLogHome();
	}

	@Override
	public void setServiceProvider(ServiceProvider serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

	@Override
	public Object call() throws Exception {
		Object ret = null;
		try {
			Object support = TaskType.getSupport(taskType, serviceProvider);
			final Method innerMethod = AsyncHandler.getMethod(support, methodName, args);
			commitLog(Status.Executing, eventid, "Task executing", null);
			ret = innerMethod.invoke(support, args);
		} catch (TaskCancelledException e) {
			throw e;
		} catch (Exception e) {
			onFailure(e);
			throw e;
		} catch (Error e) {
			onFailure(e);
			throw e;
		}
		onSuccess(ret);
		return ret;
	}

	private void onFailure(Throwable t) {
		Throwable root = ExceptionUtil.getRootCause(t);
		ThrowableWrapper wrapper = new ThrowableWrapper(root);
		commitLog(Status.Fail, eventid, "Task failed: " + root.getMessage(), wrapper);
	}

	private void onSuccess(Object ret) {
		commitLog(Status.Success, eventid, "Task complete successfully", "");
	}

	public TaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public ServiceProvider getServiceProvider() {
		return serviceProvider;
	}

	@Override
	public long getEventid() {
		return eventid;
	}

	@Override
	public void setEventid(long eventid) {
		this.eventid = eventid;
	}

	protected void commitLog(Status status, long eventid, String msg, Serializable detail) {
		try {
			TaskEvent event = taskEventHome.findById(eventid);
			TaskEventLog log = new TaskEventLog(event, new Date(), status, msg, null);
			taskEventLogHome.persist(log);
		} finally {
			try {
				EntityManagerHelper.commitAndClose();
			} catch (Exception e) {
				logger.warn("commit log failed, please check the database.", e);
			}
		}
	}

}
