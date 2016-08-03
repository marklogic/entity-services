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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;

/**
 * Tests server function es:entity-type-get-test-instances( $entity-type )
 *
 */
public class TestEsInstanceGenerator extends EntityServicesTestBase {

	@BeforeClass
	public static void setupTestInstances() {
		setupClients();
	}
	
	@Test
	public void createTestInstances() throws TestEvalException, TransformerException, IOException, SAXException {
		for (String entityType : entityTypes) {
			String entityTypeLocation = null;
			
			// we test that xml and json are equivalent elsewhere, so only test half.
			// primary-key-as-a-ref.xml is commented for bug 40666
			// valid-ref-value-as-nonString.json is commented for bug 40904
			if (entityType.contains(".json")||entityType.contains("invalid-")||entityType.contains(".jpg")||
					entityType.startsWith("primary-key-")||entityType.startsWith("valid-ref-value")) { continue; }
			
			String generateTestInstances = "es:model-get-test-instances( es:model-from-node( fn:doc('"+entityType+"') ) )";
			
			logger.info("Creating test instances from " + entityType);
			EvalResultIterator results = eval(generateTestInstances);
			int resultNumber = 0;
			while (results.hasNext()) {
				EvalResult result =  results.next();
				DOMHandle handle = result.get(new DOMHandle());              
				Document actualDoc = handle.get();
				
				//debugOutput(actualDoc);
				String entityTypeFileName = entityType.replace(".xml", "-" + resultNumber + ".xml");

				/*
                 // this is a one-time utility to auto-populate verification keys, not for checking them!
	            File outputFile = new File("src/test/resources/test-instances/" + entityTypeFileName );
	 			FileOutputStream os = new FileOutputStream(outputFile);
				debugOutput(actualDoc, os);
				logger.debug("Saved file to " + outputFile.getName());
				os.close(); */
				
				logger.debug("Control document: " + entityTypeFileName);
				InputStream is = this.getClass().getResourceAsStream("/test-instances/" + entityTypeFileName);
				Document controlDoc = builder.parse(is);
				
				XMLUnit.setIgnoreWhitespace(true);
				XMLAssert.assertXMLEqual("Failed validation of : "+entityTypeFileName, controlDoc, actualDoc);
				resultNumber++;
			}
		}
	}
	
	@Test
	public void bug38517GetTestInstances() {
		logger.info("Checking model-get-test-instances() with a document node");
		try {
			evalOneResult("es:model-get-test-instances(fn:doc('valid-datatype-array.json'))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for model-get-test-instances() with a document node");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Entity types must be map:map (or its subtype json:object)"));
		}
	}
	
	@Test
	/* test for bug 40904 to verify error msg when $ref has non string value */
	public void bug40904GetTestInstances() {
		logger.info("Checking model-get-test-instances() with a non string value as ref");
		try {
			evalOneResult("es:model-get-test-instances( es:model-from-node(fn:doc('invalid-bug40904.json')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for model-get-test-instances() with a non string value as ref");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: http://123.24: ref value must end with a simple name (xs:NCName)."));
		}
		
		try {
			evalOneResult("es:model-get-test-instances( es:model-from-node(fn:doc('invalid-bug40904.xml')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for model-get-test-instances() with a non string value as ref");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: http://123.24: ref value must end with a simple name (xs:NCName)."));
		}
	}
	
}
