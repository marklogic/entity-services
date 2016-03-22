package com.marklogic.entityservices.tests;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.io.JacksonHandle;

public class TestDatabaseProperties extends EntityServicesTestBase {
	
	@BeforeClass
	public static void setup() {
		setupClients();
	}
	
	@Test
	public void testDatabasePropertiesGenerate() throws TestEvalException {
		String entityType = "SchemaCompleteEntityType-0.0.1.json";
		
		JacksonHandle handle = evalOneResult("es:database-properties-generate(es:entity-type-from-node(fn:doc('"+entityType+"')))", new JacksonHandle());
		JsonNode databaseConfiguration = handle.get();
		
		logger.debug(databaseConfiguration.toString());
		
	}
	
}
