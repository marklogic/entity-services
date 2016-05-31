package com.marklogic.entityservices.examples;

import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.Format;
import com.marklogic.datamovement.WriteHostBatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads entity types from a directory specified in the application properties.
 */
public class EntityServicesLoader extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(EntityServicesLoader.class);

    private EntityServicesLoader() throws IOException {
        super();
    }

    private void go() throws IOException, InterruptedException {
        importJSON(Paths.get(props.getProperty("entityTypesDir")));
    }

    public static void main(String[] args) throws Exception {

        EntityServicesLoader loader = new EntityServicesLoader();
        loader.go();

    }
}
