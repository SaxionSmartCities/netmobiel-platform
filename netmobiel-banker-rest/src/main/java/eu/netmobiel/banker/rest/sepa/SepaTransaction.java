package eu.netmobiel.banker.rest.sepa;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import nl.garvelink.iban.IBAN;

public class SepaTransaction {

	public enum Currency {
		BGN, HRK, CZK, DKK, EUR, GIP, HUF, ISK, NOK, PLN, RON, SEK, CHF, GBP
	}

	@NotNull
	private IBAN accountNumber;

	@NotNull
	@SepaText
	private String name;

	@NotNull
	@DecimalMin(value = "0.01", inclusive = true)
	private BigDecimal amount;
	
	@NotNull
	@SepaIdentifier
	private String endToEndId;
	
	@NotNull
	private Currency currency;
	
	@SepaText
	private String remittance;
	
	private BiFunction<SepaTransaction, XMLNode, XMLNode> xmlEmitter;

	private SepaTransaction(IBAN accountNumber, String name, BigDecimal amount, Currency currency, String endToEndId,
			String remittance, BiFunction<SepaTransaction, XMLNode, XMLNode> xmlEmitter) {
		this.accountNumber = accountNumber;
		this.name = name;
		this.amount = amount;
		this.currency = currency;
		this.endToEndId = endToEndId;
		this.remittance = remittance;
		this.xmlEmitter = xmlEmitter;
	}

	public static class Builder {
		private IBAN accountNumber;
		private String name;
		private BigDecimal amount;
		private String endToEndId;
		private Currency currency = Currency.EUR;
		private String remittance;
		private BiFunction<SepaTransaction, XMLNode, XMLNode> xmlEmitter;

		public Builder(String account) {
			accountNumber = IBAN.valueOf(account);
		}

		public Builder withName(String accountHolder) {
			this.name = SepaFormat.text(accountHolder);
			return this;
		}

		public Builder withAmount(BigDecimal amount) {
			this.amount = amount;
			return this;
		}

		public Builder withEnd2EndId(String id) {
			this.endToEndId = SepaFormat.identifier(id);
			return this;
		}

		public Builder withRemittance(String description) {
			this.remittance = SepaFormat.text(description);
			return this;
		}

		public SepaTransaction build() {
			xmlEmitter = (tx, parent) -> {
				XMLNode node = parent.append("CdtTrfTxInf");
				node.append("PmtId").append("EndToEndId").value(tx.getEndToEndId());
				node.append("Amt").append("InstdAmt")
					.attr("Ccy", tx.getCurrency().toString())
					.value(tx.getAmount().toPlainString());
//				nodeCdtTrfTxInf.append("CdtrAgt").append("FinInstnId").append("BIC").value(transaction.getBicNumber());
				node.append("CdtrAgt").append("FinInstnId").append("Othr").append("Id").value("NOTPROVIDED");
				node.append("Cdtr").append("Nm").value(tx.getName());
				node.append("CdtrAcct").append("Id").append("IBAN").value(tx.getAccountNumber().toPlainString());
				if (tx.getRemittance() != null) {
					node.append("RmtInf").append("Ustrd").value(tx.getRemittance());
				}
				return node;
			};
			return new SepaTransaction(accountNumber, name, amount, currency, endToEndId, remittance, xmlEmitter);
		}

	}

	public IBAN getAccountNumber() {
		return accountNumber;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getEndToEndId() {
		return endToEndId;
	}

	public Currency getCurrency() {
		return currency;
	}

	public String getRemittance() {
		return remittance;
	}

	public XMLNode toXml(XMLNode parent) {
		return xmlEmitter.apply(this, parent);
	}
}
