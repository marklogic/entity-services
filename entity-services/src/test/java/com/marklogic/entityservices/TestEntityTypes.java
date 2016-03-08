/*
 * Copyright 2016 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.entityservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotNotFoundException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
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
import com.hp.hpl.jena.graph.compose.Difference;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

public class TestEntityTypes extends EntityServicesTestBase {

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
        	if (entityType.toString().contains("invalid-")) {
        		logger.info("Checking invalid: " + entityType);
        		JacksonHandle handle = null;
        		try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + entityType);
        		} catch (TestEvalException e) {
        			assertTrue("Must contain invalidity message. Message was " + e.getMessage(), e.getMessage().contains("ES-ENTITY-TYPE-INVALID"));
        		}
        	}
        	else {
        		
        		checkTriples(entityType);

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
    

	private void checkTriples(String entityTypeUri) throws TestEvalException, IOException {
        StringHandle rdfHandle = evalOneResult("xdmp:set-response-output-method('n-triples'), xdmp:quote(esi:extract-triples(fn:doc('"+entityTypeUri + "')))", new StringHandle() );

        Graph actualTriples = GraphFactory.createGraphMem();
        ByteArrayInputStream bis = new ByteArrayInputStream(rdfHandle.get().getBytes());
        RDFDataMgr.read(actualTriples, bis, Lang.NTRIPLES);
        
        
        Graph expectedTriples = GraphFactory.createGraphMem();
        Pattern filePattern = Pattern.compile("(.*)\\.(xml|json)$");
        Matcher matcher = filePattern.matcher(entityTypeUri);
        if (matcher.matches()) {
        	String triplesFileUri =  "/triples-expected/" + matcher.group(1) + ".ttl";
        	try {
        		InputStream is = this.getClass().getResourceAsStream(triplesFileUri);
        		RDFDataMgr.read(expectedTriples, is, Lang.TURTLE);
        		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        		RDFDataMgr.write(baos, actualTriples, Lang.TURTLE);
        		logger.debug("Expected number of triples: " + expectedTriples.size());
        		logger.debug("Actual number of triples: " + actualTriples.size());
        		
        		// what a great function for debugging:
        		logger.debug("Difference, expected - actual");
        		Graph diff = new Difference(expectedTriples, actualTriples);
        		
        		// more debug
        		OutputStream os = new FileOutputStream(new File("/tmp/actual.ttl"));
        		RDFDataMgr.write(os, actualTriples, Lang.TURTLE);
        		os.close();
        		os = new FileOutputStream(new File("/tmp/expected.ttl"));
        		RDFDataMgr.write(os, expectedTriples, Lang.TURTLE);
        		os.close();
        		
        		logger.debug("Difference, actual - expected");
        		Graph diff2 = new Difference(actualTriples, expectedTriples);
        		RDFDataMgr.write(System.out, diff2, Lang.TURTLE);
            	
        		
        		
        		assertTrue("Graph must match expected: " + entityTypeUri, actualTriples.isIsomorphicWith(expectedTriples));
        	} catch (NullPointerException e) {
        		logger.info("No RDF verification for " + entityTypeUri);
        	} 
        	
        }
    }
}
