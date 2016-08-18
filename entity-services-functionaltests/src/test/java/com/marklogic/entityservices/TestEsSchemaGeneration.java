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


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

/**
 * Tests the server-side function es:echema-generate($model) as
 * element(xsd:schema)
 * 
 * Stub - TODO implement.
 *
 */
public class TestEsSchemaGeneration extends EntityServicesTestBase {

	private static XMLDocumentManager docMgr;
	private static Map<String, StringHandle> schemas;

	@BeforeClass
	public static void setupClass() {
		setupClients();

		docMgr = schemasClient.newXMLDocumentManager();
		schemas = generateSchemas();
	}

	private static void storeSchema(String entityTypeName, StringHandle schemaHandle) {
		logger.debug("Loading schema " + entityTypeName);
		String moduleName = entityTypeName.replaceAll("\\.(xml|json)", ".xsd");
		docMgr.write(moduleName, schemaHandle);
	}

	private static void removeSchema(String entityTypeName) {
		logger.debug("Removing schema " + entityTypeName);
		String moduleName = entityTypeName.replaceAll("\\.(xml|json)", ".xsd");
		docMgr.delete(moduleName);
	}

	private static Map<String, StringHandle> generateSchemas() {
		Map<String, StringHandle> map = new HashMap<String, StringHandle>();

		for (String entityType : entityTypes) {
			if (entityType.contains(".json")||entityType.contains(".jpg")||entityType.contains("valid-ref-same-document")
					||entityType.contains("valid-ref-combo")||entityType.contains("valid-simple-ref")) {
				continue;
			}

			logger.info("Generating schema: " + entityType);
			StringHandle schema = new StringHandle();
			try {
				schema = evalOneResult("es:schema-generate( es:model-from-xml( fn:doc( '" + entityType + "')))",
						schema);
			} catch (TestEvalException e) {
				throw new RuntimeException(e+"for "+entityType);
			}
			map.put(entityType, schema);
		}
		return map;
	}

	@Test
	public void verifySchemaValidation() throws TestEvalException, SAXException, IOException {

		for (String entityType : entityTypes) {
			// primary-key-as-a-ref.xml is commented for bug 40666
			// valid-ref-value-as-nonString.json is commented for bug 40904
			if (entityType.contains(".json")||entityType.contains(".jpg")||entityType.contains("valid-ref-same-document")
					||entityType.contains("valid-ref-combo")||entityType.contains("valid-simple-ref")||
					entityType.contains("primary-key-as")||entityType.contains("valid-ref-value")) {
				continue;
			}
			
			StringHandle instances = evalOneResult("count(map:keys(map:get(es:model-validate( doc('"+entityType+"') ), \"definitions\")))",new StringHandle());
			//logger.info("Count of definitions is: "+Integer.valueOf(instances.get()));
			
			storeSchema(entityType, schemas.get(entityType));
			
			for(int i=0;i<Integer.valueOf(instances.get());i++) {
			
				String testInstanceName = entityType.replaceAll("\\.(json|xml)$", "-"+i+".xml");
				logger.info("Validating for instance: "+testInstanceName);
				try{
				DOMHandle validateResult = evalOneResult("validate strict { doc('" + testInstanceName + "') }",
					new DOMHandle());
				
				InputStream is = this.getClass().getResourceAsStream("/test-instances/" + testInstanceName);
				Document filesystemXML = builder.parse(is);
				XMLUnit.setIgnoreWhitespace(true);
				XMLAssert.assertXMLEqual("Must be no validation errors for schema " + entityType + ".", filesystemXML,
						validateResult.get());
				
				}catch (TestEvalException e) {
					throw new RuntimeException("Error validating "+entityType,e);
				}	
			}
			
			removeSchema(entityType);

		}

	}
	
	@Test
	/* Test for bug 40766 to verify error msg when ET and property names are not distinct*/
	public void bug40766SchemaGen() {
		logger.info("Checking schema-generate() when ET and prop names are not distinct");
		try {
			evalOneResult("es:schema-generate( es:model-validate(fn:doc('invalid-bug40766.json')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for schema-generate() when ET and prop names are not distinct");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Type names and property names must be distinct."));
		}
		
		try {
			evalOneResult("es:schema-generate( es:model-validate(fn:doc('invalid-bug40766.xml')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for schema-generate() when ET and prop names are not distinct");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Type names and property names must be distinct."));
		}
	}

	@AfterClass
	public static void cleanupSchemas() {
		for (String entityType : schemas.keySet()) {

			String moduleName = entityType.replaceAll("\\.(xml|json)", ".xsd");

			docMgr.delete(moduleName);
		}
	}
}
