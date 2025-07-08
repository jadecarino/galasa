/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import dev.galasa.extensions.common.couchdb.pojos.PutPostResponse;
import dev.galasa.extensions.common.mocks.BaseHttpInteraction;
import dev.galasa.extensions.common.mocks.HttpInteraction;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.teststructure.TestStructure;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;
import dev.galasa.ras.couchdb.internal.mocks.MockLogFactory;
import dev.galasa.ras.couchdb.internal.pojos.TestStructureCouchdb;

public class CouchdbRasStoreTest {

    class CreateCouchdbDocumentInteraction extends BaseHttpInteraction {

        private String[] expectedRequestBodyParts;

        public CreateCouchdbDocumentInteraction(String expectedUri, int statusCode, PutPostResponse response, String... expectedRequestBodyParts) {
            super(expectedUri, statusCode);
            setResponsePayload(response);
            this.expectedRequestBodyParts = expectedRequestBodyParts;
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
            if (expectedRequestBodyParts.length > 0) {
                validatePostRequestBody((HttpPost) request);
            }
        }

        private void validatePostRequestBody(HttpPost postRequest) {
            try {
                String requestBody = EntityUtils.toString(postRequest.getEntity());
                assertThat(requestBody).contains(expectedRequestBodyParts);

            } catch (IOException ex) {
                fail("Failed to parse POST request body");
            }
        }
    }

    class UpdateCouchdbDocumentInteraction extends BaseHttpInteraction {

        private String[] expectedRequestBodyParts;

        public UpdateCouchdbDocumentInteraction(String expectedUri, int statusCode, PutPostResponse response, String... expectedRequestBodyParts) {
            super(expectedUri, statusCode);
            setResponsePayload(response);
            this.expectedRequestBodyParts = expectedRequestBodyParts;
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("PUT");
            if (expectedRequestBodyParts.length > 0) {
                validatePutRequestBody((HttpPut) request);
            }
        }

        private void validatePutRequestBody(HttpPut putRequest) {
            try {
                String requestBody = EntityUtils.toString(putRequest.getEntity());
                assertThat(requestBody).contains(expectedRequestBodyParts);

            } catch (IOException ex) {
                fail("Failed to parse PUT request body");
            }
        }
    }

    CouchdbTestFixtures fixtures = new CouchdbTestFixtures();    

    private TestStructureCouchdb createTestStructure(String runName, String status, String docId, String revision) {
        TestStructureCouchdb testStructure = new TestStructureCouchdb();

        testStructure._id = docId;
        testStructure._rev = revision;

        testStructure.setRunName(runName);
        testStructure.setStatus(status);

        return testStructure;
    }


    // Creating the Ras store causes the test structure in the couchdb 
    @Test
    public void testCanCreateCouchdbRasStoreOK() throws Exception {

        // See if we can create a store...
        fixtures.createCouchdbRasStore(null);
    }

    @Test
    public void testCanUpdateTestStructureOK() throws Exception {
        // Given...
        String runId = "cdb-run1";
        String docId = "run1";
        String revision = "my-revision";
        String runName = "BOB1";
        String status = "finished";
        TestStructure newTestStructure = createTestStructure(runName, status, docId, revision);

        PutPostResponse mockPutResponse = new PutPostResponse();
        mockPutResponse.id = docId;
        mockPutResponse.rev = revision;
        mockPutResponse.ok = true;

        String baseUri = "http://my.uri";
        MockLogFactory mockLogFactory = new MockLogFactory();
        List<HttpInteraction> interactions = List.of(
            new UpdateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.RUNS_DB + "/" + docId, HttpStatus.SC_CREATED, mockPutResponse, runName, status)
        );

