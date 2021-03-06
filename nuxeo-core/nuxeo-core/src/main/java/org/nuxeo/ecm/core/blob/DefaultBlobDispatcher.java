/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Document.BlobAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Default blob dispatcher, that uses the repository name as the blob provider.
 * <p>
 * Alternatively, it can be configured through properties to dispatch to a blob provider based on document properties
 * instead of the repository name.
 * <p>
 * The property name is a list of comma-separated clauses, with each clause consisting of a property, an operator and a
 * value. The property can be a {@link Document} xpath, {@code ecm:repositoryName}, or, to match the current blob being
 * dispatched, {@code blob:name}, {@code blob:mime-type}, {@code blob:encoding}, {@code blob:digest} or
 * {@code blob:length}. Comma-separated clauses are ANDed together. The special name {@code default} defines the default
 * provider, and must be present.
 * <p>
 * Available operators between property and value are =, !=, &lt, and >.
 * <p>
 * For example, to dispatch to the "first" provider if dc:format is "video", to the "second" provider if the blob's MIME
 * type is "video/mp4", to the "third" provider if the lifecycle state is "approved" and the document is in the default
 * repository, and otherwise to the "fourth" provider:
 *
 * <pre>
 * &lt;property name="dc:format=video">first&lt;/property>
 * &lt;property name="blob:mime-type=video/mp4">second&lt;/property>
 * &lt;property name="ecm:repositoryName=default,ecm:lifeCycleState=approved">third2&lt;/property>
 * &lt;property name="default">fourth&lt;/property>
 * </pre>
 *
 * @since 7.3
 */
public class DefaultBlobDispatcher implements BlobDispatcher {

    private static final Log log = LogFactory.getLog(DefaultBlobDispatcher.class);

    protected static final String NAME_DEFAULT = "default";

    protected static final Pattern NAME_PATTERN = Pattern.compile("(.*)(=|!=|<|>)(.*)");

    /** Pseudo-property for the repository name. */
    protected static final String REPOSITORY_NAME = "ecm:repositoryName";

    protected static final String BLOB_PREFIX = "blob:";

    protected static final String BLOB_NAME = "name";

    protected static final String BLOB_MIME_TYPE = "mime-type";

    protected static final String BLOB_ENCODING = "encoding";

    protected static final String BLOB_DIGEST = "digest";

    protected static final String BLOB_LENGTH = "length";

    protected enum Op {
        EQ, NEQ, LT, GT;
    }

    protected static class Clause {
        public final String xpath;

        public final Op op;

        public final Object value;

        public Clause(String xpath, Op op, Object value) {
            this.xpath = xpath;
            this.op = op;
            this.value = value;
        }
    }

    protected static class Rule {
        public final List<Clause> clauses;

        public final String providerId;

        public Rule(List<Clause> clauses, String providerId) {
            this.clauses = clauses;
            this.providerId = providerId;
        }
    }

    // default to true when initialize is not called (default instance)
    protected boolean useRepositoryName = true;

    protected List<Rule> rules;

    protected Set<String> rulesXPaths;

    protected Set<String> providerIds;

    protected String defaultProviderId;

    @Override
    public void initialize(Map<String, String> properties) {
        providerIds = new HashSet<>();
        rulesXPaths = new HashSet<>();
        rules = new ArrayList<>();
        for (Entry<String, String> en : properties.entrySet()) {
            String clausesString = en.getKey();
            String providerId = en.getValue();
            providerIds.add(providerId);
            if (clausesString.equals(NAME_DEFAULT)) {
                defaultProviderId = providerId;
            } else {
                List<Clause> clauses = new ArrayList<Clause>(2);
                for (String name : clausesString.split(",")) {
                    Matcher m = NAME_PATTERN.matcher(name);
                    if (m.matches()) {
                        String xpath = m.group(1);
                        String ops = m.group(2);
                        Object value = m.group(3);
                        Op op;
                        switch (ops) {
                        case "=":
                            op = Op.EQ;
                            break;
                        case "!=":
                            op = Op.NEQ;
                            break;
                        case "<":
                            op = Op.LT;
                            value = Long.valueOf((String) value);
                            break;
                        case ">":
                            op = Op.GT;
                            value = Long.valueOf((String) value);
                            break;
                        default:
                            log.error("Invalid dispatcher configuration operator: " + ops);
                            continue;
                        }
                        clauses.add(new Clause(xpath, op, value));
                        rulesXPaths.add(xpath);
                    } else {
                        log.error("Invalid dispatcher configuration property name: " + name);
                    }
                    rules.add(new Rule(clauses, providerId));
                }
            }
        }
        useRepositoryName = providerIds.isEmpty();
        if (!useRepositoryName && defaultProviderId == null) {
            log.error("Invalid dispatcher configuration, missing default, configuration will be ignored");
            useRepositoryName = true;
        }
    }

