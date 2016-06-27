package com.marklogic.entityservices.tests;

import com.marklogic.client.document.DocumentManager;
import com.marklogic.client.document.TextDocumentManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.StringHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class TestVersionComparison extends EntityServicesTestBase {


    DocumentManager documentManager;
    String entityTypeTarget = "conversion-target.json";
    String entityTypeSource = "conversion-source.json";

    @Before
    public void generateArtifacts() throws TestEvalException, IOException {

        setupClients();
        InputStream is = this.getClass().getResourceAsStream("/entity-type-units/" + entityTypeTarget);
        documentManager = client.newJSONDocumentManager();
        documentManager.write(entityTypeTarget, new InputStreamHandle(is).withFormat(Format.JSON));
        is = this.getClass().getResourceAsStream("/entity-type-units/" + entityTypeSource);
        documentManager.write(entityTypeSource, new InputStreamHandle(is).withFormat(Format.JSON));

    }

    //@After
    public void remove() {

        documentManager = client.newJSONDocumentManager();
        documentManager.delete(entityTypeTarget);
        documentManager.delete(entityTypeSource);
        modulesClient.newTextDocumentManager().delete("/ext/version-comparison.xqy");

    }


    @Test
    public void testVersionComparison() throws TestEvalException {
        StringHandle handle =
            evalOneResult("let $source := doc('"+entityTypeSource+"')=>es:entity-type-from-node() "+
                          "let $target := doc('"+entityTypeTarget+"')=>es:entity-type-from-node() "+
                          "return es:version-comparison-generate($source, $target)", new StringHandle());

        TextDocumentManager mgr = modulesClient.newTextDocumentManager();
        mgr.write("/ext/version-comparison.xqy", handle);

        String instance1 = "instance-0.0.1.xml";
        InputStream is = this.getClass().getResourceAsStream("/entity-type-units/" + instance1);
        documentManager.write(instance1, new InputStreamHandle(is).withFormat(Format.XML));

        handle = evalOneResult("import module namespace c = 'http://example.org/tests/conversion-0.0.2-from-conversion-0.0.1' at '/ext/version-comparison.xqy';" +
                "doc('instance-0.0.1.xml')/*[ETOne] ! c:convert-instance-ETOne(.)", new StringHandle());

        logger.info(handle.get());


    }
}
