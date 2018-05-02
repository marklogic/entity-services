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


import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.StringHandle;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests the server-side function es:echema-generate($model) as
 * element(xsd:schema)
 */
public class TestSchemaGeneration extends EntityServicesTestBase {

    private static XMLDocumentManager docMgr;
    private static Map<String, StringHandle> schemas;

    @BeforeClass
    public static void setupClass() {
        setupClients();

        entityTypes = TestSetup.getInstance().loadEntityTypes("/json-models", ".*.json$");
        TestSetup.getInstance().loadExtraFiles("/test-instances",".*");
        docMgr = schemasClient.newXMLDocumentManager();
        schemas = generateSchemas();

        InputStream is = docMgr.getClass().getResourceAsStream("/model-units/et-duplicate-prop.json");
        JSONDocumentManager documentManager = client.newJSONDocumentManager();
        documentManager.write("et-duplicate-prop.json", new InputStreamHandle(is).withFormat(Format.JSON));
    }

    private static void storeSchema(String entityTypeName, StringHandle schemaHandle) {
        logger.debug("Loading schema " + entityTypeName);
        String moduleName = entityTypeName.replaceAll("\\.(xml|json)", ".xsd");
        docMgr.write(moduleName, schemaHandle);
    }

    private static void removeSchema(String entityTypeName) {
        logger.debug("Removing schema " + entityTypeName);
        String moduleName = entityTypeName.replaceAll("\\.(xml|json)", ".xsd");
        docMgr.delete(moduleName);
    }

    private static StringHandle generateSchema(String entityType) {
        logger.info("Generating schema: " + entityType);
        StringHandle schema = new StringHandle();
        try {
            schema = evalOneResult("", "fn:doc( '" + entityType + "')=>es:schema-generate()",
                    schema);
        } catch (TestEvalException e) {
            throw new RuntimeException(e);
        }
        return schema;
    }

    private static Map<String, StringHandle> generateSchemas() {
        Map<String, StringHandle> map = new HashMap<String, StringHandle>();

        for (String entityType : entityTypes) {
            StringHandle schema = generateSchema(entityType);
            map.put(entityType, schema);
        }
        return map;
    }

    @Test
    public void verifySchemaValidation() throws TestEvalException, SAXException, IOException {

        for (String entityType : entityTypes) {
            // there's a special test for SchemaComplete, which has two namespaces.
            if (entityType.startsWith("SchemaCompleteEntityType-0.0.1")) continue;
            String testInstanceName = entityType.replaceAll("\\.(json|xml)$", "-0.xml");

            storeSchema(entityType, schemas.get(entityType));
            DOMHandle validateResult = evalOneResult("", "validate strict { doc('" + testInstanceName + "') }",
                    new DOMHandle());

            InputStream is = this.getClass().getResourceAsStream("/test-instances/" + testInstanceName);
            Document filesystemXML = builder.parse(is);
            assertThat("Must be no validation errors for schema " + entityType + ".", filesystemXML,
                CompareMatcher.isIdenticalTo(validateResult.get()).ignoreWhitespace());
            removeSchema(entityType);
        }

    }

    @Test
    public void verifyDualNamespaceModel() throws TestEvalException, SAXException, IOException {

        String model = "SchemaCompleteEntityType-0.0.1.json";
        String schema1 = "SchemaCompleteEntityType-0.0.1.xsd";
        String schema2 = "OrderDetails-0.0.1.xsd";
        String testInstanceName = "SchemaCompleteEntityType-0.0.1-0.xml";


        try {
            EvalResultIterator results = eval("", "fn:doc( '" + model + "')=>es:schema-generate()");
            StringHandle firstResult = results.next().get(new StringHandle());
            StringHandle secondResult = results.next().get(new StringHandle());

            if (firstResult.get().contains("order-details-namespace")) {
                storeSchema(schema2, firstResult);
                storeSchema(schema1, secondResult);
            } else {
                storeSchema(schema2, secondResult);
                storeSchema(schema1, firstResult);
            }
        } catch (TestEvalException e) {
            throw new RuntimeException(e);
        }

        DOMHandle validateResult = evalOneResult("", "validate strict { doc('" + testInstanceName + "') }",
            new DOMHandle());

        InputStream is = this.getClass().getResourceAsStream("/test-instances/" + testInstanceName);
        Document filesystemXML = builder.parse(is);
        assertThat("Must be no validation errors for schema " + model + ".", filesystemXML,
            CompareMatcher.isIdenticalTo(validateResult.get()).ignoreWhitespace());
        removeSchema(schema1);
        removeSchema(schema2);

    }


    @Test
    public void verifyDuplicatePropertyComments() throws TestEvalException, TransformerException {
        String entityType = "et-duplicate-prop.json";
        StringHandle schema = generateSchema(entityType);
        storeSchema("et-duplicate-prop.xml", schema);

        String integerValidates = "<ETTwo><a>123</a></ETTwo>";
        String stringShouldnt = "<ETTwo><a>asdf</a></ETTwo>";

        DOMHandle validateResult = evalOneResult("", "validate strict { " + integerValidates + "}", new DOMHandle());
        //debugOutput(validateResult.get());
        try {
            validateResult = evalOneResult("", "validate strict { " + stringShouldnt + "}", new DOMHandle());
            fail("Should be a validation error here.");
        } catch (TestEvalException e) {
            //pass
        }
        removeSchema(entityType);
    }

    @AfterClass
    // TODO is this required?
    public static void cleanupSchemas() {

        Set<String> toDelete = new HashSet<String>();
        schemas.keySet().forEach(x -> toDelete.add(x.replaceAll("\\.(xml|json)", ".xsd")));
        docMgr.delete(toDelete.toArray(new String[] {}));
    }
}
