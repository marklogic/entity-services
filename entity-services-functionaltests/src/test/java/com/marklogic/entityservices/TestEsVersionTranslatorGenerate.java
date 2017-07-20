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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
import org.assertj.core.api.SoftAssertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests server function es:version-translator-generate
 * 
 * Covered so far: validity of XQuery module generation
 * 
 * convert-instance-Order
 * 
 * 
 */
public class TestEsVersionTranslatorGenerate extends EntityServicesTestBase {

	//private StringHandle xqueryModule = new StringHandle();
	private static TextDocumentManager docMgr;
	private static Map<String, StringHandle> conversionModules;
	
	@BeforeClass
	public static void setupClass() {
		setupClients();
		// save xquery module to modules database
		docMgr = modulesClient.newTextDocumentManager();	
		
		conversionModules = generateVersionTranslatorModule();
		storeConversionModules(conversionModules);

	}
	
	private static void storeConversionModules(Map<String, StringHandle> moduleMap) {
		
		DocumentWriteSet writeSet = docMgr.newWriteSet();
		
		for (String entityTypeName : moduleMap.keySet()) {
			
			String moduleName = "/conv/" + entityTypeName.replaceAll("\\.(xml|json)", ".xqy");
			writeSet.add(moduleName, moduleMap.get(entityTypeName));
		}
		docMgr.write(writeSet);
	}
	
	private static Map<String, StringHandle> generateVersionTranslatorModule() {
		Map<String, StringHandle> map = new HashMap<String, StringHandle>();
		
		for (String entityType : entityTypes) {
			if (entityType.startsWith("valid-")||entityType.startsWith("primary-")||entityType.startsWith("person")||entityType.startsWith("SchemaCompleteEntity")||entityType.contains(".xml")||entityType.contains("-Src.json")||entityType.contains(".jpg")||entityType.contains("invalid-")||entityType.contains("sameTgt-")) {continue; }
			
			String part = entityType.replaceAll("\\-(Src|Tgt)", "");
			String source = part.replaceAll("\\.json", "-Src.json");
			String target = part.replaceAll("\\.json", "-Tgt.json");
			logger.info("Generating version translator module for : " + source + " & " + target);
			StringHandle xqueryModule = new StringHandle();
			try {
				xqueryModule = evalOneResult("", "es:version-translator-generate(fn:doc('"+source+"'),fn:doc('"+target+"'))", xqueryModule);
				//logger.info("Ver Trans Gen for "+part+" : \n"+xqueryModule.get());
			} catch (TestEvalException e) {
				throw new RuntimeException(e);
			}
			map.put(part, xqueryModule);
		}
		return map;
	}
	
    private void compareLines(String path, String content) throws IOException {

        List<String> contentLines = Arrays.asList(content.split("\\n"));
        Iterator<String> contentIterator = contentLines.iterator();

        File expectedFile = new File("src/test/resources/"+path);
        try (BufferedReader br = new BufferedReader(new FileReader(expectedFile))) {
            long i=0;
            String line;
            SoftAssertions softly = new SoftAssertions();
            while ((line = br.readLine()) != null) {
                if (contentIterator.hasNext()) {
                    String expectedLine = contentIterator.next();
                    if (expectedLine.contains("Generated at timestamp")) { }
                    else
                        softly.assertThat(expectedLine)
                                .as("Mismatch in conversion module line " + Long.toString(i++))
                                .isEqualToIgnoringWhitespace(line);
                } else {
                    fail("Expected result has more lines than actual results");
                }
            }
            softly.assertAll();
        }
    }
    
	@Test
	public void testVersionTranslatorModule() throws TransformerException, IOException, SAXException {

		for (String entityTypeName : conversionModules.keySet()) {
			
			String actualDoc = conversionModules.get(entityTypeName).get();
			logger.info("Checking version translator for "+entityTypeName.replaceAll("\\.(xml|json)", ".xqy"));
			//logger.info(actualDoc+"\n************************************************************\n");
			compareLines("/test-version-translator/"+entityTypeName.replaceAll("\\.(xml|json)", ".xqy"), actualDoc);

		}
	}
	
