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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import com.marklogic.client.datamovement.WriteBatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.DocumentMetadataHandle.Capability;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.datamovement.DataMovementManager;

/**
 * Base class for examples. See the importJSON method for generic loading of
 * JSON from a directory tree.
 */
public abstract class ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(ExamplesBase.class);

    protected DataMovementManager moveMgr;
    protected ObjectMapper mapper;
    protected Properties props;
    protected DatabaseClient client;
	protected String projectDir;

    // this is just the first example, TODO refactor.
    protected String getExampleName() {
        return "example-races";
    };

    public ExamplesBase() {
        props = new Properties();
        try {
            props.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Error reading application.properties file");
        }

        client = DatabaseClientFactory.newClient(props.getProperty("mlHost"),
                Integer.parseInt(props.getProperty("mlRestPort")), new DatabaseClientFactory.DigestAuthContext(
                        props.getProperty("mlUsername"), props.getProperty("mlPassword")));

        Path currentRelativePath = Paths.get("");
        projectDir = currentRelativePath.toAbsolutePath().toString();
        // FIXME this is a hack for intellij.
        if (!projectDir.endsWith("examples")) projectDir += "/entity-services-examples";

        projectDir += "/" + getExampleName();
        logger.debug("Project path is: " + projectDir);
        moveMgr = client.newDataMovementManager();
        mapper = new ObjectMapper();

    }

    private WriteBatcher newBatcher() {
        WriteBatcher batcher = moveMgr.newWriteBatcher().withBatchSize(100).withThreadCount(5)
                .onBatchSuccess(batch -> logger.info("Loaded batch of documents"))
                .onBatchFailure((batch, throwable) -> {
                    logger.error("FAILURE on batch:" + batch.toString() + "\n", throwable);
                    System.err.println(throwable.getMessage());
                    System.err.println(
                            Arrays.stream(batch.getItems())
                                    .map(item -> item.getTargetUri())
                                    .collect(Collectors.joining("\n")));
                    // throwable.printStackTrace();
                });

        return batcher;
    }

    private void importOrDescend(Path entry, WriteBatcher batcher, String collection, Format format) {
        if (entry.toFile().isFile()) {
            logger.debug("Adding " + entry.getFileName().toString());
            String uri = entry.toUri().toString();
            if (collection != null) {
                DocumentMetadataHandle metadata = new DocumentMetadataHandle().withCollections(collection) //
                    .withPermission("race-reader", Capability.READ) //
                    .withPermission("race-writer", Capability.INSERT, Capability.UPDATE);
                batcher.add(uri, metadata, new FileHandle(entry.toFile()).withFormat(format));
            } else {
                batcher.add(uri, new FileHandle(entry.toFile()).withFormat(format));
            }
            logger.debug("Inserted " + format.toString() + " document " + uri);
        }
        else {
            logger.info("Reading subdirectory " + entry.getFileName().toString());
            // importOrDescend(entry, batcher, collection, format);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(entry)) {
                for (Path child : stream) {
                    importOrDescend(child, batcher, collection, format);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * This method uses a document-centric approach to RDF reference data, by
     * invoking a server-side transform that parses turtle into MarkLogic XML
     * triples.
     */
    public void importRDF(Path referenceDataDir, String collection) {

        logger.info("RDF Load Job started");

        WriteBatcher batcher = newBatcher().withTransform(new ServerTransform("turtle-to-xml"));

        importOrDescend(referenceDataDir, batcher, collection, Format.TEXT);

        moveMgr.startJob(batcher);
        batcher.flushAndWait();
    }

    public void importJSON(Path jsonDirectory) throws InterruptedException, IOException {
        importJSON(jsonDirectory, null);
    }

    public void importJSON(Path jsonDirectory, String toCollection) throws IOException {

        logger.info("JSON job started.");

        WriteBatcher batcher = newBatcher();
        importOrDescend(jsonDirectory, batcher, toCollection, Format.JSON);

        moveMgr.startJob(batcher);
        batcher.flushAndWait();
    }

    public void importXML(Path xmlDirectory, String toCollection) throws IOException {

        logger.info("JSON job started.");

        WriteBatcher batcher = newBatcher();

        importOrDescend(xmlDirectory, batcher, toCollection, Format.XML);

        moveMgr.startJob(batcher);
        batcher.flushAndWait();
    }

}
