package com.marklogic.entityservices.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.Format;
import com.marklogic.datamovement.DataMovementManager;
import com.marklogic.datamovement.JobTicket;
import com.marklogic.datamovement.WriteHostBatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Base class for examples.
 * See the importJSON method for generic loading of JSON from a directory tree.
 */
public abstract class ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(ExamplesBase.class);

    protected DataMovementManager moveMgr;
    protected JobTicket ticket;
    protected ObjectMapper mapper;
    protected Properties props;

    public ExamplesBase() throws IOException {
        props = new Properties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));

        DatabaseClient client = DatabaseClientFactory.newClient(
                props.getProperty("mlHost"),
                Integer.parseInt(props.getProperty("mlRestPort")),
                new DatabaseClientFactory.DigestAuthContext(
                    props.getProperty("mlUsername"),
                    props.getProperty("mlPassword")));

        moveMgr = DataMovementManager.newInstance().withClient(client);
        mapper = new ObjectMapper();
    }

    protected void importOrDescend(Path directory, WriteHostBatcher batcher, String collection, Format format) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (entry.toFile().isDirectory()) {
                    logger.info("Reading subdirectory " + entry.getFileName().toString());
                    importOrDescend(entry, batcher, collection, format);
                } else {
                    logger.info("Adding " + entry.getFileName().toString());
                    String uri = entry.toString();
                    if (collection != null) {
                        DocumentMetadataHandle metadata = new DocumentMetadataHandle().withCollections(collection);
                        batcher.add(uri, metadata, new FileHandle(entry.toFile()).withFormat(format));
                    }
                    else {
                        batcher.add(uri, new FileHandle(entry.toFile()).withFormat(format));
                    }
                    logger.info("Inserted " + format.toString() + " document " + uri);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void importJSON(Path jsonDirectory) throws InterruptedException, IOException {
       importJSON(jsonDirectory, null);
    }

    public void importJSON(Path jsonDirectory, String toCollection) throws InterruptedException, IOException {

        logger.info("job started.");

        WriteHostBatcher batcher = moveMgr.newWriteHostBatcher()
                .withBatchSize(10)
                .withThreadCount(2)
                .onBatchSuccess( (client, batch) ->  logger.info("Loaded entity types batch") )
                .onBatchFailure(
                        (client, batch, throwable) -> {
                            logger.warn("FAILURE on batch:" + batch.toString() + "\n",
                                    throwable);
                            throwable.printStackTrace();
                        }
                );

        ticket=moveMgr.startJob(batcher);

        importOrDescend(jsonDirectory, batcher, toCollection, Format.JSON);

        batcher.flush();
    }


}



