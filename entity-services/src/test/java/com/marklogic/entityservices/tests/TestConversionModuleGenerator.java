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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import com.marklogic.client.io.*;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.TextDocumentManager;

import static org.junit.Assert.*;


/**
 * Tests server function es:conversion-module-generate
 * 
 * Covered so far: validity of XQuery module generation
 * 
 * extract-instance-Order
 * 
 * The default extraction model is valid, and each function runs as though
 * the source for an entity is the same as its model.  That is, 
 * if you extract an instance using extract-instance-Order() the original
 * generated function expects an input that corresponds exactly to the persisted
 * output of an Order.
 */
public class TestConversionModuleGenerator extends EntityServicesTestBase {
	
	private static TextDocumentManager docMgr;
	private static Map<String, StringHandle> conversionModules;

    @BeforeClass
	public static void setupClass() {
		setupClients();
		// save xquery module to modules database
		docMgr = modulesClient.newTextDocumentManager();	

		entityTypes = TestSetup.getInstance().loadEntityTypes("/json-entity-types", ".*.json$");
		conversionModules = generateConversionModules();
		storeConversionModules(conversionModules);

	}
	
	private static void storeConversionModules(Map<String, StringHandle> moduleMap) {
		DocumentWriteSet writeSet = docMgr.newWriteSet();
		
		for (String entityTypeName : moduleMap.keySet()) {
			
			String moduleName = "/ext/" + entityTypeName.replaceAll("\\.(xml|json)", ".xqy");
			writeSet.add(moduleName, moduleMap.get(entityTypeName));
		}
		docMgr.write(writeSet);
	}
	
	private static Map<String, StringHandle> generateConversionModules() {
		Map<String, StringHandle> map = new HashMap<String, StringHandle>();
		
		for (String entityType : entityTypes) {
			logger.info("Generating conversion module: " + entityType);
			StringHandle xqueryModule = new StringHandle();
			try {
				xqueryModule = evalOneResult(" fn:doc( '"+entityType+"')=>es:entity-type-from-node()=>es:conversion-module-generate()", xqueryModule);
			} catch (TestEvalException e) {
				throw new RuntimeException(e);
			}
			map.put(entityType, xqueryModule);
		}
		return map;
	}
	
	@Test
	public void verifyCreateValidModule() throws TestEvalException {

        String initialTest = "Order-0.0.1.json";
        StringHandle moduleHandle =  evalOneResult("fn:doc( '"+ initialTest +"')=>es:entity-type-from-node()=>es:conversion-module-generate()", new StringHandle());
		HashMap<String, StringHandle> m = new HashMap<String, StringHandle>();
		m.put(initialTest, moduleHandle);
		// save conversion module into modules database
		storeConversionModules(m);
		
		String instanceDocument = "Order-Source-1.xml";
		TestSetup.getInstance().loadExtraFiles("/source-documents", instanceDocument);
		StringHandle handle = evalOneResult("import module namespace conv = \"http:///Order-0.0.1\" at \"/ext/Order-0.0.1.xqy\"; "+
		              "conv:extract-instance-Order( doc('"+instanceDocument+"') )", new StringHandle());
		
		String extractInstanceResult = handle.get();
		assertNotNull("Extract Instance Result must not be null (and should not throw error) ", extractInstanceResult);
		
	}
	
