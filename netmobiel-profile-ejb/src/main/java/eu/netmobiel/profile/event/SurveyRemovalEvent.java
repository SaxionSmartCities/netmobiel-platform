package eu.netmobiel.profile.event;

import java.io.Serializable;

import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.SurveyInteraction;

/**
 * This event is issued when a survey is removed (and subsequently a reward or payment is withdrawn/reversed). 
 * It is for testing purposes to test multiple scenarios.
 * 
 * @author Jaap Reitsma
 *
 */
public class SurveyRemovalEvent implements Serializable {
	private static final long serialVersionUID = -758586385194229611L;

	private Profile profile;
	private SurveyInteraction surveyInteraction;
	private boolean paymentOnly = false;
	
	public SurveyRemovalEvent(Profile user, SurveyInteraction surveyInteraction, boolean onlyThePayment) {
		this.profile = user;
		this.surveyInteraction = surveyInteraction;
    	this.paymentOnly = onlyThePayment;
    }

	public Profile getProfile() {
		return profile;
	}

	public SurveyInteraction getSurveyInteraction() {
		return surveyInteraction;
	}

	public boolean isPaymentOnly() {
		return paymentOnly;
	}

}
