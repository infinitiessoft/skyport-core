package com.infinities.skyport.integrated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Closeables;
import com.infinities.skyport.Main;
import com.infinities.skyport.Skyport;
import com.infinities.skyport.compute.CMD;
import com.infinities.skyport.testcase.IntegrationTest;
import com.infinities.skyport.util.JsonUtil;

@Category(IntegrationTest.class)
public class SkyportWebApiIT {

	private static Skyport main;
	private static final String URL = "http://127.0.0.1:8085/skyportWeb";
	private CloseableHttpClient client;
	private final String pool = "mock";


	@BeforeClass
	public static void beforeClass() throws Throwable {
		URL url = Thread.currentThread().getContextClassLoader().getResource("accessconfig_test.xml");
		String[] args = new String[] { "-accessconfig", url.getPath() };
		main = new Main(args);
		main.initialize();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try {
			Closeables.close(main, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		client = HttpClients.custom().setSSLSocketFactory(sslsf).setHostnameVerifier(new AllowAllHostnameVerifier()).build();
	}

	@After
	public void tearDown() throws Exception {
		client.close();
	}

	@Test
	public void testAll() throws HttpException, IOException, InterruptedException, URISyntaxException {
		Thread.sleep(45000);

		// get SystemInfo
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_SYSTEMINFO))
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			JsonNode data = root.get("data");
			assertEquals(true, data.get("ui").asBoolean());
			assertEquals("disabled", data.get("cluster").asText());
			assertEquals("GMT+8:00", data.get("timeZone").asText());
			assertEquals(true, data.get("cache").asBoolean());
			assertEquals("Infinitiessoft Skyport", data.get("title").asText());
			assertEquals(1, data.get("websockifyServerConnectors").size());
			assertEquals(3, data.get("webServerConnectors").size());
		} finally {
			response.close();
		}

