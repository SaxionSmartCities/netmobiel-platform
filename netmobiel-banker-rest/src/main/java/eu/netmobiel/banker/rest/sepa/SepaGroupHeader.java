package eu.netmobiel.banker.rest.sepa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class SepaGroupHeader {

	@NotNull
	@SepaIdentifier
	private String messageId;
	
	@NotNull
	private Instant creationDateTime;
	
	@Positive
	private int nrTransactions;
	
	@NotNull
	@DecimalMin("0.01")
	private BigDecimal controlSum;
	
	@NotNull
	@SepaText
	private String initiatingParty;

	private SepaGroupHeader(String msgId, int nrTx, BigDecimal sum, Instant creationTime, String initiator) {
		this.messageId = msgId;
		this.nrTransactions = nrTx;
		this.controlSum = sum;
		this.creationDateTime = creationTime;
		this.initiatingParty = initiator;
	}

	public static class SepaGroupHeaderBuilder {
		private int nrTransactions;
		private BigDecimal controlSum;
		private String messageId;
		private Instant creationDateTime = Instant.now();
		private String initiatingParty;
		
		public SepaGroupHeaderBuilder(String msgId) {
			this.messageId = msgId;
		}

		public SepaGroupHeaderBuilder of(Collection<SepaTransaction> transactions) {
			this.nrTransactions = transactions.size();
			this.controlSum = transactions.stream()
					.map(t -> t.getAmount())
					.reduce(BigDecimal.ZERO, (accu, v) -> accu.add(v));
			return this;
		}
		
		public SepaGroupHeaderBuilder withCreateDateTime(Instant time) {
			this.creationDateTime = time;
			return this;
		}

		public SepaGroupHeaderBuilder withInitiatingParty(String name) {
			this.initiatingParty = name;
			return this;
		}

		public SepaGroupHeader build() {
			return new SepaGroupHeader(messageId, nrTransactions, controlSum, creationDateTime, initiatingParty);
		}

	}

	public XMLNode toXml(XMLNode parent) {
		XMLNode node = parent.append("GrpHdr");
		node.append("MsgId").value(messageId);
		node.append("CreDtTm").value(DateTimeFormatter.ISO_INSTANT.format(creationDateTime));
		node.append("NbOfTxs").value(nrTransactions);
		node.append("CtrlSum").value(controlSum.toPlainString());
		node.append("InitgPty").append("Nm").value(initiatingParty);
		return node;
	}
}
