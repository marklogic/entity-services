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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

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
import com.hp.hpl.jena.sparql.function.library.e;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.StringHandle;
//import com.marklogic.entityservices.tests.TestEvalException;
import com.sun.org.apache.xerces.internal.parsers.XMLParser;

/**
 * Tests server function es:conversion-module-generate
 * 
 * Covered so far: validity of XQuery module generation
 * 
 * extract-instance-Order
 * 
 * The default extraction model is valid, and each function runs as though
 * the source for an entity is the same as its model.  That is, 
 * if you extract an instance using extract-instance-Order() the original
 * generated function expects an input that corresponds exactly to the persisted
 * output of an Order.
 */
public class TestEsConversionModuleGenerator extends EntityServicesTestBase {

	private StringHandle xqueryModule = new StringHandle();
	private static TextDocumentManager docMgr;
	private static Map<String, StringHandle> conversionModules;
	
	@BeforeClass
	public static void setupClass() {
		setupClients();
		// save xquery module to modules database
		docMgr = modulesClient.newTextDocumentManager();	
		
		conversionModules = generateConversionModules();
		storeConversionModules(conversionModules);

	}
	
	private static void storeConversionModules(Map<String, StringHandle> moduleMap) {
		DocumentWriteSet writeSet = docMgr.newWriteSet();
		
		for (String entityTypeName : moduleMap.keySet()) {
			
			String moduleName = "/conv/" + entityTypeName.replaceAll("\\.(xml|json)", ".xqy");
			writeSet.add(moduleName, moduleMap.get(entityTypeName));
		}
		docMgr.write(writeSet);
	}
	
	private static Map<String, StringHandle> generateConversionModules() {
		Map<String, StringHandle> map = new HashMap<String, StringHandle>();
		
		for (String entityType : entityTypes) {
			if (entityType.contains(".xml")||entityType.contains(".jpg")||entityType.contains("invalid-")) {continue; };
			
			logger.info("Generating conversion module: " + entityType);
			StringHandle xqueryModule = new StringHandle();
			try {
				xqueryModule = evalOneResult("es:conversion-module-generate( es:entity-type-from-node( fn:doc( '"+entityType+"')))", xqueryModule);
			} catch (com.marklogic.entityservices.TestEvalException e) {
				throw new RuntimeException(e);
			}
			map.put(entityType, xqueryModule);
		}
		return map;
	}

	
	public String[] storeConversionModuleAsXqy(String entityTypeName) throws IOException, TestEvalException {
		
		String arr[] = new String[2];
		String line;
		xqueryModule = evalOneResult("es:conversion-module-generate( es:entity-type-from-node( fn:doc( '"+entityTypeName+"')))", xqueryModule);
	
		// save xquery module to modules database
		TextDocumentManager docMgr = modulesClient.newTextDocumentManager();
		String moduleName = "/conv/" + entityTypeName.replaceAll("\\.(xml|json)", ".xqy");
		docMgr.write(moduleName, xqueryModule);
		
		//Below code is to get docTitle and namespace in the generated xqy  module
		BufferedReader bf = new BufferedReader(new StringReader(xqueryModule.get()));
		
		while((line=bf.readLine()) != null){
		    if(line.startsWith("module namespace ")) {
		        arr = line.substring(17).split(" = ");
		        int len = arr[1].length();
		        arr[1] = arr[1].substring(1, len-2);
		        //logger.info("\n" + arr[0] + "\n" + arr[1]);
		        break;
		    } else {
		        continue;
		    }
		}
		
		return arr;
		
	}
	
	public String getDocTitle(String entityTypeName) throws IOException, TestEvalException {
		
		String docTitle = null;		
		docTitle = this.storeConversionModuleAsXqy(entityTypeName)[0];
		return docTitle;
		
	}
	
	public String getNameSpace(String entityTypeName) throws IOException, TestEvalException {
		
		String ns = null;		
		ns = this.storeConversionModuleAsXqy(entityTypeName)[1];
		return ns;
		
	}
	
	/*
	 * private StringHandle getEntityTypes(String entityTypeName) throws TestEvalException, IOException {
	 
		
		String docTitle = getDocTitle(entityTypeName);
		EvalResultIterator results =  eval("map:keys(map:get(es:entity-type-from-node( doc('"+entityTypeName+"') ), \"definitions\"))");
		EvalResult result = null;

		while (results.hasNext()) {
			result = results.next();
			return result.get(new StringHandle()));
		}		
	}
	*/
	
