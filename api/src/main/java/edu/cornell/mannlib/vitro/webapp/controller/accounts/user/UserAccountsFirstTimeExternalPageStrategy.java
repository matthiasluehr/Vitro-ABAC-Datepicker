/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.controller.accounts.user;

import static javax.mail.Message.RecipientType.TO;

import java.util.HashMap;
import java.util.Map;

import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.accounts.UserAccountsPage;
import edu.cornell.mannlib.vitro.webapp.email.FreemarkerEmailFactory;
import edu.cornell.mannlib.vitro.webapp.email.FreemarkerEmailMessage;

/**
 * Handle the variations in the UserAccountsFirstTimeExternal page. If email is
 * available, inform the template, and send a notification to the user.
 *
 * If not, then don't.
 */
public abstract class UserAccountsFirstTimeExternalPageStrategy extends
		UserAccountsPage {

	public static UserAccountsFirstTimeExternalPageStrategy getInstance(
			VitroRequest vreq, UserAccountsFirstTimeExternalPage page,
			boolean emailEnabled) {
		if (emailEnabled) {
			return new EmailStrategy(vreq, page);
		} else {
			return new NoEmailStrategy(vreq, page);
		}
	}

	@SuppressWarnings("unused")
	private UserAccountsFirstTimeExternalPage page;

	public UserAccountsFirstTimeExternalPageStrategy(VitroRequest vreq,
			UserAccountsFirstTimeExternalPage page) {
		super(vreq);
		this.page = page;
	}

	public abstract void addMoreBodyValues(Map<String, Object> body);

	public abstract void notifyUser(UserAccount ua);

	// ----------------------------------------------------------------------
	// Strategy to use if email is enabled.
	// ----------------------------------------------------------------------

	public static class EmailStrategy extends
			UserAccountsFirstTimeExternalPageStrategy {

		public EmailStrategy(VitroRequest vreq,
				UserAccountsFirstTimeExternalPage page) {
			super(vreq, page);
		}

		@Override
		public void addMoreBodyValues(Map<String, Object> body) {
			body.put("emailIsEnabled", Boolean.TRUE);
		}

		@Override
		public void notifyUser(UserAccount ua) {
			Map<String, Object> body = new HashMap<String, Object>();
			body.put("userAccount", ua);
			body.put("siteName", getSiteName());

			FreemarkerEmailMessage email = FreemarkerEmailFactory
					.createNewMessage(vreq);
			email.addRecipient(TO, ua.getEmailAddress());
			final String subject = i18n.text("account_created_subject", getSiteName());
			email.setSubject(subject);
			body.put("subject", subject);
			body.put("textMessage", i18n.text("first_time_external_email_plain_text"));
			body.put("htmlMessage", i18n.text("first_time_external_email_html_text"));
			email.setBodyMap(body);
			email.processTemplate();
			email.send();
		}

	}

	// ----------------------------------------------------------------------
	// Strategy to use if email is disabled.
	// ----------------------------------------------------------------------

	public static class NoEmailStrategy extends
			UserAccountsFirstTimeExternalPageStrategy {

		public NoEmailStrategy(VitroRequest vreq,
				UserAccountsFirstTimeExternalPage page) {
			super(vreq, page);
		}

		@Override
		public void addMoreBodyValues(Map<String, Object> body) {
			// Nothing to add.
		}

		@Override
		public void notifyUser(UserAccount ua) {
			// No way to notify.
		}

	}

}
