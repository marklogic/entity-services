/*
 * Copyright 2016-2018 MarkLogic Corporation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.marklogic.client.document.DocumentManager;
import com.marklogic.client.io.*;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This class tests the various artifacts for required vs. non-required handling.
 */
public class TestRequired  extends EntityServicesTestBase {


    DocumentManager documentManager;
    String entityType = "/et-required.json";

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
                    if (expectedLine.contains("Generated at timestamp")) { }
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
        InputStream is = this.getClass().getResourceAsStream("/model-units" + entityType);
        documentManager = client.newJSONDocumentManager();
        documentManager.write("/et-required.json", new InputStreamHandle(is).withFormat(Format.JSON));

    }


    @Test
    public void testDatabasePropertiesRequired() throws TestEvalException, IOException {

        JacksonHandle handle;

        handle = evalOneResult("", "fn:doc('" + entityType + "')=>es:database-properties-generate()", new JacksonHandle());
        ObjectMapper mapper = new ObjectMapper();

        JsonNode node = handle.get();
        ObjectWriter writer = mapper.writer();

        // save("/model-units/database-properties.json", writer.writeValueAsString(node));

		InputStream is = this.getClass().getResourceAsStream("/model-units/database-properties.json");
		JsonNode control = mapper.readValue(is, JsonNode.class);

		assertThat(handle.get(), org.hamcrest.Matchers.equalTo(control));

    }


    @Test
    public void testExtractionTemplatesRequired() throws TestEvalException, IOException, SAXException {

        DOMHandle handle;
        handle = evalOneResult("", "fn:doc( '" + entityType + "')=>es:extraction-template-generate()", new DOMHandle());
        // String toWrite = evalOneResult("", "fn:doc( '" + entityType + "')=>es:extraction-template-generate()", new StringHandle()).get();
        // save("/model-units/extraction-template.xml", toWrite);
        InputStream is = this.getClass().getResourceAsStream("/model-units/extraction-template.xml");
		Document filesystemXML = builder.parse(is);
		assertThat("Control document for 'required' values extraction templates. ",
            handle.get(),
            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
    }


    @Test
    public void testSchemasRequired() throws IOException, TestEvalException, SAXException, TransformerException {

        DOMHandle handle;
        handle = evalOneResult("", "fn:doc( '" + entityType + "')=>es:schema-generate()", new DOMHandle());
        // String toWrite = evalOneResult("", "fn:doc( '" + entityType + "')=>es:schema-generate()", new StringHandle()).get();
        // save("/model-units/schema.xml", toWrite);
        InputStream is = this.getClass().getResourceAsStream("/model-units/schema.xml");
        Document filesystemXML = builder.parse(is);
        assertThat("Control document for 'required' values in schemas. ", handle.get(),
            CompareMatcher.isIdenticalTo(filesystemXML).ignoreWhitespace());
    }



    @Test
    public void testInstanceGenerator() throws IOException, TestEvalException {

        StringHandle handle;
        handle = evalOneResult("", "fn:doc( '" + entityType + "')=>es:instance-converter-generate()", new StringHandle());
        // save("/model-units/instance-converter.xqy", handle.get());
        compareLines("/model-units/instance-converter.xqy", handle.get());
    }



}
