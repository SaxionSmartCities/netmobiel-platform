package eu.netmobiel.profile.model;

/**
 * Defines the scope of the reversion of rewarding a survey.
 * @author Jaap Reitsma
 *
 */
public enum SurveyScope {
	/**
	 * The survey (interaction) and everything that is a consequence of that record.
	 */
	SURVEY,
	/**
	 * The answer (not at the survey provider, but the registration in Netmobiel. 
	 */
	ANSWER,
	/**
	 * The issuing of the reward.
	 */
	REWARD,
	/**
	 * The payment of the reward.
	 */
	PAYMENT;
}
