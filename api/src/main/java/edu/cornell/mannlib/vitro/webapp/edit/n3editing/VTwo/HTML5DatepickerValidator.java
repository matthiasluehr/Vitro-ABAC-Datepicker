/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo;

// import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.rdf.model.Literal;

public class HTML5DatepickerValidator implements N3ValidatorVTwo {
    private static Log log = LogFactory.getLog(HTML5DatepickerValidator.class);

    private String dateFieldName;
    private String templateName;
    private String dateValueName;
    private String datePrecisionName;

    public HTML5DatepickerValidator(String dateFieldName) {
        this.dateFieldName = dateFieldName;
        this.dateValueName = this.dateFieldName + "-value";
    }

    public HTML5DatepickerValidator(String dateFieldName, String template) {
        this.templateName = template;
        this.dateFieldName = dateFieldName;
    }

    public Map<String, String> validate(EditConfigurationVTwo editConfig,
            MultiValueEditSubmission editSub) {

        log.error("Invoked.");

        Map<String, List<Literal>> literalsFromForm = editSub.getLiteralsFromForm();

        for (String key : literalsFromForm.keySet()) {
            log.error("Key: " + key);
        }


        List<Literal> formDate = literalsFromForm.get(dateValueName);

        Literal inputLit = formDate.get(0);
        String inputDate = (String)inputLit.getValue();

        log.error("Found input value: " + inputDate);

        Map<String, String> errors = new HashMap<String, String>();

        errors.put(dateFieldName, "Date Validation failed. Unsupported format.");

        Pattern pattern1 = Pattern.compile("^[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}$");
        Matcher matcher1 = pattern1.matcher(inputDate);

        Pattern pattern2 = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");
        Matcher matcher2 = pattern2.matcher(inputDate);

        if (matcher1.matches()) {
            return null;
        }

        if (matcher2.matches()) {
            return null;
        }

        return errors;

    }

}
