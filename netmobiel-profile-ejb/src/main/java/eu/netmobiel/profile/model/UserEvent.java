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
	name = UserEvent.ACT_5_USER_VISITS_DAYS_PER_MONTH_COUNT,
	query = "select p.managed_identity AS managed_identity, "
			+ "date_part('year', sq.day_stamp) AS year, "
			+ "date_part('month', sq.day_stamp) AS month, " 
			+ "count(*) AS count "
			+ "from (SELECT coalesce(ev.on_behalf_of, us.real_user) AS effective_user, "
			+ "      DATE_TRUNC('day', ev.event_time) AS day_stamp "
			+ "      FROM user_event ev "
			+ "      JOIN user_session us ON us.id = ev.user_session "
			+ "      WHERE ev.event = 'PV' "
			+ "      GROUP BY effective_user, DATE_TRUNC('day', ev.event_time) "
			+ "     ) AS sq JOIN profile p ON p.id = sq.effective_user "
    		+ "WHERE sq.day_stamp >= ? and sq.day_stamp < ? "
			+ "GROUP BY p.managed_identity, year, month "
			+ "ORDER BY p.managed_identity, year, month",
	resultSetMapping = UserEvent.PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING
)
@NamedNativeQuery(
		name = UserEvent.ACT_6_HOME_PAGE_UPDATES_COUNT,
		query = "select p.managed_identity AS managed_identity, "
				+ "date_part('year', sq.day_stamp) AS year, "
				+ "date_part('month', sq.day_stamp) AS month, "
				+ "count(*) AS count "
				+ "from (SELECT coalesce(ev.on_behalf_of, us.real_user) AS effective_user, "
				+ "      DATE_TRUNC('day', ev.event_time) AS day_stamp "
				+ "      FROM user_event ev "
				+ "      JOIN user_session us ON us.id = ev.user_session "
				+ "      WHERE ev.event = 'CV' AND ev.path = '/home' "
				+ "      GROUP BY effective_user, DATE_TRUNC('day', ev.event_time) "
				+ "     ) AS sq JOIN profile p ON p.id = sq.effective_user "
	    		+ "WHERE sq.day_stamp >= ? and sq.day_stamp < ? "
				+ "GROUP BY p.managed_identity, year, month "
				+ "ORDER BY p.managed_identity, year, month",
		resultSetMapping = UserEvent.PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING
	)
@NamedNativeQuery(
		name = UserEvent.ACT_7_HOME_PAGE_UPDATES_CTA_PRESSED_COUNT,
		query = "select p.managed_identity AS managed_identity, "
				+ "date_part('year', sq.day_stamp) AS year, "
				+ "date_part('month', sq.day_stamp) AS month, "
				+ "count(*) AS count "
				+ "from (SELECT coalesce(ev.on_behalf_of, us.real_user) AS effective_user, "
				+ "      DATE_TRUNC('day', ev.event_time) AS day_stamp "
				+ "      FROM user_event ev "
				+ "      JOIN user_session us ON us.id = ev.user_session "
				+ "      WHERE ev.event = 'CS' AND ev.path = '/home' "
				+ "      GROUP BY effective_user, DATE_TRUNC('day', ev.event_time) "
				+ "     ) AS sq JOIN profile p ON p.id = sq.effective_user "
	    		+ "WHERE sq.day_stamp >= ? and sq.day_stamp < ? "
				+ "GROUP BY p.managed_identity, year, month "
				+ "ORDER BY p.managed_identity, year, month",
		resultSetMapping = UserEvent.PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING
	)
@SqlResultSetMapping(
	name = UserEvent.PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING, 
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
@Table(name = "user_event")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "user_event_sg", sequenceName = "user_event_id_seq", allocationSize = 1, initialValue = 50)
public class UserEvent implements Serializable {
	private static final long serialVersionUID = -9153483037541781268L;
	public static final String PR_USER_VISIT_YEAR_MONTH_COUNT_MAPPING = "PRUserVisitYearMonthCountMapping";
	public static final String ACT_5_USER_VISITS_DAYS_PER_MONTH_COUNT = "PRUserVisitDaysPerMonthCount";
	public static final String ACT_6_HOME_PAGE_UPDATES_COUNT = "PRHomePageUpdatesCount";
	public static final String ACT_7_HOME_PAGE_UPDATES_CTA_PRESSED_COUNT = "PRHomePageUpdatesCTAPressedCount";

	/**
	 * Primary key.
	 */
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_event_sg")
    private Long id;

	/**
	 * Association with the user session.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "user_session", foreignKey = @ForeignKey(name = "user_event_user_session_fk"))
	private UserSession userSession;

	/**
	 * Association with the effective user profile in case of delegation.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "on_behalf_of", foreignKey = @ForeignKey(name = "user_event_on_behalf_of_fk"))
	private Profile onBehalfOf;

	/**
	 * The path of the page.
	 */
	@NotNull
	@Size(max = 128)
	@Column(name = "path")
	private String path;

	/**
	 * The event 
	 */
	@NotNull
	@Column(name = "event")
	private UserEventType event;

	/**
	 * The optional arguments of the event.
	 */
	@Size(max = 256)
	@Column(name = "arguments")
	private String arguments;

	/**
	 * The visit of the path.
	 */
	@NotNull
	@Column(name = "event_time", updatable = false)
	private Instant eventTime;

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

	public Instant getEventTime() {
		return eventTime;
	}

	public void setEventTime(Instant eventTime) {
		this.eventTime = eventTime;
	}

	public UserEventType getEvent() {
		return event;
	}

	public void setEvent(UserEventType event) {
		this.event = event;
	}

	public String getArguments() {
		return arguments;
	}

	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return String.format("PageVisit [%s %s, %s]", id, path, event);
	}

}
