package com.infinities.skyport.quartz;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.infinities.skyport.cache.impl.CachedServiceProviderImpl;
import com.infinities.skyport.model.Time;
import com.infinities.skyport.quartz.QuartzConfiguration.Precondition;

public class QuartzServiceImplTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};


	public enum MockEnum {
		Entity1, Entity2
	}


	private QuartzServiceImpl<MockEnum> service;
	private ListeningScheduledExecutorService scheduler;
	private ListeningExecutorService worker;
	private QuartzConfiguration<Mock> configuration;
	private Callable<Mock> callable;
	private Precondition condition;
	private FutureCallback<Mock> callback;
	private Time delay;
	private ScheduledFuture<?> ret;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		scheduler = context.mock(ListeningScheduledExecutorService.class);
		worker = context.mock(ListeningExecutorService.class);
		callable = context.mock(Callable.class);
		condition = context.mock(Precondition.class);
		callback = context.mock(FutureCallback.class);
		ret = context.mock(ScheduledFuture.class);
		service = new QuartzServiceImpl<MockEnum>(MockEnum.class, scheduler, worker);
		delay = new Time();
		configuration =
				new QuartzConfiguration.Builder<Mock>(callable, condition).addCallback(callback).delay(delay)
						.initialDelay(CachedServiceProviderImpl.INITIAL_DELAY).name("mock cloud").build();
	}

	@After
	public void tearDown() throws Exception {
		service.close();
		context.assertIsSatisfied();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSchedule() {
		final ListenableFuture<Mock> future = context.mock(ListenableFuture.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(scheduler).schedule(with(any(Thread.class)), with(any(Long.class)), with(any(TimeUnit.class)));
				will(new CustomAction("check ScheduleTask") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Thread thread = (Thread) invocation.getParameter(0);
						thread.run();
						return ret;
					}

				});
				exactly(1).of(ret).cancel(true);
				will(returnValue(true));

				exactly(1).of(condition).check();
				will(returnValue(true));

				exactly(1).of(worker).submit(callable);
				will(returnValue(future));

				exactly(2).of(future).addListener(with(any(Runnable.class)), with(any(ListeningExecutorService.class)));

			}
		});
		service.schedule(MockEnum.Entity1, configuration);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExecuteOnce() {
		service.executeOnce(MockEnum.Entity1);
	}

	@Test
	public void testExecuteOnceWithScheduled() {
		context.checking(new Expectations() {

			{
				exactly(2).of(scheduler).schedule(with(any(Thread.class)), with(any(Long.class)), with(any(TimeUnit.class)));
				will(returnValue(ret));
				exactly(1).of(ret).cancel(true);
				will(returnValue(true));

			}
		});
		service.schedule(MockEnum.Entity1, configuration);
		service.executeOnce(MockEnum.Entity1);
	}


	public static class Mock {

		private String id;
		private String name;


		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Mock other = (Mock) obj;
			if (id == null) {
				if (other.id != null) {
					return false;
				}
			} else if (!id.equals(other.id)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}
}
