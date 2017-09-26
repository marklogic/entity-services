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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.io.JacksonHandle;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class TestDatabaseProperties extends EntityServicesTestBase {

    @BeforeClass
    public static void setup() {
           setupClients();
        TestSetup.getInstance().loadEntityTypes("/json-models", "SchemaCompleteEntityType-0.0.1.json");
        TestSetup.getInstance().loadEntityTypes("/json-models", "Person-0.0.2.json");
    }

    @Test
    public void testDatabasePropertiesGenerate() throws IOException, TestEvalException {
        JacksonHandle handle =
                evalOneResult("","fn:doc('SchemaCompleteEntityType-0.0.1.json')=>es:database-properties-generate()", new JacksonHandle());
        JsonNode databaseConfiguration = handle.get();

        //logger.debug(databaseConfiguration.toString());

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = this.getClass().getResourceAsStream("/expected-database-properties/content-database.json");
        JsonNode control = mapper.readValue(is, JsonNode.class);

        org.hamcrest.MatcherAssert.assertThat(databaseConfiguration, org.hamcrest.Matchers.equalTo(control));

        // another test case for namespace-uri checking
        handle =
            evalOneResult("","fn:doc('Person-0.0.2.json')=>es:database-properties-generate()", new JacksonHandle());
        databaseConfiguration = handle.get();

        //logger.debug(databaseConfiguration.toString());

        is = this.getClass().getResourceAsStream("/expected-database-properties/person-content-database.json");
        control = mapper.readValue(is, JsonNode.class);

        org.hamcrest.MatcherAssert.assertThat(databaseConfiguration, org.hamcrest.Matchers.equalTo(control));
    }

}
