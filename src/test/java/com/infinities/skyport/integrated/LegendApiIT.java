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
package com.infinities.skyport.integrated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class LegendApiIT {

	private static Skyport main;
	private static final String URL = "http://127.0.0.1:8085/skyport";
	private CloseableHttpClient client;


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

		// create vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.CREATE_VM))
				.setParameter("name", "demo").setParameter("description", "demo").setParameter("numofcpus", "2")
				.setParameter("memorysize", "1024").setParameter("templateid", "b905faa6-7341-467a-89cd-5891197901e1")
				.setParameter("clusterid", "1caa5fee-4368-4ad7-807e-81a17ff35adc").setParameter("config", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		String vmid = null;
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			vmid = root.get(1).get("VmId").asText();
			// get vm
			checkCreateVm(root.get(1));
		} finally {
			response.close();
		}

		// update vm
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.UPDATE_VM))
				.setParameter("vmid", vmid).setParameter("name", "demo2").setParameter("description", "demo2")
				.setParameter("numofcpus", "4").setParameter("memorysize", "512").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals(vmid, root.get(1).get("VmId").asText());
			checkUpdateVm(root.get(1));
		} finally {
			response.close();
		}

		// add nic
		String nicid = null;
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.ADD_NETWORK_ADAPTER))
				.setParameter("vmid", vmid).setParameter("network", "9a8e0df6-3ac5-4bd0-b2ff-e4b6b2d4e61b").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			nicid = root.get(1).get("NicId").asText();
			checkAddNic(root.get(1), vmid);
		} finally {
			response.close();
		}

		// remove nic
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.REMOVE_NETWORK_ADAPTER))
				.setParameter("vmid", vmid).setParameter("nicid", nicid).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals(nicid, root.get(1).get("NicId").asText());
			checkRemoveNic(root.get(1), vmid, nicid);
		} finally {
			response.close();
		}

		// add disk
		String diskid = null;
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.ADD_DISK))
				.setParameter("vmid", vmid).setParameter("size", "5").build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			diskid = root.get(1).get("DiskId").asText();
			checkAddDisk(root.get(1), vmid);
		} finally {
			response.close();
		}

		// remove disk
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.REMOVE_DISK))
				.setParameter("vmid", vmid).setParameter("diskid", diskid).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals(diskid, root.get(1).get("DiskId").asText());
			checkRemoveDisk(root.get(1), vmid, diskid);
		} finally {
			response.close();
		}

		// start vm
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.START_VM))
				.setParameter("vmid", vmid).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals(vmid, root.get(1).get("VmId").asText());
			checkStartVm(root.get(1), vmid);
		} finally {
			response.close();
		}

		// shutdown vm
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.STOP_VM))
				.setParameter("vmid", vmid).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals(vmid, root.get(1).get("VmId").asText());
			checkShutdownVm(root.get(1), vmid);
		} finally {
			response.close();
		}

		// remove vm
		method = new HttpPost(new URIBuilder(URL).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("cmd", String.valueOf(CMD.REMOVE_VM))
				.setParameter("vmid", vmid).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals(vmid, root.get(1).get("VmId").asText());
			checkRemoveVm(root.get(1), vmid);
		} finally {
			response.close();
		}
	}

	private void checkCreateVm(JsonNode vm) throws IOException, URISyntaxException {
		assertEquals("demo", vm.get("Name").asText());
		assertTrue(vm.get("Description").asText().startsWith("demo"));
		assertEquals("2", vm.get("NumOfSockets").asText());
		assertEquals("1024", vm.get("MemorySize").asText());
		assertEquals("b905faa6-7341-467a-89cd-5891197901e1", vm.get("TemplateId").asText());
		assertEquals("1caa5fee-4368-4ad7-807e-81a17ff35adc", vm.get("HostClusterId").asText());

		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vm.get("VmId").asText()).setParameter("config", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals("demo", root.get(1).get("Name").asText());
			assertTrue(root.get(1).get("Description").asText().startsWith("demo"));
			assertEquals("2", root.get(1).get("NumOfSockets").asText());
			assertEquals("1024", root.get(1).get("MemorySize").asText());
			assertEquals("b905faa6-7341-467a-89cd-5891197901e1", root.get(1).get("TemplateId").asText());
			assertEquals("1caa5fee-4368-4ad7-807e-81a17ff35adc", root.get(1).get("HostClusterId").asText());
		} finally {
			response.close();
		}
	}

	private void checkUpdateVm(JsonNode vm) throws IOException, URISyntaxException {
		assertEquals("demo2", vm.get("Name").asText());
		assertTrue(vm.get("Description").asText().startsWith("demo2"));
		assertEquals("4", vm.get("NumOfSockets").asText());
		assertEquals("512", vm.get("MemorySize").asText());
		assertEquals("b905faa6-7341-467a-89cd-5891197901e1", vm.get("TemplateId").asText());
		assertEquals("1caa5fee-4368-4ad7-807e-81a17ff35adc", vm.get("HostClusterId").asText());

		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vm.get("VmId").asText()).setParameter("config", "mock")
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertEquals("demo2", root.get(1).get("Name").asText());
			assertTrue(root.get(1).get("Description").asText().startsWith("demo2"));
			assertEquals("4", root.get(1).get("NumOfSockets").asText());
			assertEquals("512", root.get(1).get("MemorySize").asText());
			assertEquals("b905faa6-7341-467a-89cd-5891197901e1", root.get(1).get("TemplateId").asText());
			assertEquals("1caa5fee-4368-4ad7-807e-81a17ff35adc", root.get(1).get("HostClusterId").asText());
		} finally {
			response.close();
		}
	}

	private void checkAddNic(JsonNode nic, String vmid) throws IOException, URISyntaxException {
		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vmid).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			boolean exist = false;
			assertEquals(3, root.get(1).get("Nics").size());
			for (JsonNode node : root.get(1).get("Nics")) {
				if (node.has(nic.get("NicId").asText())) {
					exist = true;
				}
			}
			assertTrue(exist);
		} finally {
			response.close();
		}
	}

	private void checkRemoveNic(JsonNode nic, String vmid, String nicid) throws IOException, URISyntaxException {
		assertEquals(nicid, nic.get("NicId").asText());
		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vmid).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			boolean exist = false;
			assertEquals(2, root.get(1).get("Nics").size());
			for (JsonNode node : root.get(1).get("Nics")) {
				if (node.has(nic.get("NicId").asText())) {
					exist = true;
				}
			}
			assertFalse(exist);
		} finally {
			response.close();
		}
	}

	private void checkAddDisk(JsonNode disk, String vmid) throws IOException, URISyntaxException {
		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vmid).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			boolean exist = false;
			assertEquals(2, root.get(1).get("Disks").size());
			for (JsonNode node : root.get(1).get("Disks")) {
				if (node.has(disk.get("DiskId").asText())) {
					assertEquals("5", node.get(disk.get("DiskId").asText()).get("SizeInGB").asText());
					exist = true;
				}
			}
			assertTrue(exist);
		} finally {
			response.close();
		}
	}

	private void checkRemoveDisk(JsonNode disk, String vmid, String diskid) throws IOException, URISyntaxException {
		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vmid).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			boolean exist = false;
			assertEquals(1, root.get(1).get("Disks").size());
			for (JsonNode node : root.get(1).get("Disks")) {
				if (node.has(disk.get("DiskId").asText())) {
					assertEquals("5", node.get(disk.get("DiskId").asText()).get("SizeInGB").asText());
					exist = true;
				}
			}
			assertFalse(exist);
		} finally {
			response.close();
		}
	}

	private void checkStartVm(JsonNode vm, String vmid) throws IOException, URISyntaxException {
		assertEquals(vmid, vm.get("VmId").asText());
		assertEquals("Up", vm.get("Status").asText());

		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vmid).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertTrue("Lock".equals(root.get(1).get("Status").asText()) || "Up".equals(root.get(1).get("Status").asText()));
		} finally {
			response.close();
		}
	}

	private void checkShutdownVm(JsonNode vm, String vmid) throws IOException, URISyntaxException {
		assertEquals(vmid, vm.get("VmId").asText());
		assertEquals("Down", vm.get("Status").asText());
		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_RUNNING_VM_STATUS))
				.setParameter("vmid", vmid).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			assertTrue("Lock".equals(root.get(1).get("Status").asText())
					|| "Down".equals(root.get(1).get("Status").asText()));
		} finally {
			response.close();
		}
	}

	private void checkRemoveVm(JsonNode vm, String vmid) throws IOException, URISyntaxException {
		assertEquals(vmid, vm.get("VmId").asText());
		// get vm
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_VM))
				.setParameter("vmid", vmid).setParameter("config", "mock").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").build());
		CloseableHttpResponse response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			JsonNode root = JsonUtil.getLegendNode(ret);
			assertTrue(JsonUtil.getResult(ret));
			boolean exist = false;
			assertEquals(3, root.size());
			for (int i = 1; i < root.size(); i++) {
				JsonNode node = root.get(i);
				if (vmid.equals(node.get("VmId").asText())) {
					exist = true;
				}
			}
			assertFalse(exist);
		} finally {
			response.close();
		}
	}
}
