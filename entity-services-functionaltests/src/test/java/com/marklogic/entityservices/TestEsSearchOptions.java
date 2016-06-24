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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;

public class TestEsSearchOptions extends EntityServicesTestBase {
	
	@BeforeClass
	public static void setup() {
		setupClients();
	}
	
	@Test
	public void testSearchOptionsGenerate() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "SchemaCompleteEntityType-0.0.1.json";
		
		DOMHandle handle = evalOneResult("es:entity-type-from-node(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);


		InputStream is = this.getClass().getResourceAsStream("/test-search-options/SchemaCompleteEntityType-0.0.1.xml");
		Document filesystemXML = builder.parse(is);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreComments(true);
		XMLAssert.assertXMLEqual("Search options validation failed.  " + entityType + ".", filesystemXML,
				searchOptions);
	}

	
}
