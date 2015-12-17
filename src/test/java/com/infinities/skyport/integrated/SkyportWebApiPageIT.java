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
import org.apache.http.ParseException;
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
import com.infinities.skyport.jpa.JpaProperties;
import com.infinities.skyport.testcase.IntegrationTest;
import com.infinities.skyport.util.JsonUtil;

@Category(IntegrationTest.class)
public class SkyportWebApiPageIT extends AbstractDbTest {

	private static Skyport main;
	private static final String URL = "http://127.0.0.1:8085/skyportWeb";
	private CloseableHttpClient client;
	private final String pool = "mock";


	@BeforeClass
	public static void beforeClass() throws Throwable {
		JpaProperties.PERSISTENCE_UNIT_NAME = "com.infinities.skyport.jpa.test";
		JpaProperties.JPA_PROPERTIES_FILE = null;
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
	public void testListEvent() throws HttpException, IOException, InterruptedException, URISyntaxException {
		Thread.sleep(5000);
		checkListEvent();
		checkListGroup();
		checkListPermission();
		checkListScope();
		checkListUser();

		checkListApi();
		checkListDriver();
		checkListPool();

		Thread.sleep(30000);
		checkListHost();
		checkListNetwork();
		checkListResource();
		checkListTemplate();
		checkListVm();
		checkListVmNics();
	}

	private void checkListPool() throws URISyntaxException, IOException {
		checkList(CMD.LIST_POOL, "poolid", 11);
	}

	private void checkListVmNics() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_VM_NICS))
				.setParameter("pool", pool).setParameter("vmid", "vm0").setParameter("loginname", "admin")
				.setParameter("loginpass", "1111").setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyportWeb?vmid=vm0&loginname=admin&cmd="
				+ String.valueOf(CMD.LIST_POOL_VM_NICS) + "&pool=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "g58c8ba5-0c23-42fc-abb7-1c8d0c3e1f41",
					"g58c8ba5-0c23-42fc-bbb7-1c8d0c3e1f41", "nic0", "nic1", "nic2" };
			checkElement(data, "nicid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "nic3", "nic4", "nic5", "nic6", "nic7" };
			checkElement(data, "nicid", elements);
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(2, data.size());
			String[] elements = new String[] { "nic8", "nic9" };
			checkElement(data, "nicid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			String gd = "nic2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListVm() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_VM))
				.setParameter("pool", pool).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + String.valueOf(CMD.LIST_POOL_VM)
				+ "&pool=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "00000000-0000-0000-0000-00000000", "ee97a92a-2c9c-443a-8a6d-657d97bbe679",
					"vm0", "vm1", "vm2" };
			checkElement(data, "vmid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "vm3", "vm4", "vm5", "vm6", "vm7" };
			checkElement(data, "vmid", elements);
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(2, data.size());
			String[] elements = new String[] { "vm8", "vm9" };
			checkElement(data, "vmid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			String gd = "vm2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}

	}

	private void checkListTemplate() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_TEMPLATE))
				.setParameter("pool", pool).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + String.valueOf(CMD.LIST_POOL_TEMPLATE)
				+ "&pool=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "b905faa6-7341-467a-89cd-5891197901e1",
					"f58c8ba5-0c23-42fc-aab7-1c8d0c3e1f41", "template0", "template1", "template2" };
			checkElement(data, "templateid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "template3", "template4", "template5", "template6", "template7" };
			checkElement(data, "templateid", elements);
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(2, data.size());
			String[] elements = new String[] { "template8", "template9" };
			checkElement(data, "templateid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			String gd = "template2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}

	}

	private void checkListResource() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_RESOURCE))
				.setParameter("pool", pool).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + String.valueOf(CMD.LIST_POOL_RESOURCE)
				+ "&pool=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "1caa5fee-4368-4ad7-807e-81a17ff35adc", "cluster0", "cluster1", "cluster2",
					"cluster3" };
			checkElement(data, "resourceid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "cluster4", "cluster5", "cluster6", "cluster7", "cluster8" };
			checkElement(data, "resourceid", elements);
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			String[] elements = new String[] { "cluster9" };
			assertEquals(elements.length, data.size());
			checkElement(data, "resourceid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			String gd = "cluster3";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}

	}

	private void checkListNetwork() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_NETWORK))
				.setParameter("pool", pool).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + String.valueOf(CMD.LIST_POOL_NETWORK)
				+ "&pool=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "1fef5fba-9fc9-47e3-af3e-9ac9ad9dd6bb",
					"9a8e0df6-3ac5-4bd0-b2ff-e4b6b2d4e61b", "network0", "network1", "network2" };
			checkElement(data, "networkid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "network3", "network4", "network5", "network6", "network7" };
			checkElement(data, "networkid", elements);
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(2, data.size());
			String[] elements = new String[] { "network8", "network9" };
			checkElement(data, "networkid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			String gd = "network2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}

	}

	private void checkListHost() throws URISyntaxException, ClientProtocolException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_POOL_HOST))
				.setParameter("pool", pool).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + String.valueOf(CMD.LIST_POOL_HOST)
				+ "&pool=mock&loginpass=1111";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "1fea5fba-9fc9-47e3-af3e-9ac9ad9dd6bb",
					"e546660a-5419-4ec8-b444-1c9e0c291b94", "host0", "host1", "host2" };
			checkElement(data, "hostid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			String[] elements = new String[] { "host3", "host4", "host5", "host6", "host7" };
			checkElement(data, "hostid", elements);
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals(url + "&marker=&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(2, data.size());
			String[] elements = new String[] { "host8", "host9" };
			checkElement(data, "hostid", elements);
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			String gd = "host2";
			assertEquals(url + "&marker=" + gd + "&limit=5", prev);
		} finally {
			response.close();
		}
	}

	private void checkListDriver() throws URISyntaxException, ParseException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_DRIVER))
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").setParameter("limit", "4").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		String url = "http://127.0.0.1:8085/skyportWeb?";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(4, data.size());
			checkElement(data, "driver", new String[] { "com.infinities.skyport.azure.AzureDriver",
					"com.infinities.skyport.cake.CakeDriver", "com.infinities.skyport.ec2.Ec2Driver",
					"com.infinities.skyport.mock.MockDriver" });
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
			String gd = "com.infinities.skyport.mock.MockDriver";
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767119&loginpass=1111&marker=" + gd
					+ "&limit=4", next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(4, data.size());
			checkElement(data, "driver", new String[] { "com.infinities.skyport.openstack.grizzly.GrizzlyDriver",
					"com.infinities.skyport.rhevm.Rhevm3Driver", "com.infinities.skyport.scvmm.Scvmm2008Driver",
					"com.infinities.skyport.scvmm2012.Scvmm2012Driver" });
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767119&loginpass=1111&marker=&limit=4",
					prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
			String gd = "com.infinities.skyport.scvmm2012.Scvmm2012Driver";
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767119&loginpass=1111&marker=" + gd
					+ "&limit=4", next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(3, data.size());
			checkElement(data, "driver", new String[] { "com.infinities.skyport.softlayer.SoftlayerDriver",
					"com.infinities.skyport.vmware.VmwareDriver", "com.infinities.skyport.xen.XenDriver" });
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			String gd = "com.infinities.skyport.mock.MockDriver";
			assertEquals(url + "loginname=admin&cmd=318767119&loginpass=1111&marker=" + gd + "&limit=4", prev);
		} finally {
			response.close();
		}
	}

	private void checkListApi() throws URISyntaxException, ParseException, IOException {
		// listEvent
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_API))
				.setParameter("pool", pool).setParameter("loginname", "admin").setParameter("loginpass", "1111")
				.setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, "id", new int[] { 1, 2, 3, 4, 6 });
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767107&pool=" + pool
					+ "&loginpass=1111&marker=6&limit=5", next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, "id", new int[] { 7, 8, 10, 15, 268435457 });
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767107&pool=" + pool
					+ "&loginpass=1111&marker=&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767107&pool=" + pool
					+ "&loginpass=1111&marker=268435457&limit=5", next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, "id", new int[] { 268435458, 268435459, 268435473, 285212673, 285212674 });
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767107&pool=" + pool
					+ "&loginpass=1111&marker=6&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767107&pool=" + pool
					+ "&loginpass=1111&marker=285212674&limit=5", next);
		} finally {
			response.close();
		}
		method = new HttpPost(new URIBuilder(next).build());
		response = client.execute(method);
		String url = "http://127.0.0.1:8085/skyportWeb?";
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, "id", new int[] { 285212675, 285212676, 805306369, 1073741825, 1073741826 });
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767107&pool=" + pool
					+ "&loginpass=1111&marker=268435457&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
			assertEquals(url + "loginname=admin&cmd=318767107&pool=" + pool + "&loginpass=1111&marker=1073741826&limit=5",
					next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, "id", new int[] { 1073741827, 1342177282, 1342177283, 1342177284, 1610612737 });
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767107&pool=" + pool
					+ "&loginpass=1111&marker=285212674&limit=5", prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
			assertEquals(url + "loginname=admin&cmd=318767107&pool=" + pool + "&loginpass=1111&marker=1610612737&limit=5",
					next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(3, data.size());
			checkElement(data, "id", new int[] { 1610612738, 1610612739, 1610612740 });
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals(url + "loginname=admin&cmd=318767107&pool=" + pool + "&loginpass=1111&marker=1073741826&limit=5",
					prev);
		} finally {
			response.close();
		}
	}

	private void checkListUser() throws URISyntaxException, ClientProtocolException, IOException {
		checkList(CMD.LIST_USER, "userid", 11);
	}

	private void checkListScope() throws URISyntaxException, ParseException, IOException {
		checkList(CMD.LIST_POSSIBLE_SCOPE, "scopeid", 11);
	}

	private void checkListPermission() throws URISyntaxException, ClientProtocolException, IOException {
		checkList(CMD.LIST_PERMISSION, "permissionid", 17);
	}

	private void checkListGroup() throws URISyntaxException, ClientProtocolException, IOException {
		checkList(CMD.LIST_GROUP, "groupid", 11);
	}

	private void checkListEvent() throws URISyntaxException, ClientProtocolException, IOException {
		// listEvent
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(CMD.LIST_EVENT))
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, "eventid", new int[] { 11, 10, 9, 8, 7 });
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767135&loginpass=1111&marker=7&limit=5",
					next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, "eventid", new int[] { 6, 5, 4, 3, 2 });
			assertEquals(2, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767135&loginpass=1111&marker=&limit=5",
					prev);
			assertEquals("next", root.get("links").get(1).get("rel").asText());
			next = root.get("links").get(1).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767135&loginpass=1111&marker=2&limit=5",
					next);
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
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(1, data.size());
			checkElement(data, "eventid", new int[] { 1 });
			assertEquals(1, root.get("links").size());
			assertEquals("prev", root.get("links").get(0).get("rel").asText());
			prev = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=318767135&loginpass=1111&marker=7&limit=5",
					prev);
		} finally {
			response.close();
		}
	}

	private void checkList(long cmd, String identifier, int size) throws URISyntaxException, IOException {
		HttpPost method = new HttpPost(new URIBuilder(URL).setParameter("cmd", String.valueOf(cmd))
				.setParameter("loginname", "admin").setParameter("loginpass", "1111").setParameter("limit", "5").build());
		CloseableHttpResponse response = client.execute(method);
		JsonNode root = null;
		String next = null;
		String prev = null;
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			assertEquals(200, statusCode);
			HttpEntity entity = response.getEntity();
			String ret = EntityUtils.toString(entity);
			System.err.println(ret);
			root = JsonUtil.readJson(ret);
			assertEquals(1, root.get("status").asInt());
			JsonNode data = root.get("data");
			assertEquals(5, data.size());
			checkElement(data, identifier, new int[] { 1, 2, 3, 4, 5 });
			assertEquals(1, root.get("links").size());
			assertEquals("next", root.get("links").get(0).get("rel").asText());
			next = root.get("links").get(0).get("href").asText();
			assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + cmd + "&loginpass=1111&marker=5&limit=5",
					next);
		} finally {
			response.close();
		}
		for (int i = 6; i <= size;) {
			method = new HttpPost(new URIBuilder(next).build());
			response = client.execute(method);
			try {
				int statusCode = response.getStatusLine().getStatusCode();
				assertEquals(200, statusCode);
				HttpEntity entity = response.getEntity();
				String ret = EntityUtils.toString(entity);
				System.err.println(ret);
				root = JsonUtil.readJson(ret);
				assertEquals(1, root.get("status").asInt());
				JsonNode data = root.get("data");

				int previous = i - 6;
				String pStr = previous > 0 ? String.valueOf(previous) : "";

				int[] intArray = new int[(size + 1 - i) > 5 ? 5 : (size + 1 - i)];
				for (int j = 0; j < intArray.length; j++) {
					intArray[j] = i++;
				}

				assertEquals(intArray.length, data.size());
				String nextStr = String.valueOf(intArray[intArray.length - 1]);

				checkElement(data, identifier, intArray);
				int linkSize = i <= size ? 2 : 1;
				assertEquals(linkSize, root.get("links").size());
				assertEquals("prev", root.get("links").get(0).get("rel").asText());
				prev = root.get("links").get(0).get("href").asText();
				assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + cmd + "&loginpass=1111&marker="
						+ pStr + "&limit=5", prev);

				if (linkSize == 2) {
					assertEquals("next", root.get("links").get(1).get("rel").asText());
					next = root.get("links").get(1).get("href").asText();
					assertEquals("http://127.0.0.1:8085/skyportWeb?loginname=admin&cmd=" + cmd + "&loginpass=1111&marker="
							+ nextStr + "&limit=5", next);
				}
			} finally {
				response.close();
			}
		}
	}

	private void checkElement(JsonNode data, String identifier, int[] ids) {
		for (int i = 0; i < ids.length; i++) {
			int expect = ids[i];
			int real = data.get(i).get(identifier).asInt();
			assertEquals(expect, real);
		}
	}

	private void checkElement(JsonNode data, String identifier, String[] ids) {
		for (int i = 0; i < ids.length; i++) {
			String expect = ids[i];
			String real = data.get(i).get(identifier).asText();
			assertEquals(expect, real);
		}
	}
}
