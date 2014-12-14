package org.araqnid.stuff.test.integration.workqueue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.araqnid.stuff.test.integration.IntegrationTest;
import org.araqnid.stuff.workqueue.HibernateWorkQueue;
import org.araqnid.stuff.workqueue.WorkItem;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class HibernateWorkQueueIntegrationTest extends IntegrationTest {
	@Test
	public void create_item() {
		String queueName = randomString("queue");
		String entryId = randomString("entry");
		byte[] payload = randomString("payload").getBytes(StandardCharsets.UTF_8);
		SessionFactory sessionFactory = server.getInjector().getInstance(SessionFactory.class);
		HibernateWorkQueue testQueue = new HibernateWorkQueue(queueName, sessionFactory);
		testQueue.create(entryId, payload);

		assertThat(hqlQuery(WorkItem.class, "from WorkItem item where item.queueCode = ?", queueName),
				contains(workItem(entryId)));
	}

	private <T> List<T> hqlQuery(Class<T> clazz, String hql, Object... params) {
		SessionFactory sessionFactory = server.getInjector().getInstance(SessionFactory.class);
		StatelessSession session = sessionFactory.openStatelessSession();
		Query query = session.createQuery(hql);
		for (int i = 0; i < params.length; i++) {
			query.setParameter(i, params[i]);
		}
		List<?> rawList = query.list();
		List<T> output = Lists.newArrayListWithExpectedSize(rawList.size());
		for (Object item : rawList) {
			output.add(clazz.cast(item));
		}
		return output;
	}

	private static Matcher<WorkItem> workItem(final String entryId) {
		return new TypeSafeDiagnosingMatcher<WorkItem>() {
			@Override
			protected boolean matchesSafely(WorkItem item, Description mismatchDescription) {
				if (!item.getReference().equals(entryId)) {
					mismatchDescription.appendText("reference is ").appendValue(item.getReference());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("work item ").appendValue(entryId);
			}
		};
	}
}
