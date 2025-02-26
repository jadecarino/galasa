package dev.galasa.docker;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.client.methods.CloseableHttpResponse;

import com.google.gson.JsonObject;

import dev.galasa.http.ContentType;
import dev.galasa.http.HttpClientException;
import dev.galasa.http.HttpClientResponse;
import dev.galasa.http.IHttpClient;

public class MockHttpClient implements IHttpClient {

    private URI host;

    @Override
    public void setURI(URI host) {
        this.host = host;
    }

    @Override
    public HttpClientResponse<JsonObject> getJson(String url) throws HttpClientException {
        // // to do...
        return null;
    }

    @Override
    public HttpClientResponse<Object> getJaxb(String url, Class<?>... responseTypes) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getJaxb'");
    }

    @Override
    public HttpClientResponse<Object> putJaxb(String url, Object jaxbObject, Class<?>... responseTypes)
            throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'putJaxb'");
    }

    @Override
    public HttpClientResponse<Object> postJaxb(String url, Object jaxbObject, Class<?>... responseTypes)
            throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'postJaxb'");
    }

    @Override
    public HttpClientResponse<Object> deleteJaxb(String url, Class<?>... responseTypes) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteJaxb'");
    }

    @Override
    public HttpClientResponse<String> putXML(String url, String xml) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'putXML'");
    }

    @Override
    public HttpClientResponse<String> postSOAP(String url, String xml) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'postSOAP'");
    }

    @Override
    public HttpClientResponse<String> putSOAP(String url, String xml) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'putSOAP'");
    }

    @Override
    public HttpClientResponse<String> postXML(String url, String xml) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'postXML'");
    }

    @Override
    public HttpClientResponse<JsonObject> postJson(String url, JsonObject json) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'postJson'");
    }

    @Override
    public HttpClientResponse<JsonObject> patchJson(String url, JsonObject json) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'patchJson'");
    }

    @Override
    public HttpClientResponse<JsonObject> putJson(String url, JsonObject json) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'putJson'");
    }

    @Override
    public HttpClientResponse<JsonObject> deleteJson(String url) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteJson'");
    }

    @Override
    public HttpClientResponse<JsonObject> deleteJson(String url, JsonObject json) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteJson'");
    }

    @Override
    public HttpClientResponse<String> getText(String url) throws HttpClientException {
        
        throw new UnsupportedOperationException("Unimplemented method 'getText'");
    }

    @Override
    public HttpClientResponse<String> postText(String url, String text) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'postText'");
    }

    @Override
    public HttpClientResponse<String> putText(String url, String text) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'putText'");
    }

    @Override
    public HttpClientResponse<String> deleteText(String url) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteText'");
    }

    @Override
    public HttpClientResponse<byte[]> putBinary(String url, byte[] binary) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'putBinary'");
    }

    @Override
    public HttpClientResponse<byte[]> getBinary(String url, byte[] binary) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getBinary'");
    }

    @Override
    public HttpClientResponse<byte[]> postBinary(String url, byte[] binary) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'postBinary'");
    }

    @Override
    public HttpClientResponse<byte[]> deleteBinary(String url, byte[] binary) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteBinary'");
    }

    @Override
    public CloseableHttpResponse getFile(String path) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getFile'");
    }

    @Override
    public CloseableHttpResponse getFile(String path, ContentType... acceptTypes) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'getFile'");
    }

    @Override
    public void putFile(String path, InputStream file) {
        throw new UnsupportedOperationException("Unimplemented method 'putFile'");
    }

    @Override
    public HttpClientResponse<String> head(String url) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'head'");
    }

    @Override
    public Object post(String path, Map<String, String> queryParams, ContentType contentType, Object data,
            ContentType[] acceptTypes, Class<?>[] jaxbClasses, boolean retry) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'post'");
    }

    @Override
    public Object postForm(String path, Map<String, String> queryParams, HashMap<String, String> fields,
            ContentType[] acceptTypes, Class<?>[] jaxbClasses, boolean retry) throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'postForm'");
    }

    @Override
    public SSLContext getSSLContext() {
        throw new UnsupportedOperationException("Unimplemented method 'getSSLContext'");
    }

    @Override
    public String getUsername() {
        throw new UnsupportedOperationException("Unimplemented method 'getUsername'");
    }

    @Override
    public String getUsername(URI scope) {
        throw new UnsupportedOperationException("Unimplemented method 'getUsername'");
    }

    @Override
    public void addCommonHeader(String name, String value) {
        throw new UnsupportedOperationException("Unimplemented method 'addCommonHeader'");
    }

    @Override
    public void clearCommonHeaders() {
        throw new UnsupportedOperationException("Unimplemented method 'clearCommonHeaders'");
    }

    @Override
    public void addOkResponseCode(int responseCode) {
        throw new UnsupportedOperationException("Unimplemented method 'addOkResponseCode'");
    }

    @Override
    public IHttpClient build() {
        throw new UnsupportedOperationException("Unimplemented method 'build'");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    public IHttpClient setAuthorisation(String username, String password) {
        throw new UnsupportedOperationException("Unimplemented method 'setAuthorisation'");
    }

    @Override
    public IHttpClient setAuthorisation(String username, String password, URI scope) {
        throw new UnsupportedOperationException("Unimplemented method 'setAuthorisation'");
    }

    @Override
    public IHttpClient setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        throw new UnsupportedOperationException("Unimplemented method 'setHostnameVerifier'");
    }

    @Override
    public IHttpClient setNoopHostnameVerifier() {
        throw new UnsupportedOperationException("Unimplemented method 'setNoopHostnameVerifier'");
    }

    @Override
    public IHttpClient setSSLContext(SSLContext sslContext) {
        throw new UnsupportedOperationException("Unimplemented method 'setSSLContext'");
    }

    @Override
    public IHttpClient setTrustingSSLContext() throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'setTrustingSSLContext'");
    }

    @Override
    public IHttpClient setupClientAuth(KeyStore clientKeyStore, KeyStore serverKeyStore, String alias, String password)
            throws HttpClientException {
        throw new UnsupportedOperationException("Unimplemented method 'setupClientAuth'");
    }

}
