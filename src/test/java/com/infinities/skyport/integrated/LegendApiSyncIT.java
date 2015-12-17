package com.infinities.skyport.integrated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Closeables;
import com.infinities.skyport.Main;
import com.infinities.skyport.Skyport;
import com.infinities.skyport.testcase.IntegrationTest;
import com.infinities.skyport.util.JsonUtil;

@Category(IntegrationTest.class)
public class LegendApiSyncIT {

	// private static final Logger logger =
	// LoggerFactory.getLogger(EntityListenerIT.class);
	private static Skyport main;
	private static final String URL = "https://127.0.0.1:8443/skyport";


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

	@Test
	public void testAll() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException,
			InterruptedException, URISyntaxException {

		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(sslsf)
				.setHostnameVerifier(new AllowAllHostnameVerifier()).build();
		Thread.sleep(45000);
		try {

			URIBuilder uriBuilder = new URIBuilder(URL);
			uriBuilder.setParameter("cmd", "1").setParameter("name", "demo5").setParameter("config", "mock")
					.setParameter("templateid", "b905faa6-7341-467a-89cd-5891197901e1");

			HttpPost method = new HttpPost(uriBuilder.build());
			CloseableHttpResponse response = client.execute(method);
			String vmid = null;
			try {
				int statusCode = response.getStatusLine().getStatusCode();
				assertEquals(200, statusCode);
				HttpEntity entity = response.getEntity();
				String ret = EntityUtils.toString(entity);
				System.out.println(ret);
				JsonNode root = JsonUtil.getLegendNode(ret);
				vmid = root.get(1).get("VmId").asText();
				assertNotNull(vmid);
			} finally {
				response.close();
			}

			URIBuilder uriBuilder2 = new URIBuilder(URL);
			uriBuilder2.setParameter("cmd", "1610612740").setParameter("config", "mock").setParameter("vmid", vmid);
			HttpPost method2 = new HttpPost(uriBuilder2.build());
			CloseableHttpResponse response2 = client.execute(method2);
			try {
				int statusCode2 = response2.getStatusLine().getStatusCode();
				assertEquals(200, statusCode2);
				HttpEntity entity2 = response2.getEntity();
				String ret2 = EntityUtils.toString(entity2);
				assertNotNull(ret2);
				System.out.println(ret2);
			} finally {
				response2.close();
			}

		} finally {
			client.close();
		}
	}

}
