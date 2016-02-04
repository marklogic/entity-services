package com.marklogic.entityservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotNotFoundException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.AbstractReadHandle;

public class TestEntityTypes extends EntityServicesTestBase {

    private static DatabaseClient client;
    private static HashMap<File, String> entityTypeUris = new HashMap<File, String>();
    
    private static DocumentBuilder builder;
    
		
	
    @SuppressWarnings("unchecked")
	@BeforeClass
    public static void setupClass() throws IOException, ParserConfigurationException {
        TestSetup testSetup = TestSetup.getInstance();
        client = testSetup.getClient();
        JSONDocumentManager docMgr = client.newJSONDocumentManager();
        DocumentWriteSet writeSet = docMgr.newWriteSet();
        Collection<File> files = FileUtils.listFiles(new File("src/test/resources/json-entity-types"), 
                FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        Collection<File> xmlFiles = FileUtils.listFiles(new File("src/test/resources/xml-entity-types"), 
                FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        files.addAll(xmlFiles);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	factory.setNamespaceAware(true);
    	builder = factory.newDocumentBuilder();
    	
        for (File f : files) {
        	if (f.getName().startsWith(".")) { continue; };
        	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; };
        	
        	// uncomment for quick iteration on TDE.
        	//if (!f.getName().equals("Person-0.0.2.json")) {continue; };
        	// if (!f.getName().equals("schema-complete-entity-type.xml")) {continue; };
        	//if (!f.getName().startsWith("refs")) {continue; };
        	
        	logger.info("Loading " + f.getPath());
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
                "import module namespace es = 'http://marklogic.com/entity-services' at '/MarkLogic/entity-services/entity-services.xqy';\n" +
        		"import module namespace esi = 'http://marklogic.com/entity-services-impl' at '/MarkLogic/entity-services/entity-services-impl.xqy';\n";

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
    
    private void checkXMLRoundTrip(String message, Document original, Document actual) {
    	
    	XMLUnit.setIgnoreWhitespace(true);
    	XMLAssert.assertXMLEqual(message, original, actual);
    }
    
    @Test
    /*
     * For each entity type in the test directory, verify that
     * it parses and that it matches the entity type parsed by
     * the server.
     */
    public void testEntityTypeParse() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
        for (File entityTypeFile : entityTypeUris.keySet()) {
        	String entityTypeUri = entityTypeUris.get(entityTypeFile);
        	ObjectMapper mapper = new ObjectMapper();
        	logger.info("Checking "+entityTypeUri);
        	if (entityTypeUri.contains("invalid-")) {
        		logger.info("Checking invalid: " + entityTypeUri);
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
        		
        		// FIXME templates need to exclude triples
        		// TDE needs enhancement.
        		//checkTriples(entityTypeUri);

                if ( entityTypeFile.getName().endsWith(".json")) {
                	JsonNode original = mapper.readValue(entityTypeFile, JsonNode.class);
                	JacksonHandle handle  = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityTypeUri  + "'))", new JacksonHandle());
            		JsonNode actual = handle.get();
                    
                    checkRoundTrip("Original node should equal serialized retrieved one: " + entityTypeFile.getName(), original, actual);
                    
                	checkEntityTypeToXML("Retrieved as XML, should match equivalent XML payload.", entityTypeFile);
                } else {
                	String jsonFileName = entityTypeFile.getName().replace(".xml", ".json");
                	File jsonFile = new File("src/test/resources/json-entity-types/" + jsonFileName);
            		
                	JsonNode jsonEquivalent = mapper.readValue(jsonFile, JsonNode.class);
                	JacksonHandle handle  = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityTypeUri  + "'))", new JacksonHandle());
            		JsonNode jsonActual = handle.get();
                    checkRoundTrip("Converted to a map:map, the XML entity type should match the json equivalent", jsonEquivalent, jsonActual);
            		
                	Document xmloriginal = builder.parse(entityTypeFile);
                	DOMHandle xmlhandle  = evalOneResult("es:entity-type-to-xml(es:entity-type-from-node(fn:doc('"+ entityTypeUri  + "')))", new DOMHandle());
            		Document xmlactual = xmlhandle.get();
            		
            		debugOutput(xmloriginal);
            		debugOutput(xmlactual);
            		
            	    checkXMLRoundTrip("Original node should equal serialized retrieved one: " + entityTypeFile.getName(), xmloriginal, xmlactual);
            	       
            	    checkEntityTypeToJSON("Retrieved as JSON, should match equivalent JSON payload", entityTypeFile, jsonFile);
            		
                }	
        	}
        	
        }
    }
    
