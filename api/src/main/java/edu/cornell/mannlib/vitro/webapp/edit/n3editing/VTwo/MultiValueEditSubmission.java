/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.XSD;
import org.apache.jena.vocabulary.RDF;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.edit.EditLiteral;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.i18n.I18n;

public class MultiValueEditSubmission {

    private static final String LABEL = "label";

    String editKey;

    private Map<String,List<Literal>> literalsFromForm ;
    private Map<String,List<String>> urisFromForm ;
    private Map<String,String> validationErrors;
    private BasicValidationVTwo basicValidation;
    
    private static Model literalCreationModel;
    private String entityToReturnTo;
    private VitroRequest _vreq;

    private static final String TIME_URI = XSD.time.getURI();


    static{
        literalCreationModel = ModelFactory.createDefaultModel();
    }
	/*
	 *  UQAM
	 *   replace
	 *   public MultiValueEditSubmission(Map<String,String[]> queryParameters,  EditConfigurationVTwo editConfig)
	 *   by this new signature
	 *   This affect PostEditCleanupController and ProcessRdfFormController classes.
	 *   This replacement is justified by the fact that we need a linguistic context in this class.
	 */
    public MultiValueEditSubmission(VitroRequest vreq,  EditConfigurationVTwo editConfig){
    	// UQAM add this both lines
		_vreq  = vreq;
		Map<String,String[]> queryParameters = vreq.getParameterMap();
        if( editConfig == null )
            throw new Error("EditSubmission needs an EditConfiguration");
        this.editKey = editConfig.getEditKey();
        if( this.editKey == null || this.editKey.trim().length() == 0)
            throw new Error("EditSubmission needs an 'editKey' parameter from the EditConfiguration");

        entityToReturnTo = editConfig.getEntityToReturnTo();

        validationErrors = new HashMap<String,String>();

        this.urisFromForm = new HashMap<String,List<String>>();
        for( String var: editConfig.getUrisOnform() ){
            String[] valuesArray = queryParameters.get( var );
            //String uri = null;
            addUriToForm(editConfig, var, valuesArray);
        }

        this.literalsFromForm =new HashMap<String,List<Literal>>();
        for(String var: editConfig.getLiteralsOnForm() ){
            FieldVTwo field = editConfig.getField(var);
            if( field == null ) {
                log.error("could not find field " + var + " in EditConfiguration" );
                continue;
            } else if( field.getEditElement() != null ){
                log.debug("skipping field with edit element, it should not be in literals on form list");
            }else{
               String[] valuesArray = queryParameters.get(var);
               addLiteralToForm(editConfig, field, var, valuesArray);
            }
        }

        if( log.isDebugEnabled() ){
            for( String key : literalsFromForm.keySet() ){
                log.debug( key + " literal " + literalsFromForm.get(key) );
            }
            for( String key : urisFromForm.keySet() ){
                log.debug( key + " uri " + urisFromForm.get(key) );
            }
        }

        processEditElementFields(editConfig,queryParameters);
        //Incorporating basic validation
        //Validate URIS
        this.basicValidation = new BasicValidationVTwo(editConfig, I18n.bundle(vreq));
        Map<String,String> errors = basicValidation.validateUris( urisFromForm );
        //Validate literals and add errors to the list of existing errors
        errors.putAll(basicValidation.validateLiterals( literalsFromForm ));
        // UQAM Add empty contition
        if( errors != null  && !errors.isEmpty()) {
            validationErrors.putAll( errors);
        }

        if(editConfig.getValidators() != null ){
            for( N3ValidatorVTwo validator : editConfig.getValidators()){
                if( validator != null ){
                    //throw new Error("need to implemente a validator interface that works with the new MultivalueEditSubmission.");
                    errors = validator.validate(editConfig, this);
                    if ( errors != null )
                        validationErrors.putAll(errors);
                }
            }
        }

        if( log.isDebugEnabled() )
            log.debug( this.toString() );
    }

