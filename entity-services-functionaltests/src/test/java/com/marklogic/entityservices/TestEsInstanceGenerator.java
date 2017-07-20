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

import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
			//String entityTypeLocation = null;
			
			// we test that xml and json are equivalent elsewhere, so only test half.
			if (entityType.contains(".json")||entityType.contains("invalid-")||entityType.contains(".jpg")) { continue; }
			
			String generateTestInstances = "es:model-get-test-instances( es:model-from-xml( fn:doc('"+entityType+"') ) )";
			
			logger.info("Creating test instances from " + entityType);
			EvalResultIterator results = eval("", generateTestInstances);
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
				
				assertThat("Failed validation of : "+entityTypeFileName,
                    actualDoc,
                    CompareMatcher.isIdenticalTo(controlDoc).ignoreWhitespace());
				resultNumber++;
			}
		}
	}
	
	@Test
	/* test for bug 40904 to verify error msg when $ref has non string value */
	public void bug40904GetTestInstances() {
		logger.info("Checking model-get-test-instances() with a non string value as ref");
		try {
			evalOneResult("", "es:model-validate(fn:doc('invalid-bug40904.json'))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for model-get-test-instances() with a non string value as ref");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: http://123.24: ref value must end with a simple name (xs:NCName)."));
		}
		
		try {
			evalOneResult("", "es:model-get-test-instances(es:model-validate(fn:doc('invalid-bug40904.xml')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for model-get-test-instances() with a non string value as ref");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: http://123.24: ref value must end with a simple name (xs:NCName)."));
		}
	}
	
}
