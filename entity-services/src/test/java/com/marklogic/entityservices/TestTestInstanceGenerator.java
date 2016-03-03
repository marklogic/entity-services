package com.marklogic.entityservices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.mortbay.log.Log;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;

public class TestTestInstanceGenerator extends EntityServicesTestBase {

	@Test
	public void createTestInstances() throws TestEvalException, TransformerException, IOException, SAXException {
		for (String entityType : entityTypes) {
			String entityTypeLocation = null;
			if (entityType.contains("invalid")) { continue; };
			
			String testInstanceName = entityType.replaceAll("(.json|.xml)$", ".json");
			String generateTestInstances = "es:entity-type-get-test-instances( es:entity-type-from-node( fn:doc('"+entityType+"') ) )";
			
			Log.info("Creating test instances from " + testInstanceName);
			EvalResultIterator results = eval(generateTestInstances);
			int resultNumber = 0;
			while (results.hasNext()) {
				EvalResult result =  results.next();
				DOMHandle handle = result.get(new DOMHandle());
				Document actualDoc = handle.get();
				
				debugOutput(actualDoc);
				
// this is a one-time utility to auto-populate verification keys, not for checking them!
//				File outputFile = new File("src/test/resources/test-instances/" + testInstanceName.replace(".json", "") + "-" + resultNumber + ".xml");
//				FileOutputStream os = new FileOutputStream(outputFile);
//				debugOutput(actualDoc, os);
//				os.close();
				
				InputStream is = this.getClass().getResourceAsStream("/test-instances/" + testInstanceName.replace(".json", "") + "-" + resultNumber + ".xml");
				Document controlDoc = builder.parse(is);
				
				XMLUnit.setIgnoreWhitespace(true);
				XMLAssert.assertXMLEqual(controlDoc, actualDoc);
				resultNumber++;
			}
		}
	}
}
