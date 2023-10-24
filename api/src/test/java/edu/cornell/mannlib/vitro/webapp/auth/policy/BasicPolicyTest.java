package edu.cornell.mannlib.vitro.webapp.auth.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Collections;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;

public class BasicPolicyTest extends PolicyTest {

    public static final String BROKEN_POLICY_BROKEN_TEST = RESOURCES_RULES_PREFIX + "test_policy_broken1.n3";
    public static final String BROKEN_POLICY_BROKEN_TYPE = RESOURCES_RULES_PREFIX + "test_policy_broken2.n3";
    public static final String BROKEN_POLICY_BROKEN_TEST_ID = RESOURCES_RULES_PREFIX + "test_policy_broken3.n3";
    public static final String BROKEN_POLICY_BROKEN_TYPE_ID = RESOURCES_RULES_PREFIX + "test_policy_broken4.n3";
    public static final String BROKEN_POLICY_BROKEN_ATTRIBUTE_VALUES =
            RESOURCES_RULES_PREFIX + "test_policy_broken5.n3";

    @Test
    public void testGetPolicyUris() {
        load(VALID_POLICY);
        assertTrue(!loader.getPolicyUris().isEmpty());
    }

    @Test
    public void testValidPolicy() {
        load(VALID_POLICY);
        assertTrue(!loader.getPolicyUris().isEmpty());
    }

    @Test
    public void testValidPolicyTemplate() {
        load(VALID_POLICY_TEMPLATE);
        DynamicPolicy policy =
                loader.loadPolicy("https://vivoweb.org/ontology/vitro-application/auth/individual/ValidTestSetPolicy");
        countRulesAndAttributes(policy, 2, Collections.singleton(2));
    }

    @Test
    public void testBrokenTestId() {
        load(BROKEN_POLICY_BROKEN_TEST_ID);
        DynamicPolicy policy = loader.loadPolicy(
                "https://vivoweb.org/ontology/vitro-application/auth/individual/BrokenPolicyBrokenTestTypeId");
        assertTrue(policy == null);
    }

    @Test
    public void testBrokenTypeId() {
        load(BROKEN_POLICY_BROKEN_TYPE_ID);
        DynamicPolicy policy =
                loader.loadPolicy("https://vivoweb.org/ontology/vitro-application/auth/individual/BrokenPolicyTypeId");
        assertTrue(policy == null);
    }

    @Test
    public void testBrokenTest() {
        load(BROKEN_POLICY_BROKEN_TEST);
        DynamicPolicy policy = loader.loadPolicy(
                "https://vivoweb.org/ontology/vitro-application/auth/individual/BrokenPolicyBrokenTestType");
        assertTrue(policy == null);
    }

    @Test
    public void testBrokenType() {
        load(BROKEN_POLICY_BROKEN_TYPE);
        DynamicPolicy policy =
                loader.loadPolicy("https://vivoweb.org/ontology/vitro-application/auth/individual/BrokenPolicyType");
        assertTrue(policy == null);
    }

    @Test
    public void testBrokenAttributeValue() {
        load(BROKEN_POLICY_BROKEN_ATTRIBUTE_VALUES);
        DynamicPolicy policy =
                loader.loadPolicy("https://vivoweb.org/ontology/vitro-application/auth/individual/BrokenTestSetPolicy");
        assertTrue(policy == null);
    }

    @Test
    public void testBrokenPolicyTemplate() {
        load(BROKEN_POLICY_TEMPLATE);
        DynamicPolicy policy =
                loader.loadPolicy("https://vivoweb.org/ontology/vitro-application/auth/individual/BrokenTestSetPolicy");
        assertTrue(policy == null);
    }

    @Test
    public void testDuplicateTriplesInPolicies() {
        Model m1 = ModelFactory.createDefaultModel();
        Model m2 = ModelFactory.createDefaultModel();
        for (String entityPolicyPath : entityPolicies) {
            load(m2, entityPolicyPath);
            Model intersection = m1.intersection(m2);
            if (intersection.size() != 0) {
                StringWriter sw = new StringWriter();
                m1.write(sw, "TTL");
                System.out.println(entityPolicyPath);
                System.out.println(sw.toString());
            }
            assertEquals(0, intersection.size());
            load(m1, entityPolicyPath);
            m2.removeAll();
        }
    }
}
