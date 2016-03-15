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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.StringHandle;

/**
 * Tests the server-side function
 * es:echema-generate($entity-type) as element(xsd:schema)
 * 
 * Stub - TODO implmement.
 *
 */
public class TestSchemaGeneration extends EntityServicesTestBase {


	@Test
	public void verifySchemaGeneration() throws TestEvalException {
		String initialTest = "Order-0.0.1.xml";
		
		// save conversion module into modules database
		DOMHandle schemaXML = new DOMHandle();
		
		schemaXML = evalOneResult("es:schema-generate( es:entity-type-from-node( fn:doc( '"+initialTest+"')))", schemaXML);
	
		// save schema module to schemas database
		XMLDocumentManager docMgr = schemasClient.newXMLDocumentManager();
		docMgr.write("Order-0.0.1.xml", schemaXML);
		
	}
		

}
