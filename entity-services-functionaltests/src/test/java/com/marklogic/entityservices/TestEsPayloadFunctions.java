package com.marklogic.entityservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.marklogic.client.eval.EvalResult.Type;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotNotFoundException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.hpl.jena.sparql.algebra.Transformer;
import com.hp.hpl.jena.sparql.function.library.e;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
//import com.marklogic.entityservices.tests.TestEvalException;
import com.sun.org.apache.xerces.internal.parsers.XMLParser;


@SuppressWarnings("unused")
public class TestEsPayloadFunctions extends EntityServicesTestBase {

	@BeforeClass
	public static void setupEntityTypes() {
		setupClients();
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
    public void testValidJSON() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
        for (String entityType : entityTypes) {
        	ObjectMapper mapper = new ObjectMapper();
        	logger.info("Checking "+entityType);
        	
        	if (entityType.contains(".xml")||entityType.contains("-Src.json")||entityType.contains("-Tgt.json")||entityType.contains("invalid-")||entityType.contains("jpg")) { continue; }

                if ( entityType.toString().endsWith(".json")) {
                	InputStream is = this.getClass().getResourceAsStream("/json-entity-types/"+entityType);
                	JsonNode original = mapper.readValue(is, JsonNode.class);
                	JacksonHandle handle  = evalOneResult("fn:doc('"+ entityType  + "')", new JacksonHandle());
            		JsonNode actual = handle.get();
                    
                    checkRoundTrip("Original node should equal serialized retrieved one: " +entityType, original, actual);
                    
                	checkModelToXML("Retrieved as XML, should match equivalent XML payload: " +entityType.toString(), entityType.toString());
                }         
      }
    }
        
