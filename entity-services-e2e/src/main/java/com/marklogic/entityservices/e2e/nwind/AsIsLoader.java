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
package com.marklogic.entityservices.e2e.nwind;

import com.marklogic.entityservices.e2e.ExamplesBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by cgreer on 8/25/16.
 */
public class AsIsLoader extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(Harmonizer.class);

    public void modelsLoad() {
        try {
            importJSON(Paths.get(projectDir + "/data/models"),
                    "http://marklogic.com/entity-services/models");
        } catch (IOException e) {
            logger.error("IOException thrown by loader.");
        };
    }

    public void instanceLoad() {
        try {
            importJSON(Paths.get(projectDir + "/data/northwind"), "raw");
        } catch (IOException e) {
            logger.error("IOException thrown by loader.");
        };
    }

    public void rdfLoad() {
        importRDF(Paths.get(projectDir + "/data/third-party/rdf"), "reference");
    }
    

    public void loadAsIs() {
        modelsLoad();
        instanceLoad();
        rdfLoad();
    }
}