    protected void processEditElementFields(EditConfigurationVTwo editConfig, Map<String,String[]> queryParameters ){
        for( String fieldName : editConfig.getFields().keySet()){
            FieldVTwo field = editConfig.getFields().get(fieldName);
            if( field != null && field.getEditElement() != null ){
                EditElementVTwo element = field.getEditElement();
                log.debug("Checking EditElement for field " + fieldName + " type: " + element.getClass().getName());

                //check for validation error messages
                Map<String,String> errMsgs =
                    element.getValidationMessages(fieldName, editConfig, queryParameters);
                validationErrors.putAll(errMsgs);

                if( errMsgs == null || errMsgs.isEmpty()){
                    //only check for uris and literals when element has no validation errors
                    Map<String,List<String>> urisFromElement = element.getURIs(fieldName, editConfig, queryParameters);
                    if( urisFromElement != null )
                        urisFromForm.putAll(urisFromElement);
                    Map<String,List<Literal>> literalsFromElement = element.getLiterals(fieldName, editConfig, queryParameters);
                    if( literalsFromElement != null )
                        literalsFromForm.putAll(literalsFromElement);
                }else{
                    log.debug("got validation errors for field " + fieldName + " not processing field for literals or URIs");
                }
            }
        }
    }
    /* maybe this could be static */
    public Literal createLiteral_ORIG(String value, String datatypeUri, String lang) {
        if( datatypeUri != null ){
            if( "http://www.w3.org/2001/XMLSchema:anyURI".equals(datatypeUri) ){
                try {
                    return literalCreationModel.createTypedLiteral( URLEncoder.encode(value, "UTF8"), datatypeUri);
                } catch (UnsupportedEncodingException e) {
                    log.error(e, e);
                }
            }
            return literalCreationModel.createTypedLiteral(value, datatypeUri);
        }else if( lang != null && lang.length() > 0 )
            return literalCreationModel.createLiteral(value, lang);
        else
            return ResourceFactory.createPlainLiteral(value);
    }

	/* maybe this could be static */
	public Literal createLiteral(String value, String datatypeUri, String lang) {
		if( datatypeUri != null && !datatypeUri.isEmpty() ){
			if( XSD.anyURI.getURI().equals(datatypeUri) ){
					return literalCreationModel.createTypedLiteral( value, datatypeUri);
			} else if ( RDF.dtLangString.getURI().equals(datatypeUri) ){
				if( StringUtils.isNotEmpty(lang) )	{
				    return ResourceFactory.createLangLiteral(value, lang);
				}
			}
			return literalCreationModel.createTypedLiteral(value, datatypeUri);
		} else if( lang != null && lang.length() > 0 )
			return ResourceFactory.createLangLiteral(value, lang);
		return ResourceFactory.createPlainLiteral(value);
	}

    public Map<String,String> getValidationErrors(){
        return validationErrors;
    }

    public Map<String, List<Literal>> getLiteralsFromForm() {
        return literalsFromForm;
    }

    public Map<String, List<String>> getUrisFromForm() {
        return urisFromForm;
    }
    /**
     * need to generate something like
     *  {@code "09:10:11"^^<http://www.w3.org/2001/XMLSchema#time>}
     */
    public Literal getTime(Map<String,String[]> queryParameters,String fieldName) {
        List<String> hour = Arrays.asList(queryParameters.get("hour" + fieldName));
        List<String> minute = Arrays.asList(queryParameters.get("minute" + fieldName));

        if ( hour == null || hour.size() == 0 ||
             minute == null || minute.size() == 0 ) {
            log.info("Could not find query parameter values for time field " + fieldName);
            validationErrors.put(fieldName, "time must be supplied");
            return null;
        }

        int hourInt = -1;
        int minuteInt = -1;

        String hourParamStr = hour.get(0);
        String minuteParamStr = minute.get(0);

        // if all fields are blank, just return a null value
        if (hourParamStr.length() == 0 && minuteParamStr.length() == 0) {
            return null;
        }

         String errors = "";
         try{
             hourInt = Integer.parseInt(hour.get(0));
             if (hourInt < 0 || hourInt > 23) {
                 throw new NumberFormatException();
             }
         } catch( NumberFormatException nfe ) {
             errors += "Please enter a valid hour.  ";
         }
         try{
             minuteInt = Integer.parseInt(minute.get(0));
             if (minuteInt < 0 || minuteInt > 59) {
                 throw new NumberFormatException();
             }
         } catch( NumberFormatException nfe ) {
             errors += "Please enter a valid minute.  ";
         }
         if( errors.length() > 0 ){
             validationErrors.put( fieldName, errors);
             return null;
         }


         String hourStr = (hourInt < 10) ? "0" + Integer.toString(hourInt) :  Integer.toString(hourInt);
         String minuteStr = (minuteInt < 10) ? "0" + Integer.toString(minuteInt) :  Integer.toString(minuteInt);
         String secondStr = "00";

         return new EditLiteral(hourStr + ":" + minuteStr + ":" + secondStr, TIME_URI, null);

    }
    public void setLiteralsFromForm(Map<String, List<Literal>> literalsFromForm) {
        this.literalsFromForm = literalsFromForm;
    }

