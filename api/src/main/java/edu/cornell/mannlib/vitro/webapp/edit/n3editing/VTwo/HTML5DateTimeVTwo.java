/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cornell.mannlib.vitro.webapp.dao.VitroVocabulary;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import freemarker.template.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * This is intended to work in conjunction with a template to create the HTML for a
 * datetime with precision and to convert the submitted parameters into
 * varname -&gt; Literal and varname -&gt; URI maps.
 *
 * The variables that get passed to the template are defined in:
 * DateTimeWithPrecision.getMapForTemplate()
 *
 * Two variables will be defined for the N3 edit graphs (These are NOT variables passed to FM templates):
 * $fieldname.precision - URI of datetime precision
 * $fieldname.value - DateTime literal
 *
 */
public class HTML5DateTimeVTwo extends BaseEditElementVTwo {

    /**
     * This is the minimum datetime precision that this element
     * will accept.  If the parameters submitted do not meet this
     * requirement, then a validation error will be created.
     */
    VitroVocabulary.Precision minimumPrecision;

    /**
     * This is the maximum precision that the form should
     * allow the user to enter.  This value is not used by
     * DateTimeWithPrecision for validation, it is only passed
     * to the template.  This should be removed when it can be
     * specified in a ftl file.
     *
     * This could be thought of as the maximum precision to display.
     */

    VitroVocabulary.Precision displayRequiredLevel;
    VitroVocabulary.Precision DEFAULT_MIN_PRECISION = VitroVocabulary.Precision.DAY;
    VitroVocabulary.Precision DEFAULT_DISPLAY_LEVEL = VitroVocabulary.Precision.DAY;
    VitroVocabulary.Precision[] precisions = VitroVocabulary.Precision.values();

    protected static final String BLANK_SENTINEL = ">SUBMITTED VALUE WAS BLANK<";

    public HTML5DateTimeVTwo(FieldVTwo field) {
        super(field);
        minimumPrecision = DEFAULT_MIN_PRECISION;
        displayRequiredLevel = DEFAULT_DISPLAY_LEVEL;
    }

    public HTML5DateTimeVTwo(FieldVTwo field, String minimumPrecisionURI, String displayRequiredLevelUri) {
        this(field);

        this.minimumPrecision = toPrecision( minimumPrecisionURI);
        if ( this.minimumPrecision == null ) {
            throw new IllegalArgumentException(minimumPrecisionURI
                    + " is not a valid precision for minimumPrecision, see VitroVocabulary.Precision");
        }

        this.displayRequiredLevel = toPrecision( displayRequiredLevelUri );
        if ( this.displayRequiredLevel == null ) {
            throw new IllegalArgumentException(displayRequiredLevelUri
                    + " is not a valid precision for displayRequiredLevel, see VitroVocabulary.Precision");
        }

    }

    private String getFieldName() {
        return field.getName();
    }

    private static final Log log = LogFactory.getLog(DateTimeWithPrecisionVTwo.class);
    protected String TEMPLATE_NAME = "HTML5DateTime.ftl";

    @Override
    public String draw(String fieldName, EditConfigurationVTwo editConfig,
            MultiValueEditSubmission editSub, Configuration fmConfig) {
        Map map = getMapForTemplate( editConfig, editSub);
        return merge( fmConfig, TEMPLATE_NAME, map);
    }

    /**
     * This produces a map for use in the template. Will be using this b/c
     */
    public Map getMapForTemplate(EditConfigurationVTwo editConfig, MultiValueEditSubmission editSub) {
        Map<String,Object> map = new HashMap<String,Object>();

        map.put("fieldName", getFieldName());

        DateTime dt = getTimeValue(editConfig,editSub);
        if (dt != null) {
            DateTimeFormatter f = DateTimeFormat.forPattern("yyyy-MM-dd");
            String isodate = f.print(dt);
            map.put("isodate", isodate);
        }

        return map;
    }

    /** Adds precisionURIs for use by the templates */
    private void addPrecisionConstants(Map<String,Object> map) {
        Map<String,Object> constants = new HashMap<String,Object>();
        for ( VitroVocabulary.Precision pc: VitroVocabulary.Precision.values()) {
            constants.put(pc.name().toLowerCase(),pc.uri());
        }
        map.put("precisionConstants", constants);
    }

