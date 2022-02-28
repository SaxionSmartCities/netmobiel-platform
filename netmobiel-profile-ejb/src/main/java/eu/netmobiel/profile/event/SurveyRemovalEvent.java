package eu.netmobiel.profile.event;

import java.io.Serializable;

import eu.netmobiel.profile.model.SurveyInteraction;

/**
 * This event is issued when a survey is removed (and subsequently a reward or payment is withdrawn/reversed). 
 * The provided survey interaction must be complete, 
 * including the profile and the survey objects.  

 * TGhe event is meant to ease the testing of multiple scenarios.
 * 
 * @author Jaap Reitsma
 *
 */
public class SurveyRemovalEvent extends SurveyEvent implements Serializable {
	private static final long serialVersionUID = -207888621286042374L;
	private boolean paymentOnly = false;
	
	public SurveyRemovalEvent(SurveyInteraction surveyInteraction, boolean onlyThePayment) {
		super(surveyInteraction);
    	this.paymentOnly = onlyThePayment;
    }

	public boolean isPaymentOnly() {
		return paymentOnly;
	}

	@Override
	public String toString() {
		return String.format("SurveyRemovalEvent [%s, paymentOnly %s]", getSurveyInteraction().getUrn(), paymentOnly);
	}

}
