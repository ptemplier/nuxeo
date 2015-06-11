/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Tiry
 *
 */
package org.nuxeo.elasticsearch;

import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Deploy({ "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit", "org.nuxeo.ecm.platform.uidgen.core",
        "org.nuxeo.elasticsearch.seqgen", "org.nuxeo.elasticsearch.audit" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.audit:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:audit-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:es-audit-pageprovider-test-contrib.xml" })
public class TestAuditPageProviderWithElasticSearch {

    protected @Inject CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected ElasticSearchAdmin esa;

    protected void flushAndSync() throws Exception {

        TransactionHelper.commitOrRollbackTransaction();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        esa.getClient().admin().indices().prepareFlush(ESAuditBackend.IDX_NAME).execute().actionGet();
        esa.getClient().admin().indices().prepareRefresh(ESAuditBackend.IDX_NAME).execute().actionGet();

        TransactionHelper.startTransaction();

    }

    protected Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<String, ExtendedInfo>();
        ExtendedInfo info = ExtendedInfoImpl.createExtendedInfo(new Long(1));
        infos.put("id", info);
        return infos;
    }

    protected LogEntry doCreateEntry(String docId, String eventId, String category) {
        LogEntry createdEntry = new LogEntryImpl();
        createdEntry.setEventId(eventId);
        createdEntry.setCategory(category);
        createdEntry.setDocUUID(docId);
        createdEntry.setEventDate(new Date());
        createdEntry.setDocPath("/" + docId);
        createdEntry.setRepositoryId("test");
        createdEntry.setExtendedInfos(createExtendedInfos());

        return createdEntry;
    }

    @Test
    public void testSimplePageProvider() throws Exception {

        LogEntryGen.generate("dummy", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleESAuditPP", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>());
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        Assert.assertEquals(5, entries.size());
        Assert.assertEquals(5, pp.getCurrentPageSize());
        Assert.assertEquals(7, pp.getResultsCount());

        // check that sort does work
        Assert.assertTrue(entries.get(0).getId() < entries.get(1).getId());
        Assert.assertTrue(entries.get(3).getId() < entries.get(4).getId());
    }

    @Test
    public void testSimplePageProviderWithParams() throws Exception {

        LogEntryGen.generate("withParams", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleESAuditPPWithParams", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>(), "category1");
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertEquals(2, entries.size());

        // check that sort does work
        Assert.assertTrue(entries.get(0).getId() > entries.get(1).getId());

        pp = pps.getPageProvider("SimpleESAuditPPWithParams", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>(), "category0");
        entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertEquals(1, entries.size());

    }

    @Test
    public void testSimplePageProviderWithUUID() throws Exception {

        LogEntryGen.generate("uuid1", "uentry", "ucategory", 10);
        PageProvider<?> pp = pps.getPageProvider("SearchById", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>(), "uuid1");
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertEquals(5, entries.size());
    }

    @Test
    public void testAdminPageProvider() throws Exception {

        LogEntryGen.generate("uuid2", "aentry", "acategory", 10);

        PageProvider<?> pp = pps.getPageProvider("ADMIN_HISTORY", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>());
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertEquals(5, entries.size());
    }

}
