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
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.datamovement.ApplyTransformListener;
import com.marklogic.datamovement.JobTicket;
import com.marklogic.datamovement.QueryHostBatcher;

/**
 * Runs the load methods for entity services, rdf, and json instances.
 */
public class ExamplesLoader extends ExamplesBase {

	private static Logger logger = LoggerFactory.getLogger(ExamplesLoader.class);

	public ExamplesLoader() throws IOException {
		super();
	}

	public Thread modelsLoad() {
		Runnable task = () -> {
			try {
				importJSON(Paths.get(props.getProperty("projectDir") + "/data/models"),
						"http://marklogic.com/entity-services/models");
			} catch (IOException e) {
				logger.error("IOException thrown by loader.");
			}
		};
		task.run();
		return new Thread(task);
	}

	public Thread instanceLoad() {
		Runnable task = () -> {
			try {
				importJSON(Paths.get(props.getProperty("projectDir") + "/data/race-data"), "raw");
			} catch (IOException e) {
				logger.error("IOException thrown by loader.");
			}
		};
		task.run();
		return new Thread(task);
	}

	public Thread rdfLoad() {
		Runnable task = () -> {
			importRDF(Paths.get(props.getProperty("projectDir") + "/data/third-party/rdf"), "reference");
		};
		task.run();
		return new Thread(task);
	}

	public void harmonize() throws InterruptedException {
		StructuredQueryBuilder qb = new StructuredQueryBuilder();
		qb.collection("raw");
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

		QueryHostBatcher queryHostBatcher = moveMgr.newQueryHostBatcher(qb.build()).withBatchSize(100)
				.withThreadCount(5).onUrisReady(listener).onQueryFailure((client3, exception) -> {
					logger.error("Query error");
				});

		JobTicket ticket = moveMgr.startJob(queryHostBatcher);
		queryHostBatcher.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		moveMgr.stopJob(ticket);
	}

	public void secondSourceHarmonize() throws InterruptedException {
		StructuredQueryBuilder qb = new StructuredQueryBuilder();
		qb.collection("csv");
		ServerTransform ingester = new ServerTransform("ingester-angel-island");
		ApplyTransformListener listener = new ApplyTransformListener().withTransform(ingester)
				.withApplyResult(ApplyTransformListener.ApplyResult.IGNORE).onSuccess((dbClient, inPlaceBatch) -> {
					logger.debug("batch transform SUCCESS");
				}).onBatchFailure((dbClient, inPlaceBatch, throwable) -> {
					logger.error("FAILURE on batch:" + inPlaceBatch.toString() + "\n", throwable);
					System.err.println(throwable.getMessage());
					System.err.print(String.join("\n", inPlaceBatch.getItems()) + "\n");
					// throwable.printStackTrace();
				});

		QueryHostBatcher queryHostBatcher = moveMgr //
				.newQueryHostBatcher(qb.build()) //
				.withBatchSize(10) //
				.withThreadCount(5) //
				.onUrisReady(listener) //
				.onQueryFailure((client3, exception) -> {
					logger.error("Query error");
				});

		JobTicket ticket = moveMgr.startJob(queryHostBatcher);
		queryHostBatcher.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		moveMgr.stopJob(ticket);
	}

	private void loadAsIs() {
		modelsLoad().start();
		instanceLoad().start();
		rdfLoad().start();
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		ExamplesLoader loader = new ExamplesLoader();
		loader.loadAsIs();

		CSVLoader integrator = new CSVLoader();
		integrator.go();

		logger.info("Starting harmonize");
		loader.harmonize();
		loader.secondSourceHarmonize();

		CodeGenerator generator = new CodeGenerator();
		generator.generate();
	}
}
