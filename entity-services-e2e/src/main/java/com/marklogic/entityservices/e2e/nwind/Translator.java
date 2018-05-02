/*
 * Copyright 2016-2018 MarkLogic Corporation
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

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.client.datamovement.ApplyTransformListener;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.QueryBatcher;
import com.marklogic.entityservices.e2e.CodeGenerator;
import com.marklogic.entityservices.e2e.ExamplesBase;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.util.concurrent.TimeUnit;

public class Translator extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(Translator.class);

    public void translate() throws InterruptedException {
        StructuredQueryBuilder qb = new StructuredQueryBuilder();
        StructuredQueryDefinition qdef = qb.collection("upconverts");
        ServerTransform ingester = new ServerTransform("translator");
        ApplyTransformListener listener = new ApplyTransformListener().withTransform(ingester)
                .withApplyResult(ApplyTransformListener.ApplyResult.IGNORE).onSuccess(inPlaceBatch -> {
                    logger.debug("Batch transform SUCCESS");
                }).onBatchFailure((inPlaceBatch, throwable) -> {
                    // logger.warn("FAILURE on batch:" + inPlaceBatch.toString()
                    // + "\n", throwable);
                    // throwable.printStackTrace();
                    System.err.println(throwable.getMessage());
                    System.err.print(String.join("\n", inPlaceBatch.getItems()) + "\n");
                });

        QueryBatcher queryBatcher = moveMgr.newQueryBatcher(qdef).withBatchSize(50)
                .withThreadCount(1).onUrisReady(listener).onQueryFailure(exception -> {
                    logger.error("Query error");
                });

        JobTicket ticket = moveMgr.startJob(queryBatcher);
        queryBatcher.awaitCompletion();
        moveMgr.stopJob(ticket);
    }
    
}
