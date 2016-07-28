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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

public class TestEntityTypes extends EntityServicesTestBase {

    @BeforeClass
    public static void setupEntityTypes() {
        setupClients();
        entityTypes = TestSetup.getInstance().loadEntityTypes("/json-models", ".*.json$");
        entityTypes.addAll(TestSetup.getInstance().loadEntityTypes("/xml-models", ".*.xml$"));
    }

    private void checkRoundTrip(String message, JsonNode original, JsonNode actual) {
        assertEquals(message, original, actual);
    }
    
    private void checkXMLRoundTrip(String message, Document original, Document actual) {

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(message, original, actual);
    }
    
    private static Map<String, String> invalidMessages = new HashMap<String, String>();
    static {
        invalidMessages.put("invalid-bad-datatype.json", "Unsupported datatype");
        invalidMessages.put("invalid-missing-datatype.json", "If a property is not a reference, then it must have a datatype.");
        invalidMessages.put("invalid-missing-info.json", "Entity Type Document must contain exactly one info section.");
        invalidMessages.put("invalid-missing-title.json",   "section must be an object and contain exactly one title declaration.");
        invalidMessages.put("invalid-missing-version.json", "section must be an object and contain exactly one version declaration.");
        invalidMessages.put("invalid-property-ref-with-others.json", "If a property has $ref as a child, then it cannot have a datatype.");
        invalidMessages.put("invalid-multiple-pk.json", "only one primary key allowed.");
        invalidMessages.put("invalid-range-index-key.json", "unsupported for a range index.");
        invalidMessages.put("invalid-bad-datatype.xml", "Unsupported datatype");
        invalidMessages.put("invalid-missing-datatype.xml", "If a property is not a reference, then it must have a datatype.");
        invalidMessages.put("invalid-missing-info.xml", "Entity Type Document must contain exactly one info section.");
        invalidMessages.put("invalid-missing-title.xml", "section must be an object and contain exactly one title declaration.");
        invalidMessages.put("invalid-missing-version.xml", "section must be an object and contain exactly one version declaration.");
        invalidMessages.put("invalid-multiple-pk.xml", "only one primary key allowed");
        invalidMessages.put("invalid-property-ref-with-others.xml", "If a property has es:ref as a child, then it cannot have a datatype.");
        invalidMessages.put("invalid-range-index-key.xml", "unsupported for a range index.");
        invalidMessages.put("invalid-bad-absolute-reference.json", "must be a valid URI.");
        invalidMessages.put("invalid-bad-absolute-reference.xml", "must be a valid URI.");
        invalidMessages.put("invalid-bad-absolute-item-reference.json", "must be a valid URI.");
        invalidMessages.put("invalid-bad-absolute-item-reference.xml", "must be a valid URI.");
        invalidMessages.put("invalid-missing-reference.json", "must resolve to local entity type.");
        invalidMessages.put("invalid-missing-reference.xml", "must resolve to local entity type.");
        invalidMessages.put("invalid-array-no-items.json", "must contain an \"items\" declaration");
        invalidMessages.put("invalid-array-no-items.xml", "must contain an \"items\" declaration");
        invalidMessages.put("invalid-nested-array.json", "cannot both be an \"array\" and have items of type \"array\".");
        invalidMessages.put("invalid-nested-array.xml", "cannot both be an \"array\" and have items of type \"array\".");
        invalidMessages.put("invalid-bad-baseUri.json", "If present, baseUri (es:base-uri) must be an absolute URI.");
        invalidMessages.put("invalid-bad-baseUri.xml", "If present, baseUri (es:base-uri) must be an absolute URI.");
        invalidMessages.put("invalid-collation.json", "There is an invalid collation in the model.");
        invalidMessages.put("invalid-collation.xml", "There is an invalid collation in the model.");
        invalidMessages.put("invalid-bad-local-reference.json", "must be a valid URI.");
        invalidMessages.put("invalid-bad-local-reference.xml", "must be a valid URI.");
        invalidMessages.put("invalid-bad-local-item-reference.json", "must be a valid URI.");
        invalidMessages.put("invalid-bad-local-item-reference.xml", "must be a valid URI.");
        invalidMessages.put("invalid-required.json", "doesn't exist.");
        invalidMessages.put("invalid-required.xml", "doesn't exist.");
        invalidMessages.put("invalid-bad-title.json", "Title must have no whitespace and must start with a letter.");
        invalidMessages.put("invalid-bad-title.xml", "Title must have no whitespace and must start with a letter.");
        invalidMessages.put("invalid-missing-range-index.json", "doesn't exist.");
        invalidMessages.put("invalid-missing-range-index.xml", "doesn't exist");
        invalidMessages.put("invalid-missing-lexicon.json", "doesn't exist.");
        invalidMessages.put("invalid-missing-lexicon.xml", "doesn't exist.");
        invalidMessages.put("invalid-no-types.xml", "There must be at least one entity type in a model document");
        invalidMessages.put("invalid-no-types.json", "There must be at least one entity type in a model document");
        invalidMessages.put("invalid-bad-property.json", "Each property must be an object, with either \"datatype\" or \"$ref\" as a key.");
        invalidMessages.put("invalid-bad-external.json", "ref value must end with a simple name (xs:NCName).");
        invalidMessages.put("invalid-bad-external.xml", "ref value must end with a simple name (xs:NCName)");
        invalidMessages.put("invalid-property-type-conflict.xml", "Property names must be distinct from type names");
        invalidMessages.put("invalid-property-type-conflict.json", "Property names must be distinct from type names");
    }


