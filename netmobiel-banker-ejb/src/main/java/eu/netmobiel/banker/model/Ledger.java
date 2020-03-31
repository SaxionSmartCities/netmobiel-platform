package eu.netmobiel.banker.model;

import java.time.Instant;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "ledger")
@Vetoed
@SequenceGenerator(name = "ledger_sg", sequenceName = "ledger_seq", allocationSize = 1, initialValue = 50)
public class Ledger {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ledger_sg")
    private Long id;

	/**
	 * Start of the accounting period inclusive.
	 */
	@Column(name = "start", nullable = false)
	private Instant start;

	/**
	 * Start of the accounting period exclusive.
	 */
	@Column(name = "end", nullable = false)
	private Instant end;
	
	/**
	 * Name of the period.
	 */
	@Column(name = "name", nullable = false)
	private String name;

	@OneToMany(mappedBy = "ledger")
    private List<AccountingTransaction> transactions;

}
