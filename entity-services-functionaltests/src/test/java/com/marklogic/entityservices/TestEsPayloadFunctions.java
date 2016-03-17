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
    @Test
    public void testFromNodeValidJSON() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
        for (String entityType : entityTypes) {
        	ObjectMapper mapper = new ObjectMapper();
        	logger.info("Checking "+entityType);
        	
        	if (entityType.contains(".xml")||entityType.contains("invalid-")||entityType.contains("jpg")) { continue; }

                if ( entityType.toString().endsWith(".json")) {
                	InputStream is = this.getClass().getResourceAsStream("/json-entity-types/"+entityType);
                	JsonNode original = mapper.readValue(is, JsonNode.class);
                	JacksonHandle handle  = evalOneResult("es:entity-type-from-node(fn:doc('"+ entityType  + "'))", new JacksonHandle());
            		JsonNode actual = handle.get();
                    
                    checkRoundTrip("Original node should equal serialized retrieved one: " +entityType, original, actual);
                    
                	checkEntityTypeToXML("Retrieved as XML, should match equivalent XML payload.", entityType.toString());
                }         
      }
    }
        
    @Test
    public void testFromNodeValidXML() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    	for (String entityType : entityTypes) {
          ObjectMapper mapper = new ObjectMapper();
          logger.info("Checking "+entityType);
            	
            	if (entityType.contains(".json")||entityType.contains("invalid-")||entityType.contains("jpg")) { continue; }
            	
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
    
    @Test
    /* testing Invalid case sensitive datatype in Entity Type doc */
    public void testFromNodeInvalidDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking "+"invalid-casesensitive-datatype.json");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-casesensitive-datatype.json'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases." + "invalid-casesensitive-datatype.json");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("Unsupported datatype."));    		
    	}
    		
    }
    
    @Test
    /* testing entity-type-from-node for no arguments */
    public void testFromNodeNoArg() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    			logger.info("Checking for no arg");
        		JacksonHandle handle = null;
        	    try {
        			handle = evalOneResult("es:entity-type-from-node(fn:doc(''))", new JacksonHandle());	
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        		    assertTrue("Must contain XDMP-ARGTYPE but got: "+e.getMessage(),e.getMessage().contains("XDMP-URI"));
        }		
    }
    
    @Test
    /* testing entity-type-from-node for Binary document */
    public void testFromNodeBinaryDoc() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {

          		logger.info("Checking binary: " + "New-Year-Sayings.jpg" );
           		JacksonHandle handle = null;
          		try {
           			handle = evalOneResult("es:entity-type-from-node(fn:doc('New-Year-Sayings.jpg'))", new JacksonHandle());	
               		fail("eval should throw an exception for invalid cases." + "New-Year-Sayings.jpg");
           		} catch (TestEvalException e) {
           			logger.info(e.getMessage());             			
                   	assertTrue("Must throw XDMP-AS error but got: "+e.getMessage(),e.getMessage().contains("Invalid coercion"));
        }
    }
    
    @Test
    /* testing entity-type-from-node for unsupported datatype */
    public void testFromNodeUnsupportedDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    			logger.info("Checking invalid-bad-datatype");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-bad-datatype.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid/unsupported datatypes");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("Unsupported datatype."));
    	}
    }
    
    @Test
    /* testing entity-type-from-node for a json entity type having $ref and datatype together */
    public void testFromNodeJsonInvalidRefDatatypeTogether() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-datatype-ref-together.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-datatype-ref-together.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-datatype-ref-together.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("If using $ref, it must be the only key."));
		}
	}

    @Test
    /* testing entity-type-from-node for an xml entity type having $ref and datatype together */
    public void testFromNodeXmlInvalidRefDatatypeTogether() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-datatype-ref-together.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-datatype-ref-together.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-datatype-ref-together.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("If using es:ref, it must be the only child of es:property."));
		}
	}
    
    @Test
    /* testing entity-type-from-node for missing definitions */
    public void testFromNodeMissingDefinitions() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-definitions.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-missing-definitions.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-definitions.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("Entity Type must contain exactly one definitions declaration."));
    	}
    }

    @Test
    /* testing entity-type-from-node for missing version */
    public void testFromNodeMissingVersion() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-version.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-missing-version.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-version.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("Entity Type must contain exactly one version declaration."));
    	}
    }
 
    @Test
    /* testing entity-type-from-node for a json entity type where info is not an object */
    public void testFromNodeJsonInfoNotObject() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-info-notobject.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-info-notobject.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-info-notobject.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("Entity Type must contain exactly one title declaration. Entity Type must contain exactly one version declaration."));
    	}
    }
 
    @Test
    /* testing entity-type-from-node for an xml entity type where info is not an object */
    public void testFromNodeXmlInfoNotObject() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-info-notobject.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-from-node(fn:doc('invalid-info-notobject.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-info-notobject.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("Entity Type must contain exactly one title declaration. Entity Type must contain exactly one version declaration."));
    	}
    }
    
    @Test
    /* testing entity-type-to-json with a document node */
    public void testToXmlWithDocumentNode() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking entity-type-to-xml() with a document node");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-to-xml(fn:doc('valid-datatype-array.xml'))", new JacksonHandle());	
    				fail("eval should throw an Invalid Coercion exception for entity-type-to-xml() with a document node");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain XDMP-AS error message but got: "+e.getMessage(), e.getMessage().contains("$entity-type as map:map -- Invalid coercion: xs:untypedAtomic"));
    	}
    }

    @Test
    /* testing entity-type-to-json with a document node */
    public void testToJsonWithXmlDocumentNode() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking entity-type-to-json() with a document node");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-to-json(fn:doc('valid-datatype-array.xml'))", new JacksonHandle());	
    				fail("eval should throw an Invalid Coercion exception for entity-type-to-json() with a document node");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain XDMP-AS error message but got: "+e.getMessage(), e.getMessage().contains("$entity-type as map:map -- Invalid coercion: xs:untypedAtomic"));
    	}
    }
    
    @Test
    /* testing entity-type-to-json with a document node */
    public void testToJsonWithJsonDocumentNode() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking entity-type-to-json() with a document node");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-to-json(fn:doc('valid-datatype-array.json'))", new JacksonHandle());	
    				fail("eval should throw an Invalid Coercion exception for entity-type-to-json() with a document node");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain XDMP-AS error message but got: "+e.getMessage(), e.getMessage().contains("$entity-type as map:map -- Invalid coercion: xs:untypedAtomic"));
    	}
    }
    
    @Test
    /* testing entity-type-to-json with no args */
    public void testToJsonNoArgs() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking entity-type-to-json() with no args");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-to-json()", new JacksonHandle());	
    				fail("eval should throw XDMP-TOOFEWARGS exception for entity-type-to-json() with no args");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain XDMP-TOOFEWARGS error message but got: "+e.getMessage(), e.getMessage().contains("Too few args, expected 1 but got 0"));
    	}
    }
    
    @Test
    /* testing entity-type-to-json with too many args */
    public void testToJsonTooManyArgs() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking entity-type-to-json() with too many args");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-to-json(fn:doc('valid-datatype-array.xml'),fn:doc('valid-datatype-array.json'))", new JacksonHandle());	
    				fail("eval should throw XDMP-TOOMANYARGS exception for entity-type-to-json() with no args");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain XDMP-TOOMANYARGS error message but got: "+e.getMessage(), e.getMessage().contains("Too many args, expected 1 but got 2"));
    	}
    }
    
    @Test
    /* testing entity-type-to-json with schematron error */
    public void testToJsonSchematronError() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking entity-type-to-json() with schematron error");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:entity-type-to-json(es:entity-type-from-node(fn:doc('invalid-missing-version.xml')))", new JacksonHandle());	
    				fail("eval should throw ES-ENTITY-TYPE-INVALID  exception for entity-type-to-json() with schematron error");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain ES-ENTITY-TYPE-INVALID  error message but got: "+e.getMessage(), e.getMessage().contains("Entity Type must contain exactly one version declaration."));
    	}
    }
    
/*    private void debugOutput(Document xmldoc) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(xmldoc), new StreamResult(System.out));
   }*/
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
