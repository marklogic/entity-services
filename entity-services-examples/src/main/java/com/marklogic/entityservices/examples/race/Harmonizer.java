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
package com.marklogic.entityservices.examples.race;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.client.datamovement.ApplyTransformListener;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.QueryHostBatcher;
import com.marklogic.entityservices.examples.ExamplesBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by cgreer on 8/25/16.
 */
public class Harmonizer extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(Harmonizer.class);

    public void harmonize() throws InterruptedException {
        StructuredQueryBuilder qb = new StructuredQueryBuilder();
        StructuredQueryDefinition qdef = qb.collection("raw");
        ServerTransform ingester = new ServerTransform("ingester");
        ApplyTransformListener listener = new ApplyTransformListener().withTransform(ingester)
                .withApplyResult(ApplyTransformListener.ApplyResult.IGNORE).onSuccess((dbClient, inPlaceBatch) -> {
                    logger.debug("Batch transform SUCCESS");
                }).onBatchFailure((dbClient, inPlaceBatch, throwable) -> {
                    // logger.warn("FAILURE on batch:" + inPlaceBatch.toString()
                    // + "\n", throwable);
                    // throwable.printStackTrace();
                    System.err.println(throwable.getMessage());
                    System.err.print(String.join("\n", inPlaceBatch.getItems()) + "\n");
                });

        QueryHostBatcher queryHostBatcher = moveMgr.newQueryHostBatcher(qdef).withBatchSize(100)
                .withThreadCount(5).onUrisReady(listener).onQueryFailure((client3, exception) -> {
                    logger.error("Query error");
                });

        JobTicket ticket = moveMgr.startJob(queryHostBatcher);
        queryHostBatcher.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        moveMgr.stopJob(ticket);
    }

    public void secondSourceHarmonize() throws InterruptedException {
        StructuredQueryBuilder qb = new StructuredQueryBuilder();
        StructuredQueryDefinition qdef = qb.collection("raw", "csv");
        ServerTransform ingester = new ServerTransform("ingester-angel-island");
        ApplyTransformListener listener = new ApplyTransformListener().withTransform(ingester)
                .withApplyResult(ApplyTransformListener.ApplyResult.IGNORE).onSuccess((dbClient, inPlaceBatch) -> {
                    logger.debug("batch transform SUCCESS");
                }).onBatchFailure((dbClient, inPlaceBatch, throwable) -> {
                    logger.error("FAILURE on batch:" + inPlaceBatch.toString() + "\n", throwable);
                    //System.err.println(throwable.getMessage());
                    //System.err.print(String.join("\n", inPlaceBatch.getItems()) + "\n");
                });

        QueryHostBatcher queryHostBatcher = moveMgr //
                .newQueryHostBatcher(qdef) //
                .withBatchSize(100) //
                .withThreadCount(5) //
                .onUrisReady(listener) //
                .onQueryFailure((client3, exception) -> {
                    logger.error("Query error");
                });

        JobTicket ticket = moveMgr.startJob(queryHostBatcher);
        queryHostBatcher.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        moveMgr.stopJob(ticket);
    }

}
