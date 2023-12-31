/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.controller.individual;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import edu.cornell.mannlib.vitro.webapp.auth.permissions.SimplePermission;
import edu.cornell.mannlib.vitro.webapp.auth.policy.PolicyHelper;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.ObjectProperty;
import edu.cornell.mannlib.vitro.webapp.config.ConfigurationProperties;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.dao.DisplayVocabulary;
import edu.cornell.mannlib.vitro.webapp.dao.IndividualDao;
import edu.cornell.mannlib.vitro.webapp.dao.ObjectPropertyDao;
import edu.cornell.mannlib.vitro.webapp.dao.VitroVocabulary;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import edu.cornell.mannlib.vitro.webapp.dao.jena.QueryUtils;
import edu.cornell.mannlib.vitro.webapp.i18n.selection.SelectedLocale;
import edu.cornell.mannlib.vitro.webapp.utils.dataGetter.ExecuteDataRetrieval;
import edu.cornell.mannlib.vitro.webapp.web.beanswrappers.ReadOnlyBeansWrapper;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual.IndividualTemplateModel;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual.IndividualTemplateModelBuilder;
import edu.ucsf.vitro.opensocial.OpenSocialManager;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * We have determined that the request is for a normal Individual, and needs an
 * HTML response. Assemble the information for that response.
 *
 * TODO clean this up.
 */
class IndividualResponseBuilder {
	private static final Log log = LogFactory
			.getLog(IndividualResponseBuilder.class);

	public interface ExtendedResponse {
	    void addOptions(VitroRequest vreq, Map<String, Object> body);
    }

    private static List<ExtendedResponse> extendedResponses = new ArrayList<>();

    public static void registerExtendedResponse(ExtendedResponse extendedResponse) {
        extendedResponses.add(extendedResponse);
    }
	
    private static final Map<String, String> namespaces = new HashMap<String, String>() {{
        put("display", VitroVocabulary.DISPLAY);
        put("vitro", VitroVocabulary.vitroURI);
        put("vitroPublic", VitroVocabulary.VITRO_PUBLIC);
    }};

	private final VitroRequest vreq;
	private final WebappDaoFactory wadf;
	private final IndividualDao iDao;
	private final ObjectPropertyDao opDao;
	private final ExecuteDataRetrieval eDataRetrieval;

	private final Individual individual;

	public IndividualResponseBuilder(VitroRequest vreq, Individual individual) {
		this.vreq = vreq;
		this.wadf = vreq.getWebappDaoFactory();
		this.iDao = wadf.getIndividualDao();
		this.opDao = wadf.getObjectPropertyDao();

		this.individual = individual;
		//initializing execute data retrieval
		this.eDataRetrieval = new ExecuteDataRetrieval(this.vreq, this.vreq.getDisplayModel(), this.individual);
	}


