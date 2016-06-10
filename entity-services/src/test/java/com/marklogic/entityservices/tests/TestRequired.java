package com.marklogic.entityservices.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.document.DocumentManager;
import com.marklogic.client.io.*;
import org.assertj.core.api.SoftAssertions;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;

/**
 * This class tests the various artifacts for required vs. non-required handling.
 */
public class TestRequired  extends EntityServicesTestBase {


    DocumentManager documentManager;
    String entityType = "/et-required.json";

    private void save(String path, String content) throws IOException {
        File outputFile = new File("src/test/resources/" + path );
		FileWriter writer = new FileWriter(outputFile);
		writer.write(content);
        writer.close();
    }

    private void compareLines(String path, String content) throws IOException {

        List<String> contentLines = Arrays.asList(content.split("\\n"));
        Iterator<String> contentIterator = contentLines.iterator();

        File expectedFile = new File("src/test/resources/" + path );
        try (BufferedReader br = new BufferedReader(new FileReader(expectedFile))) {
            long i=0;
            String line;
            SoftAssertions softly = new SoftAssertions();
            while ((line = br.readLine()) != null) {
                if (contentIterator.hasNext()) {
                    String expectedLine = contentIterator.next();
                    if (expectedLine.contains("Generated at timestamp")) continue;
                    else
                        softly.assertThat(expectedLine)
                                .as("Mismatch in conversion module line " + Long.toString(i++))
                                .isEqualToIgnoringWhitespace(line);
                }
            }
            softly.assertAll();
        }
    }

    @Before
    public void generateArtifacts() throws TestEvalException, IOException {

        setupClients();
        InputStream is = this.getClass().getResourceAsStream("/entity-type-units" + entityType);
        documentManager = client.newJSONDocumentManager();
        documentManager.write("/et-required.json", new InputStreamHandle(is).withFormat(Format.JSON));

    }


    @Test
    public void testDatabasePropertiesRequired() throws TestEvalException, IOException {

        JacksonHandle handle;

        handle = evalOneResult("es:entity-type-from-node(fn:doc('" + entityType + "'))=>es:database-properties-generate()", new JacksonHandle());
        // save("/entity-type-units/database-properties.json", handle.get());

		ObjectMapper mapper = new ObjectMapper();
		InputStream is = this.getClass().getResourceAsStream("/entity-type-units/database-properties.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);

		org.hamcrest.MatcherAssert.assertThat(handle.get(), org.hamcrest.Matchers.equalTo(control));

    }


    @Test
    public void testExtractionTemplatesRequired() throws TestEvalException, IOException, SAXException {

        DOMHandle handle;
        handle = evalOneResult("es:entity-type-from-node( fn:doc( '" + entityType + "'))=>es:extraction-template-generate()", new DOMHandle());
        // String toWrite = evalOneResult("es:entity-type-from-node( fn:doc( '" + entityType + "'))=>es:extraction-template-generate()", new StringHandle()).get();
        // save("/entity-type-units/extraction-template.xml", toWrite);
        InputStream is = this.getClass().getResourceAsStream("/entity-type-units/extraction-template.xml");
		Document filesystemXML = builder.parse(is);
		XMLUnit.setIgnoreWhitespace(true);
		assertXMLEqual("Control document for 'required' values extraction templates. ", filesystemXML, handle.get());
    }


    @Test
    public void testSchemasRequired() throws IOException, TestEvalException, SAXException {

        DOMHandle handle;
        handle = evalOneResult("es:entity-type-from-node( fn:doc( '" + entityType + "'))=>es:schema-generate()", new DOMHandle());
        // String toWrite = evalOneResult("es:entity-type-from-node( fn:doc( '" + entityType + "'))=>es:schema-generate()", new StringHandle()).get();
        // save("/entity-type-units/schema.xml", toWrite);
        InputStream is = this.getClass().getResourceAsStream("/entity-type-units/schema.xml");
        Document filesystemXML = builder.parse(is);
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual("Control document for 'required' values in schemas. ", filesystemXML, handle.get());
    }



    @Test
    public void testConversionModuleRequired() throws IOException, TestEvalException {

        StringHandle handle;
        handle = evalOneResult("es:entity-type-from-node( fn:doc( '" + entityType + "'))=>es:conversion-module-generate()", new StringHandle());
        // save("/entity-type-units/conversion-module.xqy", handle.get());
        compareLines("/entity-type-units/conversion-module.xqy", handle.get());
        // handle = evalOneResult("es:entity-type-from-node( fn:doc( '" + entityType + "'))=>es:version-conversion-generate()", handle);
        // save("/entity-type-units/conversion-module.xqy", handle);
    }



}
