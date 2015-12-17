//package com.infinities.skyport.async.impl.trace;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.InputStream;
//import java.sql.SQLException;
//import java.util.Collection;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import javax.persistence.EntityManager;
//
//import org.dbunit.DatabaseUnitException;
//import org.dbunit.database.DatabaseConfig;
//import org.dbunit.database.DatabaseConnection;
//import org.dbunit.database.IDatabaseConnection;
//import org.dbunit.dataset.IDataSet;
//import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
//import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
//import org.dbunit.operation.DatabaseOperation;
//import org.hibernate.HibernateException;
//import org.hibernate.internal.SessionImpl;
//import org.jmock.Expectations;
//import org.jmock.Mockery;
//import org.jmock.api.Invocation;
//import org.jmock.integration.junit4.JUnit4Mockery;
//import org.jmock.lib.action.CustomAction;
//import org.jmock.lib.concurrent.Synchroniser;
//import org.jmock.lib.legacy.ClassImposteriser;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import com.google.common.util.concurrent.ListenableFuture;
//import com.infinities.skyport.async.AsyncDriver;
//import com.infinities.skyport.async.TaskStore;
//import com.infinities.skyport.async.command.AsyncCommandFactory.CommandEnum;
//import com.infinities.skyport.distributed.DistributedObjectFactory.Delegate;
//import com.infinities.skyport.distributed.DistributedThreadPool;
//import com.infinities.skyport.distributed.util.DistributedUtil;
//import com.infinities.skyport.entity.Job;
//import com.infinities.skyport.jpa.EntityManagerFactoryBuilder;
//import com.infinities.skyport.jpa.EntityManagerHelper;
//import com.infinities.skyport.jpa.JpaProperties;
//import com.infinities.skyport.jpa.impl.JobHome;
//import com.infinities.skyport.model.PoolConfig;
//import com.infinities.skyport.model.PoolSize;
//import com.infinities.skyport.service.jpa.IJobHome;
//
//public class TracedAsyncTaskCancelTest {
//
//	private static IDatabaseConnection connection;
//	private static IDataSet dataset;
//	protected Mockery context = new JUnit4Mockery() {
//
//		{
//			setThreadingPolicy(new Synchroniser());
//			setImposteriser(ClassImposteriser.INSTANCE);
//		}
//	};
//	private TracedAsyncTask<Boolean> task, task2;
//	private TracedAsyncVirtTask<Boolean, Boolean> vtask, vtask2;
//	private String key;
//	private String jobid, jobid2;
//	private AsyncDriver driver;
//	private long time;
//	private static DistributedThreadPool threadPool;
//	private TaskStore taskStore;
//	private static ScheduledExecutorService scheduler;
//
//
//	// protected static EntityManager entityManager;
//
//	@BeforeClass
//	public static void initEntityManager() throws HibernateException, DatabaseUnitException, SQLException {
//		JpaProperties.PERSISTENCE_UNIT_NAME = "com.infinities.skyport.jpa.test";
//		JpaProperties.JPA_PROPERTIES_FILE = null;
//		FlatXmlDataSetBuilder flatXmlDataSetBuilder = new FlatXmlDataSetBuilder();
//		flatXmlDataSetBuilder.setColumnSensing(true);
//		InputStream dataSet = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-canceltask-data.xml");
//		dataset = flatXmlDataSetBuilder.build(dataSet);
//		EntityManager entityManager = EntityManagerHelper.getEntityManager();
//		connection = new DatabaseConnection(((SessionImpl) (entityManager.getDelegate())).connection());
//		connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
//		try {
//			DatabaseOperation.INSERT.execute(connection, dataset);
//		} finally {
//			connection.close();
//			entityManager.close();
//		}
//	}
//
//	@AfterClass
//	public static void closeEntityManager() {
//		EntityManagerFactoryBuilder.shutdown();
//		EntityManagerHelper.factoryLocal.remove();
//		EntityManagerHelper.threadLocal.remove();
//	}
//
//	/**
//	 * Will clean the dataBase before each test
//	 * 
//	 * @throws SQLException
//	 * @throws DatabaseUnitException
//	 */
//	@Before
//	public void cleanDB() throws DatabaseUnitException, SQLException {
//	}
//
//	@Before
//	public void setUp() throws Exception {
//		taskStore = context.mock(TaskStore.class);
//		driver = context.mock(AsyncDriver.class);
//		key = UUID.randomUUID().toString();
//		jobid = "1";
//		jobid2 = "2";
//		time = System.currentTimeMillis();
//		task = new TracedAsyncTask<Boolean>(CommandEnum.FlushCache, key, 1L, jobid, driver, time);
//		task.init();
//
//		task2 = new TracedAsyncTask<Boolean>(CommandEnum.FlushCache, key, 2L, jobid2, driver, time);
//		task2.init();
//
//		vtask = new TracedAsyncVirtTask<Boolean, Boolean>(CommandEnum.CreateVm, true, key, 1L, "uuid", jobid, driver, time);
//		vtask.init();
//
//		CommandEnum c = CommandEnum.CreateVm;
//		vtask2 = new TracedAsyncVirtTask<Boolean, Boolean>(c, true, key, 2L, "uuid", jobid2, driver, time);
//		vtask2.init();
//
//		DistributedUtil.defaultDelegate = Delegate.disabled;
//		scheduler = Executors.newScheduledThreadPool(1);
//		threadPool = DistributedUtil.getDefaultDistributedObjectFactory().getThreadPool("test", new PoolConfig(1, 1, 60, 3),
//				new PoolConfig(1, 1, 60, 3), new PoolConfig(1, 1, 60, 3), scheduler, driver, taskStore);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		context.assertIsSatisfied();
//	}
//
//	@Test(expected = ExecutionException.class)
//	public void testJobNotFound() throws Exception {
//		TracedAsyncTask<Boolean> task3 = new TracedAsyncTask<Boolean>(CommandEnum.FlushCache, key, 3L, "3", driver, time);
//		task3.init();
//		final AtomicInteger atomic = new AtomicInteger(0);
//
//		context.checking(new Expectations() {
//
//			{
//				oneOf(driver).flushCache();
//				will(new CustomAction(null) {
//
//					@Override
//					public Object invoke(Invocation invocation) throws Throwable {
//						Thread.sleep(2000);
//						atomic.addAndGet(1);
//						return null;
//					}
//
//				});
//
//			}
//		});
//
//		ListenableFuture<Boolean> future = threadPool.getThreadPool(PoolSize.LONG).submit(task);
//		ListenableFuture<Boolean> future2 = threadPool.getThreadPool(PoolSize.LONG).submit(task3);
//		assertTrue(future.get().booleanValue());
//		assertEquals(1, atomic.get());
//		future2.get();
//	}
//
//	@Test(expected = ExecutionException.class)
//	public void testJobNotFoundWhenMerge() throws Exception {
//		IJobHome jobHome = new CustomJobHome();
//		task2.jobHome = jobHome;
//		ListenableFuture<Boolean> future = threadPool.getThreadPool(PoolSize.LONG).submit(task2);
//		future.get().booleanValue();
//	}
//
//	@Test(expected = ExecutionException.class)
//	public void testVirtJobNotFound() throws Exception {
//
//		TracedAsyncVirtTask<Boolean, Boolean> task3 = new TracedAsyncVirtTask<Boolean, Boolean>(CommandEnum.CreateVm, true,
//				key, 3L, "uuid", "jobid3", driver, time);
//		task3.init();
//
//		ListenableFuture<Boolean> future2 = threadPool.getThreadPool(PoolSize.LONG).submit(task3);
//		future2.get();
//	}
//
//	@Test(expected = ExecutionException.class)
//	public void testVirtJobNotFoundWhenMerge() throws Exception {
//		IJobHome jobHome = new CustomJobHome();
//		vtask.jobHome = jobHome;
//		ListenableFuture<Boolean> future = threadPool.getThreadPool(PoolSize.LONG).submit(vtask);
//		future.get();
//
//	}
//
//
//	class CustomJobHome implements IJobHome {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		IJobHome proxy = new JobHome();
//
//
//		@Override
//		public void persist(Job transientInstance) {
//			proxy.persist(transientInstance);
//		}
//
//		@Override
//		public void remove(Job persistentInstance) {
//			proxy.remove(persistentInstance);
//		}
//
//		@Override
//		public Job merge(final Job detachedInstance) {
//			Thread t = new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					Job job = proxy.findById(detachedInstance.getId());
//					proxy.remove(job);
//					try {
//						EntityManagerHelper.commitAndClose();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			});
//			t.start();
//			try {
//				Thread.sleep(2000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//
//			return proxy.merge(detachedInstance);
//		}
//
//		@Override
//		public Job findById(Object id) {
//			return proxy.findById(id);
//		}
//
//		@Override
//		public List<Job> findAll() {
//			return proxy.findAll();
//		}
//
//		@Override
//		public void refresh(Job detachedInstance) {
//			proxy.refresh(detachedInstance);
//		}
//
//		@Override
//		public List<Job> findByExecutorKey(String executorKey) {
//			return proxy.findByExecutorKey(executorKey);
//		}
//
//		@Override
//		public List<Job> findAll(Collection<String> keys) {
//			return proxy.findAll(keys);
//		}
//
//		@Override
//		public Job findByEventid(Object eventid) {
//			return proxy.findByEventid(eventid);
//		}
//	}
//
// }
