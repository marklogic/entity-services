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
import com.marklogic.client.io.StringHandle;

/**
 * Tests the server-side function es:echema-generate($entity-type) as
 * element(xsd:schema)
 * 
 * Stub - TODO implmement.
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
			if (entityType.contains(".xml")||entityType.contains(".jpg")||entityType.contains("valid-ref-same-document")||entityType.contains("valid-ref-combo")
					||entityType.contains("valid-datatype-array")||entityType.contains("valid-definitions-empty")) {
				continue;
			}

			logger.info("Generating schema: " + entityType);
			StringHandle schema = new StringHandle();
			try {
				schema = evalOneResult("es:schema-generate( es:entity-type-from-node( fn:doc( '" + entityType + "')))",
						schema);
			} catch (TestEvalException e) {
				throw new RuntimeException(e);
			}
			map.put(entityType, schema);
		}
		return map;
	}

	@Test
	public void verifySchemaValidation() throws TestEvalException, SAXException, IOException {

		for (String entityType : entityTypes) {
			if (entityType.contains(".xml")||entityType.contains(".jpg")||entityType.contains("valid-ref-same-document")||entityType.contains("valid-ref-combo")||entityType.contains("valid-datatype-array")
					||entityType.contains("valid-definitions-empty")||entityType.contains("valid-no-baseUri")||entityType.contains("valid-simple-ref")) {
				continue;
			}

			String testInstanceName = entityType.replaceAll("\\.(json|xml)$", "-0.xml");

			storeSchema(entityType, schemas.get(entityType));
			DOMHandle validateResult = evalOneResult("validate strict { doc('" + testInstanceName + "') }",
					new DOMHandle());
			removeSchema(entityType);

			InputStream is = this.getClass().getResourceAsStream("/test-instances/" + testInstanceName);
			Document filesystemXML = builder.parse(is);
			XMLUnit.setIgnoreWhitespace(true);
			XMLAssert.assertXMLEqual("Must be no validation errors for schema " + entityType + ".", filesystemXML,
					validateResult.get());
			
		}

	}

	@AfterClass
	public static void cleanupSchemas() {
		for (String entityType : schemas.keySet()) {

			String moduleName = "/ext/" + entityType.replaceAll("\\.(xml|json)", ".xsd");

			docMgr.delete(moduleName);
		}
	}
}
