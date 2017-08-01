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
package com.marklogic.entityservices;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;


public class TestSetup {
    
	
    private static TestSetup instance = null;
    protected static Logger logger = LoggerFactory.getLogger(EntityServicesTestBase.class);
	private DatabaseClient _client;
    private DatabaseClient _modulesClient, _schemasClient;
    protected static Collection<File> testCaseFiles;
	protected static DocumentBuilder builder;
	private Set<String> entityTypes, sourceFileUris, invalidFileUris;
			
    public Set<String> getEntityTypes() {
		return entityTypes;
	}

	public Set<String> getSourceFileUris() {
		return sourceFileUris;
	}

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
        
        
        instance.loadEntityTypes();
        instance.loadInvalidEntityTypes();
        instance.loadExtraFiles();
        instance.storeCustomConversionModules();
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
    
    

	Collection<File> getTestResources(String dirName) {
		URL filesUrl = _client.getClass().getResource(dirName);

		return FileUtils.listFiles(new File(filesUrl.getPath()),
	            FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
	}
	
	private void loadInvalidEntityTypes() {
		
	    JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    
		Collection<File> testInvalidFiles = getTestResources("/invalid-entity-types");
		invalidFileUris = new HashSet<String>();
		logger.info("Beginning to load Invalid Entity Types");
	    for (File f : testInvalidFiles) {
	    	if (f.getName().startsWith(".")) { continue; }
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml")||f.getName().endsWith(".jpg"))) { continue; }
	    	
	    	// uncomment for quick iteration on TDE.
	    	// if (!f.getName().startsWith("Person-0.0.2")) {continue; };
	    	//if (!f.getName().equals("OrderDetails-0.0.3.json")) {continue; };
	    	//if (!f.getName().startsWith("refs")) {continue; };
	    	//logger.info("Loading Invalid " + f.getName());
	    	//docMgr.write(f.getPath(), new FileHandle(f));
	    	DocumentMetadataHandle metadata = new DocumentMetadataHandle();
	        
	    	writeSet.add(f.getName(), metadata, new FileHandle(f));
	       
	    	invalidFileUris.add(f.getName());
	    }
	    docMgr.write(writeSet);
	    logger.info("Done loading Invalid Entity Types");
	}
	
	private void loadEntityTypes() {
		
	    JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    
		testCaseFiles = getTestResources("/json-entity-types");
		testCaseFiles.addAll(getTestResources("/xml-entity-types"));
		testCaseFiles.addAll(getTestResources("/binary"));
		entityTypes = new HashSet<String>();
		
		logger.info("Beginning to load Entity Types");
	    for (File f : testCaseFiles) {
	    	if (f.getName().startsWith(".")) { continue; }
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml")||f.getName().endsWith(".jpg"))) { continue; }
	    	
	    	// uncomment for quick iteration on TDE.
	    	// if (!f.getName().startsWith("Person-0.0.2")) {continue; };
	    	//if (!f.getName().equals("OrderDetails-0.0.3.json")) {continue; };
	    	//if (!f.getName().startsWith("refs")) {continue; };
	    	//logger.info("Loading ET Docs " + f.getName());
	    	//docMgr.write(f.getPath(), new FileHandle(f));
	    	DocumentMetadataHandle metadata = new DocumentMetadataHandle();
	        metadata.getCollections().addAll(
	        		"http://marklogic.com/entity-services/models",
	        		f.getName());
	        
	    	writeSet.add(f.getName(), metadata, new FileHandle(f));
	       
	        entityTypes.add(f.getName());
	    }
	    docMgr.write(writeSet);
	    logger.info("Done loading Entity Types");
	}

	private void loadExtraFiles() {
		sourceFileUris = new HashSet<String>();
	    
		JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    
	    Collection<File> sourceFiles = getTestResources("/source-documents");
	    
	    Collection<File> testDocuments = getTestResources("/test-instances");
	    
	    	    Collection<File> extraDocuments = new ArrayList<File>();
	    extraDocuments.addAll(testDocuments);
	    extraDocuments.addAll(sourceFiles);
	    
	    logger.info("Beginning to load Source Documents");
	    for (File f : extraDocuments) {
	    	if (f.getName().startsWith(".")) { continue; }
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; }
	    	
	    	//logger.info("Loading Extra Files " + f.getName());
	    	writeSet.add(f.getName(), new FileHandle(f));
	        sourceFileUris.add(f.getName());
	    }
	    docMgr.write(writeSet);
	    logger.info("Done loading Source Documents");
	}
	
    private void storeCustomConversionModules() {
        
        JSONDocumentManager docMgr = _modulesClient.newJSONDocumentManager();
        DocumentWriteSet writeSet = docMgr.newWriteSet();
        Collection<File> custConvMod = getTestResources("/customized-conversion-module");
        
        for (File f : custConvMod) {
        
            String moduleName = "/conv/" + f.getName();
            DocumentMetadataHandle metadata = new DocumentMetadataHandle();
            logger.info("Loading custom xqy module " + f.getName());
            writeSet.add(moduleName, metadata, new FileHandle(f));
        }
        docMgr.write(writeSet);
    }
	
	public void teardownClass() {
		JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    for (File f : testCaseFiles) {
	    	logger.info("Removing " + f.getName());
		    docMgr.delete(f.getName());
	    }

	    Collection<File> sourceFiles = getTestResources("/source-documents");
	    Collection<File> testDocuments = getTestResources("/test-instances");
	    Collection<File> testInvalidFiles = getTestResources("/invalid-entity-types");
	    Collection<File> extraDocuments = new ArrayList<File>();
	    extraDocuments.addAll(testDocuments);
	    extraDocuments.addAll(sourceFiles);
	    extraDocuments.addAll(testInvalidFiles);
	    
	    for (File f : extraDocuments) {
	    	docMgr.delete(f.getName());
	    }
	}
}
