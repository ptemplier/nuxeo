/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.webapp.base" })
public class TestBulkEditService {

    @Inject
    protected CoreSession session;

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected EventService eventService;

    @Inject
    protected BulkEditService bulkEditService;

    protected List<DocumentModel> createTestDocuments() throws ClientException {
        List<DocumentModel> docs = new ArrayList<>();
        DocumentModel file = session.createDocumentModel("/", "doc1", "File");
        file.setPropertyValue("dc:title", "doc1");
        file = session.createDocument(file);
        docs.add(file);

        file = session.createDocumentModel("/", "doc2", "File");
        file.setPropertyValue("dc:title", "doc2");
        file = session.createDocument(file);
        docs.add(file);

        file = session.createDocumentModel("/", "doc3", "File");
        file.setPropertyValue("dc:title", "doc3");
        file = session.createDocument(file);
        docs.add(file);

        // wait for fulltext processing before copy, to avoid transaction
        // isolation issues (SQL Server)
        eventService.waitForAsyncCompletion();

        return docs;
    }

    protected DocumentModel buildSimpleDocumentModel() throws ClientException {
        DocumentModel sourceDoc = new SimpleDocumentModel();
        sourceDoc.setProperty("dublincore", "title", "new title");
        sourceDoc.setProperty("dublincore", "description", "new description");
        sourceDoc.setPropertyValue("dublincore:creator", "new creator");
        sourceDoc.setPropertyValue("dc:source", "new source");
        ScopedMap map = sourceDoc.getContextData();
        map.put(BulkEditService.BULK_EDIT_PREFIX + "dc:title", true);
        map.put(BulkEditService.BULK_EDIT_PREFIX + "dc:description", false);
        map.put(BulkEditService.BULK_EDIT_PREFIX + "dc:creator", true);
        map.put(BulkEditService.BULK_EDIT_PREFIX + "dc:source", false);
        return sourceDoc;
    }

    @Test
    public void testBulkEdit() throws ClientException {
        // for tests, force the versioning policy to be the default one
        ((BulkEditServiceImpl) bulkEditService).defaultVersioningOption = BulkEditServiceImpl.DEFAULT_VERSIONING_OPTION;

        List<DocumentModel> docs = createTestDocuments();
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        bulkEditService.updateDocuments(session, sourceDoc, docs);
        for (DocumentModel doc : docs) {
            doc = session.getDocument(doc.getRef());
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator",
                    doc.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));

            assertTrue(doc.isCheckedOut());
            assertEquals("0.1+", doc.getVersionLabel());

            DocumentModel version = session.getLastDocumentVersion(doc.getRef());
            assertTrue(((String) version.getPropertyValue("dc:title")).startsWith("doc"));
            assertNull(version.getPropertyValue("dc:creator"));
            assertNull(version.getPropertyValue("dc:description"));
            assertNull(version.getPropertyValue("dc:source"));
            assertEquals("0.1", version.getVersionLabel());
        }
    }

    @Test
    public void testBulkEditNoVersion() throws Exception {
        URL url = getClass().getClassLoader().getResource(
                "test-bulkedit-noversion-contrib.xml");
        runtimeHarness.deployTestContrib("org.nuxeo.ecm.webapp.base", url);

        List<DocumentModel> docs = createTestDocuments();
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        bulkEditService.updateDocuments(session, sourceDoc, docs);
        for (DocumentModel doc : docs) {
            doc = session.getDocument(doc.getRef());
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator",
                    doc.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));

            assertTrue(doc.isCheckedOut());
            assertEquals("0.0", doc.getVersionLabel());

            assertNull(session.getLastDocumentVersion(doc.getRef()));
        }
    }

    @Test
    public void testBulkEditMajorVersion() throws Exception {
        URL url = getClass().getClassLoader().getResource(
                "test-bulkedit-majorversion-contrib.xml");
        runtimeHarness.deployTestContrib("org.nuxeo.ecm.webapp.base", url);

        List<DocumentModel> docs = createTestDocuments();
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        bulkEditService.updateDocuments(session, sourceDoc, docs);
        for (DocumentModel doc : docs) {
            doc = session.getDocument(doc.getRef());
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator",
                    doc.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));

            assertTrue(doc.isCheckedOut());
            assertEquals("1.0+", doc.getVersionLabel());

            DocumentModel version = session.getLastDocumentVersion(doc.getRef());
            assertTrue(((String) version.getPropertyValue("dc:title")).startsWith("doc"));
            assertNull(version.getPropertyValue("dc:creator"));
            assertNull(version.getPropertyValue("dc:description"));
            assertNull(version.getPropertyValue("dc:source"));
            assertEquals("1.0", version.getVersionLabel());
        }
    }

}