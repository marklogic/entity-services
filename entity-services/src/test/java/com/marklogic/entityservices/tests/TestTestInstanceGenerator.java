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

import javax.xml.transform.TransformerException;

import org.apache.jena.atlas.logging.Log;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;

/**
 * Tests server function es:model-get-test-instances( $model )
 *
 */
public class TestTestInstanceGenerator extends EntityServicesTestBase {

	@BeforeClass
	public static void setupTestInstances() {
		setupClients();
		entityTypes = TestSetup.getInstance().loadEntityTypes("/xml-models", ".*.xml$");
	}
	
	@Test
	public void verifyTestInstances() throws TestEvalException, TransformerException, IOException, SAXException {
		for (String entityType : entityTypes) {
			
			String generateTestInstances = " fn:doc('"+entityType+"')=>es:model-from-xml()=>es:model-get-test-instances()";
			
			logger.info("Creating test instances from " + entityType);
			EvalResultIterator results = eval(generateTestInstances);
			int resultNumber = 0;
			while (results.hasNext()) {
				EvalResult result =  results.next();
				DOMHandle handle = result.get(new DOMHandle());
				Document actualDoc = handle.get();
				
				//debugOutput(actualDoc);
				String entityTypeFileName = entityType.replace(".xml", "-" + resultNumber + ".xml");

// this is a one-time utility to auto-populate verification keys, not for checking them!
	/*		File outputFile = new File("src/test/resources/test-instances/" + entityTypeFileName );
				FileOutputStream os = new FileOutputStream(outputFile);
				debugOutput(actualDoc, os);
				logger.debug("Saved file to " + outputFile.getName());
				os.close(); */
				
				 debugOutput(actualDoc, System.out);
				// logger.debug("Control document: " + entityTypeFileName);
				InputStream is = this.getClass().getResourceAsStream("/test-instances/" + entityTypeFileName);
				Document controlDoc = builder.parse(is);
				
				XMLUnit.setIgnoreWhitespace(true);
				XMLAssert.assertXMLEqual(controlDoc, actualDoc);
				resultNumber++;
			}
		}
	}
}
