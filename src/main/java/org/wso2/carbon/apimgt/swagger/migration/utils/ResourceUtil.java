/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.swagger.migration.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceUtil {	

	private static final Log log = LogFactory.getLog(ResourceUtil.class);
	
	/**
	 * Get all the parameters related to each resource. The parameter array is
	 * an array which is part of the operations object inside each resource.
	 * 
	 * @param resource
	 *            api-doc which contains swagger 1.1 info
	 * @return map of parameter array related to all the http methods for each
	 *         resource. the key for
	 *         the map is resourcepath_httpmethod
	 */
	public static Map<String, JSONArray> getAllParametersForResources(JSONObject resource) {
		Map<String, JSONArray> parameters = new HashMap<String, JSONArray>();

		String resourcePath = (String) resource.get(Constants.API_DOC_11_RESOURCE_PATH);
		String apiVersion = (String) resource.get(Constants.API_DOC_11_API_VERSION);
		String resourcePathPrefix = resourcePath + "/" + apiVersion;

		String key = null;

		JSONArray apis = (JSONArray) resource.get(Constants.API_DOC_11_APIS);
		for (int i = 0; i < apis.size(); i++) {

			JSONObject apiInfo = (JSONObject) apis.get(i);
			String path = (String) apiInfo.get(Constants.API_DOC_11_PATH);
			JSONArray operations = (JSONArray) apiInfo.get(Constants.API_DOC_11_OPERATIONS);

			for (int j = 0; j < operations.size(); j++) {
				JSONObject operation = (JSONObject) operations.get(j);
				String httpMethod = (String) operation.get(Constants.API_DOC_11_METHOD);
				JSONArray parameterArray = (JSONArray) operation.get(Constants.API_DOC_11_PARAMETERS);

				// get the key by removing the "apiVersion" and "resourcePath"
				// from the "path" variable
				// and concat the http method
				String keyPrefix = path.substring(resourcePathPrefix.length());
				if (keyPrefix.isEmpty()) {
					keyPrefix = "/*";
				}
				key = keyPrefix + "_" + httpMethod.toLowerCase();

				parameters.put(key, parameterArray);
			}
		}

		return parameters;

	}
	
	/**
	 * Get all the operation object related to each resource. Method is inside the operation object
	 * this object contains the nickname, description related to that resource method
	 * @param resource
	 *            api-doc which contains swagger 1.1 info
	 * @return map of operations array related to all the http methods for each
	 *         resource. the key for
	 *         the map is resourcepath_httpmethod
	 */
	private static Map<String, JSONObject> getAllOperationsForResources(JSONObject resource) {
		Map<String, JSONObject> parameters = new HashMap<String, JSONObject>();

		String resourcePath = (String) resource.get(Constants.API_DOC_11_RESOURCE_PATH);
		String apiVersion = (String) resource.get(Constants.API_DOC_11_API_VERSION);
		String resourcePathPrefix = resourcePath + "/" + apiVersion;

		String key = null;

		JSONArray apis = (JSONArray) resource.get(Constants.API_DOC_11_APIS);
		for (int i = 0; i < apis.size(); i++) {

			JSONObject apiInfo = (JSONObject) apis.get(i);
			String path = (String) apiInfo.get(Constants.API_DOC_11_PATH);
			JSONArray operations = (JSONArray) apiInfo.get(Constants.API_DOC_11_OPERATIONS);

			for (int j = 0; j < operations.size(); j++) {
				JSONObject operation = (JSONObject) operations.get(j);
				String httpMethod = (String) operation.get(Constants.API_DOC_11_METHOD);
				JSONArray parameterArray = (JSONArray) operation.get(Constants.API_DOC_11_PARAMETERS);

				// get the key by removing the "apiVersion" and "resourcePath"
				// from the "path" variable
				// and concat the http method
				String keyPrefix = path.substring(resourcePathPrefix.length());
				if (keyPrefix.isEmpty()) {
					keyPrefix = "/*";
				}
				key = keyPrefix + "_" + httpMethod.toLowerCase();

				parameters.put(key, operation);
			}
		}

		return parameters;
	}
	/**
	 * Get all the apis as a map. key for map is the full resource path for that api. key is created
	 * using 'basepath + apis[i].path' values in the api-doc.json file (swagger 1.1 doc) 
	 * 
	 * @param resource
	 *            api-doc which contains swagger 1.1 info
	 * @return map of apis in the swagger 1.1 doc. key is the full resource path for that resource
	 */
	public static Map<String, JSONObject> getAllAPIsByResourcePath(JSONObject resource) {
		Map<String, JSONObject> parameters = new HashMap<String, JSONObject>();

		String basePath = (String) resource.get(Constants.API_DOC_11_BASE_PATH);
		String key = null;

		JSONArray apis = (JSONArray) resource.get(Constants.API_DOC_11_APIS);
		for (int i = 0; i < apis.size(); i++) {

			JSONObject apiInfo = (JSONObject) apis.get(i);
			String path = (String) apiInfo.get(Constants.API_DOC_11_PATH);
			key = basePath + path;
			parameters.put(key, apiInfo);
		}

		return parameters;

	}

	private static JSONArray getDefaultParameters() {
		JSONParser parser = new JSONParser();
		JSONArray defaultParam = new JSONArray();
		try {
			defaultParam = (JSONArray) parser.parse(Constants.DEFAULT_PARAM_ARRAY);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return defaultParam;

	}

	/**
	 * Modify the resource in registry location '1.2' to be compatible with the
	 * AM 1.8. add the
	 * parameters to operations element, add 'nickname' variable and the
	 * 'basePath' variable
	 * 
	 * @param resource
	 *            resource inside the 1.2 location
	 * @param allParameters
	 *            map containing all the parameters extracted from api-doc
	 *            containing
	 *            resources for swagger 1.1
	 * @param allOperations 
	 * @param basePath
	 *            base path for the resource
	 * @return modified resource for the api
	 */
	public static String getUpdatedSwagger12Resource(JSONObject resource,
	                                                 Map<String, JSONArray> allParameters,
	                                                 Map<String, JSONObject> allOperations, String basePath) {

		JSONArray apis = (JSONArray) resource.get(Constants.API_DOC_12_APIS);
		for (int i = 0; i < apis.size(); i++) {
			JSONObject apiInfo = (JSONObject) apis.get(i);
			String path = (String) apiInfo.get(Constants.API_DOC_12_PATH);
			JSONArray operations = (JSONArray) apiInfo.get(Constants.API_DOC_12_OPERATIONS);

			for (int j = 0; j < operations.size(); j++) {
				JSONObject operation = (JSONObject) operations.get(j);
				String method = (String) operation.get(Constants.API_DOC_12_METHOD);

				// nickname is method name + "_" + path without starting "/"
				// symbol
				String nickname = method.toLowerCase() + "_" + path.substring(1);
				// add nickname variable
				operation.put(Constants.API_DOC_12_NICKNAME, nickname);
				String key = path + "_" + method.toLowerCase();

				JSONArray parameters = new JSONArray();
				if (allParameters.containsKey(key)) {
					parameters = allParameters.get(key);
					
					//setting the 'type' to 'string' if this variable is missing
					for(int m = 0; m < parameters.size(); m++ ){
						JSONObject para = (JSONObject)parameters.get(m);
						if(!para.containsKey("type")){
							para.put("type", "string");
						}						
					}

				} else {
					parameters = getDefaultParameters();
					
				}
				//if there are parameters already in this 
				JSONArray existingParams = (JSONArray) operation.get(Constants.API_DOC_12_PARAMETERS);
				if(existingParams.isEmpty()) {
					JSONParser parser = new JSONParser();
					if(path.contains("{")) {
						List<String> urlParams = ResourceUtil.getURLTempateParams(path);
						for(String p: urlParams){
							try {
								JSONObject paramObj = (JSONObject) parser.parse(Constants.DEFAULT_PARAM_FOR_URL_TEMPLATE);
								paramObj.put("name", p);
								parameters.add(paramObj);
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					// add parameters array
					operation.put(Constants.API_DOC_12_PARAMETERS, parameters);
				} else {
					for(int k = 0; k < existingParams.size(); k ++ ) {
						parameters.add(existingParams.get(k));
					}									
					operation.put(Constants.API_DOC_12_PARAMETERS, parameters);
				}
				
				//updating the resource description and nickname using values in the swagger 1.1 doc
				if(allOperations.containsKey(key)){
					
					//update info related to object
					JSONObject operationObj11 = allOperations.get(key);
					//add summery
					if(operationObj11.containsKey("summary")) {
						operation.put("summary", (String) operationObj11.get("summery"));
					}
					
				}
			}
		}

		// add basePath variable
		resource.put(Constants.API_DOC_12_BASE_PATH, basePath);

		return resource.toJSONString();
	}

	/**
	 * location for the swagger 1.2 resources
	 * @param apiName
	 * @param apiVersion
	 * @param apiProvider
	 * @return
	 */
	public static String getSwagger12ResourceLocation(String apiName, String apiVersion,
	                                                  String apiProvider) {
		String resourcePath =
				APIConstants.API_DOC_LOCATION + RegistryConstants.PATH_SEPARATOR +
				apiName + "-" + apiVersion + "-" + apiProvider +
				RegistryConstants.PATH_SEPARATOR +
				APIConstants.API_DOC_1_2_LOCATION;

		return resourcePath;
	}



    /**
	 * update all the the swagger document in the registry related to an api
	 * @param apiDocJson
	 * @param docResourcePaths
	 * @param re
	 * @throws org.json.simple.parser.ParseException
	 * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
	 */
	public static void updateAPISwaggerDocs(String apiDocJson, String[] docResourcePaths,
	                                        Registry re) throws ParseException, RegistryException {

		JSONParser parser = new JSONParser();
		JSONObject apiDoc11 = (JSONObject) parser.parse(apiDocJson);

		Map<String, JSONArray> allParameters = ResourceUtil.getAllParametersForResources(apiDoc11);

		Map<String, JSONObject> allOperations = ResourceUtil.getAllOperationsForResources(apiDoc11);

		Map<String, JSONObject> apisByPath = ResourceUtil.getAllAPIsByResourcePath(apiDoc11);

		//this collection holds description given for each resource against the resource name.
		//descriptions added resources in an api in AM 1.7 are stored in api-doc 1.1. this desciption
		//is showed in am 1.7 api console in the store applicatoin. AM 1.8 uses descriptions in
		// api-doc in 1.2 resource folder. following map collects this value from api-doc 1.1 and
		// store it to add to api-doc 1.2
		Map<String, String> descriptionsForResource = new HashMap<String, String>();



		String basePath = (String) apiDoc11.get(Constants.API_DOC_11_BASE_PATH);
		String resourcePath = (String) apiDoc11.get(Constants.API_DOC_11_RESOURCE_PATH);
		String apiVersion = (String) apiDoc11.get(Constants.API_DOC_11_API_VERSION);

		resourcePath = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";
		String basePathForResource = basePath + resourcePath + apiVersion;

		String apidoc12path = "";

		//update each resource in the 1.2 folder except the api-doc resource
		for (String docResourcePath : docResourcePaths) {
			String resourceName = docResourcePath.substring(docResourcePath.lastIndexOf("/"));

			if (resourceName.equals(APIConstants.API_DOC_1_2_RESOURCE_NAME)) {
				//store api-doc in 1.2 folder for future use
				apidoc12path = docResourcePath;
				continue;
			}

			Resource resource = re.get(docResourcePath);
			JSONObject apiDoc =
					(JSONObject) parser.parse(new String((byte[]) resource.getContent()));


			String description = "";
			//generate the key to query the descriptionsForResource map
			JSONArray apisInResource = (JSONArray) apiDoc.get(Constants.API_DOC_12_APIS);
			JSONObject apiTemp = (JSONObject) apisInResource.get(0);
			String path = (String) apiTemp.get(Constants.API_DOC_12_PATH);

			String key = "";
			if (path.equals("/*")) {
				key = basePathForResource;
			} else {
				key = basePathForResource + path;
			}


			//get the description for that resource. query the api list generated using api-doc 1.1
			if(apisByPath.containsKey(key)) {
				JSONObject apiInfo = (JSONObject) apisByPath.get(key);
				description = (String) apiInfo.get("description");
				descriptionsForResource.put(resourceName, description);
			}

			String updatedJson =
					ResourceUtil.getUpdatedSwagger12Resource(apiDoc, allParameters, allOperations,
					                                         basePathForResource);
			log.info("\t update " + resourceName.substring(1));
			Resource res = re.get(docResourcePath);
			res.setContent(updatedJson);
			//update the registry
			re.put(docResourcePath, res);

		}

		//update the api-doc. add the descriptions to each api resource
		ResourceUtil.updateSwagger12APIdoc(apidoc12path, descriptionsForResource, re, parser);

	}



	/**
	 * update the swagger 1.2 api-doc. This method updates the descriptions
	 * @param apidoc12path
	 * @param descriptionsForResource
	 * @param re
	 * @param parser
	 * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
	 * @throws org.json.simple.parser.ParseException
	 */
	private static void updateSwagger12APIdoc(String apidoc12path,
	                                          Map<String, String> descriptionsForResource,
	                                          Registry re, JSONParser parser)
	                                                                         throws RegistryException,
	                                                                         ParseException {
		Resource res = re.get(apidoc12path);
		JSONObject api12Doc = (JSONObject) parser.parse(new String((byte[]) res.getContent()));
		JSONArray apis = (JSONArray) api12Doc.get(Constants.API_DOC_12_APIS);
		for (int j = 0; j < apis.size(); j++) {
			JSONObject api = (JSONObject) apis.get(j);

			// get the resource name for each api in api-doc 1.2
			String resPathName = (String) api.get(Constants.API_DOC_12_PATH);
			if (descriptionsForResource.containsKey(resPathName)) {
				// if the descrption is available for that resource update it
				api.put("description", descriptionsForResource.get(resPathName));
			}
		}
		log.info("\t update api-doc");
		res.setContent(api12Doc.toJSONString());
		// update the registry
		re.put(apidoc12path, res);
	}

	/**
	 * remove header and body parameters
	 * @param docResourcePaths
	 * @param registry
	 * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
	 * @throws org.json.simple.parser.ParseException
	 */
	public static void updateSwagger12ResourcesForAM18(String[] docResourcePaths,
			Registry registry) throws RegistryException, ParseException {
		JSONParser parser = new JSONParser();
		for (String docResourcePath : docResourcePaths) {
			
			String resourceName = docResourcePath.substring(docResourcePath.lastIndexOf("/"));
			
			//modify this if api-doc resource needed to be changed
			if (resourceName.equals(APIConstants.API_DOC_1_2_RESOURCE_NAME)) {				
				continue;
			}
			
			Resource resource = registry.get(docResourcePath);
			JSONObject resourceDoc =
					(JSONObject) parser.parse(new String((byte[]) resource.getContent()));
			JSONArray apis = (JSONArray) resourceDoc.get(Constants.API_DOC_12_APIS);
			for (int j = 0; j < apis.size(); j++) {
				JSONObject api = (JSONObject) apis.get(j);
				JSONArray operations = (JSONArray) api.get("operations");
				
				for (int k = 0; k < operations.size(); k++) {
					JSONObject operation = (JSONObject) operations.get(k);
					JSONArray parameters = (JSONArray) operation.get("parameters");
					String method = (String) operation.get("method");
					JSONArray parametersNew = new JSONArray();
					//remove header and body parameters
					for (int l = 0; l < parameters.size(); l++) {
						JSONObject parameter = (JSONObject) parameters.get(l);
						/*
						if(parameter.get("paramType").equals("header") ||
								parameter.get("paramType").equals("body")){
							continue;
						} */
						if(parameter.get("paramType").equals("header")){
							continue;
						}
						if(parameter.get("paramType").equals("body")) {
							
							//only remove body of a GET and DELETE
							if("GET".equalsIgnoreCase(method) || 
									"DELETE".equalsIgnoreCase(method)) {
								continue;
							}
						}
						parametersNew.add(parameter);
					}
					operation.put("parameters", parametersNew);
				}
				
				
			}
			
			resource.setContent(resourceDoc.toJSONString());
			//update the registry
			registry.put(docResourcePath, resource);
		}
		
	}

    /**
     * Update the API definition in Swagger 1.1
     * @param api11Path
     *          path of API 1.1 definition
     * @param api11DocJson
     *          api 1.1 definition
     * @param docResourcePaths
     *          path for API resources in 1.2 folder
     * @param registry
     *          registry
     * @throws org.json.simple.parser.ParseException
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     */
    public static void updateSwagger11APIDoc(String api11Path, String api11DocJson, String[] docResourcePaths,
                                             Registry registry) throws ParseException, RegistryException {
        JSONParser parser = new JSONParser();
        JSONObject apiDoc11 = (JSONObject) parser.parse(api11DocJson);

        //update each resource in the 1.2 folder except the api-doc resource
        for (String docResourcePath : docResourcePaths) {
            String resourceName = docResourcePath.substring(docResourcePath.lastIndexOf("/"));

            if (resourceName.equals(APIConstants.API_DOC_1_2_RESOURCE_NAME)) {
                //skip api-doc in 1.2 folder
                continue;
            }


			log.info("swagger 1.1 doc path : " + api11Path + "\n\n");

			log.info("swagger 1.1 doc json : " + apiDoc11.toJSONString() + "\n\n");
			log.info("Resource path : " + docResourcePath);
            Resource resource = registry.get(docResourcePath);
            JSONObject apiDoc =
                    (JSONObject) parser.parse(new String((byte[]) resource.getContent()));

			log.info("API doc json : " + apiDoc.toJSONString() + "\n\n");

            Map<String, JSONArray> allParameters12 = ResourceUtil.getAllParametersForResources12(apiDoc);

            String updatedJson =
                    ResourceUtil.getUpdatedSwagger11Resource(apiDoc11, allParameters12);

			log.info("updated json 1.2 : " + updatedJson + "\n\n");

            Resource res = registry.get(api11Path);
            res.setContent(updatedJson);
            //update the registry
            registry.put(api11Path, res);

        }

    }


    /**
     * @param resource        api-doc related to 1.1 definition
     * @param allParameters12 map containing all the parameters extracted
     *                        from each resource of swagger 1.2
     * @return JSON string of the updated Swagger 1.1 definition
     */
    public static String getUpdatedSwagger11Resource(JSONObject resource,
                                                     Map<String, JSONArray> allParameters12) {

		log.info("===================== getUpdatedSwagger11Resource =========================");

        String resourcePath = (String) resource.get(Constants.API_DOC_11_RESOURCE_PATH);
        String apiVersion = (String) resource.get(Constants.API_DOC_11_API_VERSION);
        String resourcePathPrefix = resourcePath + "/" + apiVersion;

		log.info("resourcePath for 1.1 : " + resourcePath);
		log.info("apiVersion : " + apiVersion);
		log.info("resourcePathPrefix : " + resourcePathPrefix);


        JSONArray apis = (JSONArray) resource.get(Constants.API_DOC_11_APIS);
        for (Object api : apis) {
            JSONObject apiInfo = (JSONObject) api;
			log.info("\n\napiInfo : " + apiInfo.toJSONString());

            String path = (String) apiInfo.get(Constants.API_DOC_11_PATH);
            path = path.substring(resourcePathPrefix.length());
            JSONArray operations = (JSONArray) apiInfo.get(Constants.API_DOC_11_OPERATIONS);

			log.info("\n\noperations : " + operations.toJSONString());

            for (Object operation1 : operations) {
                JSONObject operation = (JSONObject) operation1;
				log.info("\n\noperation : " + operation);
                String method = (String) operation.get(Constants.API_DOC_11_METHOD);

                String key = path + "_" + method.toLowerCase();

				log.info("\nkey : " + key);

                //if there are parameters already in this
                JSONArray existingParams = (JSONArray) operation.get(Constants.API_DOC_11_PARAMETERS);

				log.info("\nexistingParams : " + existingParams);
                //maintain the list of original parameters to avoid duplicates
                JSONArray originalParams = existingParams;

                JSONArray parameters;
                if (allParameters12.containsKey(key)) {
					log.info("\nallParameters12.containsKey(key) : " + key);
                    parameters = allParameters12.get(key);
					log.info("\nparameters : " + parameters.toJSONString());

                    //setting the 'type' to 'string' if this variable is missing
                    for (int m = 0; m < parameters.size(); m++) {
                        JSONObject para = (JSONObject) parameters.get(m);
						log.info("\n\npara : " + para.toJSONString());
                        if (noSuchParameter(originalParams, (String) para.get(Constants.API_DOC_11_PARAM_NAME), (String) para.get(Constants.API_DOC_11_PARAM_TYPE))) {
							log.info("\nnoSuchParameter");
                            String dataType = "";
                            if (para.containsKey(Constants.API_DOC_12_DATA_TYPE)) {
								log.info("\npara.containsKey(Constants.API_DOC_12_DATA_TYPE)");
                                dataType = (String) para.get(Constants.API_DOC_12_DATA_TYPE);
                            }

							log.info("\ndataType : " + dataType);
							para.put(Constants.API_DOC_11_DATA_TYPE, dataType);
                            para.remove(Constants.API_DOC_12_DATA_TYPE);
                            existingParams.add(existingParams.size(), para);
                        }
                    }
                }

				log.info("\nexistingParams after loop : " + existingParams);
				operation.put(Constants.API_DOC_12_PARAMETERS, existingParams);
            }
        }

        return resource.toJSONString();
    }


    /**
     * Get all the parameters related to each resource. The parameter array is
     * an array which is part of the operations object inside each resource.
     *
     * @param resource resource doc which contains swagger 1.2 info for one resource of the API
     * @return map of parameter array related to all the http methods for each
     *         resource. the key for
     *         the map is resourcepath_httpmethod
     */
    public static Map<String, JSONArray> getAllParametersForResources12(JSONObject resource) {
        Map<String, JSONArray> parameters = new HashMap<String, JSONArray>();

		log.info("\ngetAllParametersForResources12");
        String key;

        JSONArray apis = (JSONArray) resource.get(Constants.API_DOC_12_APIS);
		log.info("\napis : " + apis.toJSONString());
        for (Object api : apis) {

            JSONObject apiInfo = (JSONObject) api;
            String path = (String) apiInfo.get(Constants.API_DOC_12_PATH);
            JSONArray operations = (JSONArray) apiInfo.get(Constants.API_DOC_12_OPERATIONS);

            for (Object operation1 : operations) {
                JSONObject operation = (JSONObject) operation1;
                String httpMethod = (String) operation.get(Constants.API_DOC_12_METHOD);
                JSONArray parameterArray = (JSONArray) operation.get(Constants.API_DOC_12_PARAMETERS);

                // get the key by removing the "apiVersion" and "resourcePath"
                // from the "path" variable
                // and concat the http method
                String keyPrefix = path;
                if (keyPrefix.isEmpty()) {
                    keyPrefix = "/*";
                }
                key = keyPrefix + "_" + httpMethod.toLowerCase();

				log.info("\nkey : " + key);
				log.info("\nparameterArray : " + parameterArray.toJSONString());

				parameters.put(key, parameterArray);
            }
        }

        return parameters;

    }

    /**
     * Check if a given parameter already exists in the 1.1 definition
     *
     * @param originalParams parameters that was originally in 1.1 definition
     * @param paramName      name of the parameter to look for
     * @param paramType      type of the parameter to look for (path, query etc.)
     * @return whether the given parameter exists in 1.1 definition already
     */
    public static boolean noSuchParameter(JSONArray originalParams, String paramName, String paramType) {
        boolean notFound = true;
        for (Object originalParam : originalParams) {
            JSONObject para = (JSONObject) originalParam;
            String name = (String) para.get(Constants.API_DOC_11_PARAM_NAME);
            String type = (String) para.get(Constants.API_DOC_11_PARAM_TYPE);
            if (name.equals(paramName)) {
                if (type.equals(paramType)) {
                    notFound = false;
                    continue;
                }
            }
        }
        return notFound;
    }


	/**
	 * extract the parameters from the url tempate.
	 * @param url
	 * @return
	 */
	public static List<String> getURLTempateParams(String url) {
		boolean endVal = false;

		List<String> params = new ArrayList<String>();
		if(url.contains("{")){

			int start = 0; 
			int end = 0; 
			for(int i = 0; i < url.length(); i++) { 
			    if(url.charAt(i) == '{') 
			       start = i;
			    else if(url.charAt(i) == '}') {
			    	end = i;
			    	endVal = true;
			    }
			     
			    if(endVal){
			    	params.add(url.substring(start + 1, end));
			    	endVal = false;
			    }
			}
			
		}
		return params;
	}
	/**
	 * print stack trace
	 * @param throwable
	 * @return
	 */
	public static String getStackTrace(final Throwable throwable) {
	     StringWriter stringWriter = new StringWriter();
	     PrintWriter printWriter = new PrintWriter(stringWriter, true);
	     throwable.printStackTrace(printWriter);
	     return stringWriter.getBuffer().toString();
	}
}
