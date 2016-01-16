package com.marklogic.entityservices;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.internal.path.json.JSONAssertion;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.JacksonHandle;

public class TestEntityTypes extends EntityServicesTestBase {

    private static DatabaseClient client;
    private static List<String> entityTypeUris = new ArrayList<String>();
    
    @BeforeClass
    public static void setupClass() throws IOException {
        TestSetup testSetup = TestSetup.getInstance();
        client = testSetup.getClient();
        JSONDocumentManager docMgr = client.newJSONDocumentManager();
        DocumentWriteSet writeSet = docMgr.newWriteSet();
        Collection<File> files = FileUtils.listFiles(new File("src/test/resources/json-entity-types"), 
                FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        for (File f : files) {
            writeSet.add(f.getPath(), new FileHandle(f));
            entityTypeUris.add(f.getPath());
        }
;       docMgr.write(writeSet);
    }
    
    @AfterClass
    public static void teardownClass() {
        // teardown.
    }
    
    
    public EvalResultIterator eval(String functionCall) {
        
        String entityServicesImport = 
                "import module namespace es = 'http://marklogic.com/entity-services' at '/MarkLogic/entity-services/entity-services.xqy';\n";

        ServerEvaluationCall call = 
                client.newServerEval().xquery(entityServicesImport + functionCall);
        EvalResultIterator results = call.eval();
        return results;
    }
    
    @Test
    public void testEntityTypeParseJSON() {
        for (String entityTypeUri : entityTypeUris) {
            EvalResultIterator results = eval("es:entity-type-from-node(fn:doc('"+ entityTypeUri  + "'))");
            for (EvalResult result : results) {
                JacksonHandle handle = new JacksonHandle();
                handle = result.get(handle);
                JsonNode jsonNode = handle.get();
                JsonNode infoNode = jsonNode.get("info");
                assertNotNull(infoNode);
                
                // each type must have a info
               assertThat(infoNode.get("title").asText(), Matchers.not(Matchers.isEmptyOrNullString()));
            }
        }
    }
}
