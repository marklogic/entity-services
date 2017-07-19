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
package com.marklogic.entityservices.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


/**
 * Tests server function es:instance-converter-generate()
 *
 * Covered so far: validity of XQuery module generation
 *
 * extract-instance-Order
 *
 * The default extraction model is valid, and each function runs as though
 * the source for an entity is the same as its model.  That is,
 * if you extract an instance using extract-instance-Order() the original
 * generated function expects an input that corresponds exactly to the persisted
 * output of an Order.
 */
public class TestInstanceConverterGenerator extends EntityServicesTestBase {

    private static TextDocumentManager docMgr;
    private static Map<String, StringHandle> converters;

    @BeforeClass
    public static void setupClass() {
        setupClients();
        // save xquery module to modules database
        docMgr = modulesClient.newTextDocumentManager();

        entityTypes = TestSetup.getInstance().loadEntityTypes("/json-models", ".*.json$");
        converters = generateConversionModules();
        storeConverter(converters);

    }

    private static void storeConverter(Map<String, StringHandle> moduleMap) {
        DocumentWriteSet writeSet = docMgr.newWriteSet();

        for (String entityTypeName : moduleMap.keySet()) {

            String moduleName = "/ext/" + entityTypeName.replaceAll("\\.(xml|json)", ".xqy");
            writeSet.add(moduleName, moduleMap.get(entityTypeName));
        }
        docMgr.write(writeSet);
    }

    private static Map<String, StringHandle> generateConversionModules() {
        Map<String, StringHandle> map = new HashMap<String, StringHandle>();

        for (String entityType : entityTypes) {
            logger.info("Generating converter: " + entityType);
            StringHandle xqueryModule = new StringHandle();
            try {
                xqueryModule = evalOneResult("", " fn:doc( '"+entityType+"')=>es:instance-converter-generate()", xqueryModule);
            } catch (TestEvalException e) {
                throw new RuntimeException(e);
            }
            map.put(entityType, xqueryModule);
        }
        return map;
    }

    @Test
    public void verifyCreateValidModule() throws TestEvalException, TransformerException {

        String initialTest = "Order-0.0.4.json";
        String instanceDocument = "Order-Source-1.xml";

        DocumentBuilder builder = null;
        Document expectedDoc = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            expectedDoc = builder.parse(this.getClass().getResourceAsStream("/source-documents/" + instanceDocument));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        StringHandle moduleHandle =  evalOneResult("", "fn:doc( '"+ initialTest +"')=>es:instance-converter-generate()", new StringHandle());
        // logger.debug(moduleHandle.get());
        HashMap<String, StringHandle> m = new HashMap<String, StringHandle>();
        m.put(initialTest, moduleHandle);
        // save converter into modules database
        storeConverter(m);

        TestSetup.getInstance().loadExtraFiles("/source-documents", instanceDocument);
        DOMHandle handle = evalOneResult(
            "import module namespace conv = \"http://marklogic.com/entity-services/test#Order-0.0.4\" at \"/ext/Order-0.0.4.xqy\"; ",
            "conv:extract-instance-Order( doc('"+instanceDocument+"') )=>conv:instance-to-canonical-xml()",
            new DOMHandle());
        Document extractInstanceResult = handle.get();

        debugOutput(extractInstanceResult);
        assertThat( extractInstanceResult, CompareMatcher.isIdenticalTo(expectedDoc).ignoreWhitespace());

        assertNotNull("Extract Instance Result must not be null (and should not throw error) ", extractInstanceResult);

    }

