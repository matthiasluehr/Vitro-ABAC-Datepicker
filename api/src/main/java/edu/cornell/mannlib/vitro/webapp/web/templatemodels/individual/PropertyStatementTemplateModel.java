/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual;

import edu.cornell.mannlib.vitro.webapp.beans.FauxProperty;
import edu.cornell.mannlib.vitro.webapp.beans.Property;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.BaseTemplateModel;

public abstract class PropertyStatementTemplateModel extends BaseTemplateModel {
	protected static final String EDIT_PATH = "editRequestDispatch";

    protected final VitroRequest vreq;
    protected final String subjectUri;
    protected final Property property;
    protected FauxProperty fauxProperty;


    PropertyStatementTemplateModel(String subjectUri, Property property, VitroRequest vreq) {
        this.vreq = vreq;
        this.subjectUri = subjectUri;
        this.property = property;
        if (property instanceof FauxPropertyWrapper) {
            fauxProperty = ((FauxPropertyWrapper) property).getFauxProperty();
        }
    }

    /* Template properties */

    public abstract String getEditUrl();
    public abstract String getDeleteUrl();
    public boolean isEditable() {
        return ! getEditUrl().isEmpty();
    }
    
    protected boolean isFaux() {
        return fauxProperty != null;
    }

}
