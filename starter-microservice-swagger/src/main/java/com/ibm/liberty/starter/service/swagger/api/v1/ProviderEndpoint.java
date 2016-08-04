/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 *******************************************************************************/ 
package com.ibm.liberty.starter.service.swagger.api.v1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.NameFileFilter;

import com.ibm.liberty.starter.api.v1.model.provider.Dependency;
import com.ibm.liberty.starter.api.v1.model.provider.Dependency.Scope;
import com.ibm.liberty.starter.api.v1.model.provider.Location;
import com.ibm.liberty.starter.api.v1.model.provider.Provider;
import com.ibm.liberty.starter.api.v1.model.provider.ServerConfig;
import com.ibm.liberty.starter.api.v1.model.provider.Tag;

@Path("v1/provider")
public class ProviderEndpoint {
	
	private static final Logger log = Logger.getLogger(ProviderEndpoint.class.getName());
	
	private static final String GROUP_SUFFIX = "swagger";

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Provider details(@Context UriInfo info) {
    	Provider details = new Provider();
    	String description = getStringResource("/description.html");
    	details.setDescription(description);
    	
        Location repoLocation = new Location();
        String url = info.getBaseUri().resolve("../artifacts").toString();
        repoLocation.setUrl(url);
        details.setRepoUrl(repoLocation);
    	
        Dependency providedDependency = new Dependency();
        providedDependency.setScope(Scope.PROVIDED);
        providedDependency.setGroupId("net.wasdev.wlp.starters." + GROUP_SUFFIX);
        providedDependency.setArtifactId("provided-pom");
        providedDependency.setVersion("0.0.1");
     
        Dependency runtimeDependency = new Dependency();
        runtimeDependency.setScope(Scope.RUNTIME);
        runtimeDependency.setGroupId("net.wasdev.wlp.starters." + GROUP_SUFFIX);
        runtimeDependency.setArtifactId("runtime-pom");
        runtimeDependency.setVersion("0.0.1");
        
        Dependency[] dependencies = {providedDependency, runtimeDependency};
        details.setDependencies(dependencies);
    	return details;
    }
    
    //read the description contained in the index.html file
    private String getStringResource(String path) {
    	InputStream in = getClass().getResourceAsStream(path);
    	
    	StringBuilder index = new StringBuilder();
    	char[] buffer = new char[1024];
    	int read = 0;
    	try(InputStreamReader reader = new InputStreamReader(in)){
    		while((read = reader.read(buffer)) != -1) {
    			index.append(buffer, 0, read);
    		}
    	} catch (IOException e) {
    		//just return what we've got
    		return index.toString();
    	}
    	return index.toString();
    }

    @GET
    @Path("samples")
    @Produces(MediaType.APPLICATION_JSON)
    public Response constructSample(@Context UriInfo info) {
    	StringBuilder json = new StringBuilder("{\n");
    	String base = info.getBaseUri().resolve("../sample").toString();
    	json.append("\"base\" : \"" + base + "\",\n");
    	json.append(getStringResource("/locations.json"));
    	json.append("}\n");
    	return Response.ok(json.toString()).build();
    }
    
    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public ServerConfig getServerConfig() throws Exception {
        ServerConfig config = new ServerConfig();
        Tag[] tags = new Tag[] { new Tag("featureManager") };
        tags[0].setTags(new Tag[] { new Tag("feature", "apiDiscovery-1.0") });
        config.setTags(tags);
        return config;
    }
    @GET
    @Path("dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    public ServerConfig getDependencies() throws Exception {
        ServerConfig config = new ServerConfig();
        Tag[] tags = new Tag[] { new Tag("featureManager") };
        tags[0].setTags(new Tag[] { new Tag("feature", "apiDiscovery-1.0") });
        config.setTags(tags);
        return config;
    }
    
    @GET
    @Path("packages/prepare")
    public String prepareDynamicPackages(@QueryParam("path") String techWorkspaceDir, @QueryParam("options") String options) throws IOException {
    	if(techWorkspaceDir != null && !techWorkspaceDir.trim().isEmpty()){
    		FileUtils.deleteQuietly(new File(techWorkspaceDir + "/package"));
    		
    		if(options != null && !options.trim().isEmpty()){
    			String[] techOptions = options.split(",");
        		String codeGenType = techOptions.length >= 1 ? techOptions[0] : null;

        		if("server".equals(codeGenType) || "client".equals(codeGenType)){
        			String codeGenSrcDirPath = techWorkspaceDir + "/" + codeGenType + "/src";
    				File codeGenSrcDir = new File(codeGenSrcDirPath);
    				
    				if(codeGenSrcDir.exists() && codeGenSrcDir.isDirectory()){
    					String packageSrcDirPath = techWorkspaceDir + "/package/myProject-application/src";
    					File packageSrcDir = new File(packageSrcDirPath);
    					FileUtils.copyDirectory(codeGenSrcDir, packageSrcDir, FileFilterUtils.notFileFilter(new NameFileFilter(new String[]{"RestApplication.java", "AndroidManifest.xml"})));
    					log.fine("Copied files from " + codeGenSrcDirPath + " to " + packageSrcDirPath);
    				}else{
    					log.fine("Swagger code gen source directory doesn't exist : " + codeGenSrcDirPath);
    				}
        		}else{
        			log.fine("Invalid options : " + options);
        			return "";
        		}
    		}
    	}
    	return "success";
    }
    	
