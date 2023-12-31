/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.auth.policy;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import stubs.javax.servlet.ServletContextStub;
import stubs.javax.servlet.http.HttpServletRequestStub;
import stubs.javax.servlet.http.HttpSessionStub;
import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessObjectType;
import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessOperation;
import edu.cornell.mannlib.vitro.webapp.auth.identifier.IdentifierBundle;
import edu.cornell.mannlib.vitro.webapp.auth.objects.AccessObject;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.DecisionResult;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.PolicyDecision;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.Policy;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.AuthorizationRequest;

/**
 * Test the ability of the Policy Helper to authorize a variety of simple or
 * complex AuthorizationRequests
 */
public class PolicyHelper_AuthorizationRequestTest {
	private ServletContextStub ctx;
	private HttpServletRequestStub req;
	private AccessObject nullAr = null;

	@Before
	public void setup() {
		ctx = new ServletContextStub();
		PolicyStore.getInstance().clear();
		HttpSessionStub session = new HttpSessionStub();
		session.setServletContext(ctx);

		req = new HttpServletRequestStub();
		req.setSession(session);
	}

	// ----------------------------------------------------------------------
	// Helper methods
	// ----------------------------------------------------------------------

	private void createPolicy(AccessObject... authorizedActions) {
		PolicyStore.getInstance().add(new MySimplePolicy(authorizedActions));
	}

    /*
     * @Test public void authorizedForActionsNull() { createPolicy();
     * assertTrue("null actions", PolicyHelper.isAuthorizedForActions(req,
     * nullAr)); }
     */

	
    /*
     * @Test public void authorizedForActionsAndPass() { createPolicy(new
     * Action1(), new Action2()); assertTrue("and pass",
     * PolicyHelper.isAuthorizedForActions(req, new Action1(), new Action2()));
     * }
     * 
     * @Test public void authorizedForActionsAndFail() { createPolicy(new
     * Action2()); assertFalse("and fail",
     * PolicyHelper.isAuthorizedForActions(req, new Action1(), new Action2()));
     * }
     * 
     * @Test public void authorizedForActionsAndOrPass() { createPolicy(new
     * Action3()); assertTrue( "and-or pass",
     * PolicyHelper.isAuthorizedForActions(req, new Action1().and(new
     * Action2()).or(new Action3()))); }
     */
    /*
     * @Test public void authorizedForActionsAndOrFail() { createPolicy(new
     * Action1()); assertFalse( "and-or fail",
     * PolicyHelper.isAuthorizedForActions(req, new Action1().and(new
     * Action2()).or(new Action3()))); }
     */

    /*
     * @Test public void authorizedByACombinationOfPolicies() {
     * PolicyStore.addPolicy(new MySimplePolicy(new Action1()));
     * PolicyStore.addPolicy(new MySimplePolicy(new Action2()));
     * assertTrue("combination of policies",
     * PolicyHelper.isAuthorizedForActions(req, new Action2(), new Action1()));
     * }
     */

	// ----------------------------------------------------------------------
	// Helper Classes
	// ----------------------------------------------------------------------

	public static class Action1 extends AccessObject {

        @Override
        public AccessObjectType getType() {
            return null;
        }
		// actions must be public, with public constructor
	}

	public static class Action2 extends AccessObject {
	    
        @Override
        public AccessObjectType getType() {
            return null;
        }
		// actions must be public, with public constructor
	}

	public static class Action3 extends AccessObject {
	    
        @Override
        public AccessObjectType getType() {
            return null;
        }
		// actions must be public, with public constructor
	}

	private static class MySimplePolicy implements Policy {
		private final Set<AccessObject> authorizedActions;

		public MySimplePolicy(AccessObject... authorizedActions) {
			this.authorizedActions = new HashSet<AccessObject>(
					Arrays.asList(authorizedActions));
		}

		@Override
		public PolicyDecision decide(AuthorizationRequest ar) {
            AccessObject whatToAuth = ar.getAccessObject();
			for (AccessObject authorized : authorizedActions) {
				if (authorized.getClass().equals(whatToAuth.getClass())) {
					return new BasicPolicyDecision(DecisionResult.AUTHORIZED,
							"matched " + authorized.getClass().getSimpleName());
				}

			}
			return new BasicPolicyDecision(DecisionResult.INCONCLUSIVE, "nope");
		}

	}

}
