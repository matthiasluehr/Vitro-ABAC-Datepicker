/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.ObjectProperty;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.ObjectPropertyDao;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;

public abstract class ObjectPropertyTemplateModel extends PropertyTemplateModel {
    
    private static final Log log = LogFactory.getLog(ObjectPropertyTemplateModel.class);      
    private static final String TYPE = "object";
    
    /* NB The default post-processor is not the same as the post-processor for the default view. The latter
     * actually defines its own post-processor, whereas the default post-processor is used for custom views
     * that don't define a post-processor, to ensure that the standard post-processing applies.
     */
    private static final String DEFAULT_POSTPROCESSOR = 
        "edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual.DefaultObjectPropertyDataPostProcessor";
    
    private static final String END_DATE_TIME_VARIABLE = "dateTimeEnd";
    private static final Pattern ORDER_BY_END_DATE_TIME_PATTERN = 
        /* ORDER BY DESC(?dateTimeEnd)
         * ORDER BY ?subclass ?dateTimeEnd
         * ORDER BY DESC(?subclass) DESC(?dateTimeEnd)
         */
        Pattern.compile("ORDER\\s+BY\\s+((DESC\\()?\\?subclass\\)?\\s+)?DESC\\s*\\(\\s*\\?" + END_DATE_TIME_VARIABLE + "\\)", Pattern.CASE_INSENSITIVE);
    
    private PropertyListConfig config;

    ObjectPropertyTemplateModel(ObjectProperty op, Individual subject, VitroRequest vreq) {
        super(op);
        setName(op.getDomainPublic());
        
        // Get the config for this object property
        try {
            config = new PropertyListConfig(op, vreq);
        } catch (Exception e) {
            log.error(e, e);
        }
    }
    
    protected String getQueryString() {
        return config.queryString;
    }

    protected boolean hasDefaultListView() {
        return config.isDefaultConfig;
    }
    
    protected static ObjectPropertyTemplateModel getObjectPropertyTemplateModel(ObjectProperty op, Individual subject, VitroRequest vreq) {
        if (op.getCollateBySubclass()) {
            try {
                return new CollatedObjectPropertyTemplateModel(op, subject, vreq);
            } catch (InvalidConfigurationException e) {
                log.error(e);
                return new UncollatedObjectPropertyTemplateModel(op, subject, vreq);
            }
        } else {
            return new UncollatedObjectPropertyTemplateModel(op, subject, vreq);
        }
    }
    
    /** Apply postprocessing to query results to prepare for template */
    protected void postprocess(List<Map<String, String>> data, WebappDaoFactory wdf) {
        String postprocessorName = config.postprocessor;
        if (postprocessorName == null) {
            //return;
            postprocessorName = DEFAULT_POSTPROCESSOR;
        }

        try {
            Class<?> postprocessorClass = Class.forName(postprocessorName);
            Constructor<?> constructor = postprocessorClass.getConstructor(ObjectPropertyTemplateModel.class, WebappDaoFactory.class);
            ObjectPropertyDataPostProcessor postprocessor = (ObjectPropertyDataPostProcessor) constructor.newInstance(this, wdf);
            postprocessor.process(data);
        } catch (Exception e) {
            log.error(e, e);
        }
    }
    
    /* Post-processing that must occur after collation, because it does reordering on collated subclass
     * lists rather than on the entire list. This should ideally be configurable in the config file
     * like the pre-collation post-processing, but for now due to time constraints it applies to all views.
     */
    protected void postprocessStatementList(List<ObjectPropertyStatementTemplateModel> statements) {        
        moveEndDateTimesToTop(statements);        
    }
    
    /* SPARQL ORDER BY gives null values the lowest value, so null datetimes occur at the end
     * of a list in descending sort order. Generally we assume that a null end datetime means the
     * activity is  ongoing in the present, so we want to display those at the top of the list.
     * Application of this method should be configurable in the config file, but for now due to
     * time constraints it applies to all views that sort by DESC(?dateTimeEnd), and the variable
     * name is hard-coded here. (Note, therefore, that using a different variable name  
     * effectively turns off this post-processing.)
     */
    protected void moveEndDateTimesToTop(List<ObjectPropertyStatementTemplateModel> statements) {
        String queryString = getQueryString();
        Matcher m = ORDER_BY_END_DATE_TIME_PATTERN.matcher(queryString);
        if ( ! m.find() ) {
            return;
        }
        
        // Store the statements with null end datetimes in a temporary list, remove them from the original list,
        // and move them back to the top of the original list.
        List<ObjectPropertyStatementTemplateModel> tempList = new ArrayList<ObjectPropertyStatementTemplateModel>();
        Iterator<ObjectPropertyStatementTemplateModel> iterator = statements.iterator();
        while (iterator.hasNext()) {
            ObjectPropertyStatementTemplateModel stmt = (ObjectPropertyStatementTemplateModel)iterator.next();
            String dateTimeEnd = (String) stmt.get(END_DATE_TIME_VARIABLE);
            if (dateTimeEnd == null) {
                // If the first statement has a null end datetime, all subsequent statements in the list also do,
                // so there is nothing to reorder.
                // NB This assumption is FALSE if the query orders by subclass but the property is not collated.
                // This happens because all the queries are written with a subclass variable to support 
                // collation if switched on in the back end.
                //if (statements.indexOf(stmt) == 0) {
                //    break;
                //}               
                tempList.add(stmt); 
                iterator.remove(); 
            }
        }
        // Put all the statements with null end datetimes at the top of the list, preserving their original order.
        statements.addAll(0, tempList);
    
    }
    
