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
package com.marklogic.entityservices;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;


public class TestSetup {
    
	
    private static TestSetup instance = null;
    protected static Logger logger = LoggerFactory.getLogger(EntityServicesTestBase.class);
	private DatabaseClient _client;
    private DatabaseClient _modulesClient, _schemasClient;
    protected static Collection<File> testCaseFiles;
	protected static DocumentBuilder builder;
	
    protected TestSetup() {
        // No instantiation allowed.
    }

    public synchronized static TestSetup getInstance() {
    	Properties prop = new Properties();
    	InputStream input = null;

    	try {

    	    input = prop.getClass().getResourceAsStream("/gradle.properties");

    	    // load a properties file
    	    prop.load(input);

    	} catch (IOException ex) {
    	    ex.printStackTrace();
    	    throw new RuntimeException(ex);
    	}
    	
    	String host = prop.getProperty("mlHost");
    	String username = prop.getProperty("mlUsername");
    	String password = prop.getProperty("mlPassword");
    	String port = prop.getProperty("mlRestPort");
    	String modulesDatabase = prop.getProperty("mlModulesDatabaseName");
    	String schemasDatabase = prop.getProperty("mlSchemasDatabaseName");

    	
    	if (instance == null) {
            instance = new TestSetup();
            if (instance._client == null) {
                instance._client = DatabaseClientFactory.newClient(host, Integer.parseInt(port), username, password, Authentication.DIGEST);
            }
            if (instance._modulesClient == null) {
            	instance._modulesClient = DatabaseClientFactory.newClient(host, Integer.parseInt(port), modulesDatabase,  username, password, Authentication.DIGEST );
            }
            if (instance._schemasClient == null) {
            	instance._schemasClient = DatabaseClientFactory.newClient(host, Integer.parseInt(port), schemasDatabase,  username, password, Authentication.DIGEST );
            }
        }
        return instance;
    }
    
    public DatabaseClient getClient() {
        return _client;
    }
    
    public DatabaseClient getModulesClient() {
    	return _modulesClient;
    }
    
    public DatabaseClient getSchemasClient() {
    	return _schemasClient;
    }
    
    

	@SuppressWarnings("unchecked")
	Collection<File> getTestResources(String dirName) {
		URL filesUrl = _client.getClass().getResource(dirName);

		return FileUtils.listFiles(new File(filesUrl.getPath()), 
	            FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
	}
	
	Set<String> loadEntityTypes() throws ParserConfigurationException {
		
	    JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    
		testCaseFiles = getTestResources("/json-entity-types");
		testCaseFiles.addAll(getTestResources("/xml-entity-types"));
		testCaseFiles.addAll(getTestResources("/binary"));
		Set<String> entityTypes = new HashSet<String>();
		
	    for (File f : testCaseFiles) {
	    	if (f.getName().startsWith(".")) { continue; };

	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml")|| f.getName().endsWith(".jpg"))) { continue; };


	    	
	    	// uncomment for quick iteration on TDE.
	    	// if (!f.getName().startsWith("Person-0.0.2")) {continue; };
	    	//if (!f.getName().equals("OrderDetails-0.0.3.json")) {continue; };
	    	//if (!f.getName().startsWith("refs")) {continue; };
	    	logger.info("Loading " + f.getName());
	    	//docMgr.write(f.getPath(), new FileHandle(f));
	    	DocumentMetadataHandle metadata = new DocumentMetadataHandle();
	        metadata.getCollections().addAll(
	        		"http://marklogic.com/entity-services/entity-types",
	        		f.getName());
	        
	    	writeSet.add(f.getName(), metadata, new FileHandle(f));
	       
	        entityTypes.add(f.getName());
	    }
	    docMgr.write(writeSet);
	    return entityTypes;
	}

	Set<String> loadExtraFiles() {
		Set<String> sourceFilesUris = new HashSet<String>();
	    
		JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    
	    Collection<File> sourceFiles = getTestResources("/source-documents");
	    
	    Collection<File> testDocuments = getTestResources("/test-instances");
	    
	    	    Collection<File> extraDocuments = new ArrayList<File>();
	    extraDocuments.addAll(testDocuments);
	    extraDocuments.addAll(sourceFiles);
	    
	    for (File f : extraDocuments) {
	    	if (f.getName().startsWith(".")) { continue; };
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; };
	    	
	    	logger.info("Loading " + f.getName());
	    	writeSet.add(f.getName(), new FileHandle(f));
	        sourceFilesUris.add(f.getName());
	    }
	    docMgr.write(writeSet);
	    return sourceFilesUris;
	}
	
	//@AfterClass
	public void teardownClass() {
		JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    for (File f : testCaseFiles) {
	    	logger.info("Removing " + f.getName());
		    docMgr.delete(f.getName());
	    }
	}
}
