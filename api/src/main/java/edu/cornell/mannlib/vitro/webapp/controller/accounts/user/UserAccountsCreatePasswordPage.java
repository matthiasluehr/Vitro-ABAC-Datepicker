/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.controller.accounts.user;

import static javax.mail.Message.RecipientType.TO;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.vitro.webapp.beans.UserAccount.Status;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.authenticate.Authenticator;
import edu.cornell.mannlib.vitro.webapp.email.FreemarkerEmailFactory;
import edu.cornell.mannlib.vitro.webapp.email.FreemarkerEmailMessage;

/**
 * When the user clicks on the link in their notification email, handle their
 * request to create a password.
 */
public class UserAccountsCreatePasswordPage extends
		UserAccountsPasswordBasePage {
	private static final Log log = LogFactory
			.getLog(UserAccountsCreatePasswordPage.class);

	private static final String TEMPLATE_NAME = "userAccounts-createPassword.ftl";
	public UserAccountsCreatePasswordPage(VitroRequest vreq) {
		super(vreq);
	}

	public void createPassword() {
		userAccount.setArgon2Password(Authenticator.applyArgon2iEncoding(newPassword));
		userAccount.setMd5Password("");
		userAccount.setPasswordLinkExpires(0L);
		userAccount.setEmailKey("");
		userAccount.setPasswordChangeRequired(false);
		userAccount.setStatus(Status.ACTIVE);
		userAccountsDao.updateUserAccount(userAccount);
		log.debug("Set password on '" + userAccount.getEmailAddress()
				+ "' to '" + newPassword + "'");
		notifyUser();
	}

	@Override
	protected String alreadyLoggedInMessage(String currentUserEmail) {
		return i18n.text("cant_activate_while_logged_in", userEmail, currentUserEmail);
	}

	@Override
	protected String passwordChangeNotPendingMessage() {
		return i18n.text("account_already_activated", userEmail);
	}
	
	@Override
	protected String passwordChangeInavlidKeyMessage() {
		return i18n.text("password_change_invalid_key", userEmail);
	}

	@Override
	protected String templateName() {
		return TEMPLATE_NAME;
	}

	private void notifyUser() {
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("userAccount", userAccount);
		body.put("siteName", getSiteName());

		FreemarkerEmailMessage email = FreemarkerEmailFactory
				.createNewMessage(vreq);
		email.addRecipient(TO, userAccount.getEmailAddress());
		final String subject = i18n.text("password_created_subject", getSiteName());
		email.setSubject(subject);
		body.put("subject", subject);
		body.put("textMessage", i18n.text("password_created_email_plain_text"));
		body.put("htmlMessage", i18n.text("password_created_email_html_text"));
		email.setBodyMap(body);
		email.processTemplate();
		email.send();
	}
}
