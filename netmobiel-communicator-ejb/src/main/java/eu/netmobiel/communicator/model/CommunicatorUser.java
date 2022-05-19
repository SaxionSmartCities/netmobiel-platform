package eu.netmobiel.communicator.model;

import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedEntityGraph()
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "cm_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class CommunicatorUser extends User  {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = CommunicatorUrnHelper.createUrnPrefix("user");
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

	/**
	 * The Firebase Cloud Messaging token, a unique token to send push messages to a mobile phone.
	 */
	@Size(max = 512)
	@Column(name = "fcm_token")
	private String fcmToken;

	/**
	 * The timestamp of the last update of the FCM token.
	 * See https://firebase.google.com/docs/cloud-messaging/manage-tokens
	 */
	@Column(name = "fcm_token_timestamp")
	private Instant fcmTokenTimestamp;
	
	/**
	 * The country code according to ISO 3166-3.
	 */
	@Size(max = 3)
	@Column(name = "country_code")
	private String countryCode;

	/**
	 * The phone number of the user.
	 */
	@Size(max = 16)
	@Column(name = "phone_number")
	private String phoneNumber;

	/**
	 * The number of unread mesages count. Only available on specific calls.
	 */
	@Transient
	private int unreadMessageCount;
	
	public CommunicatorUser() {
    	
    }
    
    public CommunicatorUser(NetMobielUser nbuser) {
    	super(nbuser);
    }
    
    public CommunicatorUser(String identity, String givenName, String familyName, String email) {
    	super(identity, givenName, familyName, email);
    }
    
    public CommunicatorUser(String identity) {
    	super(identity, null, null, null);
    }
    
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
	}

	public Instant getFcmTokenTimestamp() {
		return fcmTokenTimestamp;
	}

	public void setFcmTokenTimestamp(Instant fcmTokenTimestamp) {
		this.fcmTokenTimestamp = fcmTokenTimestamp;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getLanguageCode() {
		String code = "en-gb";
		if ("NLD".equals(countryCode)) {
			code = "nl-nl";
		}
		return code;
	}

	public int getUnreadMessageCount() {
		return unreadMessageCount;
	}

	public void setUnreadMessageCount(int unreadMessageCount) {
		this.unreadMessageCount = unreadMessageCount;
	}
	
}
