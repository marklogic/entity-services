package com.marklogic.entityservices;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.io.StringHandle;

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
		
		StringHandle handle = evalOneResult("import module namespace conv = \"/Order-0.0.1\" at \"/ext/conversion-module-Order-0.0.1.xqy\"; "+
		              "conv:extract-instance-Order( doc('"+instanceDocument+"') )", new StringHandle());
		
		String extractInstanceResult = handle.get();
		assertNotNull("Extract Instance Result must not be null ", extractInstanceResult);
		System.out.println(extractInstanceResult);
	}
	
}