	ResponseValues assembleResponse() throws TemplateModelException {
		Map<String, Object> body = new HashMap<String, Object>();

		body.put("title", individual.getName());
		body.put("relatedSubject", getRelatedSubject());
		body.put("namespaces", namespaces);
		body.put("temporalVisualizationEnabled", getTemporalVisualizationFlag());
        body.put("mapOfScienceVisualizationEnabled", getMapOfScienceVisualizationFlag());
		body.put("profilePageTypesEnabled", getprofilePageTypesFlag());
		body.put("verbosePropertySwitch", getVerbosePropertyValues());

		for (ExtendedResponse extendedResponse : extendedResponses) {
		    extendedResponse.addOptions(vreq, body);
        }

		//Execute data getters that might apply to this individual, e.g. because of the class of the individual
		try{
			this.eDataRetrieval.executeDataGetters(body);
		} catch(Exception ex) {
			log.error("Data retrieval for individual lead to error", ex);
		}

		// for quick profile view - users can toggle between the quick and the full views,
		// so the "destination" let's us know which view they are targeting. On normal
		// page request, this string is empty and the default template is loaded.
        String targetedView = "";
		targetedView = vreq.getParameter("destination");
		body.put("targetedView", targetedView);

		//Individual template model
		IndividualTemplateModel itm = getIndividualTemplateModel(individual);
		/* We need to expose non-getters in displaying the individual's property list,
		 * since it requires calls to methods with parameters.
		 * This is still safe, because we are only putting BaseTemplateModel objects
		 * into the data model: no real data can be modified.
		 */
		// body.put("individual", wrap(itm, BeansWrapper.EXPOSE_SAFE));
		LabelAndLanguageCount labelAndLanguageCount = getLabelAndLanguageCount(
		        itm.getUri(), vreq);
	    body.put("labelCount", labelAndLanguageCount.getLabelCount());
	    body.put("languageCount", labelAndLanguageCount.getLanguageCount());
	    //We also need to know the number of available locales
	    body.put("localesCount", SelectedLocale.getSelectableLocales(vreq).size());
	    body.put("profileType", getProfileType(itm.getUri(), vreq));
		body.put("individual", wrap(itm, new ReadOnlyBeansWrapper()));

		body.put("headContent", getRdfLinkTag(itm));

		//If special values required for individuals like menu, include values in template values
		body.putAll(getSpecialEditingValues());

        // VIVO OpenSocial Extension by UCSF
        try {
	        OpenSocialManager openSocialManager = new OpenSocialManager(vreq,
	        		itm.isEditable() ? "individual-EDIT-MODE" : "individual", itm.isEditable());
	        openSocialManager.setPubsubData(OpenSocialManager.JSON_PERSONID_CHANNEL,
	        		OpenSocialManager.buildJSONPersonIds(individual, "1 person found"));
	        body.put(OpenSocialManager.TAG_NAME, openSocialManager);
	        if (openSocialManager.isVisible()) {
	        	body.put("bodyOnload", "my.init();");
	        }
        } catch (IOException e) {
        	log.error("IOException in doTemplate()", e);
        } catch (SQLException e) {
            log.error("SQLException in doTemplate()", e);
        }

		String template = new IndividualTemplateLocator(vreq, individual).findTemplate();

		return new TemplateResponseValues(template, body);
	}

	/**
	 * Check if a "relatedSubjectUri" parameter has been supplied, and, if so,
	 * retrieve the related individual.
	 *
	 * Some individuals make little sense standing alone and should be displayed
	 * in the context of their relationship to another.
	 */
    private Map<String, Object> getRelatedSubject() {
        Map<String, Object> map = null;

        String relatedSubjectUri = vreq.getParameter("relatedSubjectUri");
        if (relatedSubjectUri != null) {
            Individual relatedSubjectInd = iDao.getIndividualByURI(relatedSubjectUri);
            if (relatedSubjectInd != null) {
                map = new HashMap<String, Object>();
                map.put("name", relatedSubjectInd.getName());

                map.put("url", UrlBuilder.getIndividualProfileUrl(relatedSubjectInd, vreq));

                String relatingPredicateUri = vreq.getParameter("relatingPredicateUri");
                if (relatingPredicateUri != null) {
                    ObjectProperty relatingPredicateProp = opDao.getObjectPropertyByURI(relatingPredicateUri);
                    if (relatingPredicateProp != null) {
                        map.put("relatingPredicateDomainPublic", relatingPredicateProp.getDomainPublic());
                    }
                }
            }
        }
        return map;
    }

	private boolean getTemporalVisualizationFlag() {
		String property = ConfigurationProperties.getBean(vreq).getProperty(
				"visualization.temporal");
		return "enabled".equals(property);
	}

    private boolean getMapOfScienceVisualizationFlag() {
        String property = ConfigurationProperties.getBean(vreq).getProperty(
                "visualization.mapOfScience");
        return "enabled".equals(property);
    }

	private boolean getprofilePageTypesFlag() {
		String property = ConfigurationProperties.getBean(vreq).getProperty(
				"multiViews.profilePageTypes");
		return "enabled".equals(property);
	}

