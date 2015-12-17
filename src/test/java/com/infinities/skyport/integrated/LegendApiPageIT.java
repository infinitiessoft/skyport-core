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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;
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
public class LegendApiPageIT {

	private static Skyport main;
	private static final String URL = "http://127.0.0.1:8085/skyport";
	private CloseableHttpClient client;


	@BeforeClass
	public static void beforeClass() throws Throwable {
		URL url = Thread.currentThread().getContextClassLoader().getResource("accessconfig_pagetest.xml");
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
	public void testListPage() throws HttpException, IOException, InterruptedException, URISyntaxException {
		Thread.sleep(40000);
		checkListVm(CMD.LIST_VM);
		checkListCluster();
		checkListStorage();
		checkListResource();
		checkSearchHost();
		checkListVm(CMD.GET_ALL_RUNNING_VM_STATUS);
		checkListNetwork();
		checkListTemplate();
		checkListVmNics();
		checkListVmDisks();
	}

	private void checkListStorage() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_STORAGE))
				.setParameter("config", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?loginname=admin&cmd=" +  String.valueOf(CMD.LIST_STORAGE)
				+ "&config=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "1caa39ee-4368-4ad7-807e-81a17ff35adc", "storage0", "storage1", "storage2",
					"storage3" };
			checkElement(root, "StorageDomainId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "storage4", "storage5", "storage6", "storage7", "storage8" };
			checkElement(root, "StorageDomainId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(1, root.size() - 1);
			String[] elements = new String[] { "storage9" };
			checkElement(root, "StorageDomainId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "storage3";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListCluster() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_CLUSTER))
				.setParameter("config", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?loginname=admin&cmd=" +  String.valueOf(CMD.LIST_CLUSTER)
				+ "&config=mock&loginpass=1111";
		
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "1caa5fee-4368-4ad7-807e-81a17ff35adc", "cluster0", "cluster1", "cluster2",
					"cluster3" };
			checkElement(root, "ClusterId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "cluster4", "cluster5", "cluster6", "cluster7", "cluster8" };
			checkElement(root, "ClusterId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(1, root.size() - 1);
			String[] elements = new String[] { "cluster9" };
			checkElement(root, "ClusterId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "cluster3";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListVmNics() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_NETWORK_ADAPTERS))
				.setParameter("config", "mock").setParameter("vmid", "vm0").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?vmid=vm0&loginname=admin&cmd=" + String.valueOf(CMD.GET_NETWORK_ADAPTERS)
				+ "&config=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "g58c8ba5-0c23-42fc-abb7-1c8d0c3e1f41",
					"g58c8ba5-0c23-42fc-bbb7-1c8d0c3e1f41", "nic0", "nic1", "nic2" };
			checkElement(root, "NicId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "nic3", "nic4", "nic5", "nic6", "nic7" };
			checkElement(root, "NicId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(2, root.size() - 1);
			String[] elements = new String[] { "nic8", "nic9" };
			checkElement(root, "NicId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "nic2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListVmDisks() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_DISK))
				.setParameter("config", "mock").setParameter("vmid", "vm0").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?vmid=vm0&loginname=admin&cmd=" + String.valueOf(CMD.LIST_DISK)
				+ "&config=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "a58c8ba5-0c23-42fc-abb7-2b8d0c3e1f41", "disk0", "disk1", "disk2", "disk3" };
			checkElement(root, "DiskId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "disk4", "disk5", "disk6", "disk7", "disk8" };
			checkElement(root, "DiskId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(1, root.size() - 1);
			String[] elements = new String[] { "disk9" };
			checkElement(root, "DiskId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "disk3";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListVm(long cmd) throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(cmd))
				.setParameter("config", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?loginname=admin&cmd=" + String.valueOf(cmd)
				+ "&config=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "00000000-0000-0000-0000-00000000", "ee97a92a-2c9c-443a-8a6d-657d97bbe679",
					"vm0", "vm1", "vm2" };
			checkElement(root, "VmId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "vm3", "vm4", "vm5", "vm6", "vm7" };
			checkElement(root, "VmId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(2, root.size() - 1);
			String[] elements = new String[] { "vm8", "vm9" };
			checkElement(root, "VmId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "vm2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}

	}

	private void checkListTemplate() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_TEMPLATE))
				.setParameter("config", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?loginname=admin&cmd=" +  String.valueOf(CMD.LIST_TEMPLATE)
				+ "&config=mock&loginpass=1111";
		
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "b905faa6-7341-467a-89cd-5891197901e1",
					"f58c8ba5-0c23-42fc-aab7-1c8d0c3e1f41", "template0", "template1", "template2" };
			checkElement(root, "TemplateId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "template3", "template4", "template5", "template6", "template7" };
			checkElement(root, "TemplateId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(2, root.size() - 1);
			String[] elements = new String[] { "template8", "template9" };
			checkElement(root, "TemplateId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "template2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListResource() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_RESOURCE))
				.setParameter("config", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?loginname=admin&cmd=" +  String.valueOf(CMD.LIST_RESOURCE)
				+ "&config=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "1caa5fee-4368-4ad7-807e-81a17ff35adc", "cluster0", "cluster1", "cluster2",
					"cluster3" };
			checkElement(root, "ResourceId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "cluster4", "cluster5", "cluster6", "cluster7", "cluster8" };
			checkElement(root, "ResourceId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(1, root.size() - 1);
			String[] elements = new String[] { "cluster9" };
			checkElement(root, "ResourceId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "cluster3";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListNetwork() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.GET_NETWORKS))
				.setParameter("config", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?loginname=admin&cmd=" +  String.valueOf(CMD.GET_NETWORKS)
				+ "&config=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "1fef5fba-9fc9-47e3-af3e-9ac9ad9dd6bb",
					"9a8e0df6-3ac5-4bd0-b2ff-e4b6b2d4e61b", "network0", "network1", "network2" };
			checkElement(root, "NetworkId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "network3", "network4", "network5", "network6", "network7" };
			checkElement(root, "NetworkId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(2, root.size() - 1);
			String[] elements = new String[] { "network8", "network9" };
			checkElement(root, "NetworkId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "network2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkSearchHost() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.SEARCH_HOST))
				.setParameter("config", "mock").setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyport?loginname=admin&cmd=" +  String.valueOf(CMD.SEARCH_HOST)
				+ "&config=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			// JsonNode data = root.get("data");
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "1fea5fba-9fc9-47e3-af3e-9ac9ad9dd6bb",
					"e546660a-5419-4ec8-b444-1c9e0c291b94", "host0", "host1", "host2" };
			checkElement(root, "HostId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("next", root.get(0).get("links").get(0).get("rel").asText());
			next = root.get(0).get("links").get(0).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(5, root.size() - 1);
			String[] elements = new String[] { "host3", "host4", "host5", "host6", "host7" };
			checkElement(root, "HostId", elements);
			assertEquals(2, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get(0).get("links").get(1).get("rel").asText());
			next = root.get(0).get("links").get(1).get("href").asText();
			String gd = elements[4];
			assertEquals(url + "&marker=" + gd + "&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.getLegendNode(ret);
			assertEquals("TRUE", root.get(0).get("RES").asText());
			assertEquals(2, root.size() - 1);
			String[] elements = new String[] { "host8", "host9" };
			checkElement(root, "HostId", elements);
			assertEquals(1, root.get(0).get("links").size());
			assertEquals("prev", root.get(0).get("links").get(0).get("rel").asText());
			prev = root.get(0).get("links").get(0).get("href").asText();
			String gd = "host2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkElement(JsonNode data, String identifier, String[] ids) {
		for (int i = 0; i < ids.length; i++) {
			String expect = ids[i];
			String real = data.get(i + 1).get(identifier).asText();
			assertEquals(expect, real);
		}
	}
}