    @Test
    public void testValidXML() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    	for (String entityType : entityTypes) {
          ObjectMapper mapper = new ObjectMapper();
          logger.info("Checking... "+entityType);
            	
            	if (entityType.contains(".json")||entityType.contains("invalid-")||entityType.contains("jpg")) { continue; }
            	
            	String jsonFileName = entityType.toString().replace(".xml", ".json");
            	
            	InputStream jsonInputStreamControl = this.getClass().getResourceAsStream("/json-entity-types/" + jsonFileName);
               
            	JsonNode jsonEquivalent = mapper.readValue(jsonInputStreamControl, JsonNode.class);
            	JacksonHandle handle  = evalOneResult("es:model-from-xml(fn:doc('"+ entityType  + "'))", new JacksonHandle());
        		JsonNode jsonActual = handle.get();
                checkRoundTrip("Converted to a map:map, the XML entity type should match the json equivalent", jsonEquivalent, jsonActual);
        		
                InputStream xmlControl = this.getClass().getResourceAsStream("/xml-entity-types/"+entityType);
            	Document xmloriginal = builder.parse(xmlControl);
            	DOMHandle xmlhandle  = evalOneResult("es:model-to-xml(es:model-from-xml(fn:doc('"+ entityType  + "')))", new DOMHandle());
        		Document xmlactual = xmlhandle.get();
        		
        		//debugOutput(xmloriginal);
        		//debugOutput(xmlactual);
        		
        	    checkXMLRoundTrip("Original node should equal serialized retrieved one: " + entityType, xmloriginal, xmlactual);        		
    
          }
       }
    
    @Test
    /* testing zero definitions/EntityTypes : */
    public void testModelValidateNoEntityTypeJson() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-definitions-empty.json");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-definitions-empty.json'))", new JacksonHandle());	
            		fail("eval should throw an exception for zero definitions. invalid-definitions-empty.json");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: There must be at least one entity type in a model document."));    		
    	}
    		
    }
    
    @Test
    /* testing zero definitions/EntityTypes : */
    public void testModelValidateNoEntityTypeXml() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-definitions-empty.xml");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-definitions-empty.xml'))", new JacksonHandle());	
            		fail("eval should throw an exception for zero definitions. invalid-definitions-empty.xml");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: There must be at least one entity type in a model document."));    		
    	}
    		
    }
    
    @Test
    /* testing Invalid baseURi : */
    public void testModelValidateInvalidBaseUriColon() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-baseUri-colon.xml");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-baseUri-colon.xml'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid baseUri. invalid-baseUri-colon.xml");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("If present, baseUri (es:base-uri) must be an absolute URI."));    		
    	}
    		
    }
    
    @Test
    /* testing bug38858 */
    public void testEachPropertymusthaveObj() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking bug38858.json");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('bug38858.json'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid ET.");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("Each property must be an object"));    		
    	}
    		
    }
    
    @Test
    /* testing Invalid case sensitive datatype in json Entity Type doc */
    public void testModelValidateInvalidDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-casesensitive-datatype.json");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-casesensitive-datatype.json'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases. invalid-casesensitive-datatype.json");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Unsupported datatype: String."));    		
    	}
    		
    }
    
    @Test
    /* testing model-validate for no arguments */
    public void testModelValidateNoArg() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    			logger.info("Checking for no arg");
        		JacksonHandle handle = null;
        	    try {
        			handle = evalOneResult("es:model-validate(fn:doc(''))", new JacksonHandle());	
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        		    assertTrue("Must contain XDMP-ARGTYPE but got: "+e.getMessage(),e.getMessage().contains("XDMP-URI"));
        }		
    }
    
    @Test
    /* testing model-validate for Binary document */
    public void testModelValidateBinaryDoc() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {

          		logger.info("Checking binary: New-Year-Sayings.jpg" );
           		JacksonHandle handle = null;
          		try {
           			handle = evalOneResult("es:model-validate(fn:doc('New-Year-Sayings.jpg'))", new JacksonHandle());	
               		fail("eval should throw an exception for invalid cases. New-Year-Sayings.jpg");
           		} catch (TestEvalException e) {
           			logger.info(e.getMessage());             			
                   	assertTrue("Must throw XDMP-AS error but got: "+e.getMessage(),e.getMessage().contains("Invalid coercion"));
        }
    }
    
    @Test
    /* testing model-validate json for missing datatype */
    public void testModelValidateJsonMissingdDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    			logger.info("Checking invalid-missing-datatype.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-datatype.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for missing datatype");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: If a property is not a reference, then it must have a datatype."));
    	}
    }
    
    @Test
    /* testing model-validate xml for missing datatype */
    public void testModelValidateXmlMissingdDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    			logger.info("Checking invalid-missing-datatype.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-datatype.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for missing datatype");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: If a property is not a reference, then it must have a datatype."));
    	}
    }
    
    @Test
    /* testing model-validate xml for unsupported datatype */
    public void testModelValidateXmlUnsupportedDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
				logger.info("Checking invalid-bad-datatype.xml");
				JacksonHandle handle = null;
				try {
					handle = evalOneResult("es:model-validate(fn:doc('invalid-bad-datatype.xml'))", new JacksonHandle());	
					fail("eval should throw an exception for unsupported datatypes");
				} catch (TestEvalException e) {
					logger.info(e.getMessage());
					assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Unsupported datatype: botcheddt."));
		}
    }
    
    @Test
    /* testing model-validate json for unsupported datatype */
    public void testModelValidateJsonUnsupportedDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-bad-datatype.json");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-bad-datatype.json'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases. invalid-bad-datatype.json");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Unsupported datatype: anySimpleType."));    		
    	}
    		
    }
    
    @Test
    /* testing model-validate json for title with white space */
    public void testModelValidateJsonInvalidTitle() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-title-whiteSpace.json");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-title-whiteSpace.json'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases. invalid-title-whiteSpace.json");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Title must have no whitespace and must start with a letter."));    		
    	}
    		
    }
    
    @Test
    /* testing model-validate json for invalid range index */
    public void testModelValidateJsonInvalidRangeIndex() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-range-index.json");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-range-index.json'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases. invalid-range-index.json");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Range index property hello doesn't exist. Range index property world doesn't exist."));    		
    	}
    		
    }
    
    @Test
    /* testing model-validate xml for invalid range index */
    public void testModelValidateXmlInvalidRangeIndex() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
    		
            	logger.info("Checking invalid-range-index.xml");
            	JacksonHandle handle = null;
            	try {
        			handle = evalOneResult("es:model-validate(fn:doc('invalid-range-index.xml'))", new JacksonHandle());	
            		fail("eval should throw an exception for invalid cases. invalid-range-index.xml");
        		} catch (TestEvalException e) {
        			logger.info(e.getMessage());
        			assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Range index property hello doesn't exist. Range index property world doesn't exist."));    		
    	}
    		
    }
    
    @Test
    /* testing model-validate xml for missing info */
    public void testModelValidateXmlMissingInfo() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
				logger.info("Checking invalid-missing-info.xml");
				JacksonHandle handle = null;
				try {
					handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-info.xml'))", new JacksonHandle());	
					fail("eval should throw an exception for missing info");
				} catch (TestEvalException e) {
					logger.info(e.getMessage());
					assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Entity Type Document must contain exactly one info section."));
		}
    }
    
    @Test
    /* testing model-validate json for missing info */
    public void testModelValidateJsonMissingInfo() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
				logger.info("Checking invalid-missing-info.json");
				JacksonHandle handle = null;
				try {
					handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-info.json'))", new JacksonHandle());	
					fail("eval should throw an exception for missing info");
				} catch (TestEvalException e) {
					logger.info(e.getMessage());
					assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Entity Type Document must contain exactly one info section."));
		}
    }
    
    @Test
    /* testing model-validate for a json entity type having $ref and datatype together */
    public void testModelValidateJsonInvalidRefDatatypeTogether() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-datatype-ref-together.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-datatype-ref-together.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-datatype-ref-together.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: If a property has $ref as a child, then it cannot have a datatype."));
		}
	}

    @Test
    /* testing model-validate for an xml entity type having $ref and datatype together */
    public void testModelValidateXmlInvalidRefDatatypeTogether() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-datatype-ref-together.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-datatype-ref-together.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-datatype-ref-together.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: If a property has es:ref as a child, then it cannot have a datatype."));
		}
	}
    
    @Test
    /* testing model-validate json for missing title */
    public void testModelValidateJsonMissingTitle() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-title.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-title.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-title.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: \"info\" section must be an object and contain exactly one title declaration."));
    	}
    }
    
    @Test
    /* testing model-validate xml for missing title */
    public void testModelValidateXmlMissingTitle() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-title.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-title.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-title.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: \"info\" section must be an object and contain exactly one title declaration."));
    	}
    }
    
    @Test
    /* testing model-validate json for missing definitions */
    public void testModelValidateJsonMissingDefinitions() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-definitions.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-definitions.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-definitions.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Entity Type Document must contain exactly one definitions section."));
    	}
    }
    
	@Test
    /* testing model-validate xml for missing definitions */
    public void testModelValidateXmlMissingDefinitions() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-definitions.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-definitions.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-definitions.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Entity Type Document must contain exactly one definitions section."));
    	}
    }

    @Test
    /* testing model-validate xml for missing version */
    public void testModelValidateXmlMissingVersion() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-version.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-version.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-version.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: \"info\" section must be an object and contain exactly one version declaration."));
    	}
    }
    
    @Test
    /* testing model-validate json for missing version */
    public void testModelValidateJsonMissingVersion() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-version.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-version.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-version.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: \"info\" section must be an object and contain exactly one version declaration."));
    	}
    }
    
    @Test
    /* testing model-validate xml for multiple primary key */
    public void testModelValidateXmlMultiplePrimaryKey() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-multiple-primarykey.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-multiple-primarykey.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-multiple-primarykey.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: For each Entity Type, only one primary key allowed."));
    	}
    }
    
    @Test
    /* testing model-validate json for multiple primary key */
    public void testModelValidateJsonMultiplePrimaryKey() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-multiple-pkey.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-multiple-pkey.json'))", new JacksonHandle());	
    				logger.info("After eval");
    				fail("eval should throw an exception for invalid cases: invalid-multiple-pkey.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: For each Entity Type, only one primary key allowed."));
    	}
    }
     
    @Test
    /* testing model-validate for a json entity type where info is not an object */
    public void testModelValidateJsonInfoNotObject() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-info-notobject.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-info-notobject.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-info-notobject.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: \"info\" section must be an object and contain exactly one title declaration. \"info\" section must be an object and contain exactly one version declaration."));
    	}
    }
    
    @Test
    /* testing model-validate for an xml entity type where info is not an object */
    public void testModelValidateXmlInfoNotObject() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-info-notobject.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-info-notobject.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-info-notobject.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: \"info\" section must be an object and contain exactly one title declaration. \"info\" section must be an object and contain exactly one version declaration."));
    	}
    }
    
    @Test
    /* testing model-validate for an xml entity type where info is not an object */
    public void testbug40666() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-primary-key-as-ref.json");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-primary-key-as-ref.json'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-primary-key-as-ref.json");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("A reference cannot be primary key"));
    	}
    }
    
    
    @Test
    //  BUG 38392 - testing model-validate for an xml entity type where info is not an object 
    public void testModelValidateMissingItemsXml() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking invalid-missing-items-when-datatype-array.xml");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-items-when-datatype-array.xml'))", new JacksonHandle());	
    				fail("eval should throw an exception for invalid cases: invalid-missing-items-when-datatype-array.xml");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain invalidity message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Property OrderDetail is of type \"array\" and must contain an \"items\" declaration."));
    	}
    }

    @Test
    /* testing model-validate with xml document */
    public void testBug38517() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking bug38517");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate('valid-datatype-array.xml')", new JacksonHandle());	
    				fail("eval should throw an ES-MODEL-INVALID exception for model-validate() with a document");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Valid models must be JSON, XML or map:map"));
    	}
    }   
    
    @Test
    /* testing model-to-xml with no args */
    public void testToXmlNoArgs() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking model-to-xml() with no args");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-to-xml()", new JacksonHandle());	
    				fail("eval should throw XDMP-TOOFEWARGS exception for model-to-xml() with no args");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain XDMP-TOOFEWARGS error message but got: "+e.getMessage(), e.getMessage().contains("Too few args, expected 1 but got 0"));
    	}
    }
    
    @Test
    /* testing model-to-xml with too many args */
    public void testToXmlTooManyArgs() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking model-to-xml() with too many args");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-to-xml(fn:doc('valid-datatype-array.xml'),fn:doc('valid-datatype-array.json'))", new JacksonHandle());	
    				fail("eval should throw XDMP-TOOMANYARGS exception for model-to-xml() with no args");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain XDMP-TOOMANYARGS error message but got: "+e.getMessage(), e.getMessage().contains("Too many args, expected 1 but got 2"));
    	}
    }
    
    @Test
    /* testing model-validate with schematron error */
    public void testValidateSchematronError() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking model-validate() with schematron error");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-missing-version.xml'))", new JacksonHandle());	
    				fail("eval should throw ES-MODEL-INVALID  exception for model-validate() with schematron error");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain ES-MODEL-INVALID  error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: \"info\" section must be an object and contain exactly one version declaration."));
    	}
    }
    
    @Test
	public void testJS() throws Exception {
	
	
	 	try {
		String query1 = " var results = 100;results";
						
		ServerEvaluationCall evl= client.newServerEval().javascript(query1);
		
        EvalResultIterator evr = evl.eval();
		while (evr.hasNext()) {
			EvalResult er = evr.next();
			er.getNumber();
		}
		

		} catch (Exception e) {
			throw e;
		}
	}
	
    @Test
	public void testJS2() throws Exception {
	
	
	 	try {
		String query1 = "var es = require('/MarkLogic/entity-services/entity-services.xqy');es.instanceConverterGenerate( es.modelFromXml( fn.doc('valid-ref-combo-sameDocument-subIri.xml')));";
        ServerEvaluationCall evl= client.newServerEval().javascript(query1);
        String bindings="";
        EvalResultIterator evr = evl.eval();
        
		while (evr.hasNext()) {
			EvalResult er = evr.next();
			 bindings = er.getType().toString();		
		}
				
	    assertEquals("TEXTNODE",bindings);
		
		} catch (Exception e) {
			throw e;
		}
	}
    
    @Test
	public void testJS3() throws Exception {
	
	
	 	try {
		String query1 = "var es = require('/MarkLogic/entity-services/entity-services.xqy');es.modelFromXml( fn.doc('valid-ref-combo-sameDocument-subIri.xml'));";
        ServerEvaluationCall evl= client.newServerEval().javascript(query1);
        String bindings="";
        EvalResultIterator evr = evl.eval();
        
		while (evr.hasNext()) {
			EvalResult er = evr.next();
			 bindings = er.getType().toString();		
		}
				
	    assertEquals("JSON",bindings);
		
		} catch (Exception e) {
			throw e;
		}
	}
    
    @Test
  	public void testJS4() throws Exception {
  	
  	
  	 	try {
  		String query1 = "var es = require('/MarkLogic/entity-services/entity-services.xqy');es.modelFromXml( fn.doc('valid-ref-combo-sameDocument-subIri.xml'));";
          ServerEvaluationCall evl= client.newServerEval().javascript(query1);
          String bindings="";
          EvalResultIterator evr = evl.eval();
          
  		while (evr.hasNext()) {
  			EvalResult er = evr.next();
  			 bindings = er.getString();		
  		}
  			
  		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-js/js3.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);
	
		bindings.replaceAll(" ","");
		System.out.println("bindings:::"+bindings);
		control.toString().replaceAll(" ","");
		System.out.println("control:::"+control.toString());
		
  	    //assertEquals(control.toString(),bindings);
		assertNotNull(bindings);
  		
  		} catch (Exception e) {
  			throw e;
  		}
  	}

	@Test
    /* testing model-validate with rangeIndex error */
    public void testInvalidRangeIndexDatatype() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking model-validate() with rangeIndex error");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:database-properties-generate(es:model-validate(fn:doc('invalid-db-prop-rangeindex.json')))", new JacksonHandle());	
    				//fail("eval should throw ES-MODEL-INVALID  exception for model-validate with rangeIndex error");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain ES-MODEL-INVALID  error message but got: "+e.getMessage(), e.getMessage().contains("gYearMonth in property YearsofService is unsupported for a range index."));
    	}
    }

	@Test
    /* bug38353 : nested array should be disallowed in the EntityType document  */
    public void testbug38353() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking model-validate() with rangeIndex error");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-bug38353.json'))", new JacksonHandle());	
    				
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain ES-MODEL-INVALID  error message but got: "+e.getMessage(), e.getMessage().contains("If present, baseUri (es:base-uri) must be an absolute URI. Property datatype cannot both be an"));
    	}
    }
	
    @Test
    /* testing model-validate with required error */
    public void testInvalidRequired() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {       
    			logger.info("Checking model-validate() with rangeIndex error");
    			JacksonHandle handle = null;
    			try {
    				handle = evalOneResult("es:model-validate(fn:doc('invalid-required.json'))", new JacksonHandle());	
    				//fail("eval should throw ES-MODEL-INVALID  exception for model-validate with rangeIndex error");
    			} catch (TestEvalException e) {
    				logger.info(e.getMessage());
    				assertTrue("Must contain ES-MODEL-INVALID  error message but got: "+e.getMessage(), e.getMessage().contains("property hello doesn't exist."));
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
    private void checkModelToXML(String message, String entityTypeFile) throws TestEvalException, SAXException, IOException, ParserConfigurationException, TransformerException {
    	String xmlFileName = entityTypeFile.replace(".json", ".xml");
    	InputStream xmlFile = this.getClass().getResourceAsStream("/xml-entity-types/" + xmlFileName);
		
		Document expectedXML = builder.parse(xmlFile);
		String evalXML =  "es:model-to-xml(es:model-validate(fn:doc('" + entityTypeFile + "')))";
		
		DOMHandle handle = evalOneResult(evalXML, new DOMHandle());
		Document actualXML = handle.get();
		XMLUnit.setIgnoreWhitespace(true);
		//debugOutput(expectedXML);
		//debugOutput(actualXML);
		
		DetailedDiff diff = new DetailedDiff(new Diff(expectedXML, actualXML));

		@SuppressWarnings("unchecked")
		List<Difference> l = diff.getAllDifferences();
		for (Difference d : l) {
			System.out.println(d.toString());
		}
		XMLAssert.assertXMLEqual(message, expectedXML, actualXML);
	}
    
    /*
     * Removed check for parity of JSON payload since model-to-json() has been removed/discontinued
     */
    
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
