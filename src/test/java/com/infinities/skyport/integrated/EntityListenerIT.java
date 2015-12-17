//package com.infinities.skyport.integrated;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.IOException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.apache.http.HttpEntity;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.utils.URIBuilder;
//import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
//import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
//import org.apache.http.conn.ssl.SSLContextBuilder;
//import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.experimental.categories.Category;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.google.common.eventbus.Subscribe;
//import com.google.common.io.Closeables;
//import com.infinities.skyport.Main;
//import com.infinities.skyport.Skyport;
//import com.infinities.skyport.async.AsyncDriver;
//import com.infinities.skyport.compute.CMD;
//import com.infinities.skyport.compute.ITemplate;
//import com.infinities.skyport.compute.IVm;
//import com.infinities.skyport.compute.entity.Cluster;
//import com.infinities.skyport.compute.entity.DataCenter;
//import com.infinities.skyport.compute.entity.Host;
//import com.infinities.skyport.compute.entity.Network;
//import com.infinities.skyport.compute.entity.ResourcePool;
//import com.infinities.skyport.compute.entity.Storage;
//import com.infinities.skyport.compute.entity.Template;
//import com.infinities.skyport.compute.entity.comparator.ClusterComparator;
//import com.infinities.skyport.compute.entity.comparator.DataCenterComparator;
//import com.infinities.skyport.compute.entity.comparator.HostComparator;
//import com.infinities.skyport.compute.entity.comparator.ITemplateComparator;
//import com.infinities.skyport.compute.entity.comparator.IVmComparator;
//import com.infinities.skyport.compute.entity.comparator.NetworkComparator;
//import com.infinities.skyport.compute.entity.comparator.ResourcePoolComparator;
//import com.infinities.skyport.compute.entity.comparator.StorageComparator;
//import com.infinities.skyport.diff.Patch;
//import com.infinities.skyport.proxy.VmProxy;
//import com.infinities.skyport.registrar.ConfigurationHomeFactory;
//import com.infinities.skyport.service.ConfigurationHome;
//import com.infinities.skyport.service.ConfigurationHome.ClusterListener;
//import com.infinities.skyport.service.ConfigurationHome.DataCenterListener;
//import com.infinities.skyport.service.ConfigurationHome.HostListener;
//import com.infinities.skyport.service.ConfigurationHome.NetworkListener;
//import com.infinities.skyport.service.ConfigurationHome.ResourcePoolListener;
//import com.infinities.skyport.service.ConfigurationHome.StorageListener;
//import com.infinities.skyport.service.ConfigurationHome.TemplateListener;
//import com.infinities.skyport.service.ConfigurationHome.VmListener;
//import com.infinities.skyport.service.event.Event.Type;
//import com.infinities.skyport.service.event.cluster.ClusterEvent;
//import com.infinities.skyport.service.event.cluster.ClusterFailureEvent;
//import com.infinities.skyport.service.event.compute.virtualmachine.VirtualMachineEvent;
//import com.infinities.skyport.service.event.compute.virtualmachine.VmFailureEvent;
//import com.infinities.skyport.service.event.datacenter.DataCenterEvent;
//import com.infinities.skyport.service.event.datacenter.DataCenterFailureEvent;
//import com.infinities.skyport.service.event.host.HostEvent;
//import com.infinities.skyport.service.event.host.HostFailureEvent;
//import com.infinities.skyport.service.event.network.NetworkEvent;
//import com.infinities.skyport.service.event.network.NetworkFailureEvent;
//import com.infinities.skyport.service.event.resource.ResourcePoolEvent;
//import com.infinities.skyport.service.event.resource.ResourcePoolFailureEvent;
//import com.infinities.skyport.service.event.storage.StorageEvent;
//import com.infinities.skyport.service.event.storage.StorageFailureEvent;
//import com.infinities.skyport.service.event.template.TemplateEvent;
//import com.infinities.skyport.service.event.template.TemplateFailureEvent;
//import com.infinities.skyport.testcase.IntegrationTest;
//import com.infinities.skyport.util.JsonUtil;
//
//@Category(IntegrationTest.class)
//public class EntityListenerIT {
//
//	private static final Logger logger = LoggerFactory.getLogger(EntityListenerIT.class);
//	private static Skyport main;
//	private static final String URL = "http://127.0.0.1:8085/skyport";
//	private static final String name = "demo2";
//	private static final String description = "demo2";
//	private static final int numofcpus = 4;
//	private static final int memorysize = 512;
//	private CloseableHttpClient client;
//
//
//	@BeforeClass
//	public static void beforeClass() throws Throwable {
//		URL url = Thread.currentThread().getContextClassLoader().getResource("accessconfig_test.xml");
//		String[] args = new String[] { "-accessconfig", url.getPath() };
//		main = new Main(args);
//		main.initialize();
//	}
//
//	@AfterClass
//	public static void afterClass() throws Exception {
//		try {
//			Closeables.close(main, true);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	@Before
//	public void setUp() throws Exception {
//		SSLContextBuilder builder = new SSLContextBuilder();
//		builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
//		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), new String[] { "TLSv1" }, null,
//				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//		client = HttpClients.custom().setSSLSocketFactory(sslsf).setHostnameVerifier(new AllowAllHostnameVerifier()).build();
//
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		client.close();
//	}
//
//	@Test
//	public void testAll() throws Exception {
//		ConfigurationHome accessConfigHome = ConfigurationHomeFactory.getInstance();
//		VmClient vmclient = new VmClient();
//		accessConfigHome.addVmListener(vmclient, "9", new HashMap<String, IVm>());
//
//		VmClient vmclient2 = new VmClient();
//		accessConfigHome.addVmListener(vmclient2, "9", new HashMap<String, IVm>());
//
//		TemplateClient templateclient = new TemplateClient();
//		accessConfigHome.addTemplateListener(templateclient, "9", new HashMap<String, ITemplate>());
//
//		HostClient hostclient = new HostClient();
//		accessConfigHome.addHostListener(hostclient, "9", new HashMap<String, Host>());
//
//		ClusterClient clusterclient = new ClusterClient();
//		accessConfigHome.addClusterListener(clusterclient, "9", new HashMap<String, Cluster>());
//
//		ResourceClient resourceclient = new ResourceClient();
//		accessConfigHome.addResourcePoolListener(resourceclient, "9", new HashMap<String, ResourcePool>());
//
//		StorageClient storageclient = new StorageClient();
//		accessConfigHome.addStorageListener(storageclient, "9", new HashMap<String, Storage>());
//
//		NetworkClient networkclient = new NetworkClient();
//		accessConfigHome.addNetworkListener(networkclient, "9", new HashMap<String, Network>());
//
//		DataCenterClient dcclient = new DataCenterClient();
//		accessConfigHome.addDataCenterListener(dcclient, "9", new HashMap<String, DataCenter>());
//
//		Thread.sleep(40000);
//		// create vm
//		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.CREATE_VM))
//				.setParameter("name", "demo").setParameter("description", "demo").setParameter("numofcpus", "2")
//				.setParameter("memorysize", "1024").setParameter("templateid", "b905faa6-7341-467a-89cd-5891197901e1")
//				.setParameter("clusterid", "1caa5fee-4368-4ad7-807e-81a17ff35adc").setParameter("config", "mock")
//				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
//		String vmid = null;
//		CloseableHttpResponse response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			vmid = root.get(1).get("VmId").asText();
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//
//		// start vm
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.START_VM))
//				.setParameter("vmid", vmid).build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			assertEquals(vmid, root.get(1).get("VmId").asText());
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//
//		// stop vm
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.STOP_VM))
//				.setParameter("vmid", vmid).build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			assertEquals(vmid, root.get(1).get("VmId").asText());
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//		// update vm
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.UPDATE_VM))
//				.setParameter("vmid", vmid).setParameter("name", name).setParameter("description", description)
//				.setParameter("numofcpus", String.valueOf(numofcpus)).setParameter("memorysize", String.valueOf(memorysize))
//				.build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			assertEquals(vmid, root.get(1).get("VmId").asText());
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//		// add nic
//		String nicid = null;
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.ADD_NETWORK_ADAPTER))
//				.setParameter("vmid", vmid).setParameter("network", "9a8e0df6-3ac5-4bd0-b2ff-e4b6b2d4e61b").build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			nicid = root.get(1).get("NicId").asText();
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//		// remove nic
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.REMOVE_NETWORK_ADAPTER))
//				.setParameter("vmid", vmid).setParameter("nicid", nicid).build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			assertEquals(nicid, root.get(1).get("NicId").asText());
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//		// add disk
//		String diskid = null;
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.ADD_DISK))
//				.setParameter("vmid", vmid).setParameter("size", "5").build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			diskid = root.get(1).get("DiskId").asText();
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//		// remove disk
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.REMOVE_DISK))
//				.setParameter("vmid", vmid).setParameter("diskid", diskid).build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			assertEquals(diskid, root.get(1).get("DiskId").asText());
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//		// remove vm
//		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
//				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.REMOVE_VM))
//				.setParameter("vmid", vmid).build());
//		response = client.execute(method);
//		try {
//			int statusCode = response.getStatusLine().getStatusCode();
//			assertEquals(200, statusCode);
//			HttpEntity entity = response.getEntity();
//			String ret = EntityUtils.toString(entity);
//			System.err.println(ret);
//			JsonNode root = JsonUtil.getLegendNode(ret);
//			assertTrue(JsonUtil.getResult(ret));
//			assertEquals(vmid, root.get(1).get("VmId").asText());
//		} finally {
//			response.close();
//		}
//		Thread.sleep(10000);
//
//		AsyncDriver driver = accessConfigHome.findByName("mock");
//		assertEquals(2, vmclient.add.intValue());
//		assertEquals(15, vmclient.update.intValue());
//		assertEquals(1, vmclient.remove.intValue());
//		// assertEquals(1, vmclient.fail.intValue());
//		assertEquals(driver.getVmCache().size(), vmclient.vms.size());
//		for (VmProxy t : driver.getVmCache().values()) {
//			assertEquals(0, new IVmComparator().compare(t, vmclient.vms.get(t.getVmid())));
//		}
//
//		assertEquals(2, vmclient2.add.intValue());
//		assertEquals(15, vmclient2.update.intValue());
//		assertEquals(1, vmclient2.remove.intValue());
//		// assertEquals(1, vmclient2.fail.intValue());
//		assertEquals(driver.getVmCache().size(), vmclient2.vms.size());
//
//		assertEquals(1, templateclient.add.intValue());
//		assertEquals(0, templateclient.update.intValue());
//		// assertEquals(1, templateclient.fail.intValue());
//		assertEquals(driver.getTemplateCache().size(), templateclient.templates.size());
//		for (Template t : driver.getTemplateCache().values()) {
//			assertEquals(0, new ITemplateComparator().compare(t, templateclient.templates.get(t.getTemplateid())));
//		}
//
//		assertEquals(1, hostclient.add.intValue());
//		assertEquals(0, hostclient.update.intValue());
//		// assertEquals(1, hostclient.fail.intValue());
//		assertEquals(driver.getHostCache().size(), hostclient.hosts.size());
//		for (Host t : driver.getHostCache().values()) {
//			assertEquals(0, new HostComparator().compare(t, hostclient.hosts.get(t.getHostid())));
//		}
//
//		assertEquals(1, clusterclient.add.intValue());
//		assertEquals(0, clusterclient.update.intValue());
//		// assertEquals(1, clusterclient.fail.intValue());
//		assertEquals(driver.getClusterCache().size(), clusterclient.clusters.size());
//		for (Cluster t : driver.getClusterCache().values()) {
//			assertEquals(0, new ClusterComparator().compare(t, clusterclient.clusters.get(t.getClusterId())));
//		}
//
//		assertEquals(1, resourceclient.add.intValue());
//		assertEquals(0, resourceclient.update.intValue());
//		// assertEquals(1, resourceclient.fail.intValue());
//		assertEquals(driver.getResourcePoolCache().size(), resourceclient.resources.size());
//		for (ResourcePool t : driver.getResourcePoolCache().values()) {
//			assertEquals(0, new ResourcePoolComparator().compare(t, resourceclient.resources.get(t.getResourceid())));
//		}
//
//		assertEquals(1, storageclient.add.intValue());
//		assertEquals(0, storageclient.update.intValue());
//		// assertEquals(1, storageclient.fail.intValue());
//		assertEquals(driver.getStorageCache().size(), storageclient.storages.size());
//		for (Storage t : driver.getStorageCache().values()) {
//			assertEquals(0, new StorageComparator().compare(t, storageclient.storages.get(t.getStorageDomainId())));
//		}
//
//		assertEquals(1, networkclient.add.intValue());
//		assertEquals(0, networkclient.update.intValue());
//		// assertEquals(1, networkclient.fail.intValue());
//		assertEquals(driver.getNetworkCache().size(), networkclient.networks.size());
//		for (Network t : driver.getNetworkCache().values()) {
//			assertEquals(0, new NetworkComparator().compare(t, networkclient.networks.get(t.getNetworkid())));
//		}
//
//		assertEquals(1, dcclient.add.intValue());
//		assertEquals(0, dcclient.update.intValue());
//		// assertEquals(0, dcclient.fail.intValue());
//		assertEquals(driver.getDataCenterCache().size(), dcclient.dcs.size());
//		for (DataCenter t : driver.getDataCenterCache().values()) {
//			assertEquals(0, new DataCenterComparator().compare(t, dcclient.dcs.get(t.getDataCenterId())));
//		}
//	}
//
//
//	static class VmClient implements VmListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, IVm> vms = new HashMap<String, IVm>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(VirtualMachineEvent e) {
//			logger.debug("client1 vms change config: {}, type: {}, date: {}, size: {}", new Object[] { e.getConfig(),
//					e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (IVm vm : e.getEntries()) {
//					vms.put(vm.getVmid(), vm);
//					System.err.println(vm.getPoolid() + "   " + vm.getPool());
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (IVm vm : e.getEntries()) {
//					vms.remove(vm.getVmid());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				assertEquals(e.getEntries().size(), e.getPatchs().size());
//				List<String> fields = new ArrayList<String>();
//				if (update.get() == 1) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						// new create vm from lock to down
//						assertEquals("Down", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 2) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Lock", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 3) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Up", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 4) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Lock", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 5) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Down", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 6) {
//					fields.add("name");
//					fields.add("desc");
//					fields.add("status");
//					fields.add("cpunum");
//					fields.add("memorysize");
//					// update vm
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals(name, entry.getValue().getDiff().getName());
//						assertEquals(description, entry.getValue().getDiff().getDesc());
//						assertEquals(numofcpus, entry.getValue().getDiff().getCpunum().intValue());
//						assertEquals(memorysize, entry.getValue().getDiff().getMemorysize().intValue());
//						assertEquals("Lock", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 7) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Down", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 8) {
//					fields.add("status");
//					fields.add("nics");
//
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Lock", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 9) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Down", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 10) {
//					fields.add("status");
//					fields.add("nics");
//
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Lock", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 11) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Down", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 12) {
//					fields.add("status");
//					fields.add("disks");
//
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Lock", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 13) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Down", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 14) {
//					fields.add("status");
//					fields.add("disks");
//
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Lock", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				if (update.get() == 15) {
//					fields.add("status");
//					for (Entry<String, Patch<IVm>> entry : e.getPatchs().entrySet()) {
//						assertEquals(fields, entry.getValue().getFieldNames());
//						assertEquals("Down", entry.getValue().getDiff().getStatus());
//					}
//				}
//
//				for (IVm vm : e.getEntries()) {
//					vms.put(vm.getVmid(), vm);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(VmFailureEvent e) {
//			logger.debug("getting fail event", e.getThrowable());
//			fail.incrementAndGet();
//		}
//
//	}
//
//	static class TemplateClient implements TemplateListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, ITemplate> templates = new HashMap<String, ITemplate>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(TemplateEvent e) {
//			logger.debug("client1 template change config: {}, type: {}, date: {}, size: {}", new Object[] { e.getConfig(),
//					e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (ITemplate t : e.getEntries()) {
//					templates.put(t.getTemplateid(), t);
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (ITemplate t : e.getEntries()) {
//					templates.remove(t.getTemplateid());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				for (ITemplate t : e.getEntries()) {
//					templates.put(t.getTemplateid(), t);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(TemplateFailureEvent e) {
//			fail.incrementAndGet();
//		}
//
//	}
//
//	static class HostClient implements HostListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, Host> hosts = new HashMap<String, Host>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(HostEvent e) {
//			logger.debug("client1 host change config: {}, type: {}, date: {}, size: {}", new Object[] { e.getConfig(),
//					e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (Host h : e.getEntries()) {
//					hosts.put(h.getHostid(), h);
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (Host h : e.getEntries()) {
//					hosts.remove(h.getHostid());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				for (Host h : e.getEntries()) {
//					hosts.put(h.getHostid(), h);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(HostFailureEvent e) {
//			fail.incrementAndGet();
//		}
//
//	}
//
//	static class ClusterClient implements ClusterListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, Cluster> clusters = new HashMap<String, Cluster>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(ClusterEvent e) {
//			logger.debug("client1 cluster change config: {}, type: {}, date: {}, size: {}", new Object[] { e.getConfig(),
//					e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (Cluster c : e.getEntries()) {
//					clusters.put(c.getClusterId(), c);
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (Cluster c : e.getEntries()) {
//					clusters.remove(c.getClusterId());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				for (Cluster c : e.getEntries()) {
//					clusters.put(c.getClusterId(), c);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(ClusterFailureEvent e) {
//			fail.incrementAndGet();
//		}
//
//	}
//
//	static class ResourceClient implements ResourcePoolListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, ResourcePool> resources = new HashMap<String, ResourcePool>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(ResourcePoolEvent e) {
//			logger.debug("client1 resourcepool change config: {}, type: {}, date: {}, size: {}",
//					new Object[] { e.getConfig(), e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (ResourcePool r : e.getEntries()) {
//					resources.put(r.getResourceid(), r);
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (ResourcePool r : e.getEntries()) {
//					resources.remove(r.getResourceid());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				for (ResourcePool r : e.getEntries()) {
//					resources.put(r.getResourceid(), r);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(ResourcePoolFailureEvent e) {
//			fail.incrementAndGet();
//		}
//
//	}
//
//	static class NetworkClient implements NetworkListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, Network> networks = new HashMap<String, Network>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(NetworkEvent e) {
//			logger.debug("client1 network change config: {}, type: {}, date: {}, size: {}", new Object[] { e.getConfig(),
//					e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (Network n : e.getEntries()) {
//					networks.put(n.getNetworkid(), n);
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (Network n : e.getEntries()) {
//					networks.remove(n.getNetworkid());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				for (Network n : e.getEntries()) {
//					networks.put(n.getNetworkid(), n);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(NetworkFailureEvent e) {
//			fail.incrementAndGet();
//		}
//
//	}
//
//	static class StorageClient implements StorageListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, Storage> storages = new HashMap<String, Storage>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(StorageEvent e) {
//			logger.debug("client1 storage change config: {}, type: {}, date: {}, size: {}", new Object[] { e.getConfig(),
//					e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (Storage s : e.getEntries()) {
//					storages.put(s.getStorageDomainId(), s);
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (Storage s : e.getEntries()) {
//					storages.remove(s.getStorageDomainId());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				for (Storage s : e.getEntries()) {
//					storages.put(s.getStorageDomainId(), s);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(StorageFailureEvent e) {
//			fail.incrementAndGet();
//		}
//
//	}
//
//	static class DataCenterClient implements DataCenterListener {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		public AtomicInteger add = new AtomicInteger(0);
//		public AtomicInteger update = new AtomicInteger(0);
//		public AtomicInteger remove = new AtomicInteger(0);
//		public AtomicInteger fail = new AtomicInteger(0);
//		public Map<String, DataCenter> dcs = new HashMap<String, DataCenter>();
//
//
//		@Override
//		@Subscribe
//		public void onChanged(DataCenterEvent e) {
//			logger.debug("client1 datacenter change config: {}, type: {}, date: {}, size: {}", new Object[] { e.getConfig(),
//					e.getType().name(), e.getDate(), e.getEntries().size() });
//			if (e.getType().equals(Type.ADDED)) {
//				add.incrementAndGet();
//				for (DataCenter dc : e.getEntries()) {
//					dcs.put(dc.getDataCenterId(), dc);
//				}
//			}
//
//			if (e.getType().equals(Type.REMOVED)) {
//				remove.incrementAndGet();
//				for (DataCenter dc : e.getEntries()) {
//					dcs.remove(dc.getDataCenterId());
//				}
//			}
//
//			if (e.getType().equals(Type.MODIFIED)) {
//				update.incrementAndGet();
//				for (DataCenter dc : e.getEntries()) {
//					dcs.put(dc.getDataCenterId(), dc);
//				}
//			}
//		}
//
//		@Override
//		@Subscribe
//		public void onFailure(DataCenterFailureEvent e) {
//			fail.incrementAndGet();
//		}
//
//	}
//
// }
