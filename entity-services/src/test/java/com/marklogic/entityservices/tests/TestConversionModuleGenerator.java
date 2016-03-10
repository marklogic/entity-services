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
import com.marklogic.client.io.StringHandle;

/**
 * Tests server function es:conversion-module-generate
 * Stub.
 * 
 * TODO - test generated functions.
 */
public class TestConversionModuleGenerator extends EntityServicesTestBase {


	@Test
	public void verifyCreateValidModule() throws TestEvalException {
		
		String initialTest = "Order-0.0.1.xml";
		
		// save conversion module into modules database
		StringHandle xqueryModule = new StringHandle();
		
		xqueryModule = evalOneResult("es:conversion-module-generate( es:entity-type-from-node( fn:doc( '"+initialTest+"')))", xqueryModule);
	
		// save xquery module to modules database
		TextDocumentManager docMgr = modulesClient.newTextDocumentManager();
		docMgr.write("/ext/conversion-module-Order-0.0.1.xqy", xqueryModule);
		
		String instanceDocument = "Order-Source-1.xml";
		
		StringHandle handle = evalOneResult("import module namespace conv = \"http:///Order-0.0.1\" at \"/ext/conversion-module-Order-0.0.1.xqy\"; "+
		              "conv:extract-instance-Order( doc('"+instanceDocument+"') )", new StringHandle());
		
		String extractInstanceResult = handle.get();
		assertNotNull("Extract Instance Result must not be null ", extractInstanceResult);
		System.out.println(extractInstanceResult);
	}
	
}