    /**
     * Gets the currently set precision.  May return null.
     */
    private String getPrecision(EditConfigurationVTwo editConfig, MultiValueEditSubmission editSub) {
        if ( editSub != null ) {
            List<String> submittedPrecisionURI = editSub.getUrisFromForm().get( getPrecisionVariableName() );
            if ( submittedPrecisionURI != null && submittedPrecisionURI.size() > 0 &&
                submittedPrecisionURI.get(0) != null) {
                return submittedPrecisionURI.get(0);
            }
        }

        List<String> existingPrecisionURI = editConfig.getUrisInScope().get( getPrecisionVariableName() );
        if ( existingPrecisionURI != null && existingPrecisionURI.size() > 0 && existingPrecisionURI.get(0) != null) {
            return existingPrecisionURI.get(0);
        } else {
            return null;
        }
    }

    private DateTime getTimeValue(EditConfigurationVTwo editConfig, MultiValueEditSubmission editSub) {
        if ( editSub != null ) {
            List<Literal> submittedValue = editSub.getLiteralsFromForm().get( getValueVariableName() );
            if ( submittedValue != null ) {
                if (submittedValue.size() > 0 && submittedValue.get(0) != null) {
                    return new DateTime( submittedValue.get(0).getLexicalForm() );
                }
            }
        }

        List<Literal> dtValue = editConfig.getLiteralsInScope().get( getValueVariableName() );
        if ( dtValue != null ) {
            if (dtValue.size() > 0 && dtValue.get(0) != null) {
                return new DateTime( dtValue.get(0).getLexicalForm() );
            }
        }

        return null;
    }

    /**
     * This gets the literals for a submitted form from the queryParmeters.
     * It will only be called if getValidationErrors() doesn't return any errors.
     */
    @Override
    public Map<String, List<Literal>> getLiterals(String fieldName,
            EditConfigurationVTwo editConfig, Map<String, String[]> queryParameters) {
        Map<String,List<Literal>> literalMap = new HashMap<String,List<Literal>>();

        Literal datetime = getDateTime( queryParameters);
        List<Literal> literals = new ArrayList<Literal>();
        literals.add(datetime);
        literalMap.put(fieldName + "-value", literals);

        return literalMap;
    }

    protected Literal getDateTime(  Map<String, String[]> queryParameters ) {

       // we just care about DAY precision

        String submittedPrec = VitroVocabulary.Precision.DAY.uri();
        String dateTimeString = "";
        DateTimeFormatter f;


        try {
            dateTimeString = queryParameters.get(getFieldName())[0];
        } catch (NullPointerException iex) {
            return null;
        }

        if (dateTimeString == "") {
            return null;
        }

        // parse transmitted DateString to DateTime value

        Pattern gerpattern = Pattern.compile("^[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}$");
        Matcher germatcher = gerpattern.matcher(dateTimeString);

        if (germatcher.matches()) {
            f = DateTimeFormat.forPattern("dd.MM.yyyy");
        } else {
            f = DateTimeFormat.forPattern("yyyy-MM-dd");
        }

        DateTime value = f.parseDateTime(dateTimeString);

        return ResourceFactory.createTypedLiteral(
                ISODateTimeFormat.dateHourMinuteSecond().print(value), /*does not include timezone*/
                XSDDatatype.XSDdateTime);
    }

    /**
     * This gets the URIs for a submitted form from the queryParmeters.
     * It will only be called if getValidationErrors() doesn't return any errors.
     */
    @Override
    public Map<String, List<String>> getURIs(String fieldName,
            EditConfigurationVTwo editConfig, Map<String, String[]> queryParameters) {
        String precisionUri;
        try {
            precisionUri = getSubmittedPrecision( queryParameters);
        } catch (Exception e) {
            log.error("getURIs() should only be called on input that passed getValidationErrors()");
            return Collections.emptyMap();
        }
        Map<String,List<String>> uriMap = new HashMap<String,List<String>>();
        if ( precisionUri != null ) {
            List<String> uris = new ArrayList<String>();
            uris.add(precisionUri);
            uriMap.put(fieldName + "-precision", uris);
        }
        return uriMap;
    }

