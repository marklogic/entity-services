package com.marklogic.entityservices.examples;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.Format;
import com.marklogic.datamovement.WriteHostBatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *  Loads reference data from BBOC sports ontology and LDBC instance data
 *  into the "reference" collection from a property.
 *  Uses a server-side transform to parse turtle and store the parsed
 *  results within a single managed document.
 */
public class RdfDataLoader extends ExamplesBase {

    @SuppressWarnings("unused")
    private static Logger logger = LoggerFactory.getLogger(EntityServicesLoader.class);

    /*
      This method uses a document-centric approach to RDF reference data, by invoking
      a server-side transform that parses turtle into MarkLogic XML triples.
     */
    public void importRDF(Path referenceDataDir, String collection) {

        logger.info("RDF Load Job started");

        WriteHostBatcher batcher = moveMgr.newWriteHostBatcher()
                .withBatchSize(1)
                .withThreadCount(1)
                .withTransform(new ServerTransform("turtleToXml"))
                .onBatchSuccess( (client, batch) ->  logger.info("Loaded rdf data batch") )
                .onBatchFailure(
                        (client, batch, throwable) -> {
                            logger.warn("FAILURE on batch:" + batch.toString() + "\n",
                                    throwable);
                            throwable.printStackTrace();
                        }
                );
        ;
        ticket=moveMgr.startJob(batcher);

        importOrDescend(referenceDataDir, batcher, collection, Format.TEXT);

        batcher.flush();

    }

    private RdfDataLoader() throws IOException {
        super();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        RdfDataLoader load = new RdfDataLoader();
        load.importRDF(Paths.get(load.props.getProperty("referenceDataDir") + "/rdf"), "reference");

    }

}