    private String moduleImport(String entityType) {
        InputStream is = this.getClass().getResourceAsStream("/json-models/" + entityType);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode controlFile = null;
        try {
            controlFile = (ObjectNode) mapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonNode baseUriNode = controlFile.get("info").get("baseUri");
        String baseUri = null;
        if (baseUriNode == null) {
            baseUri = "http://example.org/";
        } else {
            baseUri = baseUriNode.asText();
        }
        String uriPrefix = baseUri;
        if (!baseUri.matches(".*[#/]$")) {
            uriPrefix += "#";
        }

        String entityTypeName = entityType.replace(".json",  "");
        String moduleName = "/ext/" + entityTypeName + ".xqy";

        return "import module namespace conv = \""+uriPrefix + entityTypeName +"\" at \""+moduleName+"\"; ";
    }

    /**
     * Rationale for this test is that default generated converter should
     * work out-of-the-box, and handle an identity transform from test instances.
     * This test thus tests
     * instance-extract and
     * instance-to-canonical-xml
     * instance-from-document
     * instance-json-from-document
     * instance-xml-from-document
     * instance-get-attachments
     *
     * @throws IOException
     * @throws JsonProcessingException
     * @throws TransformerException
     */
    @Test
    public void testConversionModuleExtractions() throws TestEvalException, JsonProcessingException, IOException, SAXException, TransformerException {

        TestSetup.getInstance().loadExtraFiles("/test-instances", ".*");

        // test them all adn remove
        for (String entityType : converters.keySet()) {

            String entityTypeTestFileName = entityType.replace(".json", "-0.xml");

            String entityTypeName = entityType.replace(".json",  "");
            String entityTypeNoVersion = entityTypeName.replaceAll("-.*$", "");

            logger.debug("Checking canonical XML function and envelope function and empty extraction: " + entityType);

            DOMHandle handle =
                    evalOneResult(
                        moduleImport(entityType), "let $canonical := conv:instance-to-canonical-xml( conv:extract-instance-"+entityTypeNoVersion+"( doc('"+entityTypeTestFileName+"') ) )"
                +"let $envelope := conv:instance-to-envelope( conv:extract-instance-"+entityTypeNoVersion+"( doc('"+entityTypeTestFileName+"') ) )"
                +"let $empty-extraction := conv:instance-to-canonical-xml( conv:extract-instance-"+entityTypeNoVersion+"( <bah/> ) )"
                +"return (xdmp:document-insert('"+entityTypeTestFileName+ "-envelope.xml', $envelope), " +
                        " xdmp:document-insert('"+entityTypeTestFileName+"-empty.xml' ,$empty-extraction), " +
                        "$canonical)",
                        new DOMHandle());

            Document actualInstance = handle.get();
            assertEquals("extract-canonical returns an instance", actualInstance.getDocumentElement().getLocalName(), entityTypeNoVersion);

            // dom returned from extraction must equal test instance.
            String controlFilePath = "/test-instances/" + entityTypeTestFileName;
            Document controlDom = builder.parse(this.getClass().getResourceAsStream(controlFilePath));

             logger.debug("Control doc");
             debugOutput(controlDom);
             logger.debug("Actual doc wrapped");
             debugOutput(actualInstance);

            assertThat("Extract instance by default returns identity",
                actualInstance,
                CompareMatcher.isIdenticalTo(controlDom).ignoreWhitespace());

            // test that XML from envelope returns the instance.
            String testToInstance = "es:instance-xml-from-document( doc('"+entityTypeTestFileName+"-envelope.xml') )";
            handle = evalOneResult(moduleImport(entityType), testToInstance, new DOMHandle());
            actualInstance = handle.get();
            assertThat("Extract instance by default returns identity", actualInstance,
                CompareMatcher.isIdenticalTo(controlDom).ignoreWhitespace());

            // moreover, extracting the attachments also will result in identity.
            DOMHandle domHandle = evalOneResult(moduleImport(entityType),
                "es:instance-get-attachments( doc('"+entityTypeTestFileName+"-envelope.xml') )",
                new DOMHandle());
            Document originalDocument = domHandle.get();
            assertThat("Original document also matches source", originalDocument,
                CompareMatcher.isIdenticalTo(controlDom).ignoreWhitespace());

            logger.debug("Removing test data");
            docMgr.delete(entityTypeTestFileName +"-envelope.xml", entityTypeTestFileName + "-empty.xml");

        }
    }

    private int sizeOf(EvalResultIterator results) {
        int size = 0;
        while (results.hasNext()) {
            size++;
            EvalResult result = results.next();
            // logger.debug(result.get(new StringHandle()).get());
        }
        return size;
    }

    @Test
    public void testInstanceFunctionCardinality() {
        checkCardinality("/test-envelope-with-sequences.xml", 3);
        checkCardinality("/test-envelope-no-instances.xml", 0);
    }

    private void checkCardinality(String docUri, int nResults) {
        InputStream testEnvelope = this.getClass().getResourceAsStream("/model-units" + docUri);
        XMLDocumentManager xmlDocMgr = client.newXMLDocumentManager();
        xmlDocMgr.write(docUri, new InputStreamHandle(testEnvelope).withFormat(Format.XML));

        StringHandle stringHandle;
        EvalResultIterator results;

        results = eval("",
            "es:instance-from-document( doc('" + docUri + "'))");
        assertEquals("Document has three instances.", sizeOf(results), nResults);

        results = eval("",
            "es:instance-json-from-document( doc('" + docUri + "'))");
        assertEquals("Document has three instances (json)", sizeOf(results), nResults);

        results = eval( "",
            "es:instance-xml-from-document( doc('" + docUri + "'))");
        assertEquals("Document has three instances (xml)", sizeOf(results), nResults);

        results = eval( "",
            "es:instance-get-attachments( doc('" + docUri + "'))");
        assertEquals("Document has three attachments", sizeOf(results), nResults);

        xmlDocMgr.delete(docUri);
    }


    @Test
    public void testJsonAttachments()
    {
        InputStream testEnvelope = this.getClass().getResourceAsStream("/model-units/test-envelope-json-attachment.xml");
        XMLDocumentManager xmlDocMgr = client.newXMLDocumentManager();
        xmlDocMgr.write("/test-envelope-json-attachment.xml", new InputStreamHandle(testEnvelope).withFormat(Format.XML));

        StringHandle stringHandle = evalOneResult("",
            "es:instance-get-attachments( doc('/test-envelope-json-attachment.xml') )",
            new StringHandle());

        String actual = stringHandle.get();

        assertEquals("{\"bah\":\"yes\"}", actual);

        xmlDocMgr.delete("/test-envelope-json-attachment.xml");
    }

    @Test
    public void testXMLEnvelopeFunction() throws TestEvalException {

        for (String entityType : converters.keySet()) {
            String functionCall =
                 "let $p := map:map()"
                +"let $_ := map:put($p, '$type', 'Order')"
                +"let $_ := map:put($p, 'prop', 'val')"
                +"let $_ := map:put($p, '$attachments', element source { 'bah' })"
                +"return conv:instance-to-xml-envelope( $p )";

            DOMHandle handle = evalOneResult(moduleImport(entityType), functionCall, new DOMHandle());
            Document document = handle.get();
            Element docElement = document.getDocumentElement();
            assertEquals("envelope function verification", "envelope", docElement.getLocalName());
            NodeList nl = docElement.getChildNodes();
            assertEquals("Envelope must have two children.", 2, nl.getLength());
            for (int i=0; i<nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    logger.debug("Checking node name " + n.getLocalName());
                    Element e = (Element) n;
                    assertTrue(e.getLocalName().equals("instance") || e.getLocalName().equals("attachments"));
                }
            }
        }

    }

