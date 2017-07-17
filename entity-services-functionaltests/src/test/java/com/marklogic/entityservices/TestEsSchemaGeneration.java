/*
 * Copyright 2016-2017 MarkLogic Corporation
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


import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
import org.xmlunit.matchers.CompareMatcher;

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

	private static Map<String, StringHandle> generateSchemas() {
		Map<String, StringHandle> map = new HashMap<String, StringHandle>();

		for (String entityType : entityTypes) {
			if (entityType.contains(".json")||entityType.contains(".jpg")) {
				continue;
			}

			logger.info("Generating schema: " + entityType);
			StringHandle schema = new StringHandle();
			try {
				schema = evalOneResult("",
						"es:schema-generate( es:model-from-xml( fn:doc( '" + entityType + "')))", schema);
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
			if (entityType.contains(".json")||entityType.contains(".jpg")) {
				continue;
			}
			
			StringHandle instances = evalOneResult("","count(map:keys(map:get(es:model-validate( doc('"+entityType+"') ), \"definitions\")))", new StringHandle());
			//logger.info("Count of definitions is: "+Integer.valueOf(instances.get()));
			
			storeSchema(entityType, schemas.get(entityType));
			
			for(int i=0;i<Integer.valueOf(instances.get());i++) {
			
				String testInstanceName = entityType.replaceAll("\\.(json|xml)$", "-"+i+".xml");
				logger.info("Validating for instance: "+testInstanceName);
				try{
				DOMHandle validateResult = evalOneResult("import schema \"\" at \""+entityType.replaceAll("\\.(xml|json)", ".xsd")+"\";\n",
					"validate strict { doc('" + testInstanceName + "') }", new DOMHandle());
				
				InputStream is = this.getClass().getResourceAsStream("/test-instances/" + testInstanceName);
				Document filesystemXML = builder.parse(is);
				assertThat("Must be no validation errors for schema " + entityType + ".",
                    validateResult.get(),
                    CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
				
				}catch (TestEvalException e) {
					throw new RuntimeException("Error validating "+entityType,e);
				}	
			}

		}

	}
	
	@Test
	/* Test for bug 40766 to verify error msg when ET and property names are not distinct*/
	public void bug40766SchemaGen() {
		logger.info("Checking schema-generate() when ET and prop names are not distinct");
		try {
			evalOneResult("", "es:schema-generate( es:model-validate(fn:doc('invalid-bug40766.json')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for schema-generate() when ET and prop names are not distinct");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Type names and property names must be distinct ('OrderDetails')"));
		}
		
		try {
			evalOneResult("", "es:schema-generate( es:model-validate(fn:doc('invalid-bug40766.xml')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for schema-generate() when ET and prop names are not distinct");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Type names and property names must be distinct ('OrderDetails')"));
		}
	}
	
	@Test
	/* Test for github issue #212 */
	public void bug43212SchemaGen() throws SAXException, IOException {
		logger.info("Checking schema-generate() when prop names across ET are not distinct");
		DOMHandle validateXML = evalOneResult("","es:schema-generate( es:model-from-xml(doc('invalid-bug43212.xml')))", new DOMHandle());
		//DOMHandle validateJSON = evalOneResult("","es:schema-generate( doc('invalid-bug43212.json'))", new DOMHandle());

		InputStream is = this.getClass().getResourceAsStream("/test-instances/bug43212.xml");
		Document filesystemXML = builder.parse(is);
		try {
			assertThat("Must be no validation errors for schema.",
                validateXML.get(),
                CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
			//assertThat("Must be no validation errors for schema.", filesystemXML, validateJSON.get());
		} catch (TestEvalException e) {
			throw new RuntimeException("Error validating test bug43212SchemaGen",e);
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
