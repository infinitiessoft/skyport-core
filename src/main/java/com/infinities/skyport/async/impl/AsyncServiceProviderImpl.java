package com.infinities.skyport.async.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.util.NamingConstraints;
import org.dasein.cloud.util.ResourceNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.async.AsyncServiceProvider;
import com.infinities.skyport.async.impl.service.concurrent.AsyncAdminServicesImplLazyInitializer;
import com.infinities.skyport.async.impl.service.concurrent.AsyncCIServicesImplLazyInitializer;
import com.infinities.skyport.async.impl.service.concurrent.AsyncComputeServicesImplLazyInitializer;
import com.infinities.skyport.async.impl.service.concurrent.AsyncDataCenterServicesImplLazyInitializer;
import com.infinities.skyport.async.impl.service.concurrent.AsyncIdentityServicesImplLazyInitializer;
import com.infinities.skyport.async.impl.service.concurrent.AsyncNetworkServicesImplLazyInitializer;
import com.infinities.skyport.async.impl.service.concurrent.AsyncPlatformServicesImplLazyInitializer;
import com.infinities.skyport.async.impl.service.concurrent.AsyncStorageServicesImplLazyInitializer;
import com.infinities.skyport.async.service.AsyncAdminServices;
import com.infinities.skyport.async.service.AsyncCIServices;
import com.infinities.skyport.async.service.AsyncComputeServices;
import com.infinities.skyport.async.service.AsyncDataCenterServices;
import com.infinities.skyport.async.service.AsyncIdentityServices;
import com.infinities.skyport.async.service.AsyncNetworkServices;
import com.infinities.skyport.async.service.AsyncPlatformServices;
import com.infinities.skyport.async.service.AsyncStorageServices;
import com.infinities.skyport.distributed.DistributedObjectFactory;
import com.infinities.skyport.distributed.DistributedThreadPool;
import com.infinities.skyport.distributed.util.DistributedUtil;
import com.infinities.skyport.entity.TaskEvent;
import com.infinities.skyport.entity.TaskEventLog;
import com.infinities.skyport.entity.TaskEventLog.Status;
import com.infinities.skyport.jpa.EntityManagerHelper;
import com.infinities.skyport.jpa.impl.TaskEventHome;
import com.infinities.skyport.jpa.impl.TaskEventLogHome;
import com.infinities.skyport.model.PoolSize;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.ConfigurationLifeCycleListener;
import com.infinities.skyport.service.jpa.ITaskEventHome;
import com.infinities.skyport.service.jpa.ITaskEventLogHome;

public class AsyncServiceProviderImpl implements AsyncServiceProvider, ConfigurationLifeCycleListener {

	private static final Logger logger = LoggerFactory.getLogger(AsyncServiceProviderImpl.class);
	private final ServiceProvider inner;
	private Configuration configuration;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private DistributedThreadPool threadPools;

	protected AsyncStorageServicesImplLazyInitializer asyncStorageServicesLazyInitializer;
	protected AsyncAdminServicesImplLazyInitializer asyncAdminServicesLazyInitializer;
	protected AsyncCIServicesImplLazyInitializer asyncCIServicesLazyInitializer;
	protected AsyncComputeServicesImplLazyInitializer asyncComputeServicesLazyInitializer;
	protected AsyncIdentityServicesImplLazyInitializer asyncIdentityServicesLazyInitializer;
	protected AsyncNetworkServicesImplLazyInitializer asyncNetworkServicesLazyInitializer;
	protected AsyncPlatformServicesImplLazyInitializer asyncPlatformServicesLazyInitializer;
	protected AsyncDataCenterServicesImplLazyInitializer asyncDataCenterServicesLazyInitializer;

	protected ITaskEventHome taskEventHome = new TaskEventHome();
	protected ITaskEventLogHome taskEventLogHome = new TaskEventLogHome();