	@Test
	public void testSrcAndTgtSame() throws TransformerException, IOException, SAXException {

		String entityTypeName = "sameTgt-Src.json";
			
		StringHandle xqueryModule;
		try {
			xqueryModule = evalOneResult("", "es:version-translator-generate( es:model-validate( fn:doc( '"+entityTypeName+"')),fn:doc( '"+entityTypeName+"'))", new StringHandle());
			String actualDoc = xqueryModule.get();
			//logger.info(actualDoc);
			logger.info("Checking version translator for "+entityTypeName.replaceAll("\\.(xml|json)", ".xqy"));
			compareLines("/test-version-translator/"+entityTypeName.replaceAll("\\.(xml|json)", ".xqy"), actualDoc);

		} catch (TestEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testSrcTgtSameDiffTitle() throws TransformerException, IOException, SAXException {

		String source = "sameTgt-Src.json";
		String target = "sameTgt-Tgt.json";
			
		StringHandle xqueryModule;
		try {
			xqueryModule = evalOneResult("", "es:version-translator-generate(fn:doc( '"+source+"'),es:model-validate(fn:doc( '"+target+"')))", new StringHandle());
			String actualDoc = xqueryModule.get();
			//logger.info(actualDoc);
			logger.info("Checking version translator for "+target.replaceAll("\\.(xml|json)", ".xqy"));
			compareLines("/test-version-translator/"+target.replaceAll("\\.(xml|json)", ".xqy"), actualDoc);

		} catch (TestEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testMissingSrc() {
		logger.info("Checking version-translator-generate() with a missing document node");
		try {
			evalOneResult("", "es:version-translator-generate(fn:doc('valid-datatype-array.xml'))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for version-translator-generate() with a missing document node");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("Too few args, expected 2 but got 1"));
		}
	}
	
	@Test
	public void testInvalidETDocAsSrcTgt() {
		logger.info("Checking version-translator-generate() with invalid document node");
		try {
			evalOneResult("", "es:version-translator-generate(es:model-validate(fn:doc('invalid-missing-info.json')),es:model-validate(fn:doc('invalid-missing-title.json')))", new JacksonHandle());	
			fail("eval should throw an ES-MODEL-INVALID exception for version-translator-generate() with invalid document node");
		} catch (TestEvalException e) {
			logger.info(e.getMessage());
			assertTrue("Must contain ES-MODEL-INVALID error message but got: "+e.getMessage(), e.getMessage().contains("ES-MODEL-INVALID: Model descriptor must contain exactly one info section. Primary Key orderId doesn't exist."));
		}
	}
	
	@Test
	public void testSrcRefDiffDatatype() throws TransformerException, IOException, SAXException {

		String source = "srcRefDiffDatatype-Src.json";
		String target = "srcRefDiffDatatype-Tgt.json";
			
		StringHandle xqueryModule;
		try {
			xqueryModule = evalOneResult("", "es:version-translator-generate(fn:doc( '"+source+"'),fn:doc( '"+target+"'))", new StringHandle());
			String actualDoc = xqueryModule.get();
			//logger.info(actualDoc);
			logger.info("Checking version translator for "+target.replaceAll("\\.(xml|json)", ".xqy"));
			compareLines("/test-version-translator/"+target.replaceAll("\\-Tgt.json", ".xqy"), actualDoc);

		} catch (TestEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Test
    public void testConvInst() throws TransformerException, IOException, SAXException {

        String import1 = "import module namespace locArr = 'http://localArrayRefTgt/localArrayRefTgt-0.0.2-from-localArrayRefSrc-0.0.1' at '/conv/localArrayRefs.xqy';\n" + 
                         "import module namespace locArrSrc = 'http://localArrayRefSrc/localArrayRefSrc-0.0.1' at '/conv/VT-localArrayRefs-Src.xqy';\n";
        
        String query1 = "locArr:convert-instance-Order(locArrSrc:instance-to-envelope(locArrSrc:extract-instance-Order(doc('10252.xml'))))";
        
        JacksonHandle handle;
        try {
            handle = evalOneResult(import1, query1, new JacksonHandle());
            JsonNode actualDoc = handle.get();
            logger.info("Checking convert-instance-Order()");
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = this.getClass().getResourceAsStream("/test-extract-instance/convert-instance-Order.json");

            JsonNode control = mapper.readValue(is, JsonNode.class);

            org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(actualDoc));

        } catch (TestEvalException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
	
    @Test
    public void testConvInst2() throws TransformerException, IOException, SAXException {

        String import1 = "import module namespace locArr = 'http://marklogic.com/srcRefDatatype/srcRefDatatypeTgt-0.0.2-from-srcRefDatatypeSrc-0.0.1' at '/conv/srcRefDiffDatatype.xqy';\n" + 
                         "import module namespace locArrSrc = 'http://marklogic.com/srcRefDatatype/srcRefDatatypeSrc-0.0.1' at '/conv/srcRefDiffDatatype-Src.xqy';\n";
        
        String query1 = "locArr:convert-instance-Product(locArrSrc:instance-to-envelope(locArrSrc:extract-instance-Product(doc('51.xml'))))";
        
        JacksonHandle handle;
        try {
            handle = evalOneResult(import1, query1, new JacksonHandle());
            JsonNode actualDoc = handle.get();
            logger.info("Checking convert-instance-Product()");
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = this.getClass().getResourceAsStream("/test-extract-instance/convert-instance-Product.json");

            JsonNode control = mapper.readValue(is, JsonNode.class);

            org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(actualDoc));

        } catch (TestEvalException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

}
