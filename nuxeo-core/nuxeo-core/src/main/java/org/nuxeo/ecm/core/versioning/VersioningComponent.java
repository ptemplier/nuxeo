/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Versioning service component and implementation.
 */
public class VersioningComponent extends DefaultComponent implements VersioningService {

    private static final Log log = LogFactory.getLog(VersioningComponent.class);

    public static final String VERSIONING_SERVICE_XP = "versioningService";

    public static final String VERSIONING_RULE_XP = "versioningRules";

    protected static final StandardVersioningService STANDARD_VERSIONING_SERVICE = new StandardVersioningService();

    protected Map<VersioningServiceDescriptor, VersioningService> versioningServices = new LinkedHashMap<>();

    protected VersioningRuleRegistry versioningRulesRegistry = new VersioningRuleRegistry();

    protected static class VersioningRuleRegistry extends SimpleContributionRegistry<VersioningRuleDescriptor> {

        @Override
        public String getContributionId(VersioningRuleDescriptor contrib) {
            return contrib.getTypeName();
        }

        @Override
        public VersioningRuleDescriptor clone(VersioningRuleDescriptor orig) {
            return new VersioningRuleDescriptor(orig);
        }

        @Override
        public void merge(VersioningRuleDescriptor src, VersioningRuleDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        @Override
        public void contributionUpdated(String id, VersioningRuleDescriptor contrib,
                VersioningRuleDescriptor newOrigContrib) {
            if (contrib.isEnabled()) {
                currentContribs.put(id, contrib);
            } else {
                currentContribs.remove(id);
            }
        }

        public void clear() {
            currentContribs.clear();
        }

        public Map<String, VersioningRuleDescriptor> getVersioningRuleDescriptors() {
            return currentContribs;
        }
    }

    protected Deque<DefaultVersioningRuleDescriptor> defaultVersioningRuleList = new ArrayDeque<>();

    // public for tests
    public VersioningService service = STANDARD_VERSIONING_SERVICE;

    protected ComponentContext context;

    @Override
    public void activate(ComponentContext context) {
        this.context = context;
    }

    @Override
    public void deactivate(ComponentContext context) {
        this.context = null;
    }

    @Override
    public void registerContribution(Object contrib, String point, ComponentInstance contributor) {
        if (VERSIONING_SERVICE_XP.equals(point)) {
            registerVersioningService((VersioningServiceDescriptor) contrib);
        } else if (VERSIONING_RULE_XP.equals(point)) {
            if (contrib instanceof VersioningRuleDescriptor) {
                registerVersioningRule((VersioningRuleDescriptor) contrib);
            } else if (contrib instanceof DefaultVersioningRuleDescriptor) {
                registerDefaultVersioningRule((DefaultVersioningRuleDescriptor) contrib);
            } else {
                throw new RuntimeException("Unknown contribution to " + point + ": " + contrib.getClass());
            }
        } else {
            throw new RuntimeException("Unknown extension point: " + point);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String point, ComponentInstance contributor) {
        if (VERSIONING_SERVICE_XP.equals(point)) {
            unregisterVersioningService((VersioningServiceDescriptor) contrib);
        } else if (VERSIONING_RULE_XP.equals(point)) {
            if (contrib instanceof VersioningRuleDescriptor) {
                unregisterVersioningRule((VersioningRuleDescriptor) contrib);
            } else if (contrib instanceof DefaultVersioningRuleDescriptor) {
                unregisterDefaultVersioningRule((DefaultVersioningRuleDescriptor) contrib);
            }
        }
    }

    protected void registerVersioningService(VersioningServiceDescriptor contrib) {
        String klass = contrib.className;
        try {
            VersioningService vs = (VersioningService) context.getRuntimeContext().loadClass(klass).newInstance();
            versioningServices.put(contrib, vs);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate: " + klass, e);
        }
        log.info("Registered versioning service: " + klass);
        recompute();
    }

    protected void unregisterVersioningService(VersioningServiceDescriptor contrib) {
        versioningServices.remove(contrib);
        log.info("Unregistered versioning service: " + contrib.className);
        recompute();
    }

    protected void registerVersioningRule(VersioningRuleDescriptor contrib) {
        versioningRulesRegistry.addContribution(contrib);
        log.info("Registered versioning rule: " + contrib.getTypeName());
        recompute();
    }

    protected void unregisterVersioningRule(VersioningRuleDescriptor contrib) {
        versioningRulesRegistry.removeContribution(contrib);
        log.info("Unregistered versioning rule: " + contrib.getTypeName());
        recompute();
    }

    protected void registerDefaultVersioningRule(DefaultVersioningRuleDescriptor contrib) {
        // could use a linked set instead, but given the size a linked list is enough
        defaultVersioningRuleList.add(contrib);
        recompute();
    }

    protected void unregisterDefaultVersioningRule(DefaultVersioningRuleDescriptor contrib) {
        defaultVersioningRuleList.remove(contrib);
        recompute();
    }

    protected void recompute() {
        VersioningService versioningService = STANDARD_VERSIONING_SERVICE;
        for (VersioningService vs : versioningServices.values()) {
            versioningService = vs;
        }
        if (versioningService instanceof ExtendableVersioningService) {
            ExtendableVersioningService vs = (ExtendableVersioningService) versioningService;
            vs.setVersioningRules(getVersioningRules());
            vs.setDefaultVersioningRule(getDefaultVersioningRule());
        }
        this.service = versioningService;
    }

    protected Map<String, VersioningRuleDescriptor> getVersioningRules() {
        return versioningRulesRegistry.getVersioningRuleDescriptors();
    }

    protected DefaultVersioningRuleDescriptor getDefaultVersioningRule() {
        return defaultVersioningRuleList.peekLast();
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return service.getVersionLabel(doc);
    }

    @Override
    public void doPostCreate(Document doc, Map<String, Serializable> options) throws DocumentException {
        service.doPostCreate(doc, options);
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel) throws ClientException {
        return service.getSaveOptions(docModel);
    }

    @Override
    public boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty, VersioningOption option,
            Map<String, Serializable> options) throws DocumentException {
        return service.isPreSaveDoingCheckOut(doc, isDirty, option, options);
    }

    @Override
    public VersioningOption doPreSave(Document doc, boolean isDirty, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) throws DocumentException {
        return service.doPreSave(doc, isDirty, option, checkinComment, options);
    }

    @Override
    public boolean isPostSaveDoingCheckIn(Document doc, VersioningOption option, Map<String, Serializable> options)
            throws DocumentException {
        return service.isPostSaveDoingCheckIn(doc, option, options);
    }

    @Override
    public Document doPostSave(Document doc, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) throws DocumentException {
        return service.doPostSave(doc, option, checkinComment, options);
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option, String checkinComment) throws DocumentException {
        return service.doCheckIn(doc, option, checkinComment);
    }

    @Override
    public void doCheckOut(Document doc) throws DocumentException {
        service.doCheckOut(doc);
    }
}