    private Map<String, Object> getVerbosePropertyValues() {
        Map<String, Object> map = null;

        if (PolicyHelper.isAuthorizedForActions(vreq, SimplePermission.SEE_VERBOSE_PROPERTY_INFORMATION.ACTION)) {
            // Get current verbose property display value
            String verbose = vreq.getParameter("verbose");
            Boolean verboseValue;
            // If the form was submitted, get that value
            if (verbose != null) {
                verboseValue = "true".equals(verbose);
            // If form not submitted, get the session value
            } else {
                Boolean verbosePropertyDisplayValueInSession = (Boolean) vreq.getSession().getAttribute("verbosePropertyDisplay");
                // True if session value is true, otherwise (session value is false or null) false
                verboseValue = Boolean.TRUE.equals(verbosePropertyDisplayValueInSession);
            }
            vreq.getSession().setAttribute("verbosePropertyDisplay", verboseValue);

            map = new HashMap<String, Object>();
            map.put("currentValue", verboseValue);

            /* Factors contributing to switching from a form to an anchor element:
               - Can't use GET with a query string on the action unless there is no form data, since
                 the form data is appended to the action with a "?", so there can't already be a query string
                 on it.
               - The browser (at least Firefox) does not submit a form that has no form data.
               - Some browsers might strip the query string off the form action of a POST - though
                 probably they shouldn't, because the HTML spec allows a full URI as a form action.
               - Given these three, the only reliable solution is to dynamically create hidden inputs
                 for the query parameters.
               - Much simpler is to just create an anchor element. This has the added advantage that the
                 browser doesn't ask to resend the form data when reloading the page.
             */
            StringBuilder url = new StringBuilder(vreq.getRequestURI() + "?verbose=" + !verboseValue);
            // Append request query string, except for current verbose value, to url
            String queryString = vreq.getQueryString();
            if (queryString != null) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (! param.startsWith("verbose=")) {
                        url.append("&").append(param);
                    }
                }
            }
            map.put("url", url.toString());
        } else {
            vreq.getSession().setAttribute("verbosePropertyDisplay", false);
        }

        return map;
    }

	private IndividualTemplateModel getIndividualTemplateModel(
			Individual individual) {
		//individual.sortForDisplay();
		return IndividualTemplateModelBuilder.build(individual, vreq);
	}

    private TemplateModel wrap(Object obj, BeansWrapper wrapper) throws TemplateModelException {
        return wrapper.wrap(obj);
    }

    private String getRdfLinkTag(IndividualTemplateModel itm) {
        String linkTag = null;
        String linkedDataUrl = itm.getRdfUrl();
        if (linkedDataUrl != null) {
            linkTag = "<link rel=\"alternate\" type=\"application/rdf+xml\" href=\"" +
                          linkedDataUrl + "\" /> ";
        }
        return linkTag;
    }

    //Get special values for cases such as Menu Management editing
    private Map<String, Object> getSpecialEditingValues() {
        Map<String, Object> map = new HashMap<String, Object>();

    	if(vreq.getAttribute(VitroRequest.SPECIAL_WRITE_MODEL) != null) {
    		map.put("reorderUrl", UrlBuilder.getUrl(DisplayVocabulary.REORDER_MENU_URL));
    	}

    	return map;
    }

    private static String LABEL_QUERY = ""
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
            + "SELECT ?label WHERE { \n"
            + "    ?subject rdfs:label ?label \n"
            + "    FILTER isLiteral(?label) \n"
            + "}" ;
    
//    Queries that were previously used for counts via RDFService that didn't
//        filter results by language.  With language filtering, aggregate 
//        functions like COUNT() cannot be used.
    
//    private static String LABEL_COUNT_QUERY = ""
//        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
//        + "SELECT ( str(COUNT(?label)) AS ?labelCount ) WHERE { \n"
//        + "    ?subject rdfs:label ?label \n"
//        + "    FILTER isLiteral(?label) \n"
//        + "}" ;

