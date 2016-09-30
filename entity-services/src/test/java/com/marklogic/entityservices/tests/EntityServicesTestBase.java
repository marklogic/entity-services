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

import java.io.*;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.marker.AbstractReadHandle;


public abstract class EntityServicesTestBase {

    protected static DatabaseClient client, modulesClient, schemasClient;
    protected static Set<String> entityTypes;

    protected static Logger logger = LoggerFactory.getLogger(EntityServicesTestBase.class);
    protected static DocumentBuilder builder;

    protected static void setupClients() {
        TestSetup testSetup = TestSetup.getInstance();
        client = testSetup.getClient();
        modulesClient = testSetup.getModulesClient();
        schemasClient = testSetup.getSchemasClient();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    @AfterClass
    public static void removeContent() {
        TestSetup testSetup = TestSetup.getInstance();
        testSetup.teardownClass();
    }

    protected static EvalResultIterator eval(String imports, String functionCall) throws TestEvalException {

        String entityServicesImport =
                "import module namespace es = 'http://marklogic.com/entity-services' at '/MarkLogic/entity-services/entity-services.xqy';\n" +
                "import module namespace esi = 'http://marklogic.com/entity-services-impl' at '/MarkLogic/entity-services/entity-services-impl.xqy';\n" +
                "import module namespace i = 'http://marklogic.com/entity-services-instance' at '/MarkLogic/entity-services/entity-services-instance.xqy';\n" +
                "import module namespace sem = 'http://marklogic.com/semantics' at '/MarkLogic/semantics.xqy';\n";

        String option = "declare option xdmp:mapping \"false\";";

        ServerEvaluationCall call =
                client.newServerEval().xquery(entityServicesImport + imports + option + functionCall);
        EvalResultIterator results = null;
        try {
            results = call.eval();
        } catch (FailedRequestException e) {
            throw new TestEvalException(e);
        }
        return results;
    }

    protected static <T extends AbstractReadHandle> T evalOneResult(String imports, String functionCall, T handle) throws TestEvalException {
        EvalResultIterator results =  eval(imports, functionCall);
        EvalResult result = null;
        if (results.hasNext()) {
            result = results.next();
            return result.get(handle);
        } else {
            return null;
        }
    }

    protected void debugOutput(Document xmldoc) throws TransformerException {
        debugOutput(xmldoc, System.out);
    }

    protected void debugOutput(Document xmldoc, OutputStream os) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(xmldoc), new StreamResult(os));
     }

    protected void save(String path, Document content) throws IOException, TransformerException {
        File outputFile = new File("src/test/resources/" + path );
        OutputStream os = new FileOutputStream(outputFile);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(content), new StreamResult(os));
    }

    protected void save(String path, String content) throws IOException {
        File outputFile = new File("src/test/resources/" + path );
        FileWriter writer = new FileWriter(outputFile);
        writer.write(content);
        writer.close();
    }
}

