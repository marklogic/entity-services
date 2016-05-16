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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

public class TestExtractionTemplates extends EntityServicesTestBase {
	
	private static XMLDocumentManager docMgr;
	private static Map<String, StringHandle> extractionTemplates;
	private static final String TDE_COLLECTION = "http://marklogic.com/xdmp/tde";
	
	@BeforeClass
	public static void setupClass() {
		setupClients();

		// extraction tempmlates go in schemas db.
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
			if (entityType.contains(".xml")) {continue; };
			
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
	public void testExtractionTemplates() {
		for (String entityType : entityTypes) {
			if (entityType.contains(".xml")) {continue; };
			String schemaName = entityType.replaceAll("-.*$", "");
			logger.info("Validating extraction template: " + entityType);
			JacksonHandle template = new JacksonHandle();
			try {
				template = evalOneResult("tde:get-view( '"+schemaName+"', '"+schemaName+"')", template);
			} catch (TestEvalException e) {
                // one test, with no properties, creates an invalid TDE template.
                // we accept that.
                if (schemaName.equals("NoProperties")) continue;
				fail("View " + schemaName + " didn't exist");
			}
			JsonNode schemaJson = template.get();
			
			JsonNode body = schemaJson.get("view");
			assertEquals("View name", schemaName, body.get("name").asText());
			assertTrue("View has columns", body.get("columns").isArray());
		}
	}
	
	@Test
	public void templateRowMustDropArray() throws SAXException, IOException, XpathException {
		
		// this one has an array of refs
		String entityTypeWithArray = "Order-0.0.4.json";
		String arrayEntityType = extractionTemplates.get(entityTypeWithArray).get();
		
		
		Map<String, String> ctx = new HashMap<String, String>();
		ctx.put("tde", "http://marklogic.com/xdmp/tde");
		
		XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(ctx));
		XMLAssert.assertXpathExists("//tde:row[tde:view-name='Order_hasOrderDetails']", arrayEntityType);
	    XMLAssert.assertXpathNotExists("//tde:row[tde:view-name='Order']//tde:column[tde:name='hasOrderDetails']", arrayEntityType);
	    
	    // check scalar array
	    arrayEntityType = extractionTemplates.get("SchemaCompleteEntityType-0.0.1.json").get();
	    
		XMLAssert.assertXpathExists("//tde:row[tde:view-name='SchemaCompleteEntityType_arrayKey']", arrayEntityType);
	    XMLAssert.assertXpathNotExists("//tde:row[tde:view-name='SchemaCompleteEntityType']//tde:column[tde:name='arrayKey']", arrayEntityType);
	    
	}
	
	@Test
	public void embedChildWithNoPrimaryKey() throws XpathException, IOException, SAXException {
		// this one has an array of refs
		String entityTypeWithArray = "Order-0.0.4.json";
		String arrayEntityType = extractionTemplates.get(entityTypeWithArray).get();
		
		Map<String, String> ctx = new HashMap<String, String>();
		ctx.put("tde", "http://marklogic.com/xdmp/tde");
		//logger.debug(arrayEntityType);

		
		XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(ctx));
		XMLAssert.assertXpathExists("//tde:row[tde:view-name='Order_hasOrderDetails']//tde:column[tde:name='quantity']", arrayEntityType);
	    XMLAssert.assertXpathNotExists("//tde:row[tde:view-name='OrderDetails']", arrayEntityType);

	    // negative case -- ref with primary key in target
	    entityTypeWithArray = "Order-0.0.5.json";
		arrayEntityType = extractionTemplates.get(entityTypeWithArray).get();
		
		XMLAssert.assertXpathExists("//tde:row[tde:view-name='Order_hasOrderDetails']", arrayEntityType);
	    XMLAssert.assertXpathNotExists("//tde:row[tde:view-name='Order_hasOrderDetails']//tde:column[tde:name='quantity']", arrayEntityType);
	    XMLAssert.assertXpathExists("//tde:row[tde:view-name='OrderDetails']", arrayEntityType);

	    
	}
	

	@AfterClass
	public static void removeTemplates() {
		for (String template : extractionTemplates.keySet()) {
			
			String templateName = template.replaceAll("\\.(xml|json)", ".tdex");
			
			docMgr.delete(templateName);
		}
	}
}
