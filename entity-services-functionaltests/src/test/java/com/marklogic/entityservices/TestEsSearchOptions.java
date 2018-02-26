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

import com.marklogic.client.io.DOMHandle;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestEsSearchOptions extends EntityServicesTestBase {
	
	@BeforeClass
	public static void setup() {
		setupClients();
	}
	
	@Test
	public void testSearchOptionsGenerate() throws IOException, TestEvalException, SAXException, TransformerException {
		String[] eTs = { "SchemaCompleteEntityType-0.0.1.json", "valid-db-prop-et.json", "invalid-primary-key-as-ref.json",
				"valid-no-baseUri.json" };
		
		for( String entityType : eTs) {
			logger.info("Validating for "+entityType);
			DOMHandle handle = evalOneResult("", "fn:doc('"+entityType+"')=>es:search-options-generate()", new DOMHandle());
			Document searchOptions = handle.get();

	        //debugOutput(searchOptions);
			InputStream is = this.getClass().getResourceAsStream("/test-search-options/"+entityType.replaceAll("json", "xml"));
			Document filesystemXML = builder.parse(is);
	
			assertThat("Search options validation failed.  " + entityType + ".",
	            searchOptions,
	            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
		}
	}
	
	@Test
	//Tests bug #243
	public void testSearchOptionsWithXML() throws IOException, TestEvalException, SAXException, TransformerException {
		String[] eTs = { "valid-northwind1.xml", "valid-1-namespace.xml", "valid-2-namespace.xml" };
		
		for( String entityType : eTs) {
			logger.info("Validating for "+entityType);
			DOMHandle handle = evalOneResult("", "es:model-from-xml(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
			Document searchOptions = handle.get();
	
	        //debugOutput(searchOptions);
			InputStream is = this.getClass().getResourceAsStream("/test-search-options/"+entityType);
			Document filesystemXML = builder.parse(is);
			assertThat("Search options validation failed.  " + entityType + ".",
	            searchOptions,
	            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
		}
	}	
}
