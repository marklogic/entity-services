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
package com.marklogic.entityservices.e2e;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

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
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.WriteBatcher;

/**
 * Base class for examples. See the importJSON method for generic loading of
 * JSON from a directory tree.
 */
abstract class ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(ExamplesBase.class);

    protected DataMovementManager moveMgr;
    protected JobTicket ticket;
    protected ObjectMapper mapper;
    protected Properties props;
    protected DatabaseClient client;
	protected String projectDir;

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
        logger.debug("Current relative path is: " + projectDir);

        // FIXME this is a hack for intellij.
        if (!projectDir.endsWith("e2e")) projectDir += "/entity-services-e2e";

        moveMgr = client.newDataMovementManager();
        mapper = new ObjectMapper();

    }

    private void importOrDescend(Path directory, WriteBatcher batcher, String collection, Format format) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (entry.toFile().isDirectory()) {
                    logger.info("Reading subdirectory " + entry.getFileName().toString());
                    importOrDescend(entry, batcher, collection, format);
                } else {
                    logger.debug("Adding " + entry.getFileName().toString());
                    String uri = entry.toUri().toString();
                    if (collection != null) {
                        DocumentMetadataHandle metadata = new DocumentMetadataHandle().withCollections(collection) //
                                .withPermission("nwind-reader", Capability.READ) //
                                .withPermission("nwind-writer", Capability.INSERT, Capability.UPDATE);
                        batcher.add(uri, metadata, new FileHandle(entry.toFile()).withFormat(format));
                    } else {
                        batcher.add(uri, new FileHandle(entry.toFile()).withFormat(format));
                    }
                    logger.debug("Inserted " + format.toString() + " document " + uri);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * This method uses a document-centric approach to RDF reference data, by
     * invoking a server-side transform that parses turtle into MarkLogic XML
     * triples.
     */
    /* Uncomment when using rdf datasources
    public void importRDF(Path referenceDataDir, String collection) {

        logger.info("RDF Load Job started");

        WriteHostBatcher batcher = moveMgr.newWriteHostBatcher().withBatchSize(10).withThreadCount(1)
                .withTransform(new ServerTransform("turtle-to-xml"))
                .onBatchSuccess((client, batch) -> logger.info("Loaded rdf data batch"))
                .onBatchFailure((client, batch, throwable) -> {
                    logger.error("FAILURE on batch:" + batch.toString() + "\n", throwable);
                    System.err.println(throwable.getMessage());
                    System.err.println(
                            Arrays.stream(batch.getItems())
                                    .map(item -> item.getTargetUri())
                                    .collect(Collectors.joining("\n"))
                    );
                    // throwable.printStackTrace();
                });
        ;
        ticket = moveMgr.startJob(batcher);

        importOrDescend(referenceDataDir, batcher, collection, Format.TEXT);

        batcher.flush();

    }
    */

    public void importJSON(Path jsonDirectory) throws InterruptedException, IOException {
        importJSON(jsonDirectory, null);
    }

    public void importJSON(Path jsonDirectory, String toCollection) throws IOException {

        logger.info("job started.");

        WriteBatcher batcher = moveMgr.newWriteBatcher().withBatchSize(100).withThreadCount(5)
                .onBatchSuccess(batch -> logger.info("Loaded batch of JSON documents"))
                .onBatchFailure((batch, throwable) -> {
                    logger.error("FAILURE on batch:" + batch.toString() + "\n", throwable);
                    System.err.println(throwable.getMessage());
                    System.err.println(
                            Arrays.stream(batch.getItems())
                                    .map(item -> item.getTargetUri())
                                    .collect(Collectors.joining("\n")));
                    // throwable.printStackTrace();
                });

        ticket = moveMgr.startJob(batcher);

        importOrDescend(jsonDirectory, batcher, toCollection, Format.JSON);

        batcher.flushAndWait();
    }

}
