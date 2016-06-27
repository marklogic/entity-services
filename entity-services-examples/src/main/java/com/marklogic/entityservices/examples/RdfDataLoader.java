package com.marklogic.entityservices.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 *  Loads instance data into the "raw" collection from a property
 */
public class RdfDataLoader extends ExamplesBase {

    @SuppressWarnings("unused")
    private static Logger logger = LoggerFactory.getLogger(EntityServicesLoader.class);

    private RdfDataLoader() throws IOException {
        super();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        RdfDataLoader load = new RdfDataLoader();
        load.importRDF(Paths.get(load.props.getProperty("referenceDataDir")), "reference");

    }

}
