package eu.netmobiel.banker.rest.sepa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import nl.garvelink.iban.IBAN;

public class SepaPaymentInformation {
	public enum PaymentMethods {

		Cheque("CHK"), TransferAdvice("TRF"), CreditTransfer("TRA");

		private String code;

		PaymentMethods(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

	}

	/**
	 * According ABN Amro this id is not used.
	 */
	@NotNull
	@SepaIdentifier
	private String id;

	private int nrTransactions;
	
	@NotNull
	@DecimalMin("0.01")
	private BigDecimal controlSum;
	
	@NotNull
	private IBAN account;
	
	@NotNull
	@SepaText
	
	private String accountHolder;
	@NotNull
	
	private LocalDate executionDate;

	private SepaPaymentInformation(String id, int nrTx, BigDecimal sum, LocalDate executionDate, IBAN account, String name) {
		this.id = id;
		this.nrTransactions = nrTx;
		this.controlSum = sum;
		this.executionDate = executionDate;
		this.account = account;
		this.accountHolder = name;
	}

	public static class Builder {
		private String id;
		// Number of transaction in the batch
		private int nrTransactions;
		// The sum of the amount in the batch
		private BigDecimal controlSum;
		private LocalDate executionDate = LocalDate.now();
		private IBAN account;
		private String name;
		
		public Builder(String paymentInfoId) {
			this.id = SepaFormat.identifier(paymentInfoId);
		}

		public Builder of(Collection<SepaTransaction> transactions) {
			this.nrTransactions = transactions.size();
			this.controlSum = transactions.stream()
					.map(t -> t.getAmount())
					.reduce(BigDecimal.ZERO, (accu, v) -> accu.add(v));
			return this;
		}
		public Builder withExecutionDate(LocalDate time) {
			this.executionDate = time;
			return this;
		}

		public Builder withAccount(String iban) {
			this.account = IBAN.valueOf(iban);
			return this;
		}

		public Builder withAccountHolder(String aName) {
			this.name = SepaFormat.text(aName);
			return this;
		}

		public SepaPaymentInformation build() {
			return new SepaPaymentInformation(id, nrTransactions, controlSum, executionDate, account, name);
		}

	}

	public XMLNode toXml(XMLNode parent) {
		XMLNode node = parent.append("PmtInf");
		node.append("PmtInfId").value(id);
		node.append("PmtMtd").value(PaymentMethods.TransferAdvice.getCode());
		node.append("NbOfTxs").value(nrTransactions);
		node.append("CtrlSum").value(controlSum.toPlainString());
		node.append("PmtTpInf").append("SvcLvl").append("Cd").value("SEPA");
		node.append("ReqdExctnDt").value(executionDate.toString());
		node.append("Dbtr").append("Nm").value(accountHolder);
		node.append("DbtrAcct").append("Id").append("IBAN").value(account.toPlainString());
		node.append("DbtrAgt").append("FinInstnId").append("Othr").append("Id").value("NOTPROVIDED");
		node.append("ChrgBr").value("SLEV");
		return node;
	}
}