    protected abstract String getDefaultConfigFileName();
    
    private class PropertyListConfig {

        private static final String CONFIG_FILE_PATH = "/config/";
        private static final String NODE_NAME_QUERY = "query";
        private static final String NODE_NAME_TEMPLATE = "template";
        private static final String NODE_NAME_POSTPROCESSOR = "postprocessor";
        
        private boolean isDefaultConfig;
        private String queryString;
        private String templateName;
        private String postprocessor;

        PropertyListConfig(ObjectProperty op, VitroRequest vreq) throws Exception {

            // Get the custom config filename
            WebappDaoFactory wdf = vreq.getWebappDaoFactory();
            ObjectPropertyDao opDao = wdf.getObjectPropertyDao();
            String configFileName = opDao.getCustomListConfigFileName(op);
            if (configFileName == null) { // no custom config; use default config
                configFileName = getDefaultConfigFileName();
            }
            log.debug("Using list view config file " + configFileName + " for object property " + op.getURI());
            
            String configFilePath = getConfigFilePath(configFileName);
            try {
                File config = new File(configFilePath);            
                if ( ! isDefaultConfig(configFileName) && ! config.exists() ) {
                    log.warn("Can't find config file " + configFilePath + " for object property " + op.getURI() + "\n" +
                            ". Using default config file instead.");
                    configFilePath = getConfigFilePath(getDefaultConfigFileName());
                    // Should we test for the existence of the default, and throw an error if it doesn't exist?
                }                   
                setValuesFromConfigFile(configFilePath);           

            } catch (Exception e) {
                log.error("Error processing config file " + configFilePath + " for object property " + op.getURI(), e);
                // What should we do here?
            }
            
            if ( ! isDefaultConfig(configFileName) ) {
                String invalidConfigMessage = checkForInvalidConfig(vreq);
                if ( StringUtils.isNotEmpty(invalidConfigMessage) ) {
                    log.warn("Invalid list view config for object property " + op.getURI() + 
                            " in " + configFilePath + ":\n" +                            
                            invalidConfigMessage + " Using default config instead.");
                    configFilePath = getConfigFilePath(getDefaultConfigFileName());
                    setValuesFromConfigFile(configFilePath);                    
                }
            }
            
            isDefaultConfig = isDefaultConfig(configFileName);
        }
        
        private boolean isDefaultConfig(String configFileName) {
            return configFileName.equals(getDefaultConfigFileName());
        }
        
        private String checkForInvalidConfig(VitroRequest vreq) {
            String invalidConfigMessage = null;

            if ( StringUtils.isBlank(queryString)) {
                invalidConfigMessage = "Missing query specification.";
            } else if ( StringUtils.isBlank(templateName)) {
                invalidConfigMessage = "Missing template specification.";
            } else {
                Configuration fmConfig = (Configuration) vreq.getAttribute("freemarkerConfig");
                TemplateLoader tl = fmConfig.getTemplateLoader();
                try {
                    if ( tl.findTemplateSource(templateName) == null ) {
                        invalidConfigMessage = "Specified template " + templateName + " does not exist.";
                    }
                } catch (IOException e) {
                    log.error("Error finding template " + templateName, e);
                }
            }
            return invalidConfigMessage;
        }
        
        private void setValuesFromConfigFile(String configFilePath) {
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
 
            try {
                db = dbf.newDocumentBuilder();
                Document doc = db.parse(configFilePath);
                // Required values
                queryString = getConfigValue(doc, NODE_NAME_QUERY);
                templateName = getConfigValue(doc, NODE_NAME_TEMPLATE); 
                
                // Optional values
                postprocessor = getConfigValue(doc, NODE_NAME_POSTPROCESSOR);
            } catch (Exception e) {
                log.error("Error processing config file " + configFilePath, e);
                // What should we do here?
            }            
        }
 
        private String getConfigValue(Document doc, String nodeName) {
            NodeList nodes = doc.getElementsByTagName(nodeName);
            Element element = (Element) nodes.item(0); 
            String value = null;
            if (element != null) {
                value = element.getChildNodes().item(0).getNodeValue();   
                log.debug("Value of config parameter " + nodeName + " = " + value);
            } else {
                log.debug("No value for config parameter " + nodeName);
            }
            return value;           
        }
        
        private String getConfigFilePath(String filename) {
            return servletContext.getRealPath(CONFIG_FILE_PATH + filename);
        }
    }
    
    protected class InvalidConfigurationException extends Exception { 

        private static final long serialVersionUID = 1L;

        protected InvalidConfigurationException(String s) {
            super(s);
        }
    }
    
    /* Access methods for templates */
    
    public String getType() {
        return TYPE;
    }
    
    public String getTemplate() {
        return config.templateName;
    }
    
    public abstract boolean isCollatedBySubclass();

    @Override
    public String getAddLink() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getEditLink() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getDeleteLink() {
        // TODO Auto-generated method stub
        return null;
    }
}
