package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
/**
 * Definition of a user session.  
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "page_visit")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "page_visit_sg", sequenceName = "page_visit_id_seq", allocationSize = 1, initialValue = 50)
public class PageVisit implements Serializable {
	private static final long serialVersionUID = -9153483037541781268L;

	/**
	 * Primary key.
	 */
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "page_visit_sg")
    private Long id;

	/**
	 * Association with the user session.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "user_session", foreignKey = @ForeignKey(name = "page_visit_user_session_fk"))
	private UserSession userSession;

	/**
	 * The path of the page.
	 */
	@NotNull
	@Size(max = 128)
	@Column(name = "path")
	private String path;

	/**
	 * The visit of the path.
	 */
	@NotNull
	@Column(name = "visit_time", updatable = false)
	private Instant visitTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserSession getUserSession() {
		return userSession;
	}

	public void setUserSession(UserSession userSession) {
		this.userSession = userSession;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Instant getVisitTime() {
		return visitTime;
	}

	public void setVisitTime(Instant visitTime) {
		this.visitTime = visitTime;
	}

}
