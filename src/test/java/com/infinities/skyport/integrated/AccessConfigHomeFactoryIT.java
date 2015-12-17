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
//import org.junit.experimental.categories.Category;
//
//import com.google.common.io.Closeables;
//import com.infinities.skyport.Main;
//import com.infinities.skyport.Skyport;
//import com.infinities.skyport.async.AsyncDriver;
//import com.infinities.skyport.compute.entity.Vm;
//import com.infinities.skyport.entity.TaskEvent;
//import com.infinities.skyport.registrar.ConfigurationHomeFactory;
//import com.infinities.skyport.service.ConfigurationHome;
//import com.infinities.skyport.testcase.IntegrationTest;
//
//@Category(IntegrationTest.class)
//public class AccessConfigHomeFactoryIT {
//
//	// private static final Logger logger =
//	// LoggerFactory.getLogger(AccessConfigHomeFactoryIT.class);
//	private static Skyport main;
//
//
//	// private static final String name = "demo2";
//	// private static final String description = "demo2";
//	// private static final int numofcpus = 4;
//	// private static final int memorysize = 512;
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
//
//	}
//
//	@After
//	public void tearDown() throws Exception {
//
//	}
//
//	@Test
//	public void test() throws Exception {
//		Thread.sleep(45000);
//		ConfigurationHome accessConfigHome = ConfigurationHomeFactory.getInstance();
//
//		AsyncDriver driver = accessConfigHome.findById("9L");
//
//		Vm vm = new Vm();
//		vm.setName("demo");
//		vm.setDesc("demo");
//		vm.setCpunum(2);
//		vm.setMemorysize(1024L);
//		vm.setTemplateid("b905faa6-7341-467a-89cd-5891197901e1");
//		vm.setResourceid("1caa5fee-4368-4ad7-807e-81a17ff35adc");
//		TaskEvent event = TaskEvent.getInitializedEvent(null, "Create Vm:demo", "mock");
//		driver.createVmAsync(event, vm).get();
//		Thread.sleep(120000);
//
//	}
//
// }