    @Test
    public void testInvalidEntityTypes() throws URISyntaxException {

        URL sourcesFilesUrl = client.getClass().getResource("/invalid-models");

        @SuppressWarnings("unchecked")
        Collection<File> invalidEntityTypeFiles = FileUtils.listFiles(new File(sourcesFilesUrl.getPath()),
                FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        Set<String> invalidEntityTypes = new HashSet<String>();


        JSONDocumentManager docMgr = client.newJSONDocumentManager();
        DocumentWriteSet writeSet = docMgr.newWriteSet();

        for (File f : invalidEntityTypeFiles) {
            if (f.getName().startsWith(".")) { continue; };
            if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; };
            logger.info("Loading " + f.getName());
            writeSet.add(f.getName(), new FileHandle(f));
            invalidEntityTypes.add(f.getName());
        }
        docMgr.write(writeSet);

        for (String entityType : invalidEntityTypes) {
            logger.info("Checking invalid: " + entityType);
            @SuppressWarnings("unused")
            JacksonHandle handle = null;
            try {
                handle = evalOneResult("es:model-from-node(fn:doc('"+ entityType.toString()  + "'))", new JacksonHandle());
                fail("eval should throw an exception for invalid cases." + entityType);
            } catch (TestEvalException e) {
                assertTrue("Must contain invalidity message. Message was " + e.getMessage(),
                        e.getMessage().contains("ES-MODEL-INVALID"));

                assertTrue("Message must be expected one for " + entityType.toString() + ".  Was " + e.getMessage(), e.getMessage().contains(invalidMessages.get(entityType)));
            }
        }

        logger.info("Cleaning up invalid types");
        Collection<String> names = new ArrayList<String>();
        invalidEntityTypeFiles.forEach( f -> { names.add(f.getName()); });
        docMgr.delete(names.toArray(new String[] { }));

    }
    
    
    @Test
    /*
     * For each entity type in the test directory, verify that
     * it parses and that it matches the entity type parsed by
     * the server.
     * 
     * This test cycles through each test entity type.
     * If the entity type file name contains "invalid-" then it must
     * throw a validation exception.
     * 
     * Otherwise, the entity type is tested in comparison to an equivalent entity type in
     * xml-models
     * 
     * model-from-node   Serialized JSON equal to JSON file
     * model-to-json     JSON equal to JSON file
     * model-to-xml       Serialization to XML.
     */
    public void testEntityTypeParse() throws JsonParseException, JsonMappingException, IOException, TestEvalException, SAXException, ParserConfigurationException, TransformerException {
        for (String entityType : entityTypes) {
            ObjectMapper mapper = new ObjectMapper();
            logger.info("Checking "+entityType);

            checkTriples(entityType);

            if ( entityType.toString().endsWith(".json")) {
                InputStream is = this.getClass().getResourceAsStream("/json-models/"+entityType);
                JsonNode original = mapper.readValue(is, JsonNode.class);
                is.close();
                JacksonHandle handle  = evalOneResult("es:model-from-node(fn:doc('"+ entityType  + "'))", new JacksonHandle());
                JsonNode actual = handle.get();
                
                checkRoundTrip("Original node should equal serialized retrieved one: " +entityType, original, actual);
                
                checkEntityTypeToXML("Retrieved as XML, should match equivalent XML payload.", entityType.toString());
            } else {
                String jsonFileName = entityType.toString().replace(".xml", ".json");

                InputStream jsonInputStreamControl = this.getClass().getResourceAsStream("/json-models/" + jsonFileName);

                JsonNode jsonEquivalent = mapper.readValue(jsonInputStreamControl, JsonNode.class);
                jsonInputStreamControl.close();
                logger.debug("Validating and parsing " + entityType);
                JacksonHandle handle  = evalOneResult("es:model-from-node(fn:doc('"+ entityType  + "'))", new JacksonHandle());
                JsonNode jsonActual = handle.get();
                checkRoundTrip("Converted to a map:map, the XML entity type should match the json equivalent", jsonEquivalent, jsonActual);

                InputStream xmlControl = this.getClass().getResourceAsStream("/xml-models/"+entityType);
                Document xmloriginal = builder.parse(xmlControl);
                DOMHandle xmlhandle  = evalOneResult("es:model-to-xml(es:model-from-node(fn:doc('"+ entityType  + "')))", new DOMHandle());
                Document xmlactual = xmlhandle.get();

                //debugOutput(xmloriginal);
                //debugOutput(xmlactual);

                checkXMLRoundTrip("Original node should equal serialized retrieved one: " + entityType, xmloriginal, xmlactual);

                checkEntityTypeToJSON("Retrieved as JSON, should match equivalent JSON payload", entityType.toString(), jsonFileName);

            }
        }
    }
    
   
    /*
     * Checks parity of XML payload when retrieved from entity type.
     */
    private void checkEntityTypeToXML(String message, String entityTypeFile) throws TestEvalException, SAXException, IOException, ParserConfigurationException, TransformerException {
        String xmlFileName = entityTypeFile.replace(".json", ".xml");
        InputStream xmlFile = this.getClass().getResourceAsStream("/xml-models/" + xmlFileName);

        Document expectedXML = builder.parse(xmlFile);
        String evalXML =  "es:model-to-xml(es:model-from-node(fn:doc('" + entityTypeFile + "')))";

        DOMHandle handle = evalOneResult(evalXML, new DOMHandle());
        Document actualXML = handle.get();
        XMLUnit.setIgnoreWhitespace(true);
        //debugOutput(expectedXML);
        //debugOutput(actualXML);

        DetailedDiff diff = new DetailedDiff(new Diff(expectedXML, actualXML));

        @SuppressWarnings("unchecked")
        List<Difference> l = diff.getAllDifferences();
        for (Difference d : l) {
            System.out.println(d.toString());
        }
        XMLAssert.assertXMLEqual(message, expectedXML, actualXML);
    }
    
