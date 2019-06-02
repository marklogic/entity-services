/*
 * Copyright 2016-2018 MarkLogic Corporation
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


import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.AbstractReadHandle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
		TestSetup.getInstance().storeCustomSchema();

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
			if (entityType.contains(".json")||entityType.contains(".jpg")||entityType.contains("namespace")) {
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
	
	private void validateSchema(String entityType, String testInstanceName, String imports) throws SAXException, IOException {
		
		logger.info("Validating for instance: "+testInstanceName);
		try{
		DOMHandle validateResult = evalOneResult(imports, "validate strict { doc('" + testInstanceName + "') }", new DOMHandle());
			
		InputStream is = this.getClass().getResourceAsStream("/test-instances/" + testInstanceName);
		Document filesystemXML = builder.parse(is);
		assertThat("Must be no validation errors for schema " + entityType + ".",
               validateResult.get(),
               CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
		
		}catch (TestEvalException e) {
			throw new RuntimeException("Error validating "+entityType,e);
		}
	}

	@Test
	public void verifySchemaValidation() throws TestEvalException, SAXException, IOException {

		for (String entityType : entityTypes) {
			if (entityType.contains(".json")||entityType.contains(".jpg")||entityType.contains("namespace")||entityType.contains("pii")) {
				continue;
			}
			
			StringHandle instances = evalOneResult("","count(map:keys(map:get(es:model-validate( doc('"+entityType+"') ), \"definitions\")))", new StringHandle());
			//logger.info("Count of definitions is: "+Integer.valueOf(instances.get()));
			
			storeSchema(entityType, schemas.get(entityType));
			
			for(int i=0;i<Integer.valueOf(instances.get());i++) {
				
				String testInstanceName = entityType.replaceAll("\\.(json|xml)$", "-"+i+".xml");
				validateSchema(entityType, testInstanceName, "import schema \"\" at \""+entityType.replaceAll("\\.(xml|json)", ".xsd")+"\";\n");
			}
			docMgr.delete(entityType.replaceAll("\\.(xml|json)", ".xsd"));
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
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("Invalid model: Type names and property names must be distinct ('OrderDetails')"));
		}
		
		try {
			evalOneResult("", "es:schema-generate( es:model-validate(fn:doc('invalid-bug40766.xml')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for schema-generate() when ET and prop names are not distinct");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("Invalid model: Type names and property names must be distinct ('OrderDetails')"));
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
	
	@Test
	public void test1NamespaceSchema() throws SAXException, IOException {
		
		String entityType = "valid-1-namespace.xml";
		
		validateSchema(entityType, "valid-1-namespace-0.xml", "import schema 'http://marklogic.com/customer' at 'valid-1-namespace-cust.xsd';");
		validateSchema(entityType, "valid-1-namespace-1.xml", "import schema '' at 'valid-1-namespace.xsd';");
		validateSchema(entityType, "valid-1-namespace-2.xml", "import schema '' at 'valid-1-namespace.xsd';");
		validateSchema(entityType, "valid-1-namespace-3.xml", "import schema '' at 'valid-1-namespace.xsd';");
		
		docMgr.delete("valid-1-namespace.xsd","valid-1-namespace-cust.xsd");
	}
	
	@Test
	public void test2NamespaceSchema() throws SAXException, IOException {

		String entityType = "valid-2-namespace.xml";
		
		validateSchema(entityType, "valid-2-namespace-0.xml", "import schema '' at 'valid-2-namespace.xsd';");
		validateSchema(entityType, "valid-2-namespace-1.xml", "import schema '' at 'valid-2-namespace.xsd';");
		validateSchema(entityType, "valid-2-namespace-2.xml", "import schema 'http://marklogic.com/order' at 'valid-2-namespace-ord.xsd';");
		validateSchema(entityType, "valid-2-namespace-3.xml", "import schema '' at 'valid-2-namespace.xsd';");
		validateSchema(entityType, "valid-2-namespace-4.xml", "import schema 'http://marklogic.com/super' at 'valid-2-namespace-sup.xsd';");
		validateSchema(entityType, "valid-2-namespace-5.xml", "import schema '' at 'valid-2-namespace.xsd';");
		
		docMgr.delete("valid-2-namespace.xsd","valid-2-namespace-ord.xsd","valid-2-namespace-sup.xsd");		
	}

	//@AfterClass
	public static void cleanupSchemas() {
		for (String entityType : schemas.keySet()) {

			String moduleName = entityType.replaceAll("\\.(xml|json)", ".xsd");

			docMgr.delete(moduleName);
		}
	}
}