    @GET
    @Path("uploads/process")
    @Produces(MediaType.TEXT_PLAIN)
    public String processUploads(@QueryParam("path") String uploadDirectoryPath) throws IOException {
    	
    	File uploadDirectory;
    	if(uploadDirectoryPath == null || !(uploadDirectory = new File(uploadDirectoryPath)).exists()){
    		log.fine("Invalid uploaded directory : " + uploadDirectoryPath);
    		return "Couldn't fulfill the request due to internal error";
    	}
    	
    	List<File> filesListInDir = new ArrayList<File>();
    	populateFilesList(uploadDirectory, filesListInDir);
    	
    	if(filesListInDir.size() != 1){
    		log.fine("Only one swagger file should be present in " + uploadDirectoryPath + ". Instead found these files : " + filesListInDir);
    		return filesListInDir.isEmpty() ? "No Swagger file was uploaded" : "Only one Swagger file can be processed at a time";
    	}
    	
    	File uploadedFile = filesListInDir.get(0);
    	
    	try{
			
			String sharedResourceDir =  ((String)(new InitialContext().lookup("sharedResourceDir")));
			log.finer("sharedResourceDir=" + sharedResourceDir);
			
			String codeGenPath = sharedResourceDir + "/appAccelerator/swagger/codegen";
			String swaggerCodeGenJarPath = codeGenPath + "/swagger-codegen-cli.jar";
			log.finer("swaggerCodeGenJarPath=" + swaggerCodeGenJarPath);
					
			File schemaCodeGen = new File(swaggerCodeGenJarPath);
			if(!schemaCodeGen.exists()){
				log.info("swagger-codegen-cli.jar doesn't exist: " + schemaCodeGen.getAbsolutePath());
				return "Couldn't fulfill the request due to internal error";
			}
			
			String javaHome = ((String)(new InitialContext().lookup("javaHome")));
			log.finer("javaHome=" + javaHome);
			
			//server codegen
			String codeGenType = "server";
			String codeGenLanguage = "jaxrs-spec";
			int returnCode = generateCode(javaHome, swaggerCodeGenJarPath, null, codeGenLanguage, uploadedFile.getAbsolutePath(), uploadDirectoryPath + "/" + codeGenType);
			if(returnCode != 0){
				log.info("Couldn't generate server code using the swagger file : " + uploadedFile.getAbsolutePath());
				return "Couldn't generate server code using the specified swagger file";
			}
			
			//client codegen - disable for now
			/*
			String javaClientTemplates = codeGenPath + "/javaClientTemplates";
			codeGenType = "client";
			codeGenLanguage = "java";
			returnCode = generateCode(javaHome, swaggerCodeGenJarPath, javaClientTemplates, codeGenLanguage, uploadedFile.getAbsolutePath(), directory + "/" + codeGenType);
			if(returnCode != 0){
				System.out.println("Couldn't generate client code using the swagger file : " + uploadedFile.getAbsolutePath());
				return "Couldn't generate client code using the specified swagger file";	
			}*/
		}catch (NamingException ne){
			log.info("NamingException occurred: " + ne);
			return "Internal error";
		}
    	
    	return "success";
    }
    
    private void populateFilesList(File dir, List<File> filesListInDir) {
        File[] files = dir.listFiles();
        for(File file : files){
            if(file.isFile()) filesListInDir.add(file);
            else populateFilesList(file, filesListInDir);
        }
    }
    
    private int generateCode(String javaHome, String swaggerCodeGenJarPath, String javaClientTemplates, String codeGenLanguage, String filePath, String outputDir) throws java.io.IOException {

        try {
            final ArrayList<String> commandList = new ArrayList<String>();

			if(javaHome == null || javaHome.trim().isEmpty()){
				//Get java home
	            javaHome = AccessController.doPrivileged(
	                            new PrivilegedAction<String>() {
	                                @Override
	                                public String run() {
	                                    return System.getProperty("java.home");
	                                }
	
	                            });
	            log.fine("Retrieved Java home location from System property : " + javaHome);
			}
			
            commandList.add(javaHome + "/bin/java");
            commandList.add("-jar");
            commandList.add(swaggerCodeGenJarPath);
			
			commandList.add("generate");
			commandList.add("-l");
			commandList.add(codeGenLanguage);
			
			if(javaClientTemplates != null && !javaClientTemplates.trim().isEmpty()){
				commandList.add("-t");
				commandList.add(javaClientTemplates);
			}
			
			commandList.add("-i");
			commandList.add(filePath);			
			commandList.add("-o");
			commandList.add(outputDir);   

			StringBuilder sb = new StringBuilder();
			for (String command : commandList) {
                    sb.append(command);
                    sb.append(" ");
			}
			
			log.finer("Swagger code gen commands:\n" + sb.toString());
				
            //Run the command
            ProcessBuilder builder = new ProcessBuilder(commandList);
            builder.redirectErrorStream(true); //merge error and output together

            Process codeGenProc = builder.start();
            int exitVal = codeGenProc.waitFor();

            log.finer("Exit values: " + exitVal);

            if (exitVal != 0) {
            	log.fine("Error : exit value is not 0. exitVal=" + exitVal);
            	log.finer("output=" + getOutput(codeGenProc));
            }else{
            	log.finer("Successfully generated code using SwaggerCodegen");
            }
            
    		return exitVal;
        } catch (Exception e) {
        	log.fine("Exception occurred while executing SwaggerCodegen : e=" + e);
			return -1;
        }
    }
	
	private String getOutput(Process joinProc) throws IOException {
        InputStream stream = joinProc.getInputStream();

        char[] buf = new char[512];
        int charsRead;
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(stream);
            while ((charsRead = reader.read(buf)) > 0) {
                sb.append(buf, 0, charsRead);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return sb.toString();
    }
	
}