    @Override
    public Collection<String> getBlobProviderIds() {
        if (useRepositoryName) {
            return Framework.getService(RepositoryManager.class).getRepositoryNames();
        }
        return providerIds;
    }

    protected String getProviderId(Document doc, Blob blob) throws DocumentException {
        if (useRepositoryName) {
            return doc.getRepositoryName();
        }
        for (Rule rule : rules) {
            boolean allClausesMatch = true;
            for (Clause clause : rule.clauses) {
                String xpath = clause.xpath;
                Object value;
                if (xpath.equals(REPOSITORY_NAME)) {
                    value = doc.getRepositoryName();
                } else if (xpath.startsWith(BLOB_PREFIX)) {
                    switch (xpath.substring(BLOB_PREFIX.length())) {
                    case BLOB_NAME:
                        value = blob.getFilename();
                        break;
                    case BLOB_MIME_TYPE:
                        value = blob.getMimeType();
                        break;
                    case BLOB_ENCODING:
                        value = blob.getEncoding();
                        break;
                    case BLOB_DIGEST:
                        value = blob.getDigest();
                        break;
                    case BLOB_LENGTH:
                        value = Long.valueOf(blob.getLength());
                        break;
                    default:
                        log.error("Invalid dispatcher configuration property name: " + xpath);
                        continue;
                    }
                } else {
                    try {
                        value = doc.getValue(xpath);
                    } catch (PropertyNotFoundException e) {
                        try {
                            value = doc.getPropertyValue(xpath);
                        } catch (IllegalArgumentException e2) {
                            continue;
                        }
                    }
                }
                boolean match;
                switch (clause.op) {
                case EQ:
                    match = String.valueOf(value).equals(clause.value);
                    break;
                case NEQ:
                    match = !String.valueOf(value).equals(clause.value);
                    break;
                case LT:
                    if (value == null) {
                        value = Long.valueOf(0);
                    }
                    match = ((Long) value).compareTo((Long) clause.value) < 0;
                    break;
                case GT:
                    if (value == null) {
                        value = Long.valueOf(0);
                    }
                    match = ((Long) value).compareTo((Long) clause.value) > 0;
                    break;
                default:
                    throw new AssertionError("notreached");
                }
                allClausesMatch = allClausesMatch && match;
                if (!allClausesMatch) {
                    break;
                }
            }
            if (allClausesMatch) {
                return rule.providerId;
            }
        }
        return defaultProviderId;
    }

    @Override
    public String getBlobProvider(String repositoryName) {
        if (useRepositoryName) {
            return repositoryName;
        }
        throw new NuxeoException("All blob keys should be prefixed in repository: " + repositoryName);
    }

    @Override
    public BlobDispatch getBlobProvider(Document doc, Blob blob) {
        if (useRepositoryName) {
            String providerId = doc.getRepositoryName();
            return new BlobDispatch(providerId, false);
        }
        try {
            String providerId = getProviderId(doc, blob);
            return new BlobDispatch(providerId, true);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notifyChanges(Document doc, Set<String> xpaths) {
        if (useRepositoryName) {
            return;
        }
        for (String xpath : rulesXPaths) {
            if (xpaths.contains(xpath)) {
                try {
                    doc.visitBlobs(accessor -> checkBlob(doc, accessor));
                } catch (DocumentException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
    }

    protected void checkBlob(Document doc, BlobAccessor accessor) {
        Blob blob = accessor.getBlob();
        if (!(blob instanceof ManagedBlob)) {
            return;
        }
        // compare current provider with expected
        String expectedProviderId;
        try {
            expectedProviderId = getProviderId(doc, blob);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        if (((ManagedBlob) blob).getProviderId().equals(expectedProviderId)) {
            return;
        }
        // re-write blob
        // TODO add APIs so that blob providers can copy blobs efficiently from each other
        Blob newBlob;
        try (InputStream in = blob.getStream()) {
            newBlob = Blobs.createBlob(in, blob.getMimeType(), blob.getEncoding());
            newBlob.setFilename(blob.getFilename());
            newBlob.setDigest(blob.getDigest());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        accessor.setBlob(newBlob);
    }

}