    /**
     * Precision is based on the values returned by the form. Throws an exception with
     * the error message if the queryParameters cannot make a valid date/precision because
     * there are values missing.
     */
    protected String getSubmittedPrecision(Map<String, String[]> queryParameters) throws Exception {

        // just DAYs
        return VitroVocabulary.Precision.DAY.uri();

    }


    @Override
    public Map<String, String> getValidationMessages(String fieldName,
            EditConfigurationVTwo editConfig, Map<String, String[]> queryParameters) {
        Map<String,String> errorMsgMap = new HashMap<String,String>();

        //check that any parameters we got are single values
        String[] names = {"year","month","day","hour","minute","second", "precision"};
        for ( String name:names) {
            if ( !hasNoneOrSingle(fieldName + "-" + name, queryParameters)) {
                errorMsgMap.put(fieldName + "-" + name, "must have only one value for " + name);
            }
        }

        String precisionURI = null;
        try {
            precisionURI = getSubmittedPrecision( queryParameters);
        } catch (Exception ex) {
            errorMsgMap.put(fieldName,ex.getMessage());
            return errorMsgMap;
        }

        errorMsgMap.putAll(checkDate( precisionURI,  queryParameters) );

        return errorMsgMap;
    }

    /**
     * This checks for invalid date times.
     */
    final static String NON_INTEGER_YEAR = "must enter a valid year";
    final static String NON_INTEGER_MONTH = "must enter a valid month";
    final static String NON_INTEGER_DAY = "must enter a valid day";
    final static String NON_INTEGER_HOUR = "must enter a valid hour";
    final static String NON_INTEGER_MINUTE = "must enter a valid minute";
    final static String NON_INTEGER_SECOND = "must enter a valid second";

    private Map<String,String> checkDate( String precisionURI, Map<String, String[]> qp) {
        if ( precisionURI == null ) {
            return Collections.emptyMap();
        }

        Map<String,String> errors = new HashMap<String,String>();

        return errors;
    }


    private boolean fieldMatchesPattern( String fieldName, Map<String,String[]> queryParameters, Pattern pattern) {
        String[] varg = queryParameters.get(fieldName);
        if ( varg == null || varg.length != 1 || varg[0] == null) {
            return false;
        }
        String value = varg[0];
        Matcher match = pattern.matcher(value);
        return match.matches();
    }

    private boolean emptyOrBlank(String key,Map<String, String[]> queryParameters) {
        String[] vt = queryParameters.get(key);
        return ( vt == null || vt.length == 0 || vt[0] == null || vt[0].length() == 0 );
    }

    private boolean canParseToNumber(String key,Map<String, String[]> queryParameters) {
        Integer out = null;
        try {
            String[] vt = queryParameters.get(key);
            if ( vt == null || vt.length == 0 || vt[0] == null) {
                return false;
            } else {
                out = Integer.parseInt(vt[0]);
                return true;
            }
        } catch (IndexOutOfBoundsException | NullPointerException | NumberFormatException iex) {
            out =  null;
        }
        return false;
    }



    private Integer parseToInt(String key,Map<String, String[]> queryParameters) {
        Integer out = null;
        try {
            String[] vt = queryParameters.get(key);
            if ( vt == null || vt.length == 0 || vt[0] == null) {
                out = null;
            } else {
                out = Integer.parseInt(vt[0]);
            }
        } catch (IndexOutOfBoundsException | NullPointerException | NumberFormatException iex) {
            out =  null;
        }
        return out;
    }

    public VitroVocabulary.Precision getRequiredMinimumPrecision() {
        return minimumPrecision;
    }

    public void setRequiredMinimumPrecision(VitroVocabulary.Precision requiredMinimumPrecision) {
        this.minimumPrecision = requiredMinimumPrecision;
    }

    /* returns null if it cannot convert */
    public static VitroVocabulary.Precision toPrecision(String precisionUri) {
        for ( VitroVocabulary.Precision precision : VitroVocabulary.Precision.values()) {
            if ( precision.uri().equals(precisionUri)) {
                return precision;
            }
        }
        return null;
    }

    public String getValueVariableName() {
        return getFieldName() + "-value" ;
    }

    public String getPrecisionVariableName() {
        return getFieldName() + "-precision" ;
    }
}
