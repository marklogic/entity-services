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

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

import javax.xml.transform.TransformerException;

public class TestEsExtractionTemplates extends EntityServicesTestBase {
	
	private static XMLDocumentManager docMgr;
	private static Map<String, StringHandle> extractionTemplates;
	private static final String TDE_COLLECTION = "http://marklogic.com/xdmp/tde";
	
	@BeforeClass
	public static void setupClass() {
		setupClients();

		// extraction templates go in schemas db.
		docMgr = schemasClient.newXMLDocumentManager();	
		
		extractionTemplates = generateExtractionTemplates();
		storeExtractionTempates(extractionTemplates);
	}
	
	
	private static void storeExtractionTempates(Map<String, StringHandle> templateMap) {
		DocumentWriteSet writeSet = docMgr.newWriteSet();
		
		for (String entityTypeName : templateMap.keySet()) {
			
			String moduleName = entityTypeName.replaceAll("\\.(xml|json)", ".tdex");
			DocumentMetadataHandle metadata = new DocumentMetadataHandle().withCollections(TDE_COLLECTION);
			writeSet.add(moduleName, metadata, templateMap.get(entityTypeName));
		}
		docMgr.write(writeSet);
	}
	
	private static Map<String, StringHandle> generateExtractionTemplates() {
		Map<String, StringHandle> map = new HashMap<String, StringHandle>();
		
		for (String entityType : entityTypes) {
			if (entityType.contains(".json")||entityType.contains(".jpg")) {continue; };
			
			logger.info("Generating extraction template: " + entityType);
			StringHandle template = new StringHandle();
			try {
				template = evalOneResult("es:entity-type-from-node( fn:doc( '"+entityType+"'))=>es:extraction-template-generate()", template);
			} catch (TestEvalException e) {
				throw new RuntimeException(e);
			}
			map.put(entityType, template);
		}
		return map;
	}
	
	@Test
	public void test1ExtractionTemplates() {
		
		    String entityType = "SchemaCompleteEntityType-0.0.1.json";
		    String schemaName = "SchemaCompleteEntityType";
			logger.info("Validating extraction template: " + entityType);
			JacksonHandle template = new JacksonHandle();
			try {
				template = evalOneResult("tde:get-view( '"+schemaName+"', '"+schemaName+"')", template);
			} catch (TestEvalException e) {
				fail("View " + schemaName + " didn't exist");
			}
			JsonNode schemaJson = template.get();
			
			JsonNode body = schemaJson.get("view");
			//logger.info("View body of view-name SchemaCompleteEntityType"+ body);
			//logger.info("View name  :::"+schemaName+"    Result :::"+body.get("name").asText());
			//logger.info("View has columns :::"+"Result ::::"+ body.get("columns"));
			
			assertEquals("View name", schemaName, body.get("name").asText());
			assertTrue("View has columns", body.get("columns").isArray());
			
		
		    String schemaName2 = "SchemaCompleteEntityType_arrayreferenceInThisFile";
			//logger.info("Validating extraction template: " + entityType);
			JacksonHandle template2 = new JacksonHandle();
			try {
				template2 = evalOneResult("tde:get-view( '"+schemaName+"', '"+schemaName2+"')", template2);
			} catch (TestEvalException e) {
				fail("View " + schemaName2 + " didn't exist");
			}
			JsonNode schemaJson2 = template2.get();
			
			JsonNode body2 = schemaJson2.get("view");
			//logger.info("View body of view-name SchemaCompleteEntityType"+ body2);
			//logger.info("View name  :::"+schemaName2+"    Result :::"+body2.get("name").asText());
			//logger.info("View has columns :"+ body2.get("columns"));
			
			assertEquals("View name", schemaName2, body2.get("name").asText());
			assertTrue("View has columns", body2.get("columns").isArray());
			
			String schemaName3 = "SchemaCompleteEntityType_externalArrayReference";
			//logger.info("Validating extraction template: " + entityType);
			JacksonHandle template3 = new JacksonHandle();
			try {
				template3 = evalOneResult("tde:get-view( '"+schemaName+"', '"+schemaName3+"')", template3);
			} catch (TestEvalException e) {
				fail("View " + schemaName3 + " didn't exist");
			}
			JsonNode schemaJson3 = template3.get();
			
			JsonNode body3 = schemaJson3.get("view");
			//logger.info("View body of view-name SchemaCompleteEntityType"+ body3);
			//logger.info("View name  :::"+schemaName3+"    Result :::"+body3.get("name").asText());
			//logger.info("View has columns :"+ body3.get("columns"));
			
			assertEquals("View name", schemaName3, body3.get("name").asText());
			assertTrue("View has columns", body3.get("columns").isArray());
		
	}
	
	@Test
	public void test2ExtractionTemplates() {
		
		    String entityType = "SchemaCompleteEntityType-0.0.2.json";
		    String schemaName = "SchemaCompleteEntityType";
			logger.info("Validating extraction template: " + entityType);
			JacksonHandle template = new JacksonHandle();
			try {
				template = evalOneResult("tde:get-view( '"+schemaName+"', '"+schemaName+"')", template);
			} catch (TestEvalException e) {
				fail("View " + schemaName + " didn't exist");
			}
			JsonNode schemaJson = template.get();
			
			JsonNode body = schemaJson.get("view");
			//logger.info("View body :::"+"whole view body ::::"+ body);
			//logger.info("View name  :::"+schemaName+"    Result :::"+body.get("name").asText());
			//logger.info("View has columns :::"+"Result ::::"+ body.get("columns"));
			
			assertEquals("View name", schemaName, body.get("name").asText());
			assertTrue("View has columns", body.get("columns").isArray());
		
	}
	
	@Test
	public void verifyExtractionTemplateGenerate() throws TestEvalException, SAXException, IOException, TransformerException {

		for (String entityType : entityTypes) {
			if (entityType.contains(".json")||entityType.contains(".jpg")) { continue;}

			logger.info("Validating extraction template for:" + entityType);
			//logger.info(docMgr.read(entityType.replaceAll("\\.(xml|json)", ".tdex"), new StringHandle()).get());
			DOMHandle handle = docMgr.read(entityType.replaceAll("\\.(xml|json)", ".tdex"), new DOMHandle());
			Document template = handle.get();
			
			InputStream is = this.getClass().getResourceAsStream("/test-extraction-template/" + entityType);
			Document filesystemXML = builder.parse(is);


            //debugOutput(template);

			XMLUnit.setIgnoreWhitespace(true);
			XMLAssert.assertXMLEqual("Must be no validation errors for schema " + entityType + ".", filesystemXML,
					template);
			
		}

	}
	

	@AfterClass
	public static void removeTemplates() {
		for (String template : extractionTemplates.keySet()) {
			
			String templateName = template.replaceAll("\\.(xml|json)", ".tdex");
			
			docMgr.delete(templateName);
		}
	}
}