	private String moduleImport(String entityType) {
		InputStream is = this.getClass().getResourceAsStream("/json-entity-types/" + entityType);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode controlFile = null;
		try {
			controlFile = (ObjectNode) mapper.readTree(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		JsonNode baseUriNode = controlFile.get("info").get("baseUri");
		String baseUri = null;
		if (baseUriNode == null) {
			baseUri = "http://example.org/";
		} else {
			baseUri = baseUriNode.asText();
		}
		String uriPrefix = baseUri;
		if (!baseUri.matches(".*[#/]$")) {
			uriPrefix += "#";
		}

		String entityTypeName = entityType.replace(".json",  "");
		String moduleName = "/ext/" + entityTypeName + ".xqy";
		
		return "import module namespace conv = \""+uriPrefix + entityTypeName +"\" at \""+moduleName+"\"; ";		
	}
	
	/**
	 * Rationale for this test is that default generated conversion module should
	 * work out-of-the-box, and handle an identity transform from test instances.
	 * This test thus tests 
	 * instance-extract and 
	 * instance-to-canonical-xml
	 * instance-from-document
	 * instance-json-from-document
	 * instance-xml-from-document
	 * instance-attachments-from-document
	 * 
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 * @throws TransformerException 
	 */
	@Test
	public void testConversionModuleFuntionsOnIdentityPayload() throws TestEvalException, JsonProcessingException, IOException, SAXException, TransformerException {

		TestSetup.getInstance().loadExtraFiles("/test-instances", ".*");
		
		// test them all adn remove
		for (String entityType : conversionModules.keySet()) {
			
			String entityTypeTestFileName = entityType.replace(".json", "-0.xml");
			
			String entityTypeName = entityType.replace(".json",  "");
			String entityTypeNoVersion = entityTypeName.replaceAll("-.*$", "");
			
			logger.debug("Checking canonical and envelope: " + entityType);
				
			DOMHandle handle = evalOneResult(
				moduleImport(entityType) +
				"let $canonical := conv:instance-to-canonical-xml( conv:extract-instance-"+entityTypeNoVersion+"( doc('"+entityTypeTestFileName+"') ) )"
				+"let $envelope := conv:instance-to-envelope( conv:extract-instance-"+entityTypeNoVersion+"( doc('"+entityTypeTestFileName+"') ) )"
				+"return (xdmp:document-insert('"+entityTypeTestFileName+"-envelope.xml', $envelope), $canonical)", new DOMHandle());
			Document actualInstance = handle.get();
			assertEquals("extract-canonical returns an instance", actualInstance.getDocumentElement().getLocalName(), entityTypeNoVersion);
			
			// dom returned from extraction must equal test instance.
			String controlFilePath = "/test-instances/" + entityTypeTestFileName;
			Document controlDom = builder.parse(this.getClass().getResourceAsStream(controlFilePath));

//				logger.debug("Control doc");
//				debugOutput(controlDom);
//				logger.debug("Actual doc wrapped");
//				debugOutput(actualInstance);
			
			XMLUnit.setIgnoreWhitespace(true);
			XMLAssert.assertXMLEqual("Extract instance by default returns identity", controlDom, actualInstance);
			
			// test that XML from envelope returns the instance.
			String testToInstance = moduleImport(entityType) 
					+"es:instance-xml-from-document( doc('"+entityTypeTestFileName+"-envelope.xml') )";
			handle = evalOneResult(testToInstance, new DOMHandle());
			actualInstance = handle.get();
			XMLAssert.assertXMLEqual("Extract instance by default returns identity", controlDom, actualInstance);

			// extract instance, returned as JSON, matches instance-json-from-document
			JacksonHandle instanceJSONHandle = evalOneResult(moduleImport(entityType) + "es:instance-from-document( doc('"+entityTypeTestFileName+"-envelope.xml') )", new JacksonHandle());
			JacksonHandle instanceAsJSONHandle = evalOneResult(moduleImport(entityType) + "es:instance-json-from-document( doc('"+entityTypeTestFileName+"-envelope.xml') )", new JacksonHandle());
			JsonNode instance = instanceJSONHandle.get();
			JsonNode jsonInstance = instanceAsJSONHandle.get();
			org.hamcrest.MatcherAssert.assertThat(instance, org.hamcrest.Matchers.equalTo(jsonInstance));

			// moreover, extracting the attachments also will result in identity.
			DOMHandle domHandle = evalOneResult(moduleImport(entityType) + "es:instance-get-attachments( doc('"+entityTypeTestFileName+"-envelope.xml') )", new DOMHandle());
			Document originalDocument = domHandle.get();
			XMLAssert.assertXMLEqual("Original document also matches source", controlDom, originalDocument);
			
		}
	}
	
	
	@Test
	public void testEnvelopeFunction() throws TestEvalException {
		
		for (String entityType : conversionModules.keySet()) {
			String functionCall = moduleImport(entityType)
                +"let $p := map:map()"
                +"let $_ := map:put($p, '$type', 'Order')"
                +"let $_ := map:put($p, 'prop', 'val')"
                +"let $_ := map:put($p, '$attachments', element source { 'bah' })"
			    +"return conv:instance-to-envelope( $p )";
			
			DOMHandle handle = evalOneResult(functionCall, new DOMHandle());
			Document document = handle.get();
			Element docElement = document.getDocumentElement();
			assertEquals("envelope function verification", "envelope", docElement.getLocalName());
			NodeList nl = docElement.getChildNodes();
			assertEquals("Envelope must have two children.", 2, nl.getLength());
			for (int i=0; i<nl.getLength(); i++) {
				Node n = nl.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					logger.debug("Checking node name " + n.getLocalName());
					Element e = (Element) n;
					assertTrue(e.getLocalName().equals("instance") || e.getLocalName().equals("attachments"));
				}
			}
        }
	
	}

	@AfterClass
	public static void removeConversions() {
		for (String entityType : conversionModules.keySet()) {
			
			String moduleName = "/ext/" + entityType.replaceAll("\\.(xml|json)", ".xqy");
			
			docMgr.delete(moduleName);
		}
	}
}
