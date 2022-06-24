package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
/**
 * Definition of a user session.  
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "user_session", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "session_id" }, name = "uc_session_id") })
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "user_session_sg", sequenceName = "user_session_id_seq", allocationSize = 1, initialValue = 50)
public class UserSession implements Serializable {
	private static final long serialVersionUID = -9153483037541781268L;

	/**
	 * Primary key.
	 */
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_session_sg")
    private Long id;

	/**
	 * The id of the session
	 */
	@NotNull
	@Size(max = 48)
	@Column(name = "session_id")
	private String sessionId;

	/**
	 * The remote IP address used to communicate with our server. 
	 */
	@NotNull
	@Size(max = 32)
	@Column(name = "ip_address")
	private String ipAddress;

	/**
	 * The user agent. 
	 */
	@NotNull
	@Size(max = 512)
	@Column(name = "user_agent")
	private String userAgent;

	/**
	 * The start time of the session.
	 */
	@NotNull
	@Column(name = "session_start", updatable = false)
	private Instant sessionStart;

	/**
	 * The end time of the session. Only set when logout is detected.
	 */
	@Column(name = "session_end")
	private Instant sessionEnd;

	/**
	 * Association with the user profile.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "real_user", foreignKey = @ForeignKey(name = "user_session_profile_fk"))
	private Profile realUser;

	/*
     * The user event records.
     */
    @OneToMany(mappedBy = "userSession", fetch = FetchType.LAZY)
    private List<UserEvent> userEvents;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Instant getSessionStart() {
		return sessionStart;
	}

	public void setSessionStart(Instant sessionStart) {
		this.sessionStart = sessionStart;
	}

	public Instant getSessionEnd() {
		return sessionEnd;
	}

	public void setSessionEnd(Instant sessionEnd) {
		this.sessionEnd = sessionEnd;
	}

	public Profile getRealUser() {
		return realUser;
	}

	public void setRealUser(Profile realUser) {
		this.realUser = realUser;
	}

	public List<UserEvent> getUserEvents() {
		return userEvents;
	}

	public void setUserEvents(List<UserEvent> userEvents) {
		this.userEvents = userEvents;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UserSession)) {
			return false;
		}
		UserSession other = (UserSession) obj;
		return Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return String.format("UserSession [id=%s, sid=%s]", id, sessionId);
	}
}