	private boolean getConversionValidationResult(String entityDoc, String function) throws TestEvalException {
		
		boolean status = false;
	 	try {
	 		status = xqueryModule.get().contains(function);
	 		assertTrue(function + " not generated for entity document: " + entityDoc, status);
	 	} catch (Exception e) {
   			logger.info("\nGot exception:\n" + e.getMessage());  
	 	}
	 	
	 	return status;		
	}
	
	@Test
	//This test verifies that conversion module generates 6+n functions, where is number of entity type names
	public void testConversionModuleGenerate() throws TestEvalException, IOException {
		
		String entityType = "valid-ref-same-document.xml";
		String docTitle = getDocTitle(entityType);		
		
		//Validating generated extract instances of entity type names.
		EvalResultIterator results =  eval("map:keys(map:get(es:entity-type-from-node( doc('"+entityType+"') ), \"definitions\"))");
		EvalResult result = null;

		while (results.hasNext()) {
			result = results.next();
			getConversionValidationResult(entityType, docTitle + ":extract-instance-" + result.get(new StringHandle()));
		}
	
		//Validating generated functions. TODO uncomment the verification for source document
		getConversionValidationResult(entityType, docTitle+":instance-to-canonical-xml");
		getConversionValidationResult(entityType, docTitle+":instance-to-envelope");
		getConversionValidationResult(entityType, docTitle+":instance-from-document");
		getConversionValidationResult(entityType, docTitle+":instance-xml-from-document");
		getConversionValidationResult(entityType, docTitle+":instance-json-from-document");
		//getConversionValidationResult(initialTest, docTitle+":sources-from-document");
			
	}
	
	@Test
	//This test verifies that conversion module generates 6 functions + 0 entity type names extract
	public void testConvModGenForZeroEntityTypes() throws IOException, TestEvalException {
		
		String entityType = "valid-definitions-empty.json";
		String docTitle = getDocTitle(entityType);		
		boolean test = false;
		
		//Validating generated extract instances of entity type names. Should be 0
		EvalResultIterator results =  eval("map:keys(map:get(es:entity-type-from-node( doc('"+entityType+"') ), \"definitions\"))");
		EvalResult result = null;

		while (results.hasNext()) {
			result = results.next();
			test = getConversionValidationResult(entityType, docTitle + ":extract-instance-" + result.get(new StringHandle()));
		}
		if(test==true){
			fail("extract-instance should not be generated for this test. Check the input file: "+entityType);
		}
	
		//Validating generated functions. TODO uncomment the verification for source document
		getConversionValidationResult(entityType, docTitle+":instance-to-canonical-xml");
		getConversionValidationResult(entityType, docTitle+":instance-to-envelope");
		getConversionValidationResult(entityType, docTitle+":instance-from-document");
		getConversionValidationResult(entityType, docTitle+":instance-xml-from-document");
		getConversionValidationResult(entityType, docTitle+":instance-json-from-document");
		//getConversionValidationResult(initialTest, docTitle+":sources-from-document");
			
	}
	
	@Test
	//This test verifies that conversion module does not throw an error when an invalid ET( missing info section ) is input
	public void testConvModGenForInvalidET() throws TestEvalException, IOException {
		
		String entityType = "valid-missing-info.json";
		String[] res = null;
		try {
			res = storeConversionModuleAsXqy(entityType);
		if(res[0]!=null) {
			fail("Testing for conversion module with missing info section. Check the input ET doc file: "+entityType);
		} 
		} catch (Exception e) {
			logger.info("Got exception: "+e.getMessage());
		}
	
	}
	
	@Test
	public void testExtractInstance() throws IOException, TestEvalException {
		
		String entityType = "valid-ref-combo-sameDocument-subIri.xml";
		String sourceDocument = "10248.xml";
		String ns = getNameSpace(entityType);
		
		StringHandle handle = evalOneResult("import module namespace ext = \""+ns+"\" at \"/conv/"+entityType.replaceAll("\\.(xml|json)", ".xqy")+"\"; "+
		              "ext:extract-instance-Order( doc('"+sourceDocument+"') )", new StringHandle());
		
		String extractInstanceResult = handle.get();
		logger.info("This is the extracted instance: \n" + extractInstanceResult);
		assertNotNull("Extract Instance Result must not be null (and should not throw error) ", extractInstanceResult);
		
	}
	
