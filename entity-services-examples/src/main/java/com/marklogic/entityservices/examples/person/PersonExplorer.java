package com.marklogic.entityservices.examples.person;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.expression.PlanBuilder;
import com.marklogic.client.expression.PlanBuilder.Plan;
import com.marklogic.client.expression.XsExpr;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.client.row.RowManager;
import com.marklogic.client.semantics.RDFTypes;
import com.marklogic.client.semantics.SPARQLQueryDefinition;
import com.marklogic.client.semantics.SPARQLQueryManager;
import com.marklogic.client.datamovement.ApplyTransformListener;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.QueryBatcher;
import com.marklogic.entityservices.examples.ExamplesBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class uses three separare sources and extract functions
 * to show how one might instantiate and test entity instance data.
 */
public class PersonExplorer extends ExamplesBase {

    private static Logger logger = LoggerFactory.getLogger(PersonExplorer.class);

    private static final String EXAMPLE_NAME = "example-person";

    protected String getExampleName() {
        return EXAMPLE_NAME;
    }

    private HashMap<String,String> prefixes = new HashMap<String, String>();

    private static String PREFIXES =
             "prefix es: <http://marklogic.com/entity-services#> " +
             "prefix : <http://example.org/example-person/> ";

    public PersonExplorer() {
        super();
        prefixes.put("http://marklogic.com/entity-services#", "es");
        prefixes.put("http://example.org/example-person/", "");
        prefixes.put("http://example.org/example-person/Person-0.0.1/Person/", "pt");
        prefixes.put("http://example.org/example-person/Person-0.0.1/", "ps");
        prefixes.put("http://www.w3.org/2001/XMLSchema#", "xs");
        prefixes.put("http://marklogic.com/json#", "json");
    }

    private String valueFor(JsonNode val) {
        String type = val.get("type").asText();
        String stringValue = val.get("value").asText();
        if (type.equals("uri")) {
            int index = Math.max(stringValue.lastIndexOf("/"), stringValue.lastIndexOf("#"));
            String uriPrefix = stringValue.substring(0, index + 1);
            if (prefixes.containsKey(uriPrefix)) {
                String prefix = prefixes.get(uriPrefix);
                return prefix + ":" + stringValue.substring(index + 1);
            }
        }
        // fallthrough
        return stringValue;
    }

    private String table(JsonNode sparqlResults) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        StringBuffer sb = new StringBuffer();
        // sb.append(mapper.writeValueAsString(sparqlResults));

        ArrayNode columns = (ArrayNode) sparqlResults.get("head").get("vars");
        columns.forEach( c -> { sb.append(c + "\t"); });
        sb.append("\n");
        ArrayNode bindings = (ArrayNode) sparqlResults.get("results").get("bindings");
        bindings.forEach( b -> {
            b.forEach( col -> {
                sb.append(valueFor(col));
                sb.append("\t");
            });
            sb.append("\n");
        });
        return sb.toString();
    }

    private void verifyPersonModel() {
        System.out.println("Verifying person model with SPARQL.  This query reports all the property names and types for the Person Model.");
        SPARQLQueryManager qMgr = client.newSPARQLQueryManager();
        String sparql = "select ?title ?propertyName ?scalarType ?arrayType where { ?s a es:Model ; " +
                " es:title ?title ; " +
                " es:definitions ?type ." +
                " ?type es:property ?property ." +
                " ?property es:title ?propertyName ." +
                " ?property es:datatype ?scalarType ." +
                " optional " +
                "   { " +
                "     { ?property es:items/es:ref ?arrayType } " +
                "     UNION " +
                "     { ?property es:items/es:datatype ?arraytype } " +
                "   }" +
                " }";
        SPARQLQueryDefinition qdef = qMgr.newQueryDefinition(PREFIXES + sparql);
        qdef.withBinding("title", "Person", RDFTypes.STRING);

        System.out.println(qdef.getSparql());

        JacksonHandle handle = qMgr.executeSelect(qdef, new JacksonHandle());
        JsonNode results = handle.get();
        try {
            System.out.println(table(results));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void harmonize() throws InterruptedException {
        StructuredQueryBuilder qb = new StructuredQueryBuilder();
        StructuredQueryDefinition qdef = qb.collection("raw");
        ServerTransform ingester = new ServerTransform("person-harmonizer");
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

        QueryBatcher queryBatcher = moveMgr.newQueryBatcher(qdef).withBatchSize(100)
                .withThreadCount(5).onUrisReady(listener).onQueryFailure((client3, exception) -> {
                    logger.error("Query error");
                });

        JobTicket ticket = moveMgr.startJob(queryBatcher);
        queryBatcher.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        moveMgr.stopJob(ticket);
    }

    private void verifyHarmonize() {
        System.out.println("Verifying person instances with SPARQL.");
        System.out.println("This query joins instance data to the model and returns all IRIs that are typed 'Person'");
        SPARQLQueryManager qMgr = client.newSPARQLQueryManager();
        String sparql = "select ?person ?typeName " +
                " where { ?person a ?personType . " +
                " ?personType es:title ?typeName } ";
        SPARQLQueryDefinition qdef = qMgr.newQueryDefinition(PREFIXES + sparql);
        // qdef.withBinding("title", "Person", RDFTypes.STRING);

        System.out.println(qdef.getSparql());

        JacksonHandle handle = qMgr.executeSelect(qdef, new JacksonHandle());
        JsonNode results = handle.get();
        try {
            System.out.println(table(results));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void runOpticQuery() {
        RowManager rowManager = client.newRowManager();
        PlanBuilder pb = rowManager.newPlanBuilder();
        XsExpr xs = pb.xs;


        // Plan p = pb.fromView("Person", "Person").where(pb.fn.contains(xs.string(pb.col("id")), xs.string("122")));


//        p =
//            pb.fromView("Person", "Person")
//                .where(pb.eq(pb.col("id"), xs.string("122")))
//                .select(pb.col("firstName"));

        Plan p = pb.fromTriples(pb.pattern(pb.subjects(pb.col("s")),
            pb.predicates(pb.sem.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#")),
            pb.objects(pb.col("o"))));
        JsonNode results = rowManager.resultDoc(p, new JacksonHandle()).get();

        try {
            System.out.println(table(results));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Load the models */
    public void setup() throws InterruptedException {

        try {
            importJSON(Paths.get(projectDir + "/person-0.0.1.json"), "http://marklogic.com/entity-services/models");
            importJSON(Paths.get(projectDir + "/people"), "raw");
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public static void main(String[] args) throws InterruptedException {

        PersonExplorer explorer = new PersonExplorer();
        explorer.setup();

        explorer.verifyPersonModel();

        explorer.harmonize();

        explorer.verifyHarmonize();

        explorer.runOpticQuery();

    }
}