	public AsyncServiceProviderImpl(ServiceProvider inner, Configuration configuration,
			ListeningScheduledExecutorService scheduler) throws Exception {
		this.inner = inner;
		this.configuration = configuration;
		String distributedKey = DistributedUtil.generateDistributedName(configuration);
		DistributedObjectFactory objectFactory = DistributedUtil.getDistributedObjectFactory(distributedKey);
		this.threadPools =
				objectFactory.getThreadPool(distributedKey, configuration.getShortPoolConfig(),
						configuration.getMediumPoolConfig(), configuration.getLongPoolConfig(), scheduler, inner);
		String id = configuration.getId();
		this.asyncStorageServicesLazyInitializer =
				new AsyncStorageServicesImplLazyInitializer(id, inner, configuration.getStorageConfiguration(), threadPools);
		this.asyncAdminServicesLazyInitializer =
				new AsyncAdminServicesImplLazyInitializer(id, inner, configuration.getAdminConfiguration(), threadPools);
		this.asyncCIServicesLazyInitializer =
				new AsyncCIServicesImplLazyInitializer(id, inner, configuration.getcIConfiguration(), threadPools);
		this.asyncComputeServicesLazyInitializer =
				new AsyncComputeServicesImplLazyInitializer(id, inner, configuration.getComputeConfiguration(), threadPools);
		this.asyncIdentityServicesLazyInitializer =
				new AsyncIdentityServicesImplLazyInitializer(id, inner, configuration.getIdentityConfiguration(),
						threadPools);
		this.asyncNetworkServicesLazyInitializer =
				new AsyncNetworkServicesImplLazyInitializer(id, inner, configuration.getNetworkConfiguration(), threadPools);
		this.asyncPlatformServicesLazyInitializer =
				new AsyncPlatformServicesImplLazyInitializer(id, inner, configuration.getPlatformConfiguration(),
						threadPools);
		this.asyncDataCenterServicesLazyInitializer =
				new AsyncDataCenterServicesImplLazyInitializer(id, inner, configuration.getDataCenterConfiguration(),
						threadPools);
	}

	@Override
	public void initialize() throws Exception {
		if (isInitialized.compareAndSet(false, true)) {
			inner.initialize();
			terminateUnfinishEvent();
		} else {
			throw new IllegalStateException("object have been initialized");
		}
	}

	@Override
	public String testContext() {
		return inner.testContext();
	}

	@Override
	public boolean isConnected() {
		return inner.isConnected();
	}

	@Override
	public boolean hasAdminServices() {
		return inner.hasAdminServices();
	}

	@Override
	public boolean hasCIServices() {
		return inner.hasCIServices();
	}

	@Override
	public boolean hasComputeServices() {
		return inner.hasComputeServices();
	}

	@Override
	public boolean hasIdentityServices() {
		return inner.hasIdentityServices();
	}

	@Override
	public boolean hasNetworkServices() {
		return inner.hasNetworkServices();
	}

	@Override
	public boolean hasPlatformServices() {
		return inner.hasPlatformServices();
	}

	@Override
	public boolean hasStorageServices() {
		return inner.hasStorageServices();
	}

	@Override
	public AsyncStorageServices getStorageServices() throws ConcurrentException {
		return asyncStorageServicesLazyInitializer.get();
	}

	@Override
	public AsyncAdminServices getAdminServices() throws ConcurrentException {
		return asyncAdminServicesLazyInitializer.get();
	}

	@Override
	public ProviderContext getContext() {
		return inner.getContext();
	}

	@Override
	public ContextRequirements getContextRequirements() {
		return inner.getContextRequirements();
	}

	@Override
	public AsyncDataCenterServices getDataCenterServices() throws ConcurrentException {
		return asyncDataCenterServicesLazyInitializer.get();
	}

	@Override
	public AsyncCIServices getCIServices() throws ConcurrentException {
		return asyncCIServicesLazyInitializer.get();
	}

	@Override
	public AsyncComputeServices getComputeServices() throws ConcurrentException {
		return asyncComputeServicesLazyInitializer.get();
	}

	@Override
	public AsyncIdentityServices getIdentityServices() throws ConcurrentException {
		return asyncIdentityServicesLazyInitializer.get();
	}

	@Override
	public AsyncNetworkServices getNetworkServices() throws ConcurrentException {
		return asyncNetworkServicesLazyInitializer.get();
	}

	@Override
	public AsyncPlatformServices getPlatformServices() throws ConcurrentException {
		return asyncPlatformServicesLazyInitializer.get();
	}

	@Override
	public String findUniqueName(String baseName, NamingConstraints constraints, ResourceNamespace namespace)
			throws CloudException, InternalException {
		return inner.findUniqueName(baseName, constraints, namespace);
	}

	// for testing
	public ServiceProvider getInner() {
		return inner;
	}

