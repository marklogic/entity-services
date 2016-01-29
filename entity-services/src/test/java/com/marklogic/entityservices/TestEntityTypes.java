package com.marklogic.entityservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.marker.AbstractReadHandle;

public class TestEntityTypes extends EntityServicesTestBase {

    private static DatabaseClient client;
    private static HashMap<File, String> entityTypeUris = new HashMap<File, String>();
    
    @BeforeClass
    public static void setupClass() throws IOException {
        TestSetup testSetup = TestSetup.getInstance();
        client = testSetup.getClient();
        JSONDocumentManager docMgr = client.newJSONDocumentManager();
        DocumentWriteSet writeSet = docMgr.newWriteSet();
        Collection<File> files = FileUtils.listFiles(new File("src/test/resources/json-entity-types"), 
                FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        for (File f : files) {
        	if (f.getName().startsWith(".")) { continue; };
        	if (!f.getName().endsWith(".json")) { continue; };
        	log.debug("Loading " + f.getPath());
        	//docMgr.write(f.getPath(), new FileHandle(f));
            writeSet.add(f.getPath(), new FileHandle(f));
            entityTypeUris.put(f, f.getPath());
        }
        docMgr.write(writeSet);
    }
    
    @AfterClass
    public static void teardownClass() {
        // teardown.
    }
    
    
    public EvalResultIterator eval(String functionCall) throws TestEvalException {
        
        String entityServicesImport = 
                "import module namespace es = 'http://marklogic.com/entity-services' at '/MarkLogic/entity-services/entity-services.xqy';\n";

        ServerEvaluationCall call = 
                client.newServerEval().xquery(entityServicesImport + functionCall);
        EvalResultIterator results = null;
        try {
        	results = call.eval();
        } catch (FailedRequestException e) {
        	throw new TestEvalException(e);
        }
        return results;
    }
    
    protected <T extends AbstractReadHandle> T evalOneResult(String functionCall, T handle) throws TestEvalException {
    	EvalResultIterator results =  eval(functionCall);
    	EvalResult result = null;
    	if (results.hasNext()) {
    		result = results.next();
    	}
    	//results.close();
    	return result.get(handle);
    }
   
    private void checkRoundTrip(String message, JsonNode original, JsonNode actual) {
    	assertEquals(message, original, actual);
    }
    
    @Test
    /*
     * For each entity type in the test directory, verify that
     * it parses and that it matches the entity type parsed by
     * the server.
     */
    public void testEntityTypeParseJSON() throws JsonParseException, JsonMappingException, IOException, TestEvalException {
        for (File entityTypeFile : entityTypeUris.keySet()) {
        	String entityTypeUri = entityTypeUris.get(entityTypeFile);
        	ObjectMapper mapper = new ObjectMapper();
        	JsonNode original = mapper.readValue(entityTypeFile, JsonNode.class);
        	log.error("Checking "+entityTypeUri);
        	if (entityTypeUri.contains("invalid-")) {
        		log.error("Checking invalid: " + entityTypeUri);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityTypeUri  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityTypeUri);
        		} catch (TestEvalException e) {
        			//log.error(e.getMessage());
        			assertTrue("Must contain invalidity message", e.getMessage().contains("ES-ENTITY-TYPE-INVALID"));
        		}
        	}
        	else {
        		JacksonHandle handle  = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityTypeUri  + "'))", new JacksonHandle());
        		JsonNode actual = handle.get();
                
                checkRoundTrip("Original node should equal serialized retrieved one.", original, actual);
        	}
        	
        }
    }
    
    
}
