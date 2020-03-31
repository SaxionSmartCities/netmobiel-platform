package eu.netmobiel.banker.model;

import java.sql.Timestamp;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "balance", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_balance_unique", columnNames = { "account", "ledger" })
})
@Vetoed
@SequenceGenerator(name = "balance_sg", sequenceName = "balance_seq", allocationSize = 1, initialValue = 50)
public class Balance {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "balance_sg")
    private Long id;

	@Version
	@Column(name = "version")
	private int version;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "modifiedAt", insertable = false, updatable = false)
	private Timestamp modifiedAt;
	
	@Column(name = "start_amount", nullable = false)
	private int startAmount;
	
	@Column(name = "end_amount", nullable = false)
	private int endAmount;
	
	@ManyToOne
	@Column(name = "ledger", nullable = false)
	private Ledger ledger;
	
	@ManyToOne
	@Column(name = "account", nullable = false)
	private Account account;
}
