package edu.cornell.mannlib.vitro.webapp.auth.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import edu.cornell.mannlib.vitro.webapp.auth.permissions.SimplePermission;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.DecisionResult;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.SimpleAuthorizationRequest;
import edu.cornell.mannlib.vitro.webapp.auth.rules.AccessRule;
import org.junit.Test;

public class SimplePermissionPolicyTest extends PolicyTest {

    public static final String ADMIN_SIMPLE_PERMISSIONS_DATASET_PATH = "policy_admin_simple_permissions_dataset";
    public static final String CURATOR_SIMPLE_PERMISSIONS_DATASET_PATH = "policy_curator_simple_permissions_dataset";
    public static final String EDITOR_SIMPLE_PERMISSIONS_DATASET_PATH = "policy_editor_simple_permissions_dataset";
    public static final String SELF_EDITOR_SIMPLE_PERMISSIONS_DATASET_PATH = "policy_self_editor_simple_permissions_dataset";
    public static final String PUBLIC_SIMPLE_PERMISSIONS_DATASET_PATH = "policy_public_simple_permissions_dataset";
    public static final String SIMPLE_PERMISSIONS_POLICY_PATH = "template_simple_permissions";

    @Test
    public void testAdminSimplePermissionPolicy() {
        load(PolicyTest.USER_ACCOUNTS_HOME_FIRSTTIME + SIMPLE_PERMISSIONS_POLICY_PATH + EXT);
        load(USER_ACCOUNTS_HOME_FIRSTTIME + ADMIN_SIMPLE_PERMISSIONS_DATASET_PATH + EXT);
        String dataSetUri = "https://vivoweb.org/ontology/vitro-application/auth/individual/template/simple-permissions/AdminSimplePermissionDataSet";
        DynamicPolicy policy = loader.loadPolicyFromTemplateDataSet(dataSetUri);
        assertTrue(policy != null);
        assertEquals(1000, policy.getPriority());
        assertEquals(1, policy.getRules().size());
        final AccessRule rule = policy.getRules().iterator().next();
        assertEquals(true, rule.isAllowMatched());
        assertEquals(3, rule.getAttributesCount());

        SimpleAuthorizationRequest ar = new SimpleAuthorizationRequest(SimplePermission.NS + "SeeSiteAdminPage");
        ar.setRoleUris(Arrays.asList(ROLE_CURATOR_URI));
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());
        ar.setRoleUris(Arrays.asList(ROLE_ADMIN_URI));
        assertEquals(DecisionResult.AUTHORIZED, policy.decide(ar).getDecisionResult());
    }

    @Test
    public void testCuratorSimplePermissionPolicy() {
        load(PolicyTest.USER_ACCOUNTS_HOME_FIRSTTIME + SIMPLE_PERMISSIONS_POLICY_PATH + EXT);
        load(USER_ACCOUNTS_HOME_FIRSTTIME + CURATOR_SIMPLE_PERMISSIONS_DATASET_PATH + EXT);
        String dataSetUri = "https://vivoweb.org/ontology/vitro-application/auth/individual/template/simple-permissions/CuratorSimplePermissionDataSet";
        DynamicPolicy policy = loader.loadPolicyFromTemplateDataSet(dataSetUri);
        assertTrue(policy != null);
        assertEquals(1000, policy.getPriority());
        assertEquals(1, policy.getRules().size());
        final AccessRule rule = policy.getRules().iterator().next();
        assertEquals(true, rule.isAllowMatched());
        assertEquals(3, rule.getAttributesCount());

        SimpleAuthorizationRequest ar = new SimpleAuthorizationRequest(SimplePermission.NS + "EditOntology");
        ar.setRoleUris(Arrays.asList(ROLE_EDITOR_URI));
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());
        ar.setRoleUris(Arrays.asList(ROLE_CURATOR_URI));
        assertEquals(DecisionResult.AUTHORIZED, policy.decide(ar).getDecisionResult());
    }

    @Test
    public void testEditorSimplePermissionPolicy() {
        load(PolicyTest.USER_ACCOUNTS_HOME_FIRSTTIME + SIMPLE_PERMISSIONS_POLICY_PATH + EXT);
        load(USER_ACCOUNTS_HOME_FIRSTTIME + EDITOR_SIMPLE_PERMISSIONS_DATASET_PATH + EXT);
        String dataSetUri = "https://vivoweb.org/ontology/vitro-application/auth/individual/template/simple-permissions/EditorSimplePermissionDataSet";
        DynamicPolicy policy = loader.loadPolicyFromTemplateDataSet(dataSetUri);
        assertTrue(policy != null);
        assertEquals(1000, policy.getPriority());
        assertEquals(1, policy.getRules().size());
        final AccessRule rule = policy.getRules().iterator().next();
        assertEquals(true, rule.isAllowMatched());
        assertEquals(3, rule.getAttributesCount());

        SimpleAuthorizationRequest ar = new SimpleAuthorizationRequest(SimplePermission.NS + "DoBackEndEditing");
        ar.setRoleUris(Arrays.asList(ROLE_SELF_EDITOR_URI));
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());
        ar.setRoleUris(Arrays.asList(ROLE_EDITOR_URI));
        assertEquals(DecisionResult.AUTHORIZED, policy.decide(ar).getDecisionResult());
    }

    @Test
    public void testSelfEditorSimplePermissionPolicy() {
        load(PolicyTest.USER_ACCOUNTS_HOME_FIRSTTIME + SIMPLE_PERMISSIONS_POLICY_PATH + EXT);
        load(USER_ACCOUNTS_HOME_FIRSTTIME + SELF_EDITOR_SIMPLE_PERMISSIONS_DATASET_PATH + EXT);
        String dataSetUri = "https://vivoweb.org/ontology/vitro-application/auth/individual/template/simple-permissions/SelfEditorSimplePermissionDataSet";
        DynamicPolicy policy = loader.loadPolicyFromTemplateDataSet(dataSetUri);
        assertTrue(policy != null);
        assertEquals(1000, policy.getPriority());
        assertEquals(1, policy.getRules().size());
        final AccessRule rule = policy.getRules().iterator().next();
        assertEquals(true, rule.isAllowMatched());
        assertEquals(3, rule.getAttributesCount());

        SimpleAuthorizationRequest ar = new SimpleAuthorizationRequest(SimplePermission.NS + "DoFrontEndEditing");
        ar.setRoleUris(Arrays.asList(ROLE_PUBLIC_URI));
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());
        ar.setRoleUris(Arrays.asList(ROLE_SELF_EDITOR_URI));
        assertEquals(DecisionResult.AUTHORIZED, policy.decide(ar).getDecisionResult());
    }

    @Test
    public void testPublicSimplePermissionPolicy() {
        load(PolicyTest.USER_ACCOUNTS_HOME_FIRSTTIME + SIMPLE_PERMISSIONS_POLICY_PATH + EXT);
        load(USER_ACCOUNTS_HOME_FIRSTTIME + PUBLIC_SIMPLE_PERMISSIONS_DATASET_PATH + EXT);
        String dataSetUri = "https://vivoweb.org/ontology/vitro-application/auth/individual/template/simple-permissions/PublicSimplePermissionDataSet";
        DynamicPolicy policy = loader.loadPolicyFromTemplateDataSet(dataSetUri);
        assertTrue(policy != null);
        assertEquals(1000, policy.getPriority());
        assertEquals(1, policy.getRules().size());
        final AccessRule rule = policy.getRules().iterator().next();
        assertEquals(true, rule.isAllowMatched());
        assertEquals(3, rule.getAttributesCount());

        SimpleAuthorizationRequest ar = new SimpleAuthorizationRequest(SimplePermission.NS + "QueryFullModel");
        ar.setRoleUris(Arrays.asList(""));
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());
        ar.setRoleUris(Arrays.asList(ROLE_PUBLIC_URI));
        assertEquals(DecisionResult.AUTHORIZED, policy.decide(ar).getDecisionResult());
    }

}
