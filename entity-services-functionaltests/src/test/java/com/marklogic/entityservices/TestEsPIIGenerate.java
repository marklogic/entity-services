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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
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
public class TestEsPIIGenerate extends EntityServicesTestBase {

	@BeforeClass
	public static void setup() {
		setupClients();
	}
	
	@Test
	public void testELSConfiguraionJSON() throws JsonParseException, JsonMappingException, IOException {
		
		String entityType = "valid-pii.json";
		
		JacksonHandle handle = evalOneResult("", "es:pii-generate(fn:doc('"+entityType+"'))", new JacksonHandle());
		JsonNode elsConfiguration = handle.get();

		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-pii-generate/els-config.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);

		org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(elsConfiguration));
	}
	
	@Test
	public void testELSConfiguraionXML() throws JsonParseException, JsonMappingException, IOException {
		
		String entityType = "valid-pii.xml";
		
		JacksonHandle handle = evalOneResult("", "es:pii-generate(es:model-validate(fn:doc('"+entityType+"')))", new JacksonHandle());
		JsonNode elsConfiguration = handle.get();

		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/test-pii-generate/els-config.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);

		org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(elsConfiguration));
	}
	
}
