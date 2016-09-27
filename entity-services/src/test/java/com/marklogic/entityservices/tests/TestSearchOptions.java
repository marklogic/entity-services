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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestSearchOptions extends EntityServicesTestBase {

    @BeforeClass
    public static void setup() {
        setupClients();
        TestSetup.getInstance().loadEntityTypes("/json-models", "SchemaCompleteEntityType-0.0.1.json");
    }

    @Test
    public void testSearchOptionsGenerate() throws IOException, TestEvalException, SAXException, TransformerException {
        DOMHandle handle = evalOneResult(
            "fn:doc('SchemaCompleteEntityType-0.0.1.json')=>es:search-options-generate()", new DOMHandle());
        Document searchOptions = handle.get();

        //debugOutput(searchOptions);


        InputStream is = this.getClass().getResourceAsStream("/expected-search-options/SchemaCompleteEntityType-0.0.1.xml");
        Document filesystemXML = builder.parse(is);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLAssert.assertXMLEqual("Search options validation failed.", filesystemXML, searchOptions);


        // if this call has results, the search options are not valid.
        EvalResultIterator checkOptions = eval(
           "fn:doc('SchemaCompleteEntityType-0.0.1.json')=>es:search-options-generate()=>search:check-options()",
           "import module namespace search = 'http://marklogic.com/appservices/search' at '/MarkLogic/appservices/search/search.xqy';");
        assertFalse("Too many results for check options to pass", checkOptions.hasNext());
        /* This is for diagnostics during changes:
        while (checkOptions.hasNext()) {
            EvalResult result = checkOptions.next();
            DOMHandle domHandle = result.get(new DOMHandle());
            Document dom = domHandle.get();
            String emptyOptions = "<search:options xmlns:search='http://marklogic.com/appservices/search'/>";
            XMLAssert.assertXMLEqual("Check options did not pass.", emptyOptions, dom);
        }
        */


    }

}
