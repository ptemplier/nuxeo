/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Interface for Administration of Audit service.
 *
 * @author tiry
 */
public interface AuditAdmin {

    /**
     * Forces log Synchronisation for a branch of the repository. This can be
     * useful to add the create entries if DB was initialized from a bulk
     * import.
     */
    long syncLogCreationEntries(String repoId, String path, Boolean recurs)
            throws ClientException;
}