    public void setUrisFromForm(Map<String, List<String>> urisFromForm) {
        this.urisFromForm = urisFromForm;
    }

    public String toString(){
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    private Log log = LogFactory.getLog(MultiValueEditSubmission.class);

    public String getEntityToReturnTo() {
        return entityToReturnTo;
    }

    public void setEntityToReturnTo(String string) {
        entityToReturnTo = string;
    }

    //Added specifically to help with "dynamic" forms such as addition of concept
    public void addLiteralToForm(EditConfigurationVTwo editConfig, FieldVTwo field, String var, String[] valuesArray) {
    	List<String> valueList = (valuesArray != null) ? Arrays.asList(valuesArray) : null;
        if( valueList != null && valueList.size() > 0 ) {
        	List<Literal> literalsArray = new ArrayList<Literal>();
        	//now support multiple values
        	for(String value:valueList) {
        		value = N3EditUtils.stripInvalidXMLChars(value);
                //Add to array of literals corresponding to this variable
        		/* UQAM OLD
                if (!StringUtils.isEmpty(value)) {
                    literalsArray.add(createLiteral(
                                                value,
                                                field.getRangeDatatypeUri(),
                                                field.getRangeLang()));
                }
                */
        		/*
        		 * UQAM Replaced by this to take the linguistic context into consideration.
        		 */
				if (!StringUtils.isEmpty(value)) {
					String rangeLang = field.getRangeLang();  //UQAM  Default value
					try {
						if (_vreq != null ) {
							// only if the request comes from the rdfsLabelGenerator the language should be used
							Boolean getLabelLanguage = false;
							if (!StringUtils.isBlank(editConfig.formUrl) && editConfig.formUrl.contains("RDFSLabelGenerator")) {
								getLabelLanguage = true;
							}
							// if the language is set in the given Literal, this language-tag should be used and remain the same
							// for example when you edit an label with an langauge-tag (no matter which language is selected globally)
							if (getLabelLanguage && isLangSetInFirstLiteral(editConfig) )
							{
								rangeLang = editConfig.getLiteralsInScope().get(LABEL).get(0).getLanguage();
							} else { // if the literal has no langauge-tag, use the language which is globally selected
								rangeLang = _vreq.getLocale().getLanguage();
								if (!_vreq.getLocale().getCountry().isEmpty()) {
									rangeLang += "-" + _vreq.getLocale().getCountry();
								}
							}
						}

					} catch (Exception e) {
					    log.error(e,e);
					}
					literalsArray.add(createLiteral(
							value,
							field.getRangeDatatypeUri(),
							rangeLang));
				}
        	}
        	literalsFromForm.put(var, literalsArray);

        }else{
            log.debug("could not find value for parameter " + var  );
        }
    }

    private boolean isLangSetInFirstLiteral(EditConfigurationVTwo editConfig) {
        Map<String, List<Literal>> literalsInScope = editConfig.getLiteralsInScope();
        if (!literalsInScope.containsKey(LABEL)) {
            return false;
        }
        List<Literal> labelLiterals = literalsInScope.get(LABEL);
        if (labelLiterals == null) {
            return false;
        }
        if (labelLiterals.size() == 0) {
            return false;
        }
        Literal literal = labelLiterals.get(0);
        if (literal == null){
            return false;
        }
        
        return StringUtils.isNotEmpty(literal.getLanguage());
    }
    //Add literal to form
    //Add uri to form
    public void addUriToForm(EditConfigurationVTwo editConfig, String var, String[] valuesArray) {
         List<String> values = (valuesArray != null) ? Arrays.asList(valuesArray) : null;
         if( values != null && values.size() > 0){
	            //Iterate through the values and check to see if they should be added or removed from form
	            urisFromForm.put(var, values);
	            for(String uri : values) {
		            if( uri != null && uri.length() == 0 && editConfig.getNewResources().containsKey(var) ){
		                log.debug("A new resource URI will be made for var " + var + " since it was blank on the form.");
		                urisFromForm.remove(var);
		            }
	            }
         }  else {
             log.debug("No value found for query parameter " + var);
         }
    }

    //Check if a certain key has a value associated that is not null

    public boolean hasLiteralValue(String key) {
    	return (this.literalsFromForm.containsKey(key) && this.literalsFromForm.get(key) != null);
    }

    public boolean hasUriValue(String key) {
    	return (this.urisFromForm.containsKey(key) && this.urisFromForm.get(key) != null);
    }
}
