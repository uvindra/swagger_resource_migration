package org.wso2.carbon.apimgt.swagger.migration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.swagger.migration.internal.ServiceHolder;
import org.wso2.carbon.apimgt.swagger.migration.utils.Constants;
import org.wso2.carbon.apimgt.swagger.migration.utils.ResourceUtil;
import org.wso2.carbon.apimgt.swagger.migration.utils.SwaggerResourceCreator;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.util.Arrays;



public class SwaggerResMigration {
    private static final Log log = LogFactory.getLog(SwaggerResMigration.class);

    public void migrate() throws UserStoreException{
        log.info("*** In migrate() of SwaggerResMigration ***");

        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();
        Tenant[] tenantsArray = tenantManager.getAllTenants();

        // Add  super tenant to the tenant array
        Tenant[] allTenantsArray = Arrays.copyOf(tenantsArray, tenantsArray.length + 1);
        org.wso2.carbon.user.core.tenant.Tenant superTenant = new org.wso2.carbon.user.core.tenant.Tenant();
        superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
        superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        allTenantsArray[allTenantsArray.length - 1] = superTenant;

        for (Tenant tenant : allTenantsArray) {
            log.info("*** Swagger resource migration for tenant " + tenant.getDomain() + "[" + tenant.getId() + "]" + " ***");
            try {
                //Start a new tenant flow
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant.getId());

                if (!registry.resourceExists(APIConstants.API_ROOT_LOCATION)) {
                    log.info("No apis have been created for tenant " + tenant.getDomain() + "[" + tenant.getId() + "]");
                    continue;
                }

                GenericArtifactManager manager = new GenericArtifactManager(registry, "api");
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

                for (GenericArtifact artifact : artifacts) {
                	try {
	                    API api = APIUtil.getAPI(artifact, registry);
	                    APIIdentifier apiIdentifier = api.getId();

                        /*
	                    artifact.setAttribute(APIConstants.PROTOTYPE_OVERVIEW_IMPLEMENTATION, APIConstants.IMPLEMENTATION_TYPE_ENDPOINT);
	                    manager.updateGenericArtifact(artifact);
	                    String apiDefinitionFilePath = getAPIDefinitionFilePath(apiIdentifier.getApiName(), apiIdentifier.getVersion(), apiIdentifier.getProviderName());
	                    Resource resource = registry.get(apiDefinitionFilePath);
	                    String text = new String((byte[]) resource.getContent());
	                    String newContentPath = APIUtil.getAPIDefinitionFilePath(apiIdentifier.getApiName(), apiIdentifier.getVersion(), apiIdentifier.getProviderName());
	                    Resource docContent = registry.newResource();
	                    docContent.setContent(text);
	                    docContent.setMediaType("text/plain");
	                    registry.put(newContentPath, docContent);


	                    ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager().authorizeRole(APIConstants.ANONYMOUS_ROLE,
	                                                                                                                               "_system/governance" + newContentPath,
	                                                                                                                               ActionConstants.GET);
                        */
                  
                   
                        String apiDef11Path = APIUtil.getAPIDefinitionFilePath(apiIdentifier.getApiName(), apiIdentifier.getVersion(),
                                apiIdentifier.getProviderName());

                        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                        log.info("Updating swagger docs resource for : " + apiIdentifier.getApiName() + "-" +
                                apiIdentifier.getVersion() + "-" + apiIdentifier.getProviderName());

                        Resource apiDef11 = registry.get(apiDef11Path);

                        String apiDef11Json = new String((byte[]) apiDef11.getContent());

                        String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiIdentifier.getApiName(),
                                apiIdentifier.getVersion(), apiIdentifier.getProviderName());

                        if (!registry.resourceExists(swagger12location) || !registry.resourceExists(swagger12location + "/" + Constants.API_DOC_12_ALL_RESOURCES_DOC)) {  // Swagger 1.2 resource does not exist so create it
                            log.info("Creating 1.2 swagger resource");

                            SwaggerResourceCreator.createSwagger12Resources(artifact, registry, api, tenant);

                            Resource swagger12Res = registry.get(swagger12location);
                            String[] resourcePaths = (String[]) swagger12Res.getContent();

                            ResourceUtil.updateAPISwaggerDocs(apiDef11Json, resourcePaths, registry);
                        }

                        Resource swagger12Res = registry.get(swagger12location);
                        String[] resourcePaths = (String[]) swagger12Res.getContent();

                        try {
                            ResourceUtil.updateSwagger11APIDoc(apiDef11Path, apiDef11Json, resourcePaths, registry);
                            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                        } catch (ParseException e) {
                            throw new APIManagementException("Unable to parse registry resource", e);
                        }

						
					} catch (RegistryException e) {
						log.error("RegistryException while migrating api in " + tenant.getDomain() , e);
					} catch (APIManagementException e) {
						log.error("APIManagementException while migrating api in " + tenant.getDomain() , e);						
					} catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }  
            } catch (RegistryException e) {
            	log.error("RegistryException while getting artifacts for  " + tenant.getDomain() , e);
			} catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

    }
	
}