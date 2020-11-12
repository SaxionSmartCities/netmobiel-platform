package eu.netmobiel.banker.rest.sepa;

import java.util.List;

import javax.validation.Valid;
/**
 * Model for the SEPA PAIN Credit Transfer (PAIN 001.001.03), used by the ABN Amro.
 * The validation constraints are added as an exercise for the author and because of the validation for 
 * the allowed character. The latter seems omitted in the XSD scheme. 
 * 
 * The builder applied pattern used throughout the construction of the document assures proper 
 * validation (and adaptation) of the input. 
 * 
 * Although the specification allows for multiple payment information blocks, this implementation
 * limits the number to one only. NetMobiel will never use multiple debtor accounts. 
 *  
 * @author Jaap Reitsma
 *
 */
public class SepaCreditTransferDocument extends SepaDocument {
	@Valid
	private List<SepaTransaction> transactions;
	@Valid
	private SepaPaymentInformation paymentInfo;

	private SepaCreditTransferDocument(SepaGroupHeader header, SepaPaymentInformation paymentInfo, List<SepaTransaction> transactions) {
		super(header);
		this.paymentInfo = paymentInfo;
		this.transactions = transactions;
	}

	public static class Builder {
		private List<SepaTransaction> transactions;
		private SepaPaymentInformation paymentInfo;
		private SepaGroupHeader groupHeader;
		
		public Builder() {
		}

		public Builder with(List<SepaTransaction> transactions) {
			this.transactions = transactions;
			return this;
		}
		
		public Builder with(SepaPaymentInformation paymentInfo) {
			this.paymentInfo = paymentInfo;
			return this;
		}

		public Builder with(SepaGroupHeader header) {
			this.groupHeader = header;
			return this;
		}

		public SepaCreditTransferDocument build() {
			if (groupHeader == null || paymentInfo == null || transactions == null) {
				throw new IllegalArgumentException("Group header, payment information and the transactions are manadatory parameters");
			}
			if (transactions.isEmpty()) {
				throw new IllegalArgumentException("Supply at least one transaction");
			}
			return new SepaCreditTransferDocument(groupHeader, paymentInfo, transactions);
		}

	}

	
	public String getDocumentChildTag() {
		return "CstmrCdtTrfInitn";
	}

	public XMLNode toXml() {
		XMLNode mainnode = super.toXml();
		XMLNode payNode = paymentInfo.toXml(mainnode);
		transactions.forEach(t -> t.toXml(payNode));
		return mainnode;
	}

	public List<SepaTransaction> getTransactions() {
		return transactions;
	}

	public SepaPaymentInformation getPaymentInfo() {
		return paymentInfo;
	}
}
