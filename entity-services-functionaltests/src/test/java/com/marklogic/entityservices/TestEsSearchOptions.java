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

import com.marklogic.client.io.DOMHandle;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;
import sun.plugin.com.event.COMEventHandler;

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
		String entityType = "SchemaCompleteEntityType-0.0.1.json";
		
		DOMHandle handle = evalOneResult("", "es:model-validate(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);


		InputStream is = this.getClass().getResourceAsStream("/test-search-options/SchemaCompleteEntityType-0.0.1.xml");
		Document filesystemXML = builder.parse(is);

		assertThat("Search options validation failed.  " + entityType + ".",
            searchOptions,
            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
	}
	
	@Test
	public void testSearchOptionsGenerate2() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "valid-db-prop-et.json";
		
		DOMHandle handle = evalOneResult("", "fn:doc('"+entityType+"')=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);


		InputStream is = this.getClass().getResourceAsStream("/test-search-options/valid-db-prop-et.xml");
		Document filesystemXML = builder.parse(is);
		//XMLUnit.setIgnoreComments(true);
		assertThat("Search options validation failed.  " + entityType + ".",
            searchOptions,
            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
	}
	
	@Test
	//Tests bug #243
	public void testSearchOptionsWithXML() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "valid-northwind1.xml";
		
		DOMHandle handle = evalOneResult("", "es:model-from-xml(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        debugOutput(searchOptions);
		InputStream is = this.getClass().getResourceAsStream("/test-search-options/valid-northwind1.xml");
		Document filesystemXML = builder.parse(is);
		assertThat("Search options validation failed.  " + entityType + ".",
            searchOptions,
            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
	}
	
	@Test
	public void testSearchOptions3() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "invalid-primary-key-as-ref.json";
		
		DOMHandle handle = evalOneResult("", "fn:doc('"+entityType+"')=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);
		InputStream is = this.getClass().getResourceAsStream("/test-search-options/primary-key-as-a-ref.xml");
		Document filesystemXML = builder.parse(is);
		assertThat("Search options validation failed.  " + entityType + ".",
            searchOptions,
            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
	}
	
	@Test
	public void testSearchOptionsGenerate3() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "valid-no-baseUri.json";
		
		DOMHandle handle = evalOneResult("", "fn:doc('"+entityType+"')=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);
        InputStream is = this.getClass().getResourceAsStream("/test-search-options/valid-no-baseUri.xml");
		Document filesystemXML = builder.parse(is);
		assertThat("Search options validation failed.  " + entityType + ".",
				searchOptions,
            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
	}
	
}
