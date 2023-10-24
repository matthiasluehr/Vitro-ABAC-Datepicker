/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.auth.policy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessObjectType;
import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessOperation;
import edu.cornell.mannlib.vitro.webapp.beans.Property;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EntityPolicyController {

    private static final Log log = LogFactory.getLog(EntityPolicyController.class);
    private static Map<String, String> policyKeyToDataValueMap = new HashMap<String, String>();

    /**
     * @param entityUri - entity uniform resource identifier
     * @param aot - Access object type
     * @param og - operation group
     * @param selectedRoles - list of roles to assign
     * @param allRoles - list of all available roles
     */
    public static void updateEntityPolicyDataSet(String entityUri, AccessObjectType aot, AccessOperation og,
            List<String> selectedRoles, List<String> allRoles) {
        if (StringUtils.isBlank(entityUri)) {
            return;
        }
        Set<String> selectedSet = new HashSet<>(selectedRoles);
        for (String role : allRoles) {
            boolean isInDataSet = isUriInTestDataset(entityUri, og, aot, role);
            boolean isSelected = selectedSet.contains(role);
            final PolicyLoader loader = PolicyLoader.getInstance();
            final String dataSetUri = loader.getDataSetUriByKey(og, aot, role);

            if (dataSetUri == null) {
                log.debug(
                        String.format("Policy wasn't found by key:\n%s\n%s\n%s", og.toString(), aot.toString(), role));
                continue;
            }
            if (isSelected && !isInDataSet) {
                loader.addEntityToPolicyDataSet(entityUri, aot, og, role);
                DynamicPolicy policy = loader.loadPolicyFromTemplateDataSet(dataSetUri);
                PolicyStore.getInstance().add(policy);
            } else if (!isSelected && isInDataSet) {
                loader.removeEntityFromPolicyDataSet(entityUri, aot, og, role);
                DynamicPolicy policy = loader.loadPolicyFromTemplateDataSet(dataSetUri);
                PolicyStore.getInstance().add(policy);
            }
        }
    }

    public static List<String> getGrantedRoles(String entityUri, AccessOperation og, AccessObjectType aot,
            List<String> allRoles) {
        if (StringUtils.isBlank(entityUri)) {
            return Collections.emptyList();
        }
        List<String> grantedRoles = new LinkedList<>();
        for (String role : allRoles) {
            if (isUriInTestDataset(entityUri, og, aot, role)) {
                grantedRoles.add(role);
            }
        }
        return grantedRoles;
    }

    public static void getDataValueStatements(String entityUri, AccessObjectType aot, AccessOperation ao,
            Set<String> selectedRoles, StringBuilder sb) {
        if (StringUtils.isBlank(entityUri)) {
            return;
        }
        for (String role : selectedRoles) {
            String testDataUri = getPolicyTestDataUri(aot, ao, role);
            if (testDataUri == null) {
                log.error(String.format("Policy test data wasn't found by key:\n%s\n%s\n%s", ao, aot, role));
                continue;
            }
            sb.append("<").append(testDataUri)
                    .append("> <https://vivoweb.org/ontology/vitro-application/auth/vocabulary/dataValue> <")
                    .append(entityUri).append("> .\n");
        }
    }

    public static void deletedEntityEvent(Property oldObj) {
        log.debug("Don't delete access rule if property has been deleted " + oldObj);
    }

    public static void updatedEntityEvent(Object oldObj, Object newObj) {
        if (oldObj instanceof Property || newObj instanceof Property) {
            log.debug("update entity event old " + oldObj + " new object " + newObj);
        }
    }

    public static void insertedEntityEvent(Property newObj) {
        log.debug("Nothing to do " + newObj);
    }

    private static boolean isUriInTestDataset(String entityUri, AccessOperation og, AccessObjectType aot, String role) {
        Set<String> values = PolicyLoader.getInstance().getDataSetValues(og, aot, role);
        return values.contains(entityUri);
    }

    private static String getPolicyTestDataUri(AccessObjectType aot, AccessOperation og, String role) {
        String key = aot.toString() + "." + og.toString() + "." + role;
        if (policyKeyToDataValueMap.containsKey(key)) {
            return policyKeyToDataValueMap.get(key);
        }
        String uri = PolicyLoader.getInstance().getEntityPolicyTestDataValue(og, aot, role);
        policyKeyToDataValueMap.put(key, uri);
        return uri;
    }
}