//    private static String DISTINCT_LANGUAGE_QUERY = ""
//            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
//            + "SELECT ( str(COUNT(DISTINCT lang(?label))) AS ?languageCount ) WHERE { \n"
//            + "    ?subject rdfs:label ?label \n"
//            + "    FILTER isLiteral(?label) \n"
//            + "}" ;

    private static LabelAndLanguageCount getLabelAndLanguageCount(
            String subjectUri, VitroRequest vreq) {
        // 1.12.0 Now filtering to only the labels for the current locale so as
        // to be consistent with other editing forms.  Because the language
        // filter can only act on a result set containing actual literals,
        // we can't do the counting with a COUNT() in the query itself.  So
        // we will now use the LABEL_QUERY instead of LABEL_COUNT_QUERY and 
        // count the rows and the number of distinct languages represented.
        Set<String> distinctLanguages = new HashSet<String>();
        String queryStr = QueryUtils.subUriForQueryVar(LABEL_QUERY, "subject", subjectUri);
        log.debug("queryStr = " + queryStr);
        int labelCount = 0;
        try {   
            ResultSet results = QueryUtils.getQueryResults(queryStr, vreq);
            while(results.hasNext()) {
                QuerySolution qsoln = results.next();
                labelCount++;
                String lang = qsoln.getLiteral("label").getLanguage();
                if(lang == null) {
                    lang = "";
                }
                distinctLanguages.add(lang);
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        return new LabelAndLanguageCount(labelCount, distinctLanguages.size());
    }
    
    private static class LabelAndLanguageCount {
        
        private Integer labelCount;
        private Integer languageCount;
        
        public LabelAndLanguageCount(Integer labelCount, Integer languageCount) {
            this.labelCount = labelCount;
            this.languageCount = languageCount;
        }
        
        public Integer getLabelCount() {
            return this.labelCount;
        }
        
        public Integer getLanguageCount() {
            return this.languageCount;
        }
        
    }

    //what is the number of languages represented across the labels
    // This version not compatible with language-filtering RDF services
//    private static Integer getLanguagesRepresentedCount(String subjectUri, VitroRequest vreq) {
//    	   String queryStr = QueryUtils.subUriForQueryVar(DISTINCT_LANGUAGE_QUERY, "subject", subjectUri);
//           log.debug("queryStr = " + queryStr);
//           int theCount = 0;
//           try {
//
//               ResultSet results = QueryUtils.getLanguageNeutralQueryResults(queryStr, vreq);
//               if (results.hasNext()) {
//                   QuerySolution soln = results.nextSolution();
//                   RDFNode languageCount = soln.get("languageCount");
//                   if (languageCount != null && languageCount.isLiteral()) {
//                       theCount = languageCount.asLiteral().getInt();
//                       log.info("Language count is " + theCount);
//                   }
//               }
//           } catch (Exception e) {
//               log.error(e, e);
//           }
//           log.info("Returning language count " + theCount);
//           return theCount;
//    }

    private static String PROFILE_TYPE_QUERY = ""
        + "PREFIX display: <http://vitro.mannlib.cornell.edu/ontologies/display/1.1#> \n"
        + "SELECT ?profile WHERE { \n"
        + "    ?subject display:hasDefaultProfilePageType ?profile \n"
        + "}" ;

    private static String getProfileType(String subjectUri, VitroRequest vreq) {
        String queryStr = QueryUtils.subUriForQueryVar(PROFILE_TYPE_QUERY, "subject", subjectUri);
        log.debug("queryStr = " + queryStr);
        String profileType = "none";
        try {
            ResultSet results = QueryUtils.getQueryResults(queryStr, vreq);
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String profileStr = soln.get("profile").toString();
                if ( profileStr.length() > 0 ) {
                    profileType = profileStr.substring(profileStr.indexOf("#")+1,profileStr.length());
                }
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        return profileType;
    }
}