	@Override
	public void close() throws ConcurrentException {
		if (isInitialized.compareAndSet(true, false)) {
			if (threadPools != null) {
				try {
					threadPools.shutdown();
				} catch (Exception e) {
					logger.warn("ignore", e);
				}
			}
			try {
				this.asyncStorageServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.asyncAdminServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.asyncCIServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.asyncComputeServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.asyncIdentityServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.asyncNetworkServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			try {
				this.asyncPlatformServicesLazyInitializer.get().close();
			} catch (NullPointerException e) {
				// ignore
			} catch (Exception e) {
				logger.warn("close service failed", e);
			}
			inner.close();
		}
	}

	@Override
	public Configuration getConfiguration() {
		Configuration configuration = this.configuration.clone();
		return configuration;
	}

	// for testing
	public DistributedThreadPool getThreadPools() {
		return threadPools;
	}

	@Override
	public void persist(Configuration configuration) {
		// ignore
	}

	@Override
	public void lightMerge(Configuration configuration) {
		if (configuration.getId().equals(this.configuration.getId())) {
			Configuration original = this.configuration;
			Configuration clone = configuration;

			// change thread pool size;

			if (original.getLongPoolConfig().getCoreSize() != clone.getLongPoolConfig().getCoreSize()) {
				getThreadPools().getThreadPool(PoolSize.LONG).setCorePoolSize(clone.getLongPoolConfig().getCoreSize());
			}

			if (original.getLongPoolConfig().getKeepAlive() != clone.getLongPoolConfig().getKeepAlive()) {
				getThreadPools().getThreadPool(PoolSize.LONG).setKeepAliveTime(clone.getLongPoolConfig().getKeepAlive(),
						TimeUnit.SECONDS);
			}

			if (original.getLongPoolConfig().getMaxSize() != clone.getLongPoolConfig().getMaxSize()) {
				getThreadPools().getThreadPool(PoolSize.LONG).setMaximumPoolSize(clone.getLongPoolConfig().getMaxSize());
			}

			if (original.getMediumPoolConfig().getCoreSize() != clone.getMediumPoolConfig().getCoreSize()) {
				getThreadPools().getThreadPool(PoolSize.MEDIUM).setCorePoolSize(clone.getMediumPoolConfig().getCoreSize());
			}

			if (original.getMediumPoolConfig().getKeepAlive() != clone.getMediumPoolConfig().getKeepAlive()) {
				getThreadPools().getThreadPool(PoolSize.MEDIUM).setKeepAliveTime(clone.getMediumPoolConfig().getKeepAlive(),
						TimeUnit.SECONDS);
			}

			if (original.getMediumPoolConfig().getMaxSize() != clone.getMediumPoolConfig().getMaxSize()) {
				getThreadPools().getThreadPool(PoolSize.MEDIUM).setMaximumPoolSize(clone.getMediumPoolConfig().getMaxSize());
			}

			if (original.getShortPoolConfig().getCoreSize() != clone.getShortPoolConfig().getCoreSize()) {
				getThreadPools().getThreadPool(PoolSize.SHORT).setCorePoolSize(clone.getShortPoolConfig().getCoreSize());
			}

			if (original.getShortPoolConfig().getKeepAlive() != clone.getShortPoolConfig().getKeepAlive()) {
				getThreadPools().getThreadPool(PoolSize.SHORT).setKeepAliveTime(clone.getShortPoolConfig().getKeepAlive(),
						TimeUnit.SECONDS);
			}

			if (original.getShortPoolConfig().getMaxSize() != clone.getShortPoolConfig().getMaxSize()) {
				getThreadPools().getThreadPool(PoolSize.SHORT).setMaximumPoolSize(clone.getShortPoolConfig().getMaxSize());
			}
			this.configuration = configuration;

		}
	}

	@Override
	public void heavyMerge(Configuration configuration) {
		// ignore
	}

	@Override
	public void remove(Configuration configuration) {
		// ignore
	}

	@Override
	public void clear() {
		// ignore
	}

	private void terminateUnfinishEvent() {
		logger.debug("interrupting unfinish tasks");
		final List<TaskEvent> events = taskEventHome.findAllUnfinishTask();
		final Date date = new Date();
		for (final TaskEvent event : events) {
			logger.debug("interrupting task: {}, config: {}, cmd: {}", new Object[] { event.getId(), event.getConfig(),
					event.getCmd() });
			taskEventLogHome.persist(new TaskEventLog(event, date, Status.Fail,
					"Task has been interrupted unexpectively because skyport restart.", null));
			try {
				EntityManagerHelper.commitAndClose();
			} catch (Exception e) {
				logger.error("interrupt unfinish tasks failed", e);
			}
		}
	}

	// for testing
	public void setTaskEventHome(ITaskEventHome taskEventHome) {
		this.taskEventHome = taskEventHome;
	}

	// for testing
	public void setTaskEventLogHome(ITaskEventLogHome taskEventLogHome) {
		this.taskEventLogHome = taskEventLogHome;
	}

}
