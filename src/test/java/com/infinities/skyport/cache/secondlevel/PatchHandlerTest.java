package com.infinities.skyport.cache.secondlevel;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.compute.entity.patch.PatchBuilder;
import com.infinities.skyport.service.EntityListener;
import com.infinities.skyport.service.event.Event.Type;
import com.infinities.skyport.service.event.FailureEvent;
import com.infinities.skyport.service.event.RefreshedEvent;
import com.infinities.skyport.service.event.SuccessEvent;

public class PatchHandlerTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};

	private PatchHandler<Mock> handler;
	private String idProperty = "id";
	private Comparator<Mock> comparator;
	private PatchBuilder<Mock> patchBuilder;
	private PatchListener<Mock> inner;
	private Map<String, Mock> cache;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		comparator = context.mock(Comparator.class);
		patchBuilder = context.mock(PatchBuilder.class);
		inner = context.mock(PatchListener.class);
		cache = context.mock(Map.class);
		handler = new PatchHandler<Mock>(idProperty, comparator, patchBuilder, inner, cache);
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleInvocationObjectOnEntitiesRefreshedMethodObjectArray() throws Throwable {
		MockPatchListener proxy = new MockPatchListener();
		final Mock added = new Mock();
		added.setId("id");
		added.setName("name");
		final Mock updated = new Mock();
		updated.setId("id2");
		updated.setName("name2");
		final Mock old = new Mock();
		old.setId("id2");
		old.setName("old");
		final Mock removed = new Mock();
		removed.setId("id3");
		removed.setName("name3");
		final List<Mock> newEntries = new ArrayList<Mock>();
		newEntries.add(added);
		newEntries.add(updated);
		final Set<String> cacheKeys = new HashSet<String>();
		cacheKeys.add("id2");
		cacheKeys.add("id3");
		Method method = MockPatchListener.class.getMethod("onEntitiesRefreshed", RefreshedEvent.class);
		final RefreshedEvent<Mock> event = context.mock(RefreshedEvent.class);
		final DiffResult result = new DiffBuilder(old, updated, ToStringStyle.SHORT_PREFIX_STYLE).build();
		context.checking(new Expectations() {

			{
				exactly(1).of(event).getNewEntries();
				will(returnValue(newEntries));
				exactly(1).of(cache).get("id");
				will(returnValue(null));
				exactly(1).of(cache).get("id2");
				will(returnValue(old));
				exactly(1).of(cache).get("id3");
				will(returnValue(removed));
				exactly(1).of(comparator).compare(old, updated);
				will(returnValue(-1));
				exactly(1).of(patchBuilder).diff(old, updated);
				will(returnValue(result));
				exactly(1).of(cache).keySet();
				will(returnValue(cacheKeys));
				exactly(1).of(cache).clear();

			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(cache).putAll(with(any(Map.class)));
				will(new CustomAction("check cache") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						Map<String, Mock> map = (Map<String, Mock>) invocation.getParameter(0);
						Assert.assertEquals(2, map.size());
						Assert.assertEquals(added, map.get(added.getId()));
						Assert.assertEquals(updated, map.get(updated.getId()));
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(event).getConfigid();
				will(returnValue("id"));
				exactly(1).of(inner).onChanged(with(any(SuccessEvent.class)));
				will(new CustomAction("check removed event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						SuccessEvent<Mock> event = (SuccessEvent<Mock>) invocation.getParameter(0);
						Assert.assertEquals(1, event.getEntries().size());
						Assert.assertEquals(removed, event.getEntries().iterator().next());
						Assert.assertEquals("id", event.getConfigid());
						Assert.assertEquals(Type.REMOVED, event.getType());
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(event).getConfigid();
				will(returnValue("id"));
				exactly(1).of(inner).onChanged(with(any(SuccessEvent.class)));
				will(new CustomAction("check added event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						SuccessEvent<Mock> event = (SuccessEvent<Mock>) invocation.getParameter(0);
						Assert.assertEquals(1, event.getEntries().size());
						Assert.assertEquals(added, event.getEntries().iterator().next());
						Assert.assertEquals("id", event.getConfigid());
						Assert.assertEquals(Type.ADDED, event.getType());
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(event).getConfigid();
				will(returnValue("id"));
				exactly(1).of(inner).onChanged(with(any(SuccessEvent.class)));
				will(new CustomAction("check modified event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						SuccessEvent<Mock> event = (SuccessEvent<Mock>) invocation.getParameter(0);
						Assert.assertEquals(1, event.getEntries().size());
						Assert.assertEquals(updated, event.getEntries().iterator().next());
						Assert.assertEquals("id", event.getConfigid());
						Assert.assertEquals(Type.MODIFIED, event.getType());
						Assert.assertEquals(1, event.getPatchs().size());
						Assert.assertEquals(result, event.getPatchs().get(updated.getId()));
						return null;
					}

				});
			}
		});
		context.checking(new Expectations() {

			{
				exactly(1).of(event).getConfigid();
				will(returnValue("id"));
				exactly(1).of(inner).onChanged(with(any(SuccessEvent.class)));
				will(new CustomAction("check refresh event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						SuccessEvent<Mock> event = (SuccessEvent<Mock>) invocation.getParameter(0);
						Assert.assertEquals(2, event.getEntries().size());
						Assert.assertEquals("id", event.getConfigid());
						Assert.assertEquals(Type.REFRESHED, event.getType());
						return null;
					}

				});
			}
		});
		handler.handleInvocation(proxy, method, new Object[] { event });
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandleInvocationObjectOnFailureMethodObjectArray() throws Throwable {
		MockPatchListener proxy = new MockPatchListener();
		Method method = MockPatchListener.class.getMethod("onFailure", FailureEvent.class);
		final FailureEvent<Mock> event = context.mock(FailureEvent.class);
		context.checking(new Expectations() {

			{
				exactly(1).of(inner).onFailure(event);
				will(new CustomAction("check refresh event") {

					@Override
					public Object invoke(Invocation invocation) throws Throwable {
						FailureEvent<Mock> e = (FailureEvent<Mock>) invocation.getParameter(0);
						Assert.assertEquals(event, e);
						return null;
					}

				});
			}
		});
		handler.handleInvocation(proxy, method, new Object[] { event });
	}

	@Test
	public void testHandleInvocationObjectGetMessageMethodObjectArray() throws Throwable {
		MockPatchListener proxy = new MockPatchListener();
		Method method = MockPatchListener.class.getMethod("getMessage");
		String message = (String) handler.handleInvocation(proxy, method, new Object[] {});
		assertEquals("my message", message);
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

	public class MockPatchListener implements EntityListener {

		public String getMessage() {
			return "my message";
		}

		public void onEntitiesRefreshed(RefreshedEvent<Mock> e) throws Exception {
			System.err.println("receive refresh event");
		}

		public void onFailure(FailureEvent<Mock> e) {
			System.err.println("receive fail event");
		}

	}

}
