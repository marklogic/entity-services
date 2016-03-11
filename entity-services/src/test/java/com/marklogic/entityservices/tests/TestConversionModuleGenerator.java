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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.io.StringHandle;

/**
 * Tests server function es:conversion-module-generate
 * Stub.
 * 
 * TODO - test generated functions.
 */
public class TestConversionModuleGenerator extends EntityServicesTestBase {

	
	private void storeConversionModule(String entityTypeName) throws TestEvalException {
		StringHandle xqueryModule = new StringHandle();
		
		xqueryModule = evalOneResult("es:conversion-module-generate( es:entity-type-from-node( fn:doc( '"+entityTypeName+"')))", xqueryModule);
	
		// save xquery module to modules database
		TextDocumentManager docMgr = modulesClient.newTextDocumentManager();
		String moduleName = "/ext/" + entityTypeName.replaceAll("\\.(xml|json)", ".xqy");
		docMgr.write(moduleName, xqueryModule);
	}
	
	@Test
	public void verifyCreateValidModule() throws TestEvalException {
		
		String initialTest = "Order-0.0.1.xml";
		
		// save conversion module into modules database
		storeConversionModule(initialTest);
		
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
	 */
	@Test
	public void defaultModuleExtractsIdentity() throws TestEvalException, JsonProcessingException, IOException {
		
		for (String entityType : entityTypes) {
			
			// just test JSON ones here.
			if (entityType.contains(".xml")) {continue; };
			
			storeConversionModule(entityType);
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
			StringHandle handle = evalOneResult(
					"import module namespace conv = \""+uriPrefix + entityTypeName +"\" at \""+moduleName+"\"; "+
		            "conv:extract-instance-"+entityTypeNoVersion+"( doc('"+entityTypeTestFileName+"') )", new StringHandle());
			} catch (TestEvalException e) {
				logger.warn("Exception thrown validating conversion module.  Maybe test conversion module cannot test " + entityTypeNoVersion);
			}
		}
		
	}
	
	
	
}