    private void debugOutput(Document xmldoc) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(xmldoc), new StreamResult(System.out));
   }
    /*
     * Checks parity of XML payload when retrieved from entity type.
     */
    private void checkEntityTypeToXML(String message, File entityTypeFile) throws TestEvalException, SAXException, IOException, ParserConfigurationException, TransformerException {
    	String xmlFileName = entityTypeFile.getName().replace(".json", ".xml");
    	String entityTypeUri = entityTypeUris.get(entityTypeFile);
		File xmlFile = new File("src/test/resources/xml-entity-types/" + xmlFileName);
		
		Document expectedXML = builder.parse(xmlFile);
		String evalXML =  "es:entity-type-to-xml(es:entity-type-from-node(fn:doc('" + entityTypeUri + "')))";
		
		XMLAssert.assertXMLEqual("how bout",  "<a:x xmlns=\"\" xmlns:a=\"a\"/>",  "<a:x xmlns:a=\"a\"/>" );
		DOMHandle handle = evalOneResult(evalXML, new DOMHandle());
		Document actualXML = handle.get();
		XMLUnit.setIgnoreWhitespace(true);
		debugOutput(expectedXML);
		debugOutput(actualXML);
		
		DetailedDiff diff = new DetailedDiff(new Diff(expectedXML, actualXML));

		List<Difference> l = diff.getAllDifferences();
		for (Difference d : l) {
			System.out.println(d.toString());
		}
		XMLAssert.assertXMLEqual(message, expectedXML, actualXML);
	}
    
    /*
     * Checks parity of JSON payload when retrieved from XML-sourced entity type.
     */
    private void checkEntityTypeToJSON(String message, File entityTypeFile, File jsonFile) throws TestEvalException {
    	String entityTypeUri = entityTypeUris.get(entityTypeFile);
		String evalJSONEqual =  "deep-equal("
			       + "fn:doc('"+ entityTypeUris.get(jsonFile) +"')/node(), "
                   + "es:entity-type-to-json(es:entity-type-from-node(fn:doc('" + entityTypeUri + "')))"
                   + ")";
		
		StringHandle handle = evalOneResult(evalJSONEqual, new StringHandle());
		assertEquals(message, "true", handle.get());
		
		
		// also check that default serialization matches JSON
		
	}
    

	private void checkTriples(String entityTypeUri) throws TestEvalException {
        InputStreamHandle rdfHandle = evalOneResult("xdmp:set-response-output-method('n-triples'), xdmp:quote(esi:extract-triples(fn:doc('"+entityTypeUri + "')))", new InputStreamHandle() );

        Graph actualTriples = GraphFactory.createGraphMem();
        RDFDataMgr.read(actualTriples, rdfHandle.get(), Lang.NTRIPLES);
        
        
        Graph expectedTriples = GraphFactory.createGraphMem();
        Pattern filePattern = Pattern.compile("(.*)/json-entity-types/(.*)\\.json$");
        Matcher matcher = filePattern.matcher(entityTypeUri);
        if (matcher.matches()) {
        	String triplesFileUri = matcher.group(1) + "/triples-expected/" + matcher.group(2) + ".ttl";
        	try {
        		RDFDataMgr.read(expectedTriples, triplesFileUri, Lang.TURTLE);
        		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        		RDFDataMgr.write(baos, actualTriples, Lang.TURTLE);
        		logger.debug("Actual triples returned: " + baos.toString());
        		logger.debug("Expected number of triples: " + expectedTriples.size());
        		logger.debug("Actual number of triples: " + actualTriples.size());
        		
        		// what a great function for debugging:
        		// Graph diff = new Difference(actualTriples, expectedTriples);
        		// RDFDataMgr.write(System.out, diff, Lang.TURTLE);
            	
        		assertTrue("Graph must match expected: " + entityTypeUri, expectedTriples.isIsomorphicWith(actualTriples));
        	} catch (RiotNotFoundException e) {
        		logger.info("No RDF verification for " + entityTypeUri);
        	}
        }
    }
}