    @Test
    public void testEnvelopeFunction() throws TestEvalException {

        for (String entityType : converters.keySet()) {
            String functionCall =
                "let $p := map:map()"
                    +"let $_ := map:put($p, '$type', 'Order')"
                    +"let $_ := map:put($p, 'prop', 'val')"
                    +"let $_ := map:put($p, '$attachments', element source { 'bah' })"
                    +"return conv:instance-to-envelope( $p )";

            DOMHandle handle = evalOneResult(moduleImport(entityType), functionCall, new DOMHandle());
            Document document = handle.get();
            Element docElement = document.getDocumentElement();
            assertEquals("envelope function verification", "envelope", docElement.getLocalName());
            NodeList nl = docElement.getChildNodes();
            assertEquals("Envelope must have two children.", 2, nl.getLength());
            for (int i=0; i<nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    logger.debug("Checking node name " + n.getLocalName());
                    Element e = (Element) n;
                    assertTrue(e.getLocalName().equals("instance") || e.getLocalName().equals("attachments"));
                }
            }
        }

    }

    @Test
    public void testJSONEnvelopeFunction() throws TestEvalException, IOException, SAXException {

        for (String entityType : converters.keySet()) {
            String functionCall =
                "let $p := map:map()"
                    +"let $_ := map:put($p, '$type', 'Order')"
                    +"let $_ := map:put($p, 'prop', 'val')"
                    +"let $_ := map:put($p, '$attachments', element source { 'bah' })"
                    +"return conv:instance-to-json-envelope( $p )";

            JacksonHandle handle = evalOneResult(moduleImport(entityType), functionCall, new JacksonHandle());

            JsonNode envelope = handle.get();
            assertNotNull(envelope.get("envelope").get("instance").get("info"));
            assertNotNull(envelope.get("envelope").get("instance").get("Order"));

        }


    }

    @Test
    public void testJSONSerialization() throws IOException, ParserConfigurationException, SAXException {

        String initialTest = "Order-0.0.4.json";
        String instanceDocument = "Order-Source-2.xml";
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document instanceWithArrayOfOne =
            builder.parse(this.getClass().getResourceAsStream("/source-documents/Order-Source-2.xml"));
        client.newXMLDocumentManager().write(instanceDocument, new DOMHandle(instanceWithArrayOfOne));

        JsonNode control = new ObjectMapper()
            .readValue(
                this.getClass().getResourceAsStream("/source-documents/Order-Source-2.json"),
                JsonNode.class);

        evalOneResult(
            moduleImport(initialTest),
            "let $envelope := conv:instance-to-envelope( conv:extract-instance-Order( doc('Order-Source-2.xml') ) )"
                +"return xdmp:document-insert('Order-Source-2.xml-envelope.xml', $envelope) ",
            new StringHandle());

        JacksonHandle instanceAsJSONHandle =
            evalOneResult("", "es:instance-json-from-document( doc('Order-Source-2.xml-envelope.xml') )", new JacksonHandle());

        JsonNode jsonInstance = instanceAsJSONHandle.get();
        org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(jsonInstance));

        // another test for new failing instance.
        instanceDocument = "Order-Source-4.xml";
        Document instanceWithReference =
            builder.parse(this.getClass().getResourceAsStream("/source-documents/Order-Source-4.xml"));
        client.newXMLDocumentManager().write(instanceDocument, new DOMHandle(instanceWithReference));

        control = new ObjectMapper()
            .readValue(
                this.getClass().getResourceAsStream("/source-documents/Order-Source-4.json"),
                JsonNode.class);

        evalOneResult(
            moduleImport(initialTest),
            "let $envelope := conv:instance-to-envelope( conv:extract-instance-Order( doc('Order-Source-4.xml') ) )"
                +"return xdmp:document-insert('Order-Source-4.xml-envelope.xml', $envelope) ",
            new StringHandle());

        instanceAsJSONHandle =
            evalOneResult("", "es:instance-json-from-document( doc('Order-Source-4.xml-envelope.xml') )", new JacksonHandle());

        jsonInstance = instanceAsJSONHandle.get();
        org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(jsonInstance));
    }

    @Test
    public void testJSONEmptyArray() throws IOException, ParserConfigurationException, SAXException {

        String initialTest = "Order-0.0.4.json";
        String instanceDocument = "Order-Source-3.xml";
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document instanceWithArryayOfOne =
            builder.parse(this.getClass().getResourceAsStream("/source-documents/Order-Source-3.xml"));
        client.newXMLDocumentManager().write(instanceDocument, new DOMHandle(instanceWithArryayOfOne));

        JsonNode control = new ObjectMapper()
            .readValue(
                this.getClass().getResourceAsStream("/source-documents/Order-Source-3.json"),
                JsonNode.class);

        evalOneResult(
            moduleImport(initialTest),
            "let $envelope := conv:instance-to-envelope( conv:extract-instance-Order( doc('Order-Source-3.xml') ) )"
            +"return xdmp:document-insert('Order-Source-3.xml-envelope.xml', $envelope) ",
            new StringHandle());

        JacksonHandle instanceAsJSONHandle =
            evalOneResult("", "es:instance-json-from-document( doc('Order-Source-3.xml-envelope.xml') )", new JacksonHandle());

        JsonNode jsonInstance = instanceAsJSONHandle.get();
        org.hamcrest.MatcherAssert.assertThat(control, org.hamcrest.Matchers.equalTo(jsonInstance));
    }

    @AfterClass
    public static void removeConversions() {
        Set<String> toDelete = new HashSet<String>();
        converters.keySet().forEach(x -> toDelete.add("/ext/" + x.replaceAll("\\.(xml|json)", ".xqy")));
        //docMgr.delete(toDelete.toArray(new String[] {}));
    }
}
