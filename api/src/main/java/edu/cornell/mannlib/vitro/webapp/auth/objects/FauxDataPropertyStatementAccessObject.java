/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.auth.objects;

import org.apache.jena.ontology.OntModel;

import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessObjectType;
import edu.cornell.mannlib.vitro.webapp.beans.Property;
import edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual.FauxDataPropertyWrapper;

public class FauxDataPropertyStatementAccessObject extends AccessObject {

    public FauxDataPropertyStatementAccessObject(OntModel ontModel, String subjectUri, FauxDataPropertyWrapper predicate, String dataValue) {
        setStatementOntModel(ontModel);
        setStatementSubject(subjectUri);
        setStatementPredicate(predicate);
        setStatementObject(dataValue);
    }

    @Override
    public AccessObjectType getType() {
        return AccessObjectType.FAUX_DATA_PROPERTY_STATEMENT;
    }
    
    @Override
    public String getStatementPredicateUri() {
        if (statement == null || statement.getPredicate() == null) {
            return null;
        }
        Property predicate = getPredicate();
        if (predicate instanceof FauxDataPropertyWrapper) {
            return ((FauxDataPropertyWrapper) predicate).getConfigUri();
        }
        return predicate.getURI();
    }
    
    @Override
    public String toString() {
        Property predicate = getPredicate();
        return getClass().getSimpleName() + ": <" + getStatementSubject() + "> <" + ((FauxDataPropertyWrapper) predicate).getConfigUri() + "> <"+ getStatementObject() + ">";
    }
}