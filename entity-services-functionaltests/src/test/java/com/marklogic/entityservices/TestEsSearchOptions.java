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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
	
	@Test
	public void testSearchOptionsGenerate2() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "valid-db-prop-et.json";
		
		DOMHandle handle = evalOneResult("es:entity-type-from-node(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);


		InputStream is = this.getClass().getResourceAsStream("/test-search-options/valid-db-prop-et.xml");
		Document filesystemXML = builder.parse(is);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreComments(true);
		XMLAssert.assertXMLEqual("Search options validation failed.  " + entityType + ".", filesystemXML,
				searchOptions);
	}
	
	@Test
	public void testSearchOptionsWithXML() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "valid-northwind1.xml";
		
		DOMHandle handle = evalOneResult("es:entity-type-from-node(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);
		InputStream is = this.getClass().getResourceAsStream("/test-search-options/valid-northwind1.xml");
		Document filesystemXML = builder.parse(is);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreComments(true);
		XMLAssert.assertXMLEqual("Search options validation failed.  " + entityType + ".", filesystemXML,
				searchOptions);
	}
	
	/* uncomment this test when bug#40666 gets fixed
	@Test
	public void testSearchOptions3() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "primary-key-as-a-ref.xml";
		
		DOMHandle handle = evalOneResult("es:entity-type-from-node(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);
		InputStream is = this.getClass().getResourceAsStream("/test-search-options/primary-key-as-a-ref.xml");
		Document filesystemXML = builder.parse(is);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreComments(true);
		XMLAssert.assertXMLEqual("Search options validation failed.  " + entityType + ".", filesystemXML,
				searchOptions);
	}
	*/
	
	@Test
	public void testSearchOptionsGenerate3() throws IOException, TestEvalException, SAXException, TransformerException {
		String entityType = "valid-no-baseUri.json";
		
		DOMHandle handle = evalOneResult("es:entity-type-from-node(fn:doc('"+entityType+"'))=>es:search-options-generate()", new DOMHandle());
		Document searchOptions = handle.get();

        //debugOutput(searchOptions);
        InputStream is = this.getClass().getResourceAsStream("/test-search-options/valid-no-baseUri.xml");
		Document filesystemXML = builder.parse(is);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreComments(true);
		XMLAssert.assertXMLEqual("Search options validation failed.  " + entityType + ".", filesystemXML,
				searchOptions);
	}
	
	@Test
	public void bug38517SearchOptionsGen() {
		logger.info("Checking search-options-generate() with a document node");
		try {
			evalOneResult("es:search-options-generate(fn:doc('valid-datatype-array.json'))", new JacksonHandle());	
			fail("eval should throw an ES-ENTITYTYPE INVALID exception for search-options-generate() with a document node");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-ENTITYTYPE INVALID error message but got: "+e.getMessage(), e.getMessage().contains("Entity types must be map:map (or its subtype json:object)"));
		}
	}


	
}
