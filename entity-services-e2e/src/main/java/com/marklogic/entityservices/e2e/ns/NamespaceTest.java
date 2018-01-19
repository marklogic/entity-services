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
package com.marklogic.entityservices.e2e.ns;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.entityservices.e2e.ExamplesBase;
import com.marklogic.client.datamovement.ApplyTransformListener;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.QueryBatcher;

import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.concurrent.TimeUnit;

/**
 * Created by bsrikan on 9/6/17.
 */
public class NamespaceTest extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(NamespaceTest.class);
    
    private static final String EXAMPLE_NAME = "e2e-namespace";

    protected String getE2eName() {
        return EXAMPLE_NAME;
    }

    public void harmonize() throws InterruptedException {
        StructuredQueryBuilder qb = new StructuredQueryBuilder();
        StructuredQueryDefinition qdef = qb.collection("raw");
        ServerTransform ingester = new ServerTransform("namespace");
        ApplyTransformListener listener = new ApplyTransformListener().withTransform(ingester)
                .withApplyResult(ApplyTransformListener.ApplyResult.IGNORE).onSuccess(inPlaceBatch -> {
                    logger.debug("Batch transform SUCCESS");
                }).onBatchFailure((inPlaceBatch, throwable) -> {
                    System.err.println(throwable.getMessage());
                    System.err.print(String.join("\n", inPlaceBatch.getItems()) + "\n");
                });

        QueryBatcher queryBatcher = moveMgr.newQueryBatcher(qdef).withBatchSize(100)
                .withThreadCount(5).onUrisReady(listener).onQueryFailure(exception -> {
                    logger.error("Query error");
                });

        JobTicket ticket = moveMgr.startJob(queryBatcher);
        queryBatcher.awaitCompletion();
        moveMgr.stopJob(ticket);
    }

    public void translate() throws InterruptedException {
        StructuredQueryBuilder qb = new StructuredQueryBuilder();
        StructuredQueryDefinition qdef = qb.collection("namespace-envelopes");
        ServerTransform ingester = new ServerTransform("nstranslate");
        ApplyTransformListener listener = new ApplyTransformListener().withTransform(ingester)
                .withApplyResult(ApplyTransformListener.ApplyResult.IGNORE).onSuccess(inPlaceBatch -> {
                    logger.debug("batch transform SUCCESS");
                }).onBatchFailure((inPlaceBatch, throwable) -> {
                    logger.error("FAILURE on batch:" + inPlaceBatch.toString() + "\n", throwable);
                    //System.err.println(throwable.getMessage());
                    //System.err.print(String.join("\n", inPlaceBatch.getItems()) + "\n");
                });

        QueryBatcher queryBatcher = moveMgr.newQueryBatcher(qdef).withBatchSize(100) //
                .withThreadCount(5).onUrisReady(listener).onQueryFailure(exception -> {
                    logger.error("Query error");
                });

        JobTicket ticket = moveMgr.startJob(queryBatcher);
        queryBatcher.awaitCompletion();
        moveMgr.stopJob(ticket);
    }
    
    /* Load the models and source docs*/
    public void setup() throws InterruptedException {

        try {
        	importJSON(Paths.get(projectDir + "/data/models"), "http://marklogic.com/entity-services/models");
            importJSON(Paths.get(projectDir + "/data/namespace"), "raw");
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
    
    public static void main(String[] args) throws IOException, InterruptedException {

    	NamespaceTest ns = new NamespaceTest();
    	
    	logger.info("Loading models and source docs");
    	ns.setup();
    	
        logger.info("Starting harmonize");
        ns.harmonize();
        
        logger.info("Starting translate of namespace");
        ns.translate();
    }

}