    /*
     * Checks parity of JSON payload when retrieved from XML-sourced entity type.
     */
    private void checkEntityTypeToJSON(String message, String entityTypeUri, String jsonUri) throws TestEvalException {
        String evalJSONEqual =  "deep-equal("
                   + "fn:doc('"+ jsonUri  +"')/node(), "
                   + "es:model-to-json(es:model-from-node(fn:doc('" + entityTypeUri + "')))"
                   + ")";

        StringHandle handle = evalOneResult(evalJSONEqual, new StringHandle());
        assertEquals(message, "true", handle.get());
    }
    

    private void checkTriples(String entityTypeUri) throws TestEvalException, IOException {
        StringHandle rdfHandle = evalOneResult("xdmp:set-response-output-method('n-triples'), '"+entityTypeUri + "'=>sem:iri()=>sem:graph()=>xdmp:quote()", new StringHandle() );

        Graph actualTriples = GraphFactory.createGraphMem();
        ByteArrayInputStream bis = new ByteArrayInputStream(rdfHandle.get().getBytes());
        RDFDataMgr.read(actualTriples, bis, Lang.NTRIPLES);
        
        
        Graph expectedTriples = GraphFactory.createGraphMem();
        Pattern filePattern = Pattern.compile("(.*)\\.(xml|json)$");
        Matcher matcher = filePattern.matcher(entityTypeUri);
        if (matcher.matches()) {
            String triplesFileUri =  "/triples-expected/" + matcher.group(1) + ".ttl";
            try {
                InputStream is = this.getClass().getResourceAsStream(triplesFileUri);
                RDFDataMgr.read(expectedTriples, is, Lang.TURTLE);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                RDFDataMgr.write(baos, actualTriples, Lang.TURTLE);
                logger.debug("Expected number of triples: " + expectedTriples.size());
                logger.debug("Actual number of triples: " + actualTriples.size());


                /* The following commented out code was for debuging purposes and
                   lingers for those who need to maintain on into the future. */
//                OutputStream os = new FileOutputStream(new File("/tmp/actual.ttl"));
//                RDFDataMgr.write(os, actualTriples, Lang.TURTLE);
//                os.close();
//                os = new FileOutputStream(new File("/tmp/expected.ttl"));
//                RDFDataMgr.write(os, expectedTriples, Lang.TURTLE);
//                os.close();

                // RDFDataMgr.write(System.out, actualTriples, Lang.TURTLE);

                // A great class for debugging, Defference.
//                logger.debug("Difference, expected - actual");
//                Graph diff = new com.hp.hpl.jena.graph.compose.Difference(expectedTriples, actualTriples);
//                RDFDataMgr.write(System.out, diff, Lang.TURTLE);

//                logger.debug("Difference, actual - expected");
//                Graph diff2 = new com.hp.hpl.jena.graph.compose.Difference(actualTriples, expectedTriples);
//                RDFDataMgr.write(System.out, diff2, Lang.TURTLE);


                assertTrue("Graph must match expected: " + entityTypeUri, actualTriples.isIsomorphicWith(expectedTriples));
            } catch (NullPointerException e) {
                logger.info("No RDF verification for " + entityTypeUri);
            }

        }
    }
}
