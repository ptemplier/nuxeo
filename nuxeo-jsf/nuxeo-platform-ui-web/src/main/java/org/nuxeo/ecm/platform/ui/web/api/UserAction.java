/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.api;

/**
 * Definitions for actions that are going to be performed after certain user actions.
 *
 * @author dragos
 */
public enum UserAction {

    VIEW("view"), EDIT("edit"), CREATE("create"), AFTER_EDIT("after-edit"), AFTER_CREATE("after-create"), GO_HOME(
            "go-home");

    final String value;

    UserAction(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
