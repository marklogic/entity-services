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

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.semantics.SPARQLQueryManager;

public class TestEntityTypeSPARQL extends EntityServicesTestBase {

	private static SPARQLQueryManager queryMgr;
	
	@BeforeClass
	public static void setup() {
		setupClients();
		TestSetup.getInstance().loadEntityTypes("/json-entity-types", "SchemaCompleteEntityType-0.0.1.json");
		queryMgr = client.newSPARQLQueryManager();
	}
	@Test
	public void sampleSPARQLQueries() throws JsonGenerationException, JsonMappingException, IOException {
		String docHasTypeHasPropertyHasDatatype = 
					 "PREFIX t: <http://marklogic.com/testing-entity-type#> "
					+"PREFIX es: <http://marklogic.com/entity-services#> "
					+ "ASK where { t:SchemaCompleteEntityType-0.0.1 a es:EntityServicesDocument ; "
					+ "   es:definitions ?types ."
					+ "?types es:property ?property ."
					+ "?property es:datatype ?datatype "
					+ "}";
		
		assertTrue(queryMgr.executeAsk(queryMgr.newQueryDefinition(docHasTypeHasPropertyHasDatatype)));
		
		String assertTypeHasProperties =  "PREFIX t: <http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.1/> "
				+"PREFIX es: <http://marklogic.com/entity-services#> "
				+"SELECT ?version where {"
				+ "t:SchemaCompleteEntityType ?version \"0.0.1\" ."
				+ "}";
		
		JacksonHandle handle = queryMgr.executeSelect(queryMgr.newQueryDefinition(assertTypeHasProperties), new JacksonHandle());
		JsonNode results = handle.get();
		
		// to see on System.out
		// new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, results);
		
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		assertEquals(1, bindings.size());
		assertEquals("http://marklogic.com/entity-services#version", bindings.get(0).get("version").get("value").asText());
		
	}
	
}
