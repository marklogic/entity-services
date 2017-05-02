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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.io.JacksonHandle;

public class TestEsDatabaseProperties extends EntityServicesTestBase {
	
	@BeforeClass
	public static void setup() {
		setupClients();
	}
	
	@Test
	public void testDatabasePropertiesGenerate() throws IOException, TestEvalException {
		String entityType = "SchemaCompleteEntityType-0.0.1.json";
		
		JacksonHandle handle = evalOneResult("", "es:database-properties-generate(fn:doc('"+entityType+"'))", new JacksonHandle());
		JsonNode databaseConfiguration = handle.get();
		
		//logger.debug(databaseConfiguration.toString());

		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-database-properties/content-database.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);

		org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(databaseConfiguration));
	}
	
	@Test
	public void testDBpropRangeIndexandwordLexicon() throws IOException, TestEvalException {
		String entityType = "valid-db-prop-et.json";
		
		JacksonHandle handle = evalOneResult("", "es:database-properties-generate(fn:doc('"+entityType+"'))", new JacksonHandle());
		JsonNode databaseConfiguration = handle.get();
		
		//logger.debug(databaseConfiguration.toString());

		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-database-properties/content-database2.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);
		assertEquals(control,databaseConfiguration);

		//org.hamcrest.MatcherAssert.assertThat("Expected: "+control+"\n\tGot: "+databaseConfiguration,control, org.hamcrest.Matchers.equalTo(databaseConfiguration));
	}
	
	@Test
	public void testDBpropRefSame() throws IOException, TestEvalException {
		String entityType = "valid-simple-ref.json";
		
		JacksonHandle handle = evalOneResult("", "es:database-properties-generate(fn:doc('"+entityType+"'))", new JacksonHandle());
		JsonNode databaseConfiguration = handle.get();
		
		//logger.debug(databaseConfiguration.toString());

		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-database-properties/content-database3.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);
		assertEquals(control,databaseConfiguration);

		//org.hamcrest.MatcherAssert.assertThat("Expected: "+control+"\n\tGot: "+databaseConfiguration,control, org.hamcrest.Matchers.equalTo(databaseConfiguration));
	}
	
}
