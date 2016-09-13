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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Runs the load methods for entity services, rdf, and json instances.
 */
public class ExamplesLoader {

    private static Logger logger = LoggerFactory.getLogger(ExamplesLoader.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        AsIsLoader loader = new AsIsLoader();
        loader.loadAsIs();

        CSVLoader integrator = new CSVLoader();
        integrator.go();

        logger.info("Starting harmonize");
        Harmonizer harmonizer = new Harmonizer();
        harmonizer.harmonize();
        harmonizer.secondSourceHarmonize();

        logger.info("Starting translate of Races");
        Translator translator = new Translator();
        translator.translate();

        CodeGenerator generator = new CodeGenerator();
        generator.generate();
    }
}
