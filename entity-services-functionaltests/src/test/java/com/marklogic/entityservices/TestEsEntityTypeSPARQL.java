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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.semantics.SPARQLQueryManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestEsEntityTypeSPARQL extends EntityServicesTestBase {

    private static SPARQLQueryManager queryMgr;
	
	@BeforeClass
	public static void setup() {
		setupClients();
		queryMgr = client.newSPARQLQueryManager();
	}
	@Test
	public void sampleSPARQLQuery1() throws JsonGenerationException, JsonMappingException, IOException {
		String docHasTypeHasPropertyHasDatatype = 
					 "PREFIX t: <http://marklogic.com/testing-entity-type#> "
					+"PREFIX es: <http://marklogic.com/entity-services#> "
					+ "ASK where { t:SchemaCompleteEntityType-0.0.1 a es:Model ; "
					+ "   es:definitions ?types ."
					+ "?types es:property ?property ."
					+ "?property es:datatype ?datatype "
					+ "}";
		
		assertTrue(queryMgr.executeAsk(queryMgr.newQueryDefinition(docHasTypeHasPropertyHasDatatype)));
	}
	@Test
	public void sampleSPARQLQuery2() throws JsonGenerationException, JsonMappingException, IOException {
		String assertTypeHasProperties =  "PREFIX t: <http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.1/> "
				+"PREFIX es: <http://marklogic.com/entity-services#> "
				+"SELECT ?version where {"
				+ "t:SchemaCompleteEntityType ?version \"0.0.1\" ."
				+ "}";
		
		JacksonHandle handle = queryMgr.executeSelect(queryMgr.newQueryDefinition(assertTypeHasProperties), new JacksonHandle());
		JsonNode results = handle.get();
		
		// to see on System.out
		//new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, results);
		
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		assertEquals(1, bindings.size());
		
		assertEquals("http://marklogic.com/entity-services#version", bindings.get(0).get("version").get("value").asText());
	}
	
	@Test
	public void testSPARQLEntityTypeDoc() throws JsonGenerationException, JsonMappingException, IOException {
		
		// This test verifies that EntityType doc SchemaCompleteEntityType-0.0.1 has all the ET in it and version and title
		String assertTypeDocHasRDFType =  "PREFIX t: <http://marklogic.com/testing-entity-type#> "
				+"SELECT ?p ?o "
				+"WHERE {  t:SchemaCompleteEntityType-0.0.1 ?p ?o    }"
				+"order by ?s";
				
		
		JacksonHandle handle2= queryMgr.executeSelect(queryMgr.newQueryDefinition(assertTypeDocHasRDFType), new JacksonHandle());
		JsonNode results2 = handle2.get();
		// to see on System.out
		//new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, results2);
	
		ArrayNode bindings2 = (ArrayNode) results2.get("results").get("bindings");
		//logger.info(bindings2.toString());
		assertEquals(6, bindings2.size());
		// Verify that Entity type doc has RDF type in it.
		assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", bindings2.get(0).get("p").get("value").asText());
		assertEquals("http://marklogic.com/entity-services#Model", bindings2.get(0).get("o").get("value").asText());
		
		// Verify that Entity type doc has EnityType OrderDetails in it.
		assertEquals("http://marklogic.com/entity-services#definitions", bindings2.get(1).get("p").get("value").asText());
		assertEquals("http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.1/OrderDetails", bindings2.get(1).get("o").get("value").asText());
		
		// Verify that Entity type doc has EnityType OrderDetails in it.
		assertEquals("http://marklogic.com/entity-services#definitions", bindings2.get(2).get("p").get("value").asText());
		assertEquals("http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.1/SchemaCompleteEntityType", bindings2.get(2).get("o").get("value").asText());
         
		// Verify that Entity type doc has version in it.
		assertEquals("http://marklogic.com/entity-services#version", bindings2.get(3).get("p").get("value").asText());
		assertEquals("0.0.1", bindings2.get(3).get("o").get("value").asText());
		
		// Verify that Entity type doc has title in it.
		assertEquals("http://marklogic.com/entity-services#description", bindings2.get(4).get("p").get("value").asText());
		assertEquals("All Schema Elements represented in this type.  Collations and datatypes are all happy-path and valid.", bindings2.get(4).get("o").get("value").asText());
				
		// Verify that Entity type doc has title in it.
		assertEquals("http://marklogic.com/entity-services#title", bindings2.get(5).get("p").get("value").asText());
		assertEquals("SchemaCompleteEntityType", bindings2.get(5).get("o").get("value").asText());
		     
	}
	
	@Test
	public void testSPARQLEntityType() throws JsonGenerationException, JsonMappingException, IOException {
		
		// This test verifies that EntityType doc SchemaCompleteEntityType-0.0.1 has all the ET in it and version and title
		String assertEachEntityTypeHasProperties =  "PREFIX t: <http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.2/>"
				+"SELECT ?p ?o "
				+"WHERE {  t:OrderDetails ?p ?o    }"
				+"order by ?s";
				
		JacksonHandle handle= queryMgr.executeSelect(queryMgr.newQueryDefinition(assertEachEntityTypeHasProperties), new JacksonHandle());
		JsonNode results = handle.get();
		// to see on System.out
		//new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, results);
	
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		assertEquals(6, bindings.size());
		//Each Entity Type has a RDF type
		assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", bindings.get(0).get("p").get("value").asText());
		assertEquals("http://marklogic.com/entity-services#EntityType", bindings.get(0).get("o").get("value").asText());
		
		//Entity type has property ProductName
		assertEquals("http://marklogic.com/entity-services#property", bindings.get(1).get("p").get("value").asText());
		assertEquals("http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.2/OrderDetails/productName", bindings.get(1).get("o").get("value").asText());
		
		//Entity type has primaryKey quantity
		assertEquals("http://marklogic.com/entity-services#primaryKey", bindings.get(2).get("p").get("value").asText());
		assertEquals("http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.2/OrderDetails/quantity", bindings.get(2).get("o").get("value").asText());
     
		//Entity type has property quantity
	    assertEquals("http://marklogic.com/entity-services#property", bindings.get(3).get("p").get("value").asText());
	    assertEquals("http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.2/OrderDetails/quantity", bindings.get(3).get("o").get("value").asText());
	
	   //Entity type has version
	    assertEquals("http://marklogic.com/entity-services#version", bindings.get(4).get("p").get("value").asText());
	    assertEquals("0.0.2", bindings.get(4).get("o").get("value").asText());
	    
	    //Entity type has title
	    assertEquals("http://marklogic.com/entity-services#title", bindings.get(5).get("p").get("value").asText());
	    assertEquals("OrderDetails", bindings.get(5).get("o").get("value").asText());
	
      }
	
	@Test
	public void testSPARQLProperty() throws JsonGenerationException, JsonMappingException, IOException {
		
		// This test verifies that property has RDFtype,title and data type
		String assertPropertyHasRDFtitledatatype =  "PREFIX t: <http://marklogic.com/testing-entity-type/SchemaCompleteEntityType-0.0.1/SchemaCompleteEntityType/>"
				+"SELECT ?p ?o "
				+"WHERE { t:base64BinaryKey ?p ?o }"
				+"order by ?s";
				
	
		JacksonHandle handle= queryMgr.executeSelect(queryMgr.newQueryDefinition(assertPropertyHasRDFtitledatatype), new JacksonHandle());
		JsonNode results = handle.get();
		// to see on System.out
		//new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, results);
	
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		assertEquals(3, bindings.size());
		//Property has RDF type
		assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", bindings.get(0).get("p").get("value").asText());
		assertEquals("http://marklogic.com/entity-services#Property", bindings.get(0).get("o").get("value").asText());
		
		//Property has data type 
		assertEquals("http://marklogic.com/entity-services#datatype", bindings.get(1).get("p").get("value").asText());
		assertEquals("http://www.w3.org/2001/XMLSchema#base64Binary", bindings.get(1).get("o").get("value").asText());
     
		//Property has title
	    assertEquals("http://marklogic.com/entity-services#title", bindings.get(2).get("p").get("value").asText());
	    assertEquals("base64BinaryKey", bindings.get(2).get("o").get("value").asText());
	
      }
	
	@Test
	public void testSPARQLPropertyOrderId() throws JsonGenerationException, JsonMappingException, IOException {
		
		// This test verifies that property has RDFtype,title,rangeIndex, wordLexicon,required,title,version,description,collation and data type
		String assertPropertyHasRDFtitledatatype =  "PREFIX t: <http://marklogic.com/testing-entity-type/DBProp-Ref-Same-0.0.1/SchemaCompleteEntityType/>"
				+"SELECT ?p ?o "
				+"WHERE { t:orderId ?p ?o }"
				+"order by ?s";
					
		JacksonHandle handle= queryMgr.executeSelect(queryMgr.newQueryDefinition(assertPropertyHasRDFtitledatatype), new JacksonHandle());
		JsonNode results = handle.get();
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		
		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-sparql/testSPARQLPropertyOrderId.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);
		
		assertEquals(control, bindings);
	}
	
	@Test
	public void testModel1Namespace() throws JsonGenerationException, JsonMappingException, IOException {
		
		// This test verifies that property has RDFtype,title,rangeIndex, wordLexicon,required,title,version,description,collation and data type
		String query =  "PREFIX t: <http://marklogic.com/ns1/Model_1ns-0.0.1/>"
				+"SELECT ?p ?o WHERE { t:Customer ?p ?o } order by ?s";
					
		JacksonHandle handle= queryMgr.executeSelect(queryMgr.newQueryDefinition(query), new JacksonHandle());
		JsonNode results = handle.get();
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		
		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-sparql/testModel1Namespace.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);
		
		assertEquals(control, bindings);
	}
	
	@Test
	public void testModel2NamespaceOrder() throws JsonGenerationException, JsonMappingException, IOException {
		
		// This test verifies that property has RDFtype,title,rangeIndex, wordLexicon,required,title,version,description,collation and data type
		String query =  "PREFIX t: <http://marklogic.com/ns2/Model_2ns-0.0.1/>"
				+"SELECT ?p ?o WHERE { t:Order ?p ?o }	order by ?s";
					
		JacksonHandle handle= queryMgr.executeSelect(queryMgr.newQueryDefinition(query), new JacksonHandle());
		JsonNode results = handle.get();
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		
		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-sparql/testModel2NamespaceOrder.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);
		
		assertEquals(control, bindings);
	}
	
	@Test
	public void testModel2NamespaceSuper() throws JsonGenerationException, JsonMappingException, IOException {
		
		// This test verifies that property has RDFtype,title,rangeIndex, wordLexicon,required,title,version,description,collation and data type
		String query =  "PREFIX t: <http://marklogic.com/ns2/Model_2ns-0.0.1/>"
				+"SELECT ?p ?o WHERE { t:Superstore ?p ?o }	order by ?s";
					
		JacksonHandle handle= queryMgr.executeSelect(queryMgr.newQueryDefinition(query), new JacksonHandle());
		JsonNode results = handle.get();
		ArrayNode bindings = (ArrayNode) results.get("results").get("bindings");
		
		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-sparql/testModel2NamespaceSuper.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);
		
		assertEquals(control, bindings);
	}

}
