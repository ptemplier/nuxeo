package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;

import java.util.Map;

/**
 *
 * Interface of the pluggable factory used to create a PublishedDocument in a
 * give PublicationTree
 *
 * @author tiry
 *
 */
public interface PublishedDocumentFactory {

    String getName();

    PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode) throws ClientException;

    PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException;

    void init(CoreSession coreSession, ValidatorsRule validatorsRule, Map<String, String> parameters)
            throws ClientException;

    void init(CoreSession coreSession, Map<String, String> parameters)
            throws ClientException;

    DocumentModel snapshotDocumentBeforePublish(DocumentModel doc)
            throws ClientException;

    PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException;

    /**
     * Computes the list of publishing validators given the document model of
     * the document just published.
     *
     * The string can be prefixed with 'group:' or 'user:'. If there is no
     * prefix (no : in the string) it is assumed to be a user.
     *
     * @param dm a Nuxeo Core document model. (the document that just has been
     *            published)
     * @return a list of principal names.
     * @throws org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException
     */
    String[] getValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException;

    /**
     * Returns the registered section validators rule.
     *
     * @return a validators rule
     */
    ValidatorsRule getValidatorsRule() throws PublishingValidatorException;

    /**
     * A validator (the current user) approves the publication.
     *
     * @param publishedDocument the current published document that will be
     *            approved
     * @throws PublishingException
     */
    void validatorPublishDocument(PublishedDocument publishedDocument)
            throws ClientException;

    /**
     * A validator (the current user) rejects the publication.
     *
     * @param publishedDocument the currently published document that will be
     *            rejected
     * @param comment
     * @throws PublishingException
     */
    void validatorRejectPublication(PublishedDocument publishedDocument,
            String comment) throws ClientException;

    boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException;

    boolean canManagePublishing(PublishedDocument publishedDocument) throws ClientException;

}