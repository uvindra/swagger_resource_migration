package org.wso2.carbon.apimgt.swagger.migration.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.rmi.RemoteException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.swagger.migration.internal.ServiceHolder;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceResourceServiceExceptionException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.api.doc.model.Operation;
import org.wso2.carbon.apimgt.api.doc.model.Parameter;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;


public class SwaggerResourceCreator {


    public static void createSwagger12Resources(GovernanceArtifact artifact, Registry registry,
        API api, Tenant tenant) throws APIManagementException, LoginAuthenticationExceptionException, UserStoreException,
                                                    ResourceAdminServiceResourceServiceExceptionException, RemoteException  {

        JSONParser parser = new JSONParser();
        String pathJsonTemplate = "{\n    \"path\": \"\",\n    \"operations\": []\n}";
        String operationJsonTemplate = "{\n    \"method\": \"\",\n    \"parameters\": []\n}";

        // for apidoc
        String apiJsonTemplate = "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"apis\": [],\n    \"info\": {\n        \"title\": \"\",\n        \"description\": \"\",\n        \"termsOfServiceUrl\": \"\",\n        \"contact\": \"\",\n        \"license\": \"\",\n        \"licenseUrl\": \"\"\n    },\n    \"authorizations\": {\n        \"oauth2\": {\n            \"type\": \"oauth2\",\n            \"scopes\": []\n        }\n    }\n}";

        // for each resource
        // String apiResourceJsontemplate =
        // "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"resourcePath\":\"\",\n    \"apis\": [],\n    \"info\": {\n        \"title\": \"\",\n        \"description\": \"\",\n        \"termsOfServiceUrl\": \"\",\n        \"contact\": \"\",\n        \"license\": \"\",\n        \"licenseUrl\": \"\"\n    },\n    \"authorizations\": {\n        \"oauth2\": {\n            \"type\": \"oauth2\",\n            \"scopes\": []\n        }\n    }\n}";
        String apiResourceJsontemplate = "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"resourcePath\":\"\",\n    \"apis\": [] \n}";

        //Auth Types
        HashMap<String,String> auth_types = new HashMap<String, String>();
        auth_types.put("None","None");
        auth_types.put("Application_User","Application User");
        auth_types.put("Application","Application");
        auth_types.put("Any","Application & Application User");

        //List<String> uriTemplateNames = new ArrayList<String>();

        //new
        List<String> uriTemplatePathNames = new ArrayList<String>();

        Map<String, JSONObject> resourceNameJSONs = new HashMap<String, JSONObject>();


        // resousepath-operations list
        Map<String, List<JSONObject>> resourcePathOperations = new HashMap<String, List<JSONObject>>();

        Map<String, String> resourceNamePathNameMap = new HashMap<String, String>();
        JSONObject mainAPIJson = null;
        try {

        String apiVersion = artifact
        .getAttribute(APIConstants.API_OVERVIEW_VERSION);


        int i = 0;

        Set<URITemplate> uriTemplates = api.getUriTemplates();
        Map<String, List<String>> resourceNamepaths = new HashMap<String, List<String>>();
        Map<String, List<JSONObject>> resourcePathJSONs = new HashMap<String, List<JSONObject>>();

        for (URITemplate template : uriTemplates) {
        List<Operation> ops;
        List<Parameter> parameters = null;

        String path = template.getUriTemplate();

        if (path != null && (path.equals("/*") || (path.equals("/")))) {
        path = "/*";
        }
        List<String> resourcePaths;
        int resourceNameEndIndex = path.indexOf("/", 1);
        String resourceName = "/default";
        if(resourceNameEndIndex != -1) {
        resourceName = path.substring(1, resourceNameEndIndex);
        }

        if(!resourceName.startsWith("/")) {
        resourceName = "/" + resourceName;
        }

        if(resourceNamepaths.get(resourceName) != null) {
        resourcePaths = resourceNamepaths.get(resourceName);
        if (!resourcePaths.contains(path)) {
        resourcePaths.add(path);
        }
        //verbs comes as a [POST, GET] type of a string
        String[] httpVerbs = template.getMethodsAsString().split(" ");
        String[] authtypes = template.getAuthTypeAsString().split(" ");
        String[] tries = template.getThrottlingTiersAsString().split(" ");

        for(int j = 0; j < httpVerbs.length ; j ++) {
        final JSONObject operationJson = (JSONObject) parser.parse(operationJsonTemplate);
        operationJson.put("method", httpVerbs[j]);
        operationJson.put("auth_type", auth_types.get(authtypes[j]));
        operationJson.put("throttling_tier", tries[j]);

        if(resourcePathJSONs.get(path) != null) {
        resourcePathJSONs.get(path).add(operationJson);

        } else {
        resourcePathJSONs.put(path, new ArrayList<JSONObject>() {{
        add(operationJson);
        }});
        }
        }
        resourceNamepaths.put(resourceName, resourcePaths);
        } else {
        JSONObject resourcePathJson = (JSONObject) parser.parse(apiResourceJsontemplate);

//	    			resourcePathJson.put("apiVersion", version);
//	    			resourcePathJson.put("resourcePath", resourceName);
//	    			resourceNameJSONs.put(resourceName, resourcePathJson);

        resourcePaths = new ArrayList<String>();
        resourcePaths.add(path);

        //verbs comes as a [POST, GET] type of a string
        String[] httpVerbs = template.getMethodsAsString().split(" ");
        String[] authtypes = template.getAuthTypeAsString().split(" ");
        String[] tries = template.getThrottlingTiersAsString().split(" ");

        for(int j = 0; j < httpVerbs.length ; j ++) {
        final JSONObject operationJson = (JSONObject) parser.parse(operationJsonTemplate);
        operationJson.put("method", httpVerbs[j]);
        operationJson.put("auth_type", auth_types.get(authtypes[j]));
        operationJson.put("throttling_tier", tries[j]);

        if(resourcePathJSONs.get(path) != null) {
        resourcePathJSONs.get(path).add(operationJson);

        } else {
        resourcePathJSONs.put(path, new ArrayList<JSONObject>() {{
        add(operationJson);
        }});
        }
        }

        resourceNamepaths.put(resourceName, resourcePaths);
        }
        }



        // store api object(which contains operations objects) against the resource path
        Map<String, JSONObject> pathNameApi = new HashMap<String, JSONObject>();

        //list to store the api array objects
        List<JSONObject> apiArray = new ArrayList<JSONObject>();

        for (Entry<String, List<JSONObject>> entry : resourcePathJSONs
        .entrySet()) {
        String resourcePath = entry.getKey();
        // JSONObject jsonOb = resourceNameJSONs.get(resourcePath);
        // List<JSONObject> pathItems = entry.getValue();
        // for (JSONObject pathItem : pathItems) {
        JSONObject pathJson = (JSONObject) parser
        .parse(pathJsonTemplate);
        pathJson.put("path", resourcePath);
        List<JSONObject> methodJsons = entry.getValue();
        for (JSONObject methodJson : methodJsons) {
        JSONArray operations = (JSONArray) pathJson
        .get("operations");
        operations.add(methodJson);
        }

        pathNameApi.put(resourcePath, pathJson);

        apiArray.add(pathJson);
        }



        /**
         * create only one resource doc for all the resources. name it as 'resources'
         */
        // create resources in the registry
        APIIdentifier apiIdentfier = api.getId();
        String apiDefinitionFilePath = APIUtil.getSwagger12DefinitionFilePath(apiIdentfier.getApiName(),
        apiIdentfier.getVersion(),apiIdentfier.getProviderName());

        String resourceName = Constants.API_DOC_12_ALL_RESOURCES_DOC;
        JSONObject resourcesObj = (JSONObject) parser.parse(apiResourceJsontemplate);
        resourcesObj.put("apiVersion", apiVersion);
        resourcesObj.put("resourcePath", "/" + resourceName);
        JSONArray apis = (JSONArray) resourcesObj.get("apis");
        //add all the apis to single one
        for(JSONObject arraObj : apiArray){
        apis.add(arraObj);
        }
        String registryRes = apiDefinitionFilePath
        + RegistryConstants.PATH_SEPARATOR + resourceName;
        createResource(registry, resourcesObj.toJSONString(), registryRes, api, tenant);

        // create api-doc file in the 1.2 resource location

        mainAPIJson = (JSONObject) parser.parse(apiJsonTemplate);
        mainAPIJson.put("apiVersion", apiVersion);
        ((JSONObject)mainAPIJson.get("info")).put("description", "Available resources");
        ((JSONObject)mainAPIJson.get("info")).put("title", artifact.getAttribute(APIConstants.API_OVERVIEW_NAME));

        JSONArray apis1 = (JSONArray) mainAPIJson.get("apis");
        JSONObject pathjob = new JSONObject();
        pathjob.put("path", "/" + resourceName);
        pathjob.put("description", "All resources for the api");
        apis1.add(pathjob);


        createResource(registry, mainAPIJson.toJSONString(),
        apiDefinitionFilePath
        + APIConstants.API_DOC_1_2_RESOURCE_NAME, api, tenant);

        } catch (GovernanceException e) {
        String msg = "Failed to get API fro artifact ";
        throw new APIManagementException(msg, e);
        } catch (ParseException e) {
        throw new APIManagementException(
        "Error while generating swagger 1.2 resource for api ", e);
        }

    }


