package edu.cornell.mannlib.vitro.webapp.migration.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessObjectType;
import edu.cornell.mannlib.vitro.webapp.auth.attributes.AccessOperation;
import edu.cornell.mannlib.vitro.webapp.auth.permissions.SimplePermission;
import edu.cornell.mannlib.vitro.webapp.auth.policy.PolicyLoader;
import edu.cornell.mannlib.vitro.webapp.auth.policy.PolicyTemplateController;
import edu.cornell.mannlib.vitro.webapp.auth.policy.PolicyTest;
import edu.cornell.mannlib.vitro.webapp.dao.VitroVocabulary;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelNames;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.junit.Before;
import org.junit.Test;

public class SimplePermissionMigratorTest extends AuthMigratorTest {

    private OntModel userAccountsModel;
    private SimplePermissionMigrator spm;
    private static final String TEMPLATE_PATH = "template_simple_permissions";
    public static final String ADMIN_SIMPLE_PERMISSIONS_PATH = "simple_permissions_admin";
    public static final String CURATOR_SIMPLE_PERMISSIONS_PATH = "simple_permissions_curator";
    public static final String EDITOR_SIMPLE_PERMISSIONS_PATH = "simple_permissions_editor";
    public static final String SELF_EDITOR_SIMPLE_PERMISSIONS_PATH = "simple_permissions_self_editor";
    public static final String PUBLIC_SIMPLE_PERMISSIONS_PATH = "simple_permissions_public";

    @Before
    public void initMigration() {
        userAccountsModel = ModelFactory.createOntologyModel();
        configurationDataSet.addNamedModel(ModelNames.USER_ACCOUNTS, userAccountsModel);
        spm = new SimplePermissionMigrator(userAccountsModel);
        load(PolicyTest.USER_ACCOUNTS_HOME_FIRSTTIME + TEMPLATE_PATH + EXT);
    }

    private void addUserAccountsStatement(String subjUri, String pUri, String objUri) {
        Statement statement =
                new StatementImpl(new ResourceImpl(subjUri), new PropertyImpl(pUri), new ResourceImpl(objUri));
        userAccountsModel.add(statement);
    }

    @Test
    public void getPermissionSetsTest() {
        addUserAccountsStatement(PolicyTest.CUSTOM, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.ADMIN, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.CURATOR, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.EDITOR, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.SELF_EDITOR, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        Set<String> sets = spm.getPermissionSets();
        assertEquals(5, sets.size());
        assertTrue(sets.contains(PolicyTest.CUSTOM));
        assertTrue(sets.contains(PolicyTest.ADMIN));
        assertTrue(sets.contains(PolicyTest.CURATOR));
        assertTrue(sets.contains(PolicyTest.EDITOR));
        assertTrue(sets.contains(PolicyTest.SELF_EDITOR));
    }

    @Test
    public void getUserAccountPermissionsTest() {
        addUserAccountsStatement(PolicyTest.CUSTOM, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.ADMIN, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.CUSTOM, VitroVocabulary.PERMISSIONSET_HAS_PERMISSION,
                SimplePermission.ACCESS_SPECIAL_DATA_MODELS.getUri());
        addUserAccountsStatement(PolicyTest.CUSTOM, VitroVocabulary.PERMISSIONSET_HAS_PERMISSION,
                SimplePermission.DO_BACK_END_EDITING.getUri());
        Set<String> permissions = spm.getUserAccountsPermissions(PolicyTest.CUSTOM);
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains(SimplePermission.ACCESS_SPECIAL_DATA_MODELS.getUri()));
        assertTrue(permissions.contains(SimplePermission.DO_BACK_END_EDITING.getUri()));
        permissions = spm.getUserAccountsPermissions(PolicyTest.ADMIN);
        assertEquals(0, permissions.size());
    }

    @Test
    public void migrateConfigurationTest() {
        addUserAccountsStatement(PolicyTest.CUSTOM, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.CUSTOM, VitroVocabulary.PERMISSIONSET_HAS_PERMISSION,
                SimplePermission.ACCESS_SPECIAL_DATA_MODELS.getUri());
        addUserAccountsStatement(PolicyTest.PUBLIC, VitroVocabulary.RDF_TYPE, VitroVocabulary.PERMISSIONSET);
        addUserAccountsStatement(PolicyTest.PUBLIC, VitroVocabulary.PERMISSIONSET_HAS_PERMISSION,
                SimplePermission.ACCESS_SPECIAL_DATA_MODELS.getUri());
        load(USER_ACCOUNTS_HOME_FIRSTTIME + PUBLIC_SIMPLE_PERMISSIONS_PATH + EXT);
        PolicyTemplateController.createRoleDataSets(CUSTOM);
        spm.migrateConfiguration();
        PolicyLoader policyLoader = PolicyLoader.getInstance();
        Set<String> entities = policyLoader.getDataSetValues(AccessOperation.EXECUTE, AccessObjectType.NAMED_OBJECT,
                PolicyTest.CUSTOM);
        assertEquals(1, entities.size());
        assertTrue(entities.contains(SimplePermission.ACCESS_SPECIAL_DATA_MODELS.getUri()));
        entities = policyLoader.getDataSetValues(AccessOperation.EXECUTE, AccessObjectType.NAMED_OBJECT,
                PolicyTest.PUBLIC);
        assertEquals(1, entities.size());
        assertTrue(entities.contains(SimplePermission.ACCESS_SPECIAL_DATA_MODELS.getUri()));
    }

}
