package org.nuxeo.ecm.platform.publisher.remoting.restHandler;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.*;
import java.util.Map;

//@Consumes("nuxeo/remotepub")
@Produces("nuxeo/remotepub; charset=UTF-8")
public class RestPublishingHandler extends DefaultObject {

    // Yeurk !!
    static {
        WebEngine we = Framework.getLocalService(WebEngine.class);

        we.getRegistry().addMessageBodyWriter(new RemotePubMessageWriter());
        we.getRegistry().addMessageBodyReader(new RemotePubMessageReader());
    }

    protected RemotePublicationTreeManager getPublisher() {
        return Framework.getLocalService(RemotePublicationTreeManager.class);
    }

    // List<PublishedDocument> getChildrenDocuments(PublicationNode node) throws
    // ClientException;
    @POST
    @Path("getChildrenDocuments")
    public RemotePubResult getChildrenDocuments(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getChildrenDocuments(
                param.getAsNode()));
    }

    // List<PublicationNode> getChildrenNodes(PublicationNode node) throws
    // ClientException;
    @POST
    @Path("getChildrenNodes")
    public RemotePubResult getChildrenNodes(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getChildrenNodes(
                param.getAsNode()));
    }

    // PublicationNode getParent(PublicationNode node);
    @POST
    @Path("getParent")
    public RemotePubResult getParent(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getParent(param.getAsNode()));
    }

    // PublicationNode getNodeByPath(String sid, String path) throws
    // ClientException;
    @POST
    @Path("getNodeByPath")
    public RemotePubResult getNodeByPath(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getNodeByPath(
                (String) param.getParams().get(0),
                (String) param.getParams().get(1)));
    }

    // List<PublishedDocument> getExistingPublishedDocument(String sid,
    // DocumentLocation docLoc) throws ClientException;
    @POST
    @Path("getExistingPublishedDocument")
    public RemotePubResult getExistingPublishedDocument(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getExistingPublishedDocument(
                (String) param.getParams().get(0),
                (DocumentLocation) param.getParams().get(1)));
    }

    // List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node)
    // throws ClientException;
    @POST
    @Path("getPublishedDocumentInNode")
    public RemotePubResult getPublishedDocumentInNode(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getPublishedDocumentInNode(
                param.getAsNode()));
    }

    // PublishedDocument publish(DocumentModel doc, PublicationNode targetNode,
    // Map<String,String> params) throws ClientException;
    @POST
    @Path("publish")
    public RemotePubResult publish(RemotePubParam param) throws ClientException {
        RemotePubResult result;
        if (param.getParams().size() == 2) {
            result = new RemotePubResult(getPublisher().publish(
                    (DocumentModel) param.getParams().get(0),
                    (PublicationNode) param.getParams().get(1)));
        } else {
            result = new RemotePubResult(getPublisher().publish(
                    (DocumentModel) param.getParams().get(0),
                    (PublicationNode) param.getParams().get(1),
                    (Map<String, String>) param.getParams().get(2)));
        }
        return result;
    }

    // void unpublish(DocumentModel doc, PublicationNode targetNode) throws
    // ClientException;
    @POST
    @Path("unpublish")
    public void unpublish(RemotePubParam param) throws ClientException {
        Object object = param.getParams().get(1);
        if (object instanceof PublicationNode) {
            getPublisher().unpublish((DocumentModel) param.getParams().get(0),
                    (PublicationNode) object);
        } else if (object instanceof PublishedDocument) {
            getPublisher().unpublish((String) param.getParams().get(0),
                    (PublishedDocument) object);
        }
    }

    // Map<String,String> initRemoteSession(String treeConfigName, Map<String,
    // String> params) throws Exception;
    @POST
    @Path("initRemoteSession")
    public RemotePubResult initRemoteSession(RemotePubParam param)
            throws Exception {
        return new RemotePubResult(getPublisher().initRemoteSession(
                (String) param.getParams().get(0),
                (Map<String, String>) param.getParams().get(1)));
    }

    // void release(String sid);
    @POST
    @Path("release")
    public void release(RemotePubParam param) {
        getPublisher().release((String) param.getParams().get(0));
    }

    // void release(String sid);
    @GET
    @Path("release/{sid}")
    public void release(@PathParam("sid") String sid) {
        getPublisher().release(sid);
    }

    @GET
    @Path("ping")
    public String ping() {
        return "pong";
    }

    @GET
    @Path("superPing")
    public RemotePubResult superPing() {
        return new RemotePubResult("pong");
    }

}