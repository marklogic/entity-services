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
package com.marklogic.entityservices.examples.suppliers;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.extensions.ResourceServices;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.util.RequestParameters;
import com.marklogic.entityservices.examples.ExamplesBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * This example has two models -- one that mimics a suppliers/parts schema from wikipedia,
 * and one that has some more readable names.  We generate random data to populate
 * instances of the first model, then convert them with the version translator to the new one.
 */
public class SuppliersParts extends ExamplesBase {

    public class DataGenerator extends ResourceManager {

        public DataGenerator(DatabaseClient client) throws IOException {
            super();
            client.init("new-suppliers", this);
        }

        public void genCode(int size) {
            RequestParameters params = new RequestParameters();
            params.add("n", Integer.toString(size));
            StringHandle input = new StringHandle("input");
            StringHandle output = new StringHandle("input");
            this.getServices().put(params, input, output);
        }
    }

    private static Logger logger = LoggerFactory.getLogger(SuppliersParts.class);

    private static final String EXAMPLE_NAME = "example-suppliers";

    protected String getExampleName() {
        return EXAMPLE_NAME;
    }


    public void setup() throws InterruptedException, IOException {

        /* Load the model */
        importJSON(Paths.get(projectDir + "/suppliers-0.0.1.json"), "http://marklogic.com/entity-services/models");


        /* generate data */
        new DataGenerator(client).genCode(1000);

    }


    public static void main(String[] args) throws InterruptedException, IOException {

        SuppliersParts sp = new SuppliersParts();

        sp.setup();

    }
}
