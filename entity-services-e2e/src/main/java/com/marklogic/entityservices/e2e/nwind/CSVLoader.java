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
package com.marklogic.entityservices.e2e.nwind;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.marklogic.client.datamovement.*;
import com.marklogic.entityservices.e2e.ExamplesBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.DocumentMetadataHandle.Capability;

/**
 * This file is simple an example of how to load CSV files into MarkLogic. It
 * uses a csvMapper to create JSON documents, one for each row in the CSV. For
 * EA-3 this is just an example on how to use DMSDK wtih CSV and JSON. It loads
 * these 'raw' JSON documents for later processing with in-place transform
 * services. (Post EA-3)
 */
public class CSVLoader extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(CSVLoader.class);

    private CsvSchema bootstrapSchema;
    private ObjectMapper csvMapper;

    public CSVLoader() {
        super();

        bootstrapSchema = CsvSchema.emptySchema().withHeader();
        csvMapper = new CsvMapper();
    }

    public void go() throws InterruptedException {

        logger.info("job started.");

        File dir = new File(projectDir + "/data/third-party/csv");

        WriteBatcher batcher = moveMgr.newWriteBatcher().withBatchSize(100).withThreadCount(10)
                .onBatchSuccess(batch -> logger.info(getSummaryReport(batch)))
                .onBatchFailure((batch, throwable) -> {
                    logger.warn("FAILURE on batch:" + batch.toString() + "\n", throwable);
                    throwable.printStackTrace();
                });

        moveMgr.startJob(batcher);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir.toPath(), "*.csv")) {
            for (Path entry : stream) {
                logger.debug("Adding " + entry.getFileName().toString());

                MappingIterator<ObjectNode> it = csvMapper.readerFor(ObjectNode.class).with(bootstrapSchema)
                        .readValues(entry.toFile());
                long i = 0;
                while (it.hasNext()) {
                    ObjectNode jsonNode = it.next();
                    String jsonString = mapper.writeValueAsString(jsonNode);

                    String uri = genUri(entry) + "-" + Long.toString(i++) + ".json";
                    DocumentMetadataHandle metadata = new DocumentMetadataHandle() //
                            .withCollections("raw", "csv") //
                            .withPermission("nwind-reader", Capability.READ) //
                            .withPermission("nwind-writer", Capability.INSERT, Capability.UPDATE);
                    batcher.add(uri, metadata, new StringHandle(jsonString));
                    if (i % 1000 == 0)
                        logger.debug("Inserting JSON document " + uri);
                }
                it.close();
            }
        }

        catch (IOException e) {
            e.printStackTrace();
        }
        batcher.flushAndWait();
    }

    private String getSummaryReport(Batch<WriteEvent> batch) {
        JobTicket ticket = batch.getJobTicket();
        JobReport report = moveMgr.getJobReport(ticket);
        if (report == null) {
            // is this a bug or not implemented TODO
            return "Report is null";
        } else {
            return "batches: " + report.getSuccessBatchesCount() + ", failures: "
                    + report.getFailureBatchesCount();
        }
    }

    public static void main(String[] args) throws Exception {

        CSVLoader integrator = new CSVLoader();
        integrator.go();

    }
}
