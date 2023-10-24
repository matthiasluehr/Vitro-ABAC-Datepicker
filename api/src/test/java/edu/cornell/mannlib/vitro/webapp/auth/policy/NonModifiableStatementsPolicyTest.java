package edu.cornell.mannlib.vitro.webapp.auth.policy;

import static edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessOperation.DROP;
import static edu.cornell.mannlib.vitro.webapp.auth.objects.AccessObject.SOME_LITERAL;
import static edu.cornell.mannlib.vitro.webapp.auth.objects.AccessObject.SOME_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.cornell.mannlib.vitro.webapp.auth.objects.AccessObject;
import edu.cornell.mannlib.vitro.webapp.auth.objects.DataPropertyStatementAccessObject;
import edu.cornell.mannlib.vitro.webapp.auth.objects.ObjectPropertyStatementAccessObject;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.DecisionResult;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.SimpleAuthorizationRequest;
import edu.cornell.mannlib.vitro.webapp.auth.rules.AccessRule;
import edu.cornell.mannlib.vitro.webapp.beans.Property;
import org.junit.Test;

public class NonModifiableStatementsPolicyTest extends PolicyTest {

    private static final String VALID = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Valid";
    private static final String MOD_TIME = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#modTime";
    public static final String NOT_MODIFIABLE_STATEMENTS_POLICY_PATH = "policy_not_modifiable_statements";

    @Test
    public void testNonModifiableStatementsPolicy() {
        load(PolicyTest.USER_ACCOUNTS_HOME_FIRSTTIME + NOT_MODIFIABLE_STATEMENTS_POLICY_PATH + EXT);
        load(USER_ACCOUNTS_HOME_FIRSTTIME + NOT_MODIFIABLE_STATEMENTS_POLICY_PATH + DATASET + EXT);

        String policyUri =
                "https://vivoweb.org/ontology/vitro-application/auth/individual/NotModifiableStatementsPolicy";
        DynamicPolicy policy = loader.loadPolicy(policyUri);
        assertTrue(policy != null);
        assertEquals(8000, policy.getPriority());
        assertEquals(5, policy.getRules().size());
        AccessRule rule = policy.getRules().iterator().next();
        assertEquals(false, rule.isAllowMatched());
        for (AccessRule irule : policy.getRules()) {
            assertEquals(3, irule.getAttributesCount());
        }

        AccessObject ao = new ObjectPropertyStatementAccessObject(null, VALID, null, null);
        SimpleAuthorizationRequest ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.UNAUTHORIZED, policy.decide(ar).getDecisionResult());
        ao = new ObjectPropertyStatementAccessObject(null, MOD_TIME, null, null);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());

        ao = new ObjectPropertyStatementAccessObject(null, null, new Property(VALID), null);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.UNAUTHORIZED, policy.decide(ar).getDecisionResult());
        ao = new ObjectPropertyStatementAccessObject(null, null, new Property(MOD_TIME), null);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());

        ao = new ObjectPropertyStatementAccessObject(null, null, null, VALID);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.UNAUTHORIZED, policy.decide(ar).getDecisionResult());
        ao = new ObjectPropertyStatementAccessObject(null, null, null, MOD_TIME);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());

        // Data property statement
        ao = new DataPropertyStatementAccessObject(null, VALID, SOME_URI, SOME_LITERAL);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.UNAUTHORIZED, policy.decide(ar).getDecisionResult());
        ao = new DataPropertyStatementAccessObject(null, MOD_TIME, SOME_URI, SOME_LITERAL);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());

        ao = new DataPropertyStatementAccessObject(null, SOME_URI, VALID, SOME_LITERAL);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.UNAUTHORIZED, policy.decide(ar).getDecisionResult());
        ao = new DataPropertyStatementAccessObject(null, SOME_URI, MOD_TIME, SOME_LITERAL);
        ar = new SimpleAuthorizationRequest(ao, DROP);
        assertEquals(DecisionResult.INCONCLUSIVE, policy.decide(ar).getDecisionResult());
    }
}
