package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

import java.util.Map;

/**
 * 
 * {@link PublishedDocumentFactory} implementation that creates
 * {@link DocumentModel} instead of simple proxies
 * 
 * @author tiry
 * 
 */
public class SimpleExternalDocumentModelFactory extends
        AbstractBasePublishedDocumentFactory implements
        PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        String name = IdUtils.generateId(doc.getTitle());
        doc.setPathInfo(targetNode.getPath(), "remote_doc_" + name);
        // We don't want to erase the current version
        final ScopedMap ctxData = doc.getContextData();
        ctxData.putScopedValue(ScopeType.REQUEST,
                VersioningActions.SKIP_VERSIONING, true);
        doc = coreSession.createDocument(doc);
        coreSession.save();

        return new ExternalCorePublishedDocument(doc);
    }

    @Override
    protected boolean needToVersionDocument(DocumentModel doc) {
        if (!doc.getRepositoryName().equalsIgnoreCase(
                coreSession.getRepositoryName())) {
            return false;
        } else {
            return super.needToVersionDocument(doc);
        }
    }

    /*
     * public DocumentModel unwrapPublishedDocument(PublishedDocument pubDoc)
     * throws ClientException { if (pubDoc instanceof
     * SimpleCorePublishedDocument) { SimpleCorePublishedDocument pubProxy =
     * (SimpleCorePublishedDocument) pubDoc; return pubProxy.getProxy(); } if
     * (pubDoc instanceof ExternalCorePublishedDocument) {
     * ExternalCorePublishedDocument pubExt = (ExternalCorePublishedDocument)
     * pubDoc; return pubExt.getDocumentModel(); } throw new ClientException(
     * "factory can not unwrap this PublishedDocument"); }
     */

    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        if (doc.isProxy())
            return new SimpleCorePublishedDocument(doc);
        return new ExternalCorePublishedDocument(doc);
    }
}