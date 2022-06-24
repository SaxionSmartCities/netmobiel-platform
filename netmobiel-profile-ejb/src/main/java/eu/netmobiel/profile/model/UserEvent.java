package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Definition of a single page visit
 * 
 * @author Jaap Reitsma
 *
 */

import eu.netmobiel.commons.report.NumericReportValue;

@NamedNativeQuery(
	name = PageVisit.ACT_5_USER_VISITS_DAYS_PER_MONTH_COUNT,
	query = "select p.managed_identity AS managed_identity, "
			+ "date_part('year', sq.day_stamp) AS year, "
			+ "date_part('month', sq.day_stamp) AS month, count(*) AS count "
			+ "from (SELECT coalesce(pv.on_behalf_of, us.real_user) AS effective_user, "
			+ "      DATE_TRUNC('day', pv.visit_time) AS day_stamp "
			+ "      FROM page_visit pv "
			+ "      JOIN user_session us ON us.id = pv.user_session "
			+ "      GROUP BY effective_user, DATE_TRUNC('day', pv.visit_time) "
			+ "     ) AS sq JOIN profile p ON p.id = sq.effective_user "
    		+ "WHERE sq.day_stamp >= ? and sq.day_stamp < ? "
			+ "GROUP BY p.managed_identity, year, month "
			+ "ORDER BY p.managed_identity, year, month",
	resultSetMapping = PageVisit.PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING
)
@SqlResultSetMapping(
	name = PageVisit.PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING, 
	classes = @ConstructorResult(
		targetClass = NumericReportValue.class, 
		columns = {
				@ColumnResult(name = "managed_identity", type = String.class),
				@ColumnResult(name = "year", type = int.class),
				@ColumnResult(name = "month", type = int.class),
				@ColumnResult(name = "count", type = int.class)
		}
	)
)

@Entity
@Table(name = "page_visit")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "page_visit_sg", sequenceName = "page_visit_id_seq", allocationSize = 1, initialValue = 50)
public class PageVisit implements Serializable {
	private static final long serialVersionUID = -9153483037541781268L;
	public static final String PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING = "PRUserVisitYearMonthCountMapping";
	public static final String ACT_5_USER_VISITS_DAYS_PER_MONTH_COUNT = "PRUserVisitDaysPerMonthCount";

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
	 * Association with the effective user profile in case of delegation.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "on_behalf_of", foreignKey = @ForeignKey(name = "page_visit_on_behalf_of_fk"))
	private Profile onBehalfOf;

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

	public Profile getOnBehalfOf() {
		return onBehalfOf;
	}

	public void setOnBehalfOf(Profile onBehalfOf) {
		this.onBehalfOf = onBehalfOf;
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

	@Override
	public String toString() {
		return String.format("PageVisit [%s %s]", id, path);
	}

}
