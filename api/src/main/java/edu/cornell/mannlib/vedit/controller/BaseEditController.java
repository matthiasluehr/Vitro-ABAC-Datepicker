/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vedit.controller;

import static edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess.ReasoningOption.ASSERTIONS_ONLY;

import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import edu.cornell.mannlib.vedit.beans.EditProcessObject;
import edu.cornell.mannlib.vedit.beans.Option;
import edu.cornell.mannlib.vedit.util.FormUtils;
import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessObjectType;
import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessOperation;
import edu.cornell.mannlib.vitro.webapp.auth.policy.EntityPolicyController;
import edu.cornell.mannlib.vitro.webapp.beans.PermissionSet;
import edu.cornell.mannlib.vitro.webapp.controller.Controllers;
import edu.cornell.mannlib.vitro.webapp.controller.VitroHttpServlet;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BaseEditController extends VitroHttpServlet {

    public static final String ENTITY_URI_ATTRIBUTE_NAME = "_permissionsEntityURI";
    public static final String ENTITY_TYPE_ATTRIBUTE_NAME = "_permissionsEntityType";

    public static final boolean FORCE_NEW = true; // when you know you're starting a new edit process

    public static final String JSP_PREFIX = "/templates/edit/specific/";

    protected static DateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    protected static final int BASE_10 = 10;

    private static final Log log = LogFactory.getLog(BaseEditController.class.getName());
    private static final String DEFAULT_LANDING_PAGE = Controllers.SITE_ADMIN;
    protected static final String MULTIPLEXED_PARAMETER_NAME = "multiplexedParam";
    private final String EPO_HASH_ATTR = "epoHash";
    private final String EPO_KEYLIST_ATTR = "epoKeylist";
    private final int MAX_EPOS = 5;
    private final Calendar cal = Calendar.getInstance();

    /*
     * EPO is reused if the controller is passed an epoKey, e.g. if a previous form submission failed validation, or the
     * edit is a multistage process.
     */

    protected EditProcessObject createEpo(HttpServletRequest request) {
        return createEpo(request, false);
    }

    protected EditProcessObject createEpo(HttpServletRequest request, boolean forceNew) {
        /*
         * this is actually a bit of a misnomer, because we will reuse an epo if an epoKey parameter is passed
         */
        EditProcessObject epo = null;
        HashMap epoHash = getEpoHash(request);
        String existingEpoKey = request.getParameter("_epoKey");
        if (!forceNew && existingEpoKey != null && epoHash.get(existingEpoKey) != null) {
            epo = (EditProcessObject) epoHash.get(existingEpoKey);
            epo.setKey(existingEpoKey);
            epo.setUseRecycledBean(true);
        } else {
            LinkedList epoKeylist = getEpoKeylist(request);
            if (epoHash.size() == MAX_EPOS) {
                try {
                    epoHash.remove(epoKeylist.getFirst());
                    epoKeylist.removeFirst();
                } catch (Exception e) {
                    // see JIRA issue VITRO-340, "Odd exception from backend editing"
                    // possible rare concurrency issue here
                    log.error("Error removing old EPO", e);
                }
            }
            Random rand = new Random();
            String epoKey = createEpoKey();
            while (epoHash.get(epoKey) != null) {
                epoKey += Integer.toHexString(rand.nextInt());
            }
            epo = new EditProcessObject();
            epoHash.put(epoKey, epo);
            epoKeylist.add(epoKey);
            epo.setKey(epoKey);
            epo.setReferer((forceNew) ? request.getRequestURL().append('?').append(request.getQueryString()).toString()
                    : request.getHeader("Referer"));
            epo.setSession(request.getSession());
        }
        return epo;
    }

    private LinkedList getEpoKeylist(HttpServletRequest request) {
        return (LinkedList) request.getSession().getAttribute(EPO_KEYLIST_ATTR);
    }

    private HashMap getEpoHash(HttpServletRequest request) {
        HashMap epoHash = (HashMap) request.getSession().getAttribute(EPO_HASH_ATTR);
        if (epoHash == null) {
            epoHash = new HashMap();
            request.getSession().setAttribute(EPO_HASH_ATTR, epoHash);
            // since we're making a new EPO hash, we should also make a new keylist.
            LinkedList epoKeylist = new LinkedList();
            request.getSession().setAttribute(EPO_KEYLIST_ATTR, epoKeylist);
        }
        return epoHash;
    }

    private String createEpoKey() {
        return Long.toHexString(cal.getTimeInMillis());
    }

    protected void setRequestAttributes(HttpServletRequest request, EditProcessObject epo) {
        VitroRequest vreq = new VitroRequest(request);
        request.setAttribute("epoKey", epo.getKey());
        request.setAttribute("epo", epo);
        request.setAttribute("globalErrorMsg", epo.getAttribute("globalErrorMsg"));
        request.setAttribute("css", "<link rel=\"stylesheet\" type=\"text/css\" href=\""
                + vreq.getAppBean().getThemeDir() + "css/edit.css\"/>");
    }

    protected void populateBeanFromParams(Object bean, HttpServletRequest request) {
        Map params = request.getParameterMap();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String key = "";
            try {
                key = (String) paramNames.nextElement();
            } catch (ClassCastException cce) {
                log.error("populateBeanFromParams() could not cast parameter name to String");
            }
            String value = "";
            if (key.equals(MULTIPLEXED_PARAMETER_NAME)) {
                String multiplexedStr = request.getParameterValues(key)[0];
                Map<String, String> paramMap = FormUtils.beanParamMapFromString(multiplexedStr);
                for (String param : paramMap.keySet()) {
                    String demultiplexedValue = (String) paramMap.get(param);
                    FormUtils.beanSet(bean, param, demultiplexedValue);
                }

            } else {
                try {
                    value = (String) request.getParameterValues(key)[0];
                } catch (ClassCastException cce) {
                    try {
                        value = ((Integer) params.get(key)).toString();
                    } catch (ClassCastException ccf) {
                        log.error("populateBeanFromParams() could not cast parameter name to String");
                    }
                }
                FormUtils.beanSet(bean, key, value);
            }
        }
    }

    public List<Option> getSortedList(HashMap<String, Option> hashMap, List<Option> optionList, VitroRequest vreq) {

        class ListComparator implements Comparator<String> {

            Collator collator;

            public ListComparator(Collator collator) {
                this.collator = collator;
            }

            @Override
            public int compare(String str1, String str2) {
                return collator.compare(str1, str2);
            }

        }

        List<String> bodyVal = new ArrayList<String>();
        List<Option> options = new ArrayList<Option>();
        for (Option option : optionList) {
            hashMap.put(option.getBody(), option);
            bodyVal.add(option.getBody());
        }

        bodyVal.sort(new ListComparator(vreq.getCollator()));
        for (String aBodyVal : bodyVal) {
            options.add(hashMap.get(aBodyVal));
        }
        return options;
    }

    protected WebappDaoFactory getWebappDaoFactory() {
        return ModelAccess.getInstance().getWebappDaoFactory(ASSERTIONS_ONLY);
    }

    protected WebappDaoFactory getWebappDaoFactory(String userURI) {
        return getWebappDaoFactory().getUserAwareDaoFactory(userURI);
    }

    public String getDefaultLandingPage(HttpServletRequest request) {
        return (request.getContextPath() + DEFAULT_LANDING_PAGE);
    }

    protected static void addAccessAttributes(HttpServletRequest req, String entityURI, AccessObjectType aot) {
        // Add the permissionsEntityURI (if we are creating a new property, this will be empty)
        req.setAttribute(ENTITY_URI_ATTRIBUTE_NAME, entityURI);

        // Get the available permission sets
        List<PermissionSet> permissionSets = buildListOfSelectableRoles(ModelAccess.on(req).getWebappDaoFactory());
        List<RoleInfo> roles = new ArrayList<>();
        List<String> roleUris = new ArrayList<>();

        for (PermissionSet permissionSet : permissionSets) {
            roles.add(new RoleInfo(permissionSet));
            roleUris.add(permissionSet.getUri());
        }
        List<AccessOperation> accessOperations = AccessOperation.getOperations(aot);
        // Operation, list of roles>
        Map<String, List<RoleInfo>> operationsToRoles = new LinkedHashMap<>();
        for (AccessOperation operation : accessOperations) {
            List<RoleInfo> roleInfos = new LinkedList<>();
            String operationName = StringUtils.capitalize(operation.toString().toLowerCase());
            operationsToRoles.put(operationName, roleInfos);
            for (RoleInfo role : roles) {
                RoleInfo roleCopy = role.clone();
                roleInfos.add(roleCopy);
                if (operation.equals(AccessOperation.ADD) || operation.equals(AccessOperation.DROP)
                        || operation.equals(AccessOperation.EDIT) || operation.equals(AccessOperation.PUBLISH)
                        || operation.equals(AccessOperation.UPDATE)) {
                    if (roleCopy.isPublic) {
                        roleCopy.setEnabled(false);
                        roleCopy.setGranted(false);
                    }
                }
            }
            // If we are creating a new record
            if (!StringUtils.isEmpty(entityURI)) {
                Set<String> grantedRoles = new HashSet<String>(
                        EntityPolicyController.getGrantedRoles(entityURI, operation, aot, roleUris));
                for (RoleInfo roleInfo : roleInfos) {
                    if (!grantedRoles.contains(roleInfo.getUri())) {
                        roleInfo.setGranted(false);
                    }
                }
            }
        }
        req.setAttribute("operationsToRoles", operationsToRoles);

    }

    public static class RoleInfo {
        String uri;
        String label;
        private boolean enabled = true;
        private boolean granted = true;
        private boolean isPublic;

        public RoleInfo(PermissionSet ps) {
            uri = ps.getUri();
            label = ps.getLabel();
            isPublic = ps.isForPublic();
        }

        public RoleInfo(String uri, String label, boolean isPublic) {
            this.uri = uri;
            this.label = label;
            this.isPublic = isPublic;
        }

        public String getUri() {
            return uri;
        }

        public String getLabel() {
            return label;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isGranted() {
            return granted;
        }

        public void setGranted(boolean granted) {
            this.granted = granted;
        }

        public RoleInfo clone() {
            return new RoleInfo(uri, label, isPublic);
        }

        public boolean isPublic() {
            return isPublic;
        }
    }

    /**
     * Create a list of all known PermissionSets.
     */
    protected static List<PermissionSet> buildListOfSelectableRoles(WebappDaoFactory wadf) {
        List<PermissionSet> permissionSets = new ArrayList<>();

        // Get the non-public PermissionSets.
        for (PermissionSet ps : wadf.getUserAccountsDao().getAllPermissionSets()) {
            if (!ps.isForPublic()) {
                permissionSets.add(ps);
            }
        }

        // Sort the non-public PermissionSets
        permissionSets.sort(new Comparator<PermissionSet>() {
            @Override
            public int compare(PermissionSet ps1, PermissionSet ps2) {
                return ps1.getUri().compareTo(ps2.getUri());
            }
        });

        // Add the public PermissionSets.
        for (PermissionSet ps : wadf.getUserAccountsDao().getAllPermissionSets()) {
            if (ps.isForPublic()) {
                permissionSets.add(ps);
            }
        }

        return permissionSets;
    }
}
