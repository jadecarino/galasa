/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.docker.internal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


import org.apache.http.HttpStatus;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonObject;

import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.docker.DockerManagerException;
import dev.galasa.docker.MockCPSStore;
import dev.galasa.docker.MockCREDSStore;
import dev.galasa.docker.MockDSSStore;
import dev.galasa.docker.MockDockerManagerImpl;
import dev.galasa.docker.MockFramework;
import dev.galasa.docker.MockHttpClient;
import dev.galasa.docker.MockHttpManager;
import dev.galasa.framework.spi.creds.CredentialsException;
import dev.galasa.framework.spi.creds.ICredentialsService;
import dev.galasa.http.HttpClientException;
import dev.galasa.http.HttpClientResponse;
public class TestDockerRegistryImpl {

	private final String NAMESPACE = "docker";

    private MockDockerManagerImpl dockerManagerMock;

    private MockHttpClient clientMock;

    private HttpClientResponse<JsonObject> responseMock;

    private HttpClientResponse<JsonObject> bearerResponseMock; 

    private ICredentialsUsernamePassword credentialsMock;

    private ICredentialsService credentialServiceMock;

	private DockerRegistryImpl createRegistryImplObject() throws DockerManagerException, MalformedURLException, CredentialsException {
		MockCPSStore mockCps = new MockCPSStore(NAMESPACE, new HashMap<>());
		MockDSSStore mockDss = new MockDSSStore(new HashMap<>());
		MockCREDSStore mockCreds = new MockCREDSStore();
		MockFramework frameworkMock = new MockFramework(mockCps, mockDss, mockCreds);

		this.clientMock = new MockHttpClient();
        MockHttpManager httpManagerMock = new MockHttpManager(this.clientMock);
        this.dockerManagerMock = new MockDockerManagerImpl(httpManagerMock, mockCps);

    	DockerRegistryImpl dockerRegistry = new DockerRegistryImpl(frameworkMock, dockerManagerMock, "DOCKERHUB");
    	return dockerRegistry;
    }

    private DockerImageImpl createImageImplObject() {
    	DockerImageImpl dockerImageImpl = new DockerImageImpl(null, dockerManagerMock, null, "bob:latest");
    	return dockerImageImpl;
    }

    private void retrieveRealm(DockerRegistryImpl dockerRegistry, DockerImageImpl dockerImageImpl) throws HttpClientException, DockerManagerException {
    	// Setting registryRealmURL for our test registry
    	String path = "/v2/bob/manifests/latest";

    	// when(clientMock.getJson(path)).thenReturn(responseMock);
		HttpClientResponse<JsonObject> responseMock = this.clientMock.getJson(path);
    	when(responseMock.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
		
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put("WWW-Authenticate", "Bearer realm=\"http://x.x.x.x/service/token\"");
    	when(responseMock.getheaders()).thenReturn(headers);
    	dockerRegistry.retrieveRealm(dockerImageImpl);
    }
    
    // @Test
    public void testRetrieveBearerTokenAuthorised() throws DockerManagerException, MalformedURLException, CredentialsException, HttpClientException, URISyntaxException {
    	// Given...

		// Creating object
    	DockerRegistryImpl dockerRegistry = createRegistryImplObject();

		// Create Docker image object used for realm retrieval 
    	DockerImageImpl dockerImageImpl = createImageImplObject();
    	
    	// Setting registryRealmURL for our test object
    	retrieveRealm(dockerRegistry, dockerImageImpl);
    	
		// When...

    	// Attempting to use retrieve bearer token method an authorised response 
    	when(clientMock.getJson("")).thenReturn(bearerResponseMock);
    	when(bearerResponseMock.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    	JsonObject jsonAuthorisation = new JsonObject();
    	jsonAuthorisation.addProperty("token", "tokenValue");
    	when(bearerResponseMock.getContent()).thenReturn(jsonAuthorisation);
    	String actualToken = dockerRegistry.retrieveBearerToken();    	
    	
		// Then...
    	assertThat(actualToken).as("Checking bearer token value").isEqualTo("tokenValue");
    	verify(clientMock, times(1)).addCommonHeader("Authorization", "Bearer tokenValue");
    }
    
    // @Test
    public void testRetrieveBearerTokenUnauthorised() throws DockerManagerException, MalformedURLException, CredentialsException, HttpClientException, URISyntaxException, NoSuchFieldException, SecurityException {
    	// Given...
		
		// Creating object
    	DockerRegistryImpl dockerRegistry = createRegistryImplObject();

		// Create Docker image object used for realm retrieval 
    	DockerImageImpl dockerImageImpl = createImageImplObject();
    	
    	// Setting registryRealmURL
    	retrieveRealm(dockerRegistry, dockerImageImpl);
    	
		// When...

    	// Attempting to use retrieve bearer token method with an unauthorised response 
    	when(clientMock.getJson("")).thenReturn(bearerResponseMock);
    	when(bearerResponseMock.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put("WWW-Authenticate", "Basic realm");
    	when(bearerResponseMock.getheaders()).thenReturn(headers);
    	
    	// Mocking the user credentials returned from the credential service
    	when(credentialServiceMock.getCredentials(Mockito.anyString())).thenReturn(credentialsMock);
    	when(credentialsMock.getUsername()).thenReturn("testUsername");
    	when(credentialsMock.getPassword()).thenReturn("testPassword");
    	String user = "testUsername";
    	String password = "testPassword"; //unit test mock password //pragma: allowlist secret
    	when(clientMock.setAuthorisation(user, password)).thenReturn(clientMock);
    	when(clientMock.build()).thenReturn(clientMock);
    	// Base64 encoding credentials to replicate private encoding method (generateDockerRegistryAuthStructure)
    	JsonObject creds = new JsonObject();
		creds.addProperty("username", user);
		creds.addProperty("password", password);
		String token = Base64.getEncoder().encodeToString(creds.toString().getBytes());
    	String actualToken = dockerRegistry.retrieveBearerToken();    	
    	
    	// Then...
    	assertThat(actualToken).as("Checking bearer token value").isEqualTo(token);
    }

}
