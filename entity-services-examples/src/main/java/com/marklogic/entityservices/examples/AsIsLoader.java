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
package com.marklogic.entityservices.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by cgreer on 8/25/16.
 */
public class AsIsLoader extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(Harmonizer.class);

    public Thread modelsLoad() {
        Runnable task = () -> {
            try {
                importJSON(Paths.get(projectDir + "/data/models"),
                        "http://marklogic.com/entity-services/models");
            } catch (IOException e) {
                logger.error("IOException thrown by loader.");
            }
        };
        task.run();
        return new Thread(task);
    }

    public Thread instanceLoad() {
        Runnable task = () -> {
            try {
                importJSON(Paths.get(projectDir + "/data/race-data"), "raw");
            } catch (IOException e) {
                logger.error("IOException thrown by loader.");
            }
        };
        task.run();
        return new Thread(task);
    }

    public Thread rdfLoad() {
        Runnable task = () -> {
            importRDF(Paths.get(projectDir + "/data/third-party/rdf"), "reference");
        };
        task.run();
        return new Thread(task);
    }

    public void loadAsIs() {
        modelsLoad().start();
        instanceLoad().start();
        rdfLoad().start();
    }

}
