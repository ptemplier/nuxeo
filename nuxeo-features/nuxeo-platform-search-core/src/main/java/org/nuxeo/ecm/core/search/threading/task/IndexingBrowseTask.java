/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.threading.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingHelper;

/**
 * Recursive indexing task.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
class IndexingBrowseTask extends AbstractIndexingTask {

    private static final Log log = LogFactory.getLog(IndexingBrowseTask.class);

    public IndexingBrowseTask(DocumentRef docRef, String repositoryName) {
        super(docRef, repositoryName);
    }

    public void run() {

        log.debug("Indexing browse task started for document: " + docRef);

        try {
            DocumentModel dm = getCoreSession().getDocument(docRef);
            IndexingHelper.recursiveIndex(dm, searchService);
            log.debug("Browse task done for document: " + dm.getTitle()
                    + " docRef: " + docRef);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof IndexingBrowseTask) {
            IndexingBrowseTask task = (IndexingBrowseTask) obj;
            return docRef.equals(task.docRef)
                    && repositoryName.equals(task.repositoryName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (docRef == null ? 0 : docRef.hashCode());
        result = 37 * result
                + (repositoryName == null ? 0 : repositoryName.hashCode());
        return result;
    }
}