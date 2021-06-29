package eu.netmobiel.banker.rest.sepa;

import javax.validation.Valid;

public abstract class SepaDocument {
	private XMLNode document;
	@Valid
	private SepaGroupHeader groupHeader;
	
	protected SepaDocument(SepaGroupHeader header) {
		this.groupHeader = header;
	}

	public abstract String getDocumentChildTag();
	
	public XMLNode toXml() {
		document = new XMLNode().append("Document")
				.attr("xmlns", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")
				.attr("xsi:schemaLocation", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03 pain.001.001.03.xsd")
				.attr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		XMLNode node = document.append(getDocumentChildTag());
		node.append(groupHeader.toXml(document));
		return node;
	}

	public XMLNode getDocument() {
		return document;
	}

	public SepaGroupHeader getGroupHeader() {
		return groupHeader;
	}
	
}
