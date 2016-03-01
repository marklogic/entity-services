package com.marklogic.entityservices;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.FileHandle;


public class EntityServicesTestBase {

	protected static Logger logger = LoggerFactory.getLogger(EntityServicesTestBase.class);
	protected static DatabaseClient client;
	protected static Set<String> entityTypes = new HashSet<String>();
	protected static DocumentBuilder builder;

	@SuppressWarnings("unchecked")
	@BeforeClass
	public static void setupClass() throws IOException, ParserConfigurationException {
	    TestSetup testSetup = TestSetup.getInstance();
	    client = testSetup.getClient();
	    JSONDocumentManager docMgr = client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    
		URL jsonFilesUrl = client.getClass().getResource("/json-entity-types");
		URL xmlFilesUrl = client.getClass().getResource("/xml-entity-types");
		
		Collection<File> files = FileUtils.listFiles(new File(jsonFilesUrl.getPath()), 
	            FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
	    Collection<File> xmlFiles = FileUtils.listFiles(new File(xmlFilesUrl.getPath()), 
	            FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
	    files.addAll(xmlFiles);
	    
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		builder = factory.newDocumentBuilder();
		
	    for (File f : files) {
	    	if (f.getName().startsWith(".")) { continue; };
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; };
	    	
	    	// uncomment for quick iteration on TDE.
	    	//if (!f.getName().equals("Person-0.0.2.json")) {continue; };
	    	// if (!f.getName().equals("schema-complete-entity-type.xml")) {continue; };
	    	//if (!f.getName().startsWith("refs")) {continue; };
	    	logger.info("Loading " + f.getName());
	    	//docMgr.write(f.getPath(), new FileHandle(f));
	        writeSet.add(f.getName(), new FileHandle(f));
	        entityTypes.add(f.getName());
	    }
	    docMgr.write(writeSet);
	}

	@AfterClass
	public static void teardownClass() {
	    // teardown.
	}
   
}