        CouchdbRasStore rasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);

        // When...
        rasStore.updateTestStructure(runId, newTestStructure);

        // Then...
        // None of the interaction assertions should have failed.
    }

    @Test
    public void testUpdateTestStructureRetriesOnConflict() throws Exception {
        // Given...
        String runId = "cdb-run1";
        String docId = "run1";
        String revision = "my-revision";
        String runName = "BOB1";
        String status = "finished";
        TestStructure newTestStructure = createTestStructure(runName, status, docId, revision);

        PutPostResponse mockPutResponse = new PutPostResponse();
        mockPutResponse.id = docId;
        mockPutResponse.rev = revision;
        mockPutResponse.ok = true;

        String baseUri = "http://my.uri";
        MockLogFactory mockLogFactory = new MockLogFactory();
        List<HttpInteraction> interactions = List.of(
            new UpdateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.RUNS_DB + "/" + docId, HttpStatus.SC_CONFLICT, null, runName, status),
            new UpdateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.RUNS_DB + "/" + docId, HttpStatus.SC_CONFLICT, null, runName, status),
            new UpdateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.RUNS_DB + "/" + docId, HttpStatus.SC_CREATED, mockPutResponse, runName, status)
        );

        CouchdbRasStore rasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);

        // When...
        rasStore.updateTestStructure(runId, newTestStructure);

        // Then...
        // None of the interaction assertions should have failed.
    }

    @Test
    public void testUpdateTestStructureWithMissingRevisionThrowsCorrectError() throws Exception {
        // Given...
        String runId = "cdb-run1";
        String docId = "run1";
        String revision = null;
        String runName = "BOB1";
        String status = "finished";
        TestStructure newTestStructure = createTestStructure(runName, status, docId, revision);

        PutPostResponse mockPutResponse = new PutPostResponse();
        mockPutResponse.id = docId;
        mockPutResponse.rev = revision;
        mockPutResponse.ok = true;

        MockLogFactory mockLogFactory = new MockLogFactory();
        List<HttpInteraction> interactions = new ArrayList<>();

        CouchdbRasStore rasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);

        // When...
        ResultArchiveStoreException thrown = catchThrowableOfType(() -> {
            rasStore.updateTestStructure(runId, newTestStructure);
        }, ResultArchiveStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown).hasMessage("Failed to get run document revision");
    }

    @Test
    public void testWriteLogUpdatesRunLogLineCountUnder100NoFlushLogCache() throws Exception {
        // Given...

        // If the number of lines written to the run log in writeLog is under 100,
        // so the logCache is under 100 lines, the logCache is not flushed but the
        // lines just stored there, until it reaches 100.
        List<String> runLogLines = new ArrayList<String>();
        int desiredRunLogLineCount = 99;
        for (int i = 1; i <= desiredRunLogLineCount; i++) {
            runLogLines.add("This is run log line number " + i);
        }

        CouchdbRasStore rasStore = fixtures.createCouchdbRasStore(null);

        // When...
        rasStore.writeLog(runLogLines);

        // Then...
        assertThat(rasStore.retrieveRunLogLineCount()).isEqualTo(desiredRunLogLineCount);
    }

    @Test
    public void testWriteLogUpdatesRunLogLineCountUnder100NoFlushLogCache_AllLinesEndWithNewLineChars() throws Exception {
        // Given...
        String runLogLine = "1. This is a run log line with a return character\r" +
                            "2. and a run log line with a new line character\n" +
                            "3. and a run log line ending with both\r\n";
        List<String> runLogLines = new ArrayList<String>();
        runLogLines.add(runLogLine);

        CouchdbRasStore rasStore = fixtures.createCouchdbRasStore(null);

        // When...
        rasStore.writeLog(runLogLines);

        // Then...
        assertThat(rasStore.retrieveRunLogLineCount()).isEqualTo(3);
    }

    @Test
    public void testWriteLogUpdatesRunLogLineCountUnder100NoFlushLogCache_InBetweenLinesHaveNewLineChars() throws Exception {
        // Given...
        String runLogLine = "1. This is a run log line with a return character\r" +
                            "2. and a run log line with a new line character\n" +
                            "3. and a run log line with nothing";
        List<String> runLogLines = new ArrayList<String>();
        runLogLines.add(runLogLine);

        CouchdbRasStore rasStore = fixtures.createCouchdbRasStore(null);

        // When...
        rasStore.writeLog(runLogLines);

        // Then...
        assertThat(rasStore.retrieveRunLogLineCount()).isEqualTo(3);
    }

    @Test
    public void testWriteLogUpdatesRunLogLineCountOver100FlushesLogCache() throws Exception {
        // Given...

        // Once the number of lines written to the run log in writeLog exceeds 100,
        // the logCache is flushed (log is actually written to the CouchDB RAS store)
        List<String> runLogLines = new ArrayList<String>();
        int desiredRunLogLineCount = 101;
        for (int i = 1; i <= desiredRunLogLineCount; i++) {
            runLogLines.add("This is run log line number " + i);
        }

        String docId = "run1";
        String revision = "my-revision";

        PutPostResponse mockPutResponse = new PutPostResponse();
        mockPutResponse.id = docId;
        mockPutResponse.rev = revision;
        mockPutResponse.ok = true;

        String baseUri = "http://my.uri";
        MockLogFactory mockLogFactory = new MockLogFactory();
        List<HttpInteraction> interactions = List.of(
            // Create the run document
            new CreateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.RUNS_DB, HttpStatus.SC_CREATED, mockPutResponse),
            // Create the artifacts document
            new CreateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.ARTIFACTS_DB, HttpStatus.SC_CREATED, mockPutResponse),
            // Create the run log document
            new CreateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.LOG_DB, HttpStatus.SC_CREATED, mockPutResponse),
            // Update the run document
            new UpdateCouchdbDocumentInteraction(baseUri + "/" + CouchdbRasStore.RUNS_DB + "/" + docId, HttpStatus.SC_CREATED, mockPutResponse)
        );

        Map<String,String> inputProps = new HashMap<String,String>();

        CouchdbRasStore rasStore = fixtures.createCouchdbRasStore(inputProps, interactions, mockLogFactory);

        // When...
        rasStore.writeLog(runLogLines);

        // Then...
        assertThat(rasStore.retrieveRunLogLineCount()).isEqualTo(desiredRunLogLineCount);
    }

}
