/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: PropertiesDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Action property descriptor
 *
 * @since 5.6
 */
@XObject("properties")
public class ActionPropertiesDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@append")
    boolean append;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<String, String>();

    @XNodeMap(value = "propertyList", key = "@name", type = HashMap.class, componentType = ActionPropertyListDescriptor.class)
    Map<String, ActionPropertyListDescriptor> listProperties = new HashMap<String, ActionPropertyListDescriptor>();

    @XNodeMap(value = "propertyMap", key = "@name", type = HashMap.class, componentType = ActionPropertiesDescriptor.class)
    Map<String, ActionPropertiesDescriptor> mapProperties = new HashMap<String, ActionPropertiesDescriptor>();

    public HashMap<String, Serializable> getAllProperties() {
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        map.putAll(properties);
        for (Map.Entry<String, ActionPropertyListDescriptor> prop : listProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getValues());
        }
        for (Map.Entry<String, ActionPropertiesDescriptor> prop : mapProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getAllProperties());
        }
        return map;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, ActionPropertyListDescriptor> getListProperties() {
        return listProperties;
    }

    public void setListProperties(Map<String, ActionPropertyListDescriptor> listProperties) {
        this.listProperties = listProperties;
    }

    public Map<String, ActionPropertiesDescriptor> getMapProperties() {
        return mapProperties;
    }

    public void setMapProperties(Map<String, ActionPropertiesDescriptor> mapProperties) {
        this.mapProperties = mapProperties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void merge(ActionPropertiesDescriptor other) {
        if (other != null) {
            if (other.getProperties() != null) {
                if (properties == null) {
                    properties = other.getProperties();
                } else {
                    properties.putAll(other.getProperties());
                }
            }
            if (other.getListProperties() != null) {
                if (listProperties == null) {
                    listProperties = other.getListProperties();
                } else {
                    listProperties.putAll(other.getListProperties());
                }
            }
            if (other.getMapProperties() != null) {
                if (mapProperties == null) {
                    mapProperties = other.getMapProperties();
                } else {
                    mapProperties.putAll(other.getMapProperties());
                }
            }
        }
    }

    public ActionPropertiesDescriptor clone() {
        ActionPropertiesDescriptor clone = new ActionPropertiesDescriptor();
        if (properties != null) {
            clone.properties = new HashMap<String, String>(properties);
        }
        if (listProperties != null) {
            clone.listProperties = new HashMap<String, ActionPropertyListDescriptor>();
            for (Map.Entry<String, ActionPropertyListDescriptor> item : listProperties.entrySet()) {
                clone.listProperties.put(item.getKey(), item.getValue().clone());
            }
        }
        if (mapProperties != null) {
            clone.mapProperties = new HashMap<String, ActionPropertiesDescriptor>();
            for (Map.Entry<String, ActionPropertiesDescriptor> item : mapProperties.entrySet()) {
                clone.mapProperties.put(item.getKey(), item.getValue().clone());
            }
        }
        return clone;
    }

}
