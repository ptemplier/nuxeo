package org.nuxeo.ecm.platform.ec.notification;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.placeful.PlacefulServiceImpl;
import org.nuxeo.ecm.platform.ec.placeful.TestHibernateConfiguration;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple test Case for DocUIDGeneratorListener
 *
 * @author Julien Thimonier <jt@nuxeo.com>
 */
public class NotificationEventListenerTest extends RepositoryOSGITestCase {

	private EmailHelperMock emailHelperMock = new EmailHelperMock();

	private static final Log log = LogFactory.getLog(NotificationService.class);

	@Override
	public void setUp() throws Exception {
		super.setUp();
		openRepository();
		deployBundle("org.nuxeo.ecm.platform.placeful.api");
		deployBundle("org.nuxeo.ecm.platform.placeful.core");
		deployBundle("org.nuxeo.ecm.platform.notification.core");
		deployBundle("org.nuxeo.ecm.platform.notification.api");
		deployBundle("org.nuxeo.ecm.core.event");
		deployBundle("org.nuxeo.ecm.platform.usermanager");
		deployBundle("org.nuxeo.ecm.directory");
		deployBundle("org.nuxeo.ecm.platform.types.api");
		deployBundle("org.nuxeo.ecm.platform.types.core");
		deployBundle("org.nuxeo.ecm.platform.url.api");
		deployBundle("org.nuxeo.ecm.platform.url.core");

		deployContrib("org.nuxeo.ecm.platform.notification.core.tests",
				"test-userservice-config.xml");
		deployContrib("org.nuxeo.ecm.platform.notification.core.tests",
				"notification-contrib.xml");

		deployContrib("org.nuxeo.ecm.platform.notification.core.tests",
				"test-usermanagerimpl/schemas-config.xml");

		PlacefulServiceImpl.persistenceProvider
				.setHibernateConfiguration(new TestHibernateConfiguration());

		// Injection of the EmailHelper Mock to track mails sending
		EventService eventService = Framework.getService(EventService.class);
		List<PostCommitEventListener> listeners = eventService
				.getPostCommitEventListeners();

		for (PostCommitEventListener postCommitEventListener : listeners) {
			if (postCommitEventListener.getClass().equals(
					NotificationEventListener.class))
				((NotificationEventListener) postCommitEventListener)
						.setEmailHelper(emailHelperMock);
		}
		log.info("setup Finnished");
	}

	protected DocumentModel createNoteDocument() throws ClientException {

		DocumentModel folder = getCoreSession().createDocumentModel("/",
				"test", "Folder");

		folder = getCoreSession().createDocument(folder);
		getCoreSession().saveDocument(folder);

		DocumentModel noteDoc = getCoreSession().createDocumentModel("/test/",
				"testFile", "Note");

		noteDoc.setProperty("dublincore", "title", "TestFile");
		noteDoc.setProperty("dublincore", "description", "RAS");

		noteDoc = getCoreSession().createDocument(noteDoc);

		getCoreSession().saveDocument(noteDoc);
		getCoreSession().save();

		return noteDoc;
	}

	protected void waitForAsyncExec() {

		EventServiceImpl evtService = (EventServiceImpl) Framework
				.getLocalService(EventService.class);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while ((evtService.getActiveAsyncTaskCount()) > 0) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void testListener() throws ClientException {
		EventService eventService = Framework
				.getLocalService(EventService.class);
		PlacefulServiceImpl placefulServiceImpl = (PlacefulServiceImpl) runtime
				.getComponent(PlacefulService.ID);
		DocumentModel noteDoc = createNoteDocument();
		// Enregistrement de la notification
		UserSubscription userSubscription = new UserSubscription(
				"Workflow Change", "user:"
						+ getCoreSession().getPrincipal().getName(), noteDoc
						.getId());
		placefulServiceImpl.setAnnotation(userSubscription);

		// D�clenchement de la notification
		DocumentEventContext ctx = new DocumentEventContext(getCoreSession(),
				getCoreSession().getPrincipal(), noteDoc);
		ctx.setProperty("recipients", new Object[] { "jt@nuxeo.com" });
		ctx.getProperties().put("comment", "RAS");
		eventService.fireEvent(ctx.newEvent("workflowAbandoned"));
		getCoreSession().save();
		waitForAsyncExec();
		// Check that at least one email has been sending
		assertTrue(emailHelperMock.getCompteur() > 0);

	}

}