		// create vm
		String eventid = null;
		String vmid = null;
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.CREATE_VM_FROM_SKYPORT))
				.setParameter("name", "demo").setParameter("desc", "demo").setParameter("core", "2")
				.setParameter("memory", "1024").setParameter("templateid", "b905faa6-7341-467a-89cd-5891197901e1")
				.setParameter("resourceid", "1caa5fee-4368-4ad7-807e-81a17ff35adc").setParameter("pool", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			vmid = checkCreateVm(eventid);
		} finally {
			response.close();
		}

		// update vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.UPDATE_VM_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("name", "demo2").setParameter("desc", "demo2")
				.setParameter("core", "4").setParameter("memory", "512").setParameter("pool", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			checkUpdateVm(eventid, vmid);
		} finally {
			response.close();
		}

		// add nic
		String nicid = null;
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.ADD_NIC_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("networkid", "9a8e0df6-3ac5-4bd0-b2ff-e4b6b2d4e61b")
				.setParameter("pool", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			nicid = checkAddNic(eventid, vmid);
		} finally {
			response.close();
		}

		// remove nic
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.REMOVE_NIC_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("nicid", nicid).setParameter("pool", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			checkRemoveNic(eventid, vmid, nicid);
		} finally {
			response.close();
		}

		// add disk
		String diskid = null;
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.ADD_DISK_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("size", "5").setParameter("pool", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			diskid = checkAddDisk(eventid, vmid);
		} finally {
			response.close();
		}

		// remove disk
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.REMOVE_DISK_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("diskid", diskid).setParameter("pool", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			checkRemoveDisk(eventid, vmid, diskid);
		} finally {
			response.close();
		}

		// start vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.START_VM_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("pool", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			checkStartVm(eventid, vmid);
		} finally {
			response.close();
		}

		// shutdown vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.STOP_VM_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("pool", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			checkShutdownVm(eventid, vmid);
		} finally {
			response.close();
		}

		// remove vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.REMOVE_VM_FROM_SKYPORT))
				.setParameter("vmid", vmid).setParameter("pool", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			eventid = root.get("data").get("eventid").asText();
			assertNotNull(eventid);
			Thread.sleep(12000);
			// get vm
			checkRemoveVm(eventid, vmid);
		} finally {
			response.close();
		}
	}

	private String checkCreateVm(String eventid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String vmid = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			String status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			vmid = root.get("data").get(0).get("detail").get("vmid").asText();
			assertNotNull(vmid);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			assertEquals("demo", root.get("data").get("name").asText());
			assertTrue(root.get("data").get("desc").asText().startsWith("demo"));
			assertEquals("2", root.get("data").get("cpunum").asText());
			assertEquals("1024", root.get("data").get("memorysize").asText());
			assertEquals("b905faa6-7341-467a-89cd-5891197901e1", root.get("data").get("templateid").asText());
			assertEquals("1caa5fee-4368-4ad7-807e-81a17ff35adc", root.get("data").get("resourceid").asText());
		} finally {
			response.close();
		}
		return vmid;
	}

	private void checkUpdateVm(String eventid, String vmid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String vmid2 = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			vmid2 = root.get("data").get(0).get("detail").get("vmid").asText();
			assertNotNull(vmid2);
			assertEquals(vmid, vmid2);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			assertEquals("demo2", root.get("data").get("name").asText());
			assertTrue(root.get("data").get("desc").asText().startsWith("demo2"));
			assertEquals("4", root.get("data").get("cpunum").asText());
			assertEquals("512", root.get("data").get("memorysize").asText());
			assertEquals("b905faa6-7341-467a-89cd-5891197901e1", root.get("data").get("templateid").asText());
			assertEquals("1caa5fee-4368-4ad7-807e-81a17ff35adc", root.get("data").get("resourceid").asText());
		} finally {
			response.close();
		}
	}

	private String checkAddNic(String eventid, String vmid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String nicid = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			nicid = root.get("data").get(0).get("detail").get("nicid").asText();
			assertNotNull(nicid);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			boolean exist = false;
			assertEquals(3, root.get("data").get("nics").size());
			for (JsonNode node : root.get("data").get("nics")) {
				if (nicid.equals(node.get("nicid").asText())) {
					exist = true;
				}
			}
			assertTrue(exist);
		} finally {
			response.close();
		}
		return nicid;
	}

	private void checkRemoveNic(String eventid, String vmid, String nicid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String nicid2 = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			nicid2 = root.get("data").get(0).get("detail").get("nicid").asText();
			assertNotNull(nicid2);
			assertEquals(nicid, nicid2);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			boolean exist = false;
			assertEquals(2, root.get("data").get("nics").size());
			for (JsonNode node : root.get("data").get("nics")) {
				if (nicid.equals(node.get("nicid").asText())) {
					exist = true;
				}
			}
			assertFalse(exist);
		} finally {
			response.close();
		}

		// get vm nics
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_VM_NICS))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			assertEquals(2, root.get("data").size());
		} finally {
			response.close();
		}
	}

	private String checkAddDisk(String eventid, String vmid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String diskid = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			diskid = root.get("data").get(0).get("detail").get("diskid").asText();
			assertNotNull(diskid);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			boolean exist = false;
			assertEquals(2, root.get("data").get("disks").size());
			for (JsonNode node : root.get("data").get("disks")) {
				if (diskid.equals(node.get("diskid").asText())) {
					assertEquals("5", node.get("sizegb").asText());
					exist = true;
				}
			}
			assertTrue(exist);
		} finally {
			response.close();
		}
		return diskid;
	}

	private void checkRemoveDisk(String eventid, String vmid, String diskid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String diskid2 = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			diskid2 = root.get("data").get(0).get("detail").get("diskid").asText();
			assertNotNull(diskid2);
			assertEquals(diskid, diskid2);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			boolean exist = false;
			assertEquals(1, root.get("data").get("disks").size());
			for (JsonNode node : root.get("data").get("disks")) {
				if (diskid.equals(node.get("diskid").asText())) {
					assertEquals("5", node.get("sizegb").asText());
					exist = true;
				}
			}
			assertFalse(exist);
		} finally {
			response.close();
		}
	}

	private void checkStartVm(String eventid, String vmid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			String vmid3 = root.get("data").get(0).get("detail").get("vmid").asText();
			assertNotNull(vmid3);
			assertEquals(vmid, vmid3);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			assertTrue("Lock".equals(root.get("data").get("status").asText())
					|| "Up".equals(root.get("data").get("status").asText()));
		} finally {
			response.close();
		}
	}

	private void checkShutdownVm(String eventid, String vmid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			String vmid4 = root.get("data").get(0).get("detail").get("vmid").asText();
			assertNotNull(vmid4);
			assertEquals(vmid, vmid4);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			assertTrue("Lock".equals(root.get("data").get("status").asText())
					|| "Down".equals(root.get("data").get("status").asText()));
		} finally {
			response.close();
		}
	}

	private void checkRemoveVm(String eventid, String vmid) throws IOException, URISyntaxException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_EVENT))
				.setParameter("eventid", eventid).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String status = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			status = root.get("data").get(0).get("status").asText();
			assertEquals("Success", status);
			String vmid5 = root.get("data").get(0).get("detail").get("vmid").asText();
			assertNotNull(vmid5);
			assertEquals(vmid, vmid5);
		} finally {
			response.close();
		}

		// get vm
		method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_VM))
				.setParameter("pool", pool).setParameter("vmid", vmid).setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			boolean exist = false;
			assertEquals(2, root.get("data").size());
			for (JsonNode node : root.get("data")) {
				if (vmid.equals(node.get("vmid").asText())) {
					exist = true;
				}
			}
			assertFalse(exist);
		} finally {
			response.close();
		}
	}
}
