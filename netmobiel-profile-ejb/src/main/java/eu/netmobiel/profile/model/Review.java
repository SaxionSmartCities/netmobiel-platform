package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.report.NumericReportValue;

@NamedNativeQueries({
	@NamedNativeQuery(
		name = Review.IMP_9_TRIPS_REVIEWED_COUNT,
		query = "select p.managed_identity as managed_identity, "
        		+ "date_part('year', r.published) as year, " 
        		+ "date_part('month', r.published) as month, "
        		+ "count(*) as count "
        		+ "from review r "
        		+ "join profile p on p.id = r.sender "
        		+ "where r.published >= ? and r.published < ? "
//        		+ "and the review is about a trip"
        		+ "group by p.managed_identity, year, month "
        		+ "order by p.managed_identity, year, month",
        resultSetMapping = Review.PR_REVIEW_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Review.IMC_10_RIDES_REVIEWED_COUNT,
			query = "select p.managed_identity as managed_identity, "
	        		+ "date_part('year', r.published) as year, " 
	        		+ "date_part('month', r.published) as month, "
	        		+ "count(*) as count "
	        		+ "from review r "
	        		+ "join profile p on p.id = r.sender "
	        		+ "where r.published >= ? and r.published < ? "
//	        		+ "and the review is about a ride"
	        		+ "group by p.managed_identity, year, month "
	        		+ "order by p.managed_identity, year, month",
	        resultSetMapping = Review.PR_REVIEW_USER_YEAR_MONTH_COUNT_MAPPING),
})
@SqlResultSetMappings({
	@SqlResultSetMapping(
			name = Review.PR_REVIEW_USER_YEAR_MONTH_COUNT_MAPPING, 
			classes = @ConstructorResult(
				targetClass = NumericReportValue.class, 
				columns = {
						@ColumnResult(name = "managed_identity", type = String.class),
						@ColumnResult(name = "year", type = int.class),
						@ColumnResult(name = "month", type = int.class),
						@ColumnResult(name = "count", type = int.class)
				}
			)
		),
})
@NamedEntityGraphs({
	@NamedEntityGraph(name = Review.LIST_REVIEWS_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "receiver"),
			@NamedAttributeNode(value = "sender"),
	}),
})
@Entity
@Table(name = "review")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "review_sg", sequenceName = "review_id_seq", allocationSize = 1, initialValue = 50)
public class Review implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	public static final String LIST_REVIEWS_ENTITY_GRAPH = "list-reviews-entity-graph";
	public static final String PR_REVIEW_USER_YEAR_MONTH_COUNT_MAPPING = "PRReviewUserYearMonthCountMapping";
	public static final String IMP_9_TRIPS_REVIEWED_COUNT = "ListTripsReviewedCount";
	public static final String IMC_10_RIDES_REVIEWED_COUNT = "ListRidesReviewedCount";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_sg")
    private Long id;
	
	@NotNull
	@Size(max = 256)
	@Column(name = "review")
	private String review;

	@NotNull
	@Column(name = "published")
	private Instant published;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver", nullable = false, foreignKey = @ForeignKey(name = "review_receiver_profile_fk"))
	private Profile receiver;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender", nullable = false, foreignKey = @ForeignKey(name = "review_sender_profile_fk"))
	private Profile sender;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getReview() {
		return review;
	}

	public void setReview(String review) {
		this.review = review;
	}

	public Instant getPublished() {
		return published;
	}

	public void setPublished(Instant published) {
		this.published = published;
	}

	public Profile getReceiver() {
		return receiver;
	}

	public void setReceiver(Profile receiver) {
		this.receiver = receiver;
	}

	public Profile getSender() {
		return sender;
	}

	public void setSender(Profile sender) {
		this.sender = sender;
	}

	@Override
	public int hashCode() {
		return Objects.hash(published, receiver, review, sender);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Review)) {
			return false;
		}
		Review other = (Review) obj;
		return Objects.equals(published, other.published) && Objects.equals(receiver, other.receiver)
				&& Objects.equals(review, other.review) && Objects.equals(sender, other.sender);
	}
	
}
