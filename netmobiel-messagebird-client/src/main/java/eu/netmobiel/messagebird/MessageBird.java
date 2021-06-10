package eu.netmobiel.messagebird;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.messagebird.MessageBirdClient;
import com.messagebird.MessageBirdService;
import com.messagebird.MessageBirdServiceImpl;
import com.messagebird.exceptions.GeneralException;
import com.messagebird.exceptions.MessageBirdException;
import com.messagebird.exceptions.NotFoundException;
import com.messagebird.exceptions.UnauthorizedException;
import com.messagebird.objects.ErrorReport;
import com.messagebird.objects.IfMachineType;
import com.messagebird.objects.Message;
import com.messagebird.objects.MessageList;
import com.messagebird.objects.MessageResponse;
import com.messagebird.objects.VoiceMessage;
import com.messagebird.objects.VoiceMessageResponse;
import com.messagebird.objects.VoiceType;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;

/**
 * MessageBird interface class.
 * 
 * @author Jaap Reitsma
 * 
 */
@Logging
@ApplicationScoped
public class MessageBird {

	private static final String defaultOriginator = "NetMobiel";
	
	private MessageBirdClient messageBirdClient;

	@Inject
    private Logger logger;

	@Resource(lookup = "java:global/messageBird/accessKey")
	private String accessKey;

    private PhoneNumberUtil phoneUtil;

	/**
	 * AccessKey for MessageBird. Setter for testing purposes only.
	 * @param accessKey
	 */
	void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	@PostConstruct
	public void initialize() {
		MessageBirdService messageBirdService = new MessageBirdServiceImpl(accessKey);
		messageBirdClient = new MessageBirdClient(messageBirdService);
	    phoneUtil = PhoneNumberUtil.getInstance();
	}

	protected String handleMessageBirdException(MessageBirdException ex) {
		StringBuilder sb = new StringBuilder();
		if (ex.getErrors() != null) {
			for (ErrorReport report: ex.getErrors()) {
				if (sb.length() != 0) {
					sb.append("\n\t");
				}
				sb.append(report.toString());
			}
		}
		return ExceptionUtil.unwindException(ex) + " - " + sb.toString();
	}

    public boolean isMobileNumber(String inputPhoneNumber,  String defaultCountry) {
	    boolean isMobile = false;
		try {
			PhoneNumber number = phoneUtil.parse(inputPhoneNumber, defaultCountry);
		    if (!phoneUtil.isValidNumber(number)) {
		    	throw new IllegalArgumentException("Not a valid phone number: " + inputPhoneNumber);
		    }
		    isMobile = phoneUtil.getNumberType(number) == PhoneNumberType.MOBILE;
		} catch (NumberParseException e) {
	    	throw new IllegalArgumentException("Not a valid phone number: " + inputPhoneNumber);
		}
	    return isMobile;
    }

	public String formatPhoneNumberNational(String inputPhoneNumber, String defaultCountry) {
	    String output = null;
		try {
			PhoneNumber number = phoneUtil.parse(inputPhoneNumber, defaultCountry);
		    if (!phoneUtil.isValidNumber(number)) {
		    	throw new IllegalArgumentException("Not a valid phone number: " + inputPhoneNumber);
		    }
		    // Convert the input to the national format
		    output = phoneUtil.format(number, PhoneNumberFormat.NATIONAL);
		} catch (NumberParseException e) {
	    	throw new IllegalArgumentException("Not a valid phone number: " + inputPhoneNumber);
		}
		return output;
	}

	public String formatPhoneNumberTechnical(String inputPhoneNumber, String defaultCountry) {
	    String output = null;
		try {
			PhoneNumber number = phoneUtil.parse(inputPhoneNumber, defaultCountry);
		    if (!phoneUtil.isValidNumber(number)) {
		    	throw new IllegalArgumentException("Not a valid phone number: " + inputPhoneNumber);
		    }
		    // Convert the input to an international number and remove the '+'
		    output = phoneUtil.format(number, PhoneNumberFormat.E164).substring(1);
		} catch (NumberParseException e) {
	    	throw new IllegalArgumentException("Not a valid phone number: " + inputPhoneNumber);
		}
		return output;
	}

	public String sendSMS(String originator, String text, String[] recipients) throws BadRequestException {
		String messageId = null;
		try {
			if (originator == null) {
				originator = defaultOriginator;
			}
			if (originator.length() > 11) {
				throw new BadRequestException("Originator cannot exceed 11 characters");
			}
			if (!StringUtils.isAlphanumeric(originator)) {
				throw new BadRequestException("Originator must be alphanumeric");
			}
			Message msg = new Message(originator, text, String.join(",", recipients));
			MessageResponse mr = messageBirdClient.sendMessage(msg);
			messageId = mr.getId();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("SMS from %s to %s: %s - %s", originator, String.join(",", recipients), text, messageId));
			}
		} catch (UnauthorizedException e) {
			throw new SecurityException(handleMessageBirdException(e));
		} catch (GeneralException e) {
			throw new SystemException(handleMessageBirdException(e));
		}
		return messageId;
	}

	/**
	 * 
	 * @param originator Optional. The sender of the message. A telephone number (including country code).
	 * @param text The text to speak.
	 * @param recipients The array of recipient telephone numbers
	 * @param language The language in which the message needs to be read to the recipient. Default: en-gb.
	 * @return The id of the message just send.
	 */
	public String sendVoiceMessage(String originator, String text, String[] recipients, String language) {
		String messageId = null;
		try {
				VoiceMessage msg = new VoiceMessage(text, String.join(",", recipients));
				msg.setOriginator(originator);
				msg.setLanguage(language);
				msg.setVoice(VoiceType.female);
				msg.setIfMachine(IfMachineType.delay);
				VoiceMessageResponse vmr = messageBirdClient.sendVoiceMessage(msg);
				messageId = vmr.getId();
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Voice message from %s to %s: %s - %s", originator, String.join(",", recipients), text, messageId));
				}
		} catch (UnauthorizedException e) {
			throw new SecurityException(handleMessageBirdException(e));
		} catch (GeneralException e) {
			throw new SystemException(handleMessageBirdException(e));
		}
		return messageId;
	}

	public MessageList listMessages(Integer offset, Integer limit) {
		MessageList ml = null;
		try {
			if (offset == null) {
				offset = 0;
			}
			if (limit == null) {
				limit = 10;
			}
			ml = messageBirdClient.listMessages(offset, limit);
		} catch (UnauthorizedException e) {
			throw new SecurityException(handleMessageBirdException(e));
		} catch (GeneralException e) {
			throw new SystemException(handleMessageBirdException(e));
		}
		return ml;
	}

	public MessageResponse getMessage(String messageId) throws eu.netmobiel.commons.exception.NotFoundException {
		MessageResponse msg = null;
		try {
			msg = messageBirdClient.viewMessage(messageId);
		} catch (UnauthorizedException e) {
			throw new SecurityException(handleMessageBirdException(e));
		} catch (GeneralException e) {
			throw new SystemException(handleMessageBirdException(e));
		} catch (NotFoundException e) {
			throw new eu.netmobiel.commons.exception.NotFoundException(e.getMessage());
		}
		return msg;
	}
}

