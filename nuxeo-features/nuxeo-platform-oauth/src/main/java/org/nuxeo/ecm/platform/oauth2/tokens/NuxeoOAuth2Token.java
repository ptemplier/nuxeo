/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.tokens;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.auth.oauth2.StoredCredential;
import org.apache.commons.lang.RandomStringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class NuxeoOAuth2Token {

    public static final String SCHEMA = "oauth2Token";

    public static final String KEY_SERVICE_LOGIN = "serviceLogin";

    public static final String KEY_NUXEO_LOGIN = "nuxeoLogin";

    protected Long id;

    protected String serviceName;

    protected String nuxeoLogin;

    protected String accessToken;

    protected String clientId;

    protected Calendar creationDate;

    private String refreshToken;

    private Long expirationTimeMilliseconds;

    private boolean isShared;

    protected String serviceLogin;

    public NuxeoOAuth2Token(long expirationTimeMilliseconds, String clientId) {
        this("", "", expirationTimeMilliseconds);
        this.clientId = clientId;
        refresh();
    }

    public NuxeoOAuth2Token(String accessToken, String refreshToken, Long expirationTimeMilliseconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTimeMilliseconds = expirationTimeMilliseconds;
        this.creationDate = Calendar.getInstance();
        this.isShared = false;
    }

    public NuxeoOAuth2Token(StoredCredential credential) {
        this(credential.getAccessToken(), credential.getRefreshToken(), credential.getExpirationTimeMilliseconds());
    }

    public NuxeoOAuth2Token(DocumentModel entry) throws ClientException {
        this.id = (Long) entry.getProperty(SCHEMA, "id");
        this.accessToken = (String) entry.getProperty(SCHEMA, "accessToken");
        this.refreshToken = (String) entry.getProperty(SCHEMA, "refreshToken");
        this.expirationTimeMilliseconds = (Long) entry.getProperty(SCHEMA, "expirationTimeMilliseconds");
        this.serviceName = (String) entry.getProperty(SCHEMA, "serviceName");
        this.nuxeoLogin = (String) entry.getProperty(SCHEMA, "nuxeoLogin");
        this.clientId = (String) entry.getProperty(SCHEMA, "clientId");
        this.creationDate = (Calendar) entry.getProperty(SCHEMA, "creationDate");
        this.isShared = (Boolean) entry.getProperty(SCHEMA, "isShared");
        this.serviceLogin = (String) entry.getProperty(SCHEMA, "serviceLogin");
    }

    public static StoredCredential asCredential(DocumentModel entry) {
        StoredCredential credential = new StoredCredential();
        String accessToken = (String) entry.getProperty(SCHEMA, "accessToken");
        String refreshToken = (String) entry.getProperty(SCHEMA, "refreshToken");
        Long expirationTimeMilliseconds = (Long) entry.getProperty(SCHEMA, "expirationTimeMilliseconds");
        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);
        credential.setExpirationTimeMilliseconds(expirationTimeMilliseconds);
        return credential;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("serviceName", serviceName);
        map.put("nuxeoLogin", nuxeoLogin);
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        map.put("expirationTimeMilliseconds", expirationTimeMilliseconds);
        map.put("clientId", clientId);
        map.put("creationDate", creationDate);
        map.put("isShared", isShared);
        map.put("serviceLogin", serviceLogin);
        return map;
    }

    public Map<String, Object> toJsonObject() {
        Map<String, Object> m = new HashMap<>();
        m.put("access_token", accessToken);
        m.put("refresh_token", refreshToken);
        m.put("token_type", "bearer");
        m.put("expires_in",
                Math.floor((creationDate.getTimeInMillis() + expirationTimeMilliseconds - new Date().getTime()) / 1000));
        return m;
    }

    public void updateEntry(DocumentModel entry) throws ClientException {
        entry.setProperty(SCHEMA, "serviceName", this.serviceName);
        entry.setProperty(SCHEMA, "nuxeoLogin", this.nuxeoLogin);
        entry.setProperty(SCHEMA, "accessToken", this.accessToken);
        entry.setProperty(SCHEMA, "refreshToken", this.refreshToken);
        entry.setProperty(SCHEMA, "expirationTimeMilliseconds", this.expirationTimeMilliseconds);
        entry.setProperty(SCHEMA, "clientId", this.clientId);
        entry.setProperty(SCHEMA, "isShared", this.isShared);
        entry.setProperty(SCHEMA, "serviceLogin", this.serviceLogin);
    }

    public void refresh() {
        accessToken = RandomStringUtils.random(32, true, true);
        refreshToken = RandomStringUtils.random(64, true, true);
        creationDate = Calendar.getInstance();
    }

    public boolean isExpired() {
        return creationDate != null
                && creationDate.getTimeInMillis() + expirationTimeMilliseconds < Calendar.getInstance().getTimeInMillis();
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setNuxeoLogin(String userId) {
        this.nuxeoLogin = userId;
    }

    public String getNuxeoLogin() {
        return nuxeoLogin;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpirationTimeMilliseconds() {
        return expirationTimeMilliseconds;
    }

    public void setExpirationTimeMilliseconds(Long expirationTimeMilliseconds) {
        this.expirationTimeMilliseconds = expirationTimeMilliseconds;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setIsShared(boolean isShared) {
        this.isShared = isShared;
    }

    public String getServiceLogin() {
        return serviceLogin;
    }

    public void setServiceLogin(String serviceLogin) {
        this.serviceLogin = serviceLogin;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }
}
