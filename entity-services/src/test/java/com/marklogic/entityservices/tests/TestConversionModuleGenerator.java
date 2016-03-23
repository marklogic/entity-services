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
package com.marklogic.entityservices.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.StringHandle;

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
public class TestConversionModuleGenerator extends EntityServicesTestBase {
	
	private static TextDocumentManager docMgr;
	
	@BeforeClass
	public static void setupClass() {
		setupClients();
		// save xquery module to modules database
		docMgr = modulesClient.newTextDocumentManager();	
	}
	
	private void storeConversionModules(Map<String, StringHandle> moduleMap) throws TestEvalException {
		DocumentWriteSet writeSet = docMgr.newWriteSet();
		
		for (String entityTypeName : moduleMap.keySet()) {
			
			String moduleName = "/ext/" + entityTypeName.replaceAll("\\.(xml|json)", ".xqy");
			writeSet.add(moduleName, moduleMap.get(entityTypeName));
		}
		docMgr.write(writeSet);
	}
	
	@Test
	public void verifyCreateValidModule() throws TestEvalException {
		
		String initialTest = "Order-0.0.1.xml";
		StringHandle moduleHandle =  evalOneResult("es:conversion-module-generate( es:entity-type-from-node( fn:doc( '"+initialTest+"')))", new StringHandle());
		HashMap<String, StringHandle> m = new HashMap<String, StringHandle>();
		m.put(initialTest, moduleHandle);
		// save conversion module into modules database
		storeConversionModules(m);
		
		String instanceDocument = "Order-Source-1.xml";
		
		StringHandle handle = evalOneResult("import module namespace conv = \"http:///Order-0.0.1\" at \"/ext/Order-0.0.1.xqy\"; "+
		              "conv:extract-instance-Order( doc('"+instanceDocument+"') )", new StringHandle());
		
		String extractInstanceResult = handle.get();
		assertNotNull("Extract Instance Result must not be null (and should not throw error) ", extractInstanceResult);
		
	}
	
	/**
	 * Rationale for this test is that default generated conversion module should
	 * work out-of-the-box, and handle an identity transform from test instances.
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws TransformerException 
	 */
	@Test
	public void defaultModuleExtractsIdentity() throws TestEvalException, JsonProcessingException, IOException, SAXException, TransformerException {
		
		Map<String, StringHandle> conversionModules = generateConversionModules();
		storeConversionModules(conversionModules);
		
		// test them all adn remove
		for (String entityType : conversionModules.keySet()) {
			
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
		
	
		for (String entityType : conversionModules.keySet()) {
			
			String moduleName = "/ext/" + entityType.replaceAll("\\.(xml|json)", ".xqy");
			
			docMgr.delete(moduleName);
		}
	
	}

	private Map<String, StringHandle> generateConversionModules() throws TestEvalException {
		Map<String, StringHandle> map = new HashMap<String, StringHandle>();
		
		for (String entityType : entityTypes) {
			if (entityType.contains(".xml")) {continue; };
			
			logger.info("Generating conversion module: " + entityType);
			StringHandle xqueryModule = new StringHandle();
			xqueryModule = evalOneResult("es:conversion-module-generate( es:entity-type-from-node( fn:doc( '"+entityType+"')))", xqueryModule);
			map.put(entityType, xqueryModule);
		}
		return map;
	}
}
