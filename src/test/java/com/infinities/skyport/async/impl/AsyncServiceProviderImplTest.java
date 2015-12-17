package com.infinities.skyport.async.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.dc.DataCenterServices;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.entity.TaskEvent;
import com.infinities.skyport.entity.TaskEventLog;
import com.infinities.skyport.entity.TaskEventLog.Status;
import com.infinities.skyport.jpa.EntityManagerHelper;
import com.infinities.skyport.model.configuration.Configuration;
import com.infinities.skyport.service.jpa.ITaskEventHome;
import com.infinities.skyport.service.jpa.ITaskEventLogHome;

public class AsyncServiceProviderImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private AsyncServiceProviderImpl provider;
	private ServiceProvider serviceProvider;
	private Configuration configuration;
	private ListeningScheduledExecutorService scheduler;

	private ComputeServices computeServices;
	// private NetworkServices networkServices;
	// private AdminServices adminServices;
	// private CIServices ciServices;
	// private IdentityServices identityServices;
	// private PlatformServices platformServices;
	// private StorageServices storageServices;
	private DataCenterServices dataCenterServices;

	protected ITaskEventHome taskEventHome;
	protected ITaskEventLogHome taskEventLogHome;
	protected EntityManager entityManager;
	protected EntityTransaction transaction;
	protected EntityManagerFactory factory;
	protected CriteriaBuilder cb;
	protected CriteriaQuery<TaskEvent> cq;
	protected Root<TaskEvent> root;
	protected Subquery<TaskEventLog> subQuery;
	protected Root<TaskEventLog> subRoot;
	protected Predicate fail, success, or, exist, not;
	protected Path<Object> path;
	protected TypedQuery<TaskEvent> q;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		factory = context.mock(EntityManagerFactory.class);
		entityManager = context.mock(EntityManager.class);
		transaction = context.mock(EntityTransaction.class);
		taskEventHome = context.mock(ITaskEventHome.class);
		taskEventLogHome = context.mock(ITaskEventLogHome.class);
		cb = context.mock(CriteriaBuilder.class);
		cq = context.mock(CriteriaQuery.class);
		root = context.mock(Root.class);
		subQuery = context.mock(Subquery.class);
		subRoot = context.mock(Root.class, "subroot");
		path = context.mock(Path.class);
		q = context.mock(TypedQuery.class);

		fail = context.mock(Predicate.class, "fail");
		success = context.mock(Predicate.class, "success");
		or = context.mock(Predicate.class, "or");
		exist = context.mock(Predicate.class, "exist");
		not = context.mock(Predicate.class, "not");

		EntityManagerHelper.threadLocal.set(entityManager);
		EntityManagerHelper.factoryLocal.set(factory);

		serviceProvider = context.mock(ServiceProvider.class);
		computeServices = context.mock(ComputeServices.class);
		// networkServices = context.mock(NetworkServices.class);
		// adminServices = context.mock(AdminServices.class);
		// ciServices = context.mock(CIServices.class);
		// identityServices = context.mock(IdentityServices.class);
		// platformServices = context.mock(PlatformServices.class);
		// storageServices = context.mock(StorageServices.class);
		dataCenterServices = context.mock(DataCenterServices.class);
		configuration = new Configuration();
		configuration.setId("id");
		scheduler = context.mock(ListeningScheduledExecutorService.class);

		context.checking(new Expectations() {

			{
				exactly(1).of(serviceProvider).initialize();

			}
		});
		context.checking(new Expectations() {

			{
				exactly(7).of(entityManager).isOpen();
				will(returnValue(true));
				exactly(6).of(entityManager).getTransaction();
				will(returnValue(transaction));
				exactly(3).of(transaction).isActive();
				will(returnValue(true));
				exactly(1).of(transaction).commit();
				exactly(1).of(entityManager).close();
				exactly(1).of(entityManager).persist(with(any(TaskEventLog.class)));
				exactly(1).of(entityManager).getCriteriaBuilder();
				will(returnValue(cb));
				exactly(1).of(cb).createQuery(TaskEvent.class);
				will(returnValue(cq));
				exactly(1).of(cq).from(TaskEvent.class);
				will(returnValue(root));
				exactly(1).of(cq).select(root);
				exactly(1).of(cq).subquery(TaskEventLog.class);
				will(returnValue(subQuery));
				exactly(1).of(subQuery).from(TaskEventLog.class);
				will(returnValue(subRoot));
			}
		});
		final TaskEvent event = new TaskEvent();
		event.setId(0L);
		final List<TaskEvent> events = new ArrayList<TaskEvent>();
		events.add(event);
		context.checking(new Expectations() {

			{
				exactly(1).of(subQuery).select(subRoot);
				exactly(2).of(subRoot).get("status");
				will(returnValue(path));
				exactly(1).of(cb).equal(path, Status.Fail);
				will(returnValue(fail));
				exactly(1).of(cb).equal(path, Status.Success);
				will(returnValue(success));
				exactly(1).of(cb).or(fail, success);
				will(returnValue(or));
				exactly(1).of(subQuery).where(or);
				exactly(1).of(cb).exists(subQuery);
				will(returnValue(exist));
				exactly(1).of(cb).not(exist);
				will(returnValue(not));
				exactly(1).of(cq).where(not);
				exactly(1).of(entityManager).createQuery(cq);
				will(returnValue(q));
				exactly(1).of(q).getResultList();
				will(returnValue(events));
			}
		});

		// context.checking(new Expectations() {
		//
		// {
		// exactly(1).of(serviceProvider).getAdminServices();
		// will(returnValue(adminServices));
		//
		// exactly(1).of(adminServices).hasPrepaymentSupport();
		// will(returnValue(true));
		// exactly(1).of(adminServices).hasAccountSupport();
		// will(returnValue(true));
		// exactly(1).of(adminServices).hasBillingSupport();
		// will(returnValue(true));
		// }
		// });

		// context.checking(new Expectations() {
		//
		// {
		// exactly(1).of(serviceProvider).getCIServices();
		// will(returnValue(ciServices));
		//
		// exactly(1).of(ciServices).hasConvergedHttpLoadBalancerSupport();
		// will(returnValue(true));
		// exactly(1).of(ciServices).hasConvergedInfrastructureSupport();
		// will(returnValue(true));
		// exactly(1).of(ciServices).hasTopologySupport();
		// will(returnValue(true));
		// }
		// });

		// context.checking(new Expectations() {
		//
		// {
		// exactly(1).of(serviceProvider).getIdentityServices();
		// will(returnValue(identityServices));
		//
		// exactly(1).of(identityServices).hasIdentityAndAccessSupport();
		// will(returnValue(true));
		// exactly(1).of(identityServices).hasShellKeySupport();
		// will(returnValue(true));
		// }
		// });

		// context.checking(new Expectations() {
		//
		// {
		// exactly(1).of(serviceProvider).getNetworkServices();
		// will(returnValue(networkServices));
		//
		// exactly(1).of(networkServices).hasDnsSupport();
		// will(returnValue(true));
		// exactly(1).of(networkServices).hasFirewallSupport();
		// will(returnValue(true));
		// exactly(1).of(networkServices).hasIpAddressSupport();
		// will(returnValue(true));
		// exactly(1).of(networkServices).hasLoadBalancerSupport();
		// will(returnValue(true));
		// exactly(1).of(networkServices).hasNetworkFirewallSupport();
		// will(returnValue(true));
		// exactly(1).of(networkServices).hasVlanSupport();
		// will(returnValue(true));
		// exactly(1).of(networkServices).hasVpnSupport();
		// will(returnValue(true));
		// }
		// });

		// context.checking(new Expectations() {
		//
		// {
		// exactly(1).of(serviceProvider).getPlatformServices();
		// will(returnValue(platformServices));
		//
		// exactly(1).of(platformServices).hasCDNSupport();
		// will(returnValue(true));
		// exactly(1).of(platformServices).hasDataWarehouseSupport();
		// will(returnValue(true));
		// exactly(1).of(platformServices).hasKeyValueDatabaseSupport();
		// will(returnValue(true));
		// exactly(1).of(platformServices).hasMessageQueueSupport();
		// will(returnValue(true));
		// exactly(1).of(platformServices).hasPushNotificationSupport();
		// will(returnValue(true));
		// exactly(1).of(platformServices).hasRelationalDatabaseSupport();
		// will(returnValue(true));
		// exactly(1).of(platformServices).hasMonitoringSupport();
		// will(returnValue(true));
		// }
		// });

		// context.checking(new Expectations() {
		//
		// {
		//
		// exactly(1).of(serviceProvider).getStorageServices();
		// will(returnValue(storageServices));
		//
		// exactly(1).of(storageServices).hasOfflineStorageSupport();
		// will(returnValue(true));
		//
		// exactly(1).of(storageServices).hasOnlineStorageSupport();
		// will(returnValue(true));
		// }
		// });

		// context.checking(new Expectations() {
		//
		// {
		//
		// exactly(1).of(serviceProvider).getDataCenterServices();
		// will(returnValue(dataCenterServices));
		//
		// }
		// });

		provider = new AsyncServiceProviderImpl(serviceProvider, configuration, scheduler);
		provider.initialize();

	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@Test
	public void testGetComputeServices() throws Exception {

		context.checking(new Expectations() {

			{
				exactly(1).of(serviceProvider).getComputeServices();
				will(returnValue(computeServices));
				exactly(1).of(serviceProvider).hasComputeServices();
				will(returnValue(true));

				exactly(1).of(computeServices).hasAffinityGroupSupport();
				will(returnValue(true));
				exactly(1).of(computeServices).hasAutoScalingSupport();
				will(returnValue(true));
				exactly(1).of(computeServices).hasImageSupport();
				will(returnValue(true));
				exactly(1).of(computeServices).hasSnapshotSupport();
				will(returnValue(true));
				exactly(1).of(computeServices).hasVirtualMachineSupport();
				will(returnValue(true));
				exactly(1).of(computeServices).hasVolumeSupport();
				will(returnValue(true));
			}
		});

		provider.getComputeServices();
	}

	@Test
	public void testLifeMerge() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setId("id");
		configuration.getLongPoolConfig().setCoreSize(100);
		provider.lightMerge(configuration);
	}

}