	@Test
	public void testExtInstWithMalformedDoc() throws IOException, TestEvalException {
		
		String entityType = "valid-ref-combo-sameDocument-subIri.xml";
		String sourceDocument = "10248.xml";
		String ns = getNameSpace(entityType);
		
		StringHandle handle = evalOneResult("import module namespace ext = \""+ns+"\" at \"/conv/"+entityType.replaceAll("\\.(xml|json)", ".xqy")+"\"; "+
		              "ext:extract-instance-Order( doc('"+sourceDocument+"') )", new StringHandle());
		
		String extractInstanceResult = handle.get();
		logger.info("This is the extracted instance: \n" + extractInstanceResult);
		assertNotNull("Extract Instance Result must not be null (and should not throw error) ", extractInstanceResult);
		
	}
	
	@Test
	public void testExtInstWithNoArgs() throws IOException, TestEvalException {
		
		String entityType = "valid-ref-combo-sameDocument-subIri.xml";
		String ns = getNameSpace(entityType);
		try{
		StringHandle handle = evalOneResult("import module namespace ext = \""+ns+"\" at \"/conv/"+entityType.replaceAll("\\.(xml|json)", ".xqy")+"\"; "+
		              "ext:extract-instance-Order()", new StringHandle());
		
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Too few args, expected 1 but got 0"));
		}
		
	}
	
	@Test
	public void testExtInstWithTooManyArgs() throws IOException, TestEvalException {
		
		String entityType = "valid-ref-combo-sameDocument-subIri.xml";
		String source1 = "10248.xml";
		String source2 = "ANTON.xml";
		String ns = getNameSpace(entityType);

		try {
		StringHandle handle = evalOneResult("import module namespace ext = \""+ns+"\" at \"/conv/"+entityType.replaceAll("\\.(xml|json)", ".xqy")+"\"; "+
		              "ext:extract-instance-Order( doc('"+source1+"'),doc('"+source2+"'))", new StringHandle());
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Too many args, expected 1 but got 2"));
		}
		
	}
	/**
	 * Rationale for this test is that default generated conversion module should
	 * work out-of-the-box, and handle an identity transform from test instances.
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws TransformerException 
	 */
	/*
	@Test
	public void defaultModuleExtractsIdentity() throws TestEvalException, JsonProcessingException, IOException, SAXException, TransformerException {
		
		for (String entityType : entityTypes) {
			
			// just test JSON ones here.
			if (entityType.contains(".xml")) {continue; };
			
			//if (!entityType.equals("SchemaCompleteEntityType-0.0.1.json")) {continue;}
			storeConversionModuleAsXqy(entityType);
			String entityTypeTestFileName = entityType.replace(".json", "-0.xml");
			
			String entityTypeName = entityType.replace(".json",  "");
			
			InputStream is = this.getClass().getResourceAsStream("/json-entity-types/" + entityType);
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode controlFile = (ObjectNode) mapper.readTree(is);
			JsonNode baseUriNode = controlFile.get("info").get("baseUri");
			String baseUri = null;
			if (baseUriNode == null) {
				baseUri = "http://example.org/";
			} else {
				baseUri = baseUriNode.asText();
			}
			String uriPrefix = baseUri;
			if (!baseUri.matches(".*[#/]$")) {
				uriPrefix += "#";
			}
			String moduleName = "/ext/" + entityTypeName + ".xqy";
			String entityTypeNoVersion = entityTypeName.replaceAll("-.*$", "");
			
			try {
				
				DOMHandle handle = evalOneResult(
					"import module namespace conv = \""+uriPrefix + entityTypeName +"\" at \""+moduleName+"\"; "+
					"conv:instance-to-canonical-xml( conv:extract-instance-"+entityTypeNoVersion+"( doc('"+entityTypeTestFileName+"') ) )/"+entityTypeNoVersion, new DOMHandle());
				
				// dom returned from extraction must equal test instance.
				String controlFilePath = "/test-instances/" + entityTypeTestFileName;
				Document controlDom = builder.parse(this.getClass().getResourceAsStream(controlFilePath));
				
				Document actualInstance = handle.get();
				assertEquals("extract-canonical returns an instance", actualInstance.getDocumentElement().getLocalName(), entityTypeNoVersion);
				Element actualDocumentElement = actualInstance.getDocumentElement();
				
//				logger.debug("Control doc");
//				debugOutput(controlDom);
//				logger.debug("Actual doc wrapped");
//				debugOutput(actualInstance);
				
				XMLUnit.setIgnoreWhitespace(true);
				XMLAssert.assertXMLEqual("Extract instance by default returns identity", controlDom, actualInstance);
				
				
			} catch (TestEvalException e) {
				logger.warn("Exception thrown validating conversion module.  Maybe test conversion module cannot test " + entityTypeNoVersion);
				fail("Evaluation exception thrown during conversion module testing." + e.getMessage());
			}
			}
		
	} */
	
	
	
}