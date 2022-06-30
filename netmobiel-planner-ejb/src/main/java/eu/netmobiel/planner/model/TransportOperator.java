package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Size;

/**
 * Registration of a TOMP transport operator.
 * 
 */
@Entity
@Table(name = "transport_operator")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "transport_operator_sg", sequenceName = "transport_operator_id_seq", allocationSize = 1, initialValue = 50)
public class TransportOperator implements Serializable {

	private static final long serialVersionUID = -3789784762166689723L;
	

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transport_operator_sg")
    private Long id;

	/**
	 * The base url of the transport operator.
	 */
    @Size(max = 256)
    @Column(name = "base_url")
    private String baseUrl;

    /**
     * A short name, intended for the maintainer.
     */
    @Size(max = 32)
    @Column(name = "display_name")
    private String displayName;

    /**
     * A short description, intended for the maintainer.
     */
    @Size(max = 256)
    @Column(name = "description")
    private String description;

    /**
     * Is the transport operator enabled? If not, the entry is ignored.
     */
    @Column(name = "enabled")
    private boolean enabled;

    public TransportOperator() {
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
         if (!(o instanceof TransportOperator)) {
            return false;
        }
         TransportOperator other = (TransportOperator) o;
        return Objects.equals(getId(), other.getId());
    }

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
    @Override
    public int hashCode() {
        return 31;
    }

	@Override
	public String toString() {
		return String.format("TransportOperator [%s %s]", id, displayName);
	}

}
