package com.marklogic.entityservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

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
			if (entityType.contains(".xml")||entityType.contains(".jpg")) {continue; };
			
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
	
	

	@AfterClass
	public static void removeTemplates() {
		for (String template : extractionTemplates.keySet()) {
			
			String templateName = template.replaceAll("\\.(xml|json)", ".tdex");
			
			docMgr.delete(templateName);
		}
	}
}
