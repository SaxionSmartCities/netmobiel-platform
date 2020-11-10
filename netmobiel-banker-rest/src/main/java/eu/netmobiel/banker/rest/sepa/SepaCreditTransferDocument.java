package eu.netmobiel.banker.rest.sepa;

import java.util.List;

import javax.validation.Valid;

public class SepaCreditTransferDocument extends SepaDocument {
	@Valid
	private List<SepaTransaction> transactions;
	@Valid
	private SepaPaymentInformation paymentInfo;

	public SepaCreditTransferDocument(SepaGroupHeader header, SepaPaymentInformation paymentInfo, List<SepaTransaction> transactions) {
		super(header);
		this.paymentInfo = paymentInfo;
		this.transactions = transactions;
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
}
