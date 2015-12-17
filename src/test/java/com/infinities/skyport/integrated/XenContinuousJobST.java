//package com.infinities.skyport.integrated;
//
//import java.io.IOException;
//import java.net.URL;
//
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import com.google.common.io.Closeables;
//import com.google.common.util.concurrent.ListenableFuture;
//import com.infinities.skyport.Main;
//import com.infinities.skyport.Skyport;
//import com.infinities.skyport.async.AsyncDriver;
//import com.infinities.skyport.compute.IVm;
//import com.infinities.skyport.compute.entity.Disk;
//import com.infinities.skyport.compute.entity.Vm;
//import com.infinities.skyport.registrar.ConfigurationHomeFactory;
//import com.infinities.skyport.service.ConfigurationHome;
//
//public class XenContinuousJobST {
//
//	private static Skyport main;
//	private final String pool = "xen";
//	private static String templateid = "4dc152f7-2b44-41fe-6349-8b7866021a97";
//
//
//	@BeforeClass
//	public static void beforeClass() throws Throwable {
//		URL url = Thread.currentThread().getContextClassLoader().getResource("accessconfig_xen.xml");
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
//	}
//
//	@After
//	public void tearDown() throws Exception {
//	}
//
//	@Test
//	public void testAll() throws Exception {
//		Thread.sleep(45000);
//		ConfigurationHome accessConfigHome = ConfigurationHomeFactory.getInstance();
//		AsyncDriver driver = accessConfigHome.findByName(pool);
//
//		Vm args = new Vm();
//		args.setName("continuousTest");
//		args.setTemplateid(templateid);
//		ListenableFuture<IVm> future = driver.createVmAsync(null, args);
//
//		IVm newVm = future.get();
//
//		Disk diskArgs = new Disk();
//		diskArgs.setVmId(newVm.getVmid());
//		diskArgs.setSizegb(5L);
//		ListenableFuture<Disk> diskFuture = driver.addDiskAsync(null, diskArgs);
//		diskFuture.get();
//
//		args.setVmid(newVm.getVmid());
//		ListenableFuture<IVm> removeFuture = driver.removeVmAsync(null, args);
//		removeFuture.get();
//	}
// }
