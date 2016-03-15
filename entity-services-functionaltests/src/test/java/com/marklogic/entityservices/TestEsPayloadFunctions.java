package com.marklogic.entityservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotNotFoundException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.AbstractReadHandle;

public class TestEsPayloadFunctions extends EntityServicesTestBase {

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
     * 
     * This test cycles through each test entity type.
     * If the entity type file name contains "invalid-" then it must
     * throw a validation exception.
     * 
     * Otherwise, the entity type is tested in comparison to an equivalent entity type in
     * xml-entity-types
     * 
     * entity-type-from-node   Serialized JSON equal to JSON file
     * entity-type-to-json     JSON equal to JSON file
     * entity-type-to-xml       Serialization to XML.
     */
    public void testEntityTypeParse() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
        for (String entityType : entityTypes) {
        	ObjectMapper mapper = new ObjectMapper();
        	logger.info("Checking "+entityType);
        	if (entityType.toString().contains("jpg")) {
        		logger.info("Checking binary: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			
        			assertTrue("Must throw XDMP-AS error",e.getMessage().contains("Invalid coercion"));
        		}
        	}
        	else if (entityType.toString().contains("invalid-bad-datatype")) {
        		logger.info("Checking invalid: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message", e.getMessage().contains("Unsupported datatype."));
        		}
        	}
        	else if (entityType.toString().contains("invalid-datatype-ref-together")) {
        		logger.info("Checking invalid: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message", e.getMessage().contains("If using $ref, it must be the only key."));
        		}
        	}
        	else if (entityType.toString().contains("invalid-missing-definitions")) {
        		
        		logger.info("Checking invalid: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message", e.getMessage().contains("Entity Type must contain exactly one definitions declaration."));
        		}
        	}
        	else if (entityType.toString().contains("invalid-missing-version")) {
        		logger.info("Checking invalid: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message", e.getMessage().contains("Entity Type must contain exactly one version declaration."));
        		}
        	}
        	else if (entityType.toString().contains("invalid-info-notobject")) {
        		logger.info("Checking invalid: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message", e.getMessage().contains("Entity Type must contain exactly one title declaration."));
        		}
        	}
        	else if (entityType.toString().contains("invalid-casesensitive-datatype")) {
        		logger.info("Checking invalid: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message", e.getMessage().contains("Unsupported datatype."));
        		}
        	}
        	else {
        		
        		// FIXME templates need to exclude triples
        		// TDE needs enhancement.
        		// checkTriples(entityTypeUri);

                if ( entityType.toString().endsWith(".json")) {
                	InputStream is = this.getClass().getResourceAsStream("/json-entity-types/"+entityType);
                	JsonNode original = mapper.readValue(is, JsonNode.class);
                	JacksonHandle handle  = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType  + "'))", new JacksonHandle());
            		JsonNode actual = handle.get();
                    
                    checkRoundTrip("Original node should equal serialized retrieved one: " +entityType, original, actual);
                    
                	checkEntityTypeToXML("Retrieved as XML, should match equivalent XML payload.", entityType.toString());
                } else {
                	String jsonFileName = entityType.toString().replace(".xml", ".json");
                	
                	InputStream jsonInputStreamControl = this.getClass().getResourceAsStream("/json-entity-types/" + jsonFileName);
                   
                	JsonNode jsonEquivalent = mapper.readValue(jsonInputStreamControl, JsonNode.class);
                	JacksonHandle handle  = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType  + "'))", new JacksonHandle());
            		JsonNode jsonActual = handle.get();
                    checkRoundTrip("Converted to a map:map, the XML entity type should match the json equivalent", jsonEquivalent, jsonActual);
            		
                    InputStream xmlControl = this.getClass().getResourceAsStream("/xml-entity-types/"+entityType);
                	Document xmloriginal = builder.parse(xmlControl);
                	DOMHandle xmlhandle  = evalOneResult("es:entity-type-to-xml(es:entity-type-from-node(fn:doc('"+ entityType  + "')))", new DOMHandle());
            		Document xmlactual = xmlhandle.get();
            		
            		//debugOutput(xmloriginal);
            		//debugOutput(xmlactual);
            		
            	    checkXMLRoundTrip("Original node should equal serialized retrieved one: " + entityType, xmloriginal, xmlactual);
            	       
            	    checkEntityTypeToJSON("Retrieved as JSON, should match equivalent JSON payload", entityType.toString(), jsonFileName);
            		
                }	
        	}
        	
        }
   
        logger.info("Checking for no arg");
		JacksonHandle handle = null;
        try {
			handle = evalOneResult("es:entity-type-from-node(fn:doc(''))", new JacksonHandle());	
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
		    assertTrue("XDMP-ARGTYPE",e.getMessage().contains("XDMP-URI"));
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
    private void checkEntityTypeToXML(String message, String entityTypeFile) throws TestEvalException, SAXException, IOException, ParserConfigurationException, TransformerException {
    	String xmlFileName = entityTypeFile.replace(".json", ".xml");
    	InputStream xmlFile = this.getClass().getResourceAsStream("/xml-entity-types/" + xmlFileName);
		
		Document expectedXML = builder.parse(xmlFile);
		String evalXML =  "es:entity-type-to-xml(es:entity-type-from-node(fn:doc('" + entityTypeFile + "')))";
		
		DOMHandle handle = evalOneResult(evalXML, new DOMHandle());
		Document actualXML = handle.get();
		XMLUnit.setIgnoreWhitespace(true);
		//debugOutput(expectedXML);
		//debugOutput(actualXML);
		
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
    private void checkEntityTypeToJSON(String message, String entityTypeUri, String jsonUri) throws TestEvalException {
		String evalJSONEqual =  "deep-equal("
			       + "fn:doc('"+ jsonUri  +"')/node(), "
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
