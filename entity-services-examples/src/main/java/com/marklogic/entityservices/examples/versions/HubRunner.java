package com.marklogic.entityservices.examples.versions;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.util.RequestParameters;
import com.marklogic.entityservices.examples.ExamplesBase;
import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Implements a trivial data hub backed by a REST API
 * resource extension.
 */
public class HubRunner extends ResourceManager {

    /* loads the data models */
    class ModelLoader extends ExamplesBase {

        // this is just the first example, TODO refactor.
        protected String getExampleName() {
            return "example-versions";
        };

        public void modelsLoad() {
            try {
                importJSON(Paths.get(projectDir + "/data/models"),
                    "http://marklogic.com/entity-services/models");
            } catch (IOException e) {
                logger.error("IOException thrown by loader.");
            };
        }

    }


    /* the number of records in the example */
    private static int N_RECORDS = 3;

    private static Logger logger = LoggerFactory.getLogger(HubRunner.class);
    private DatabaseClient client;

    private Properties props;

    public HubRunner(String hubName) throws IOException {
        super();
        props = new Properties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
        Path currentRelativePath = Paths.get("");
        String projectDir = currentRelativePath.toAbsolutePath().toString();
        if (!projectDir.endsWith("examples")) projectDir += "/entity-services-examples";

        client = DatabaseClientFactory.newClient(props.getProperty("mlHost"),
            Integer.parseInt(props.getProperty("mlRestPort")), new DatabaseClientFactory.DigestAuthContext(
                props.getProperty("mlAdminUsername"), props.getProperty("mlAdminPassword")));

        client.init(hubName, this);
    }

    public void loadData(long nRecords, int nFields) {
        p("Loading and extracting "+N_RECORDS+" records with "+nFields+" fields.");
        Fairy fairy = Fairy.create();
        for (long i=0; i<nRecords; i++) {
            RequestParameters params = new RequestParameters();
            StringBuilder sb = new StringBuilder();
            StringHandle handle = new StringHandle().withFormat(Format.JSON);
            Person person = fairy.person();
            sb.append("{\"id\":"+i + ",");
            sb.append(" \"firstName\":\""+person.getFirstName()+"\"");
            if (nFields > 2) {
                sb.append(", \"fullName\":\""+person.getFullName()+"\"}");
            } else {
                sb.append("}");
            }
            handle.set(sb.toString());
            getServices().put(params, handle, new StringHandle());
        }
    }

    public void search(String queryString) {
        search(queryString, null);
    }

    public void search(String queryString, String version) {
        RequestParameters params = new RequestParameters();
        params.add("q", queryString);
        if (version != null) params.add("version",version);
        StringHandle handle = getServices().get(params, new StringHandle());
        p(handle.get());
    }

    public void sql(String queryString) {
        RequestParameters params = new RequestParameters();
        params.add("sql", queryString);
        try {
            StringHandle handle = getServices().get(params, new StringHandle());
            p(handle.get());
        } catch (FailedRequestException e) {
            if (e.getMessage().contains("SQL-TABLE")) {
                p("View not found");
            } else {
                throw e;
            }

        }
    }

    // hubC has a PUT method that
    public void upgradeData() {
        String emptyPayload = "";
        StringHandle bodyHandle = new StringHandle(emptyPayload);
        StringHandle responseHandle = getServices().put(new RequestParameters(), bodyHandle, new StringHandle());
        p(responseHandle.get());
    }

    public void clear() {
        p("Clearing all content from the content db.");
        getServices().delete(new RequestParameters(), new StringHandle());
        new ModelLoader().modelsLoad();
    }

    private static void p(String msg) {
        System.out.println(msg);
    }

    public void queries() {
        queries(null);
    }

    public void queries(String version) {
        p("Search for id:1");
        search("id:1");

        if (version != null) {
            p("id:1 plus version " + version);
            search("id:1", version);
        }

        p("Search for fullName:A*");
        search("fullName:A*");

        p("Select *");
        sql("select * from Model.Person order by id");

        p("Select *");
        sql("select * from ModelNext.Person order by fullName");
    }

    public static void main(String[] args) throws IOException {
        HubRunner hub = new HubRunner("hub");

        p("Original Hub");
        hub.clear();
        hub.loadData(N_RECORDS, 2);
        hub.queries();

        hub.clear();
        hub.loadData(N_RECORDS, 3);
        hub.queries();

        // optional interim hub A, up-converting.
        p("Making transitional hub A");
        HubRunner hubA = new HubRunner("transition-A");
        hubA.clear();
        hubA.loadData(N_RECORDS, 2);
        hubA.queries();
        hubA.queries("next");

        // optional interim hub B, hybrid hub
        p("Making transitional hub B");
        HubRunner hubB = new HubRunner("transition-B");
        hubB.clear();
        hubB.loadData(N_RECORDS, 3);
        hubB.queries();
        hubB.queries("next");

        // optional interim hub C, migrate instances
        p("Making transitional hub C");
        HubRunner hubC = new HubRunner("transition-C");
        // refresh original hub data
        hub.clear();
        hub.loadData(N_RECORDS, 3);
        hubC.upgradeData();
        hubC.queries("original");
        hubC.queries();

        p("Making transitional hub D");
        HubRunner hubD = new HubRunner("transition-D");
        // refresh original hub data
        hub.clear();
        hub.loadData(N_RECORDS, 3);
        hubD.upgradeData();
        hubD.queries("original");
        hubD.queries();

        p("Making hub-next");
        HubRunner hubNext = new HubRunner("hub-next");
        hubNext.clear();
        hubNext.loadData(N_RECORDS, 3);
        hubNext.queries();
    }
}