    private static void createResource(Registry re, String content, String resourcePath, API api, Tenant tenant) throws UserStoreException,
                                                        LoginAuthenticationExceptionException, ResourceAdminServiceResourceServiceExceptionException, RemoteException {
        try {
            Resource docContent = re.newResource();
            docContent.setContent(content);
            docContent.setMediaType("text/plain");
            re.put(resourcePath, docContent);

            ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager().authorizeRole(APIConstants.ANONYMOUS_ROLE,
                    "_system/governance" + resourcePath,
                    ActionConstants.GET);


            /*
            String sessionCookie = login(CommandHandler.getUsername(),
                    CommandHandler.getPassword(), CommandHandler.getHost());
            //String permissionString = "ra^false:rd^false:wa^false:wd^false:da^false:dd^false:aa^false:ad^false";
            ResourceAdminServiceStub stub = new ResourceAdminServiceStub(CommandHandler.getServiceURL() + "ResourceAdminService");

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(HTTPConstants.COOKIE_STRING, sessionCookie);
            stub.addRolePermission("_system/governance" + resourcePath, APIConstants.ANONYMOUS_ROLE, "2", "1");
            */

        } catch (RegistryException e) {
            String msg = "Failed to add the API Definition content of : "
                    + APIConstants.API_DEFINITION_DOC_NAME + " of API :" + api.getId().getApiName();
            //log.error(msg);
        }
    }



}