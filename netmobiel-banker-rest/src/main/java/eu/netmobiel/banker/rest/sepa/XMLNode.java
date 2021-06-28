package eu.netmobiel.banker.rest.sepa;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.netmobiel.commons.exception.SystemException;

public class XMLNode {

	private Document document;
	private Element node;

	public XMLNode() {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			this.document = dBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public XMLNode(Document document, Element childNode) {
		this.document = document;
		this.node = childNode;
	}

	public XMLNode append(XMLNode xmlnode) {
		this.node.appendChild(xmlnode.node);
		return xmlnode;
	}

	public XMLNode append(String name) {
		Element childElement = this.document.createElement(name);
		if (this.node == null) {
			this.document.appendChild(childElement);
		} else {
			this.node.appendChild(childElement);
		}
		XMLNode childNode = new XMLNode(this.document, childElement);
		return childNode;
	}

	public XMLNode attr(String key, String value) {
		Attr attr = this.document.createAttribute(key);
		attr.setValue(value);
		this.node.setAttributeNode(attr);
		return this;
	}

	public XMLNode value(String value) {
		this.node.appendChild(this.document.createTextNode(value));
		return this;
	}

	public XMLNode value(int value) {
		value(Integer.toString(value));
		return this;
	}

	public XMLNode value(double value) {
		value(Double.toString(value));
		return this;
	}

	public void write(Writer writer) throws TransformerException {
		write(writer, false, 0);
	}

	public void write(Writer writer, boolean pretty, int indent) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		Transformer transformer = transformerFactory.newTransformer();
		if (pretty) {
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
		}
		DOMSource source = new DOMSource(this.document);
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
	}

	public String toString() {
		String s = null;
		try (StringWriter sw = new StringWriter()) {
			write(sw, true, 2);
			s = sw.toString();
		} catch (IOException | TransformerException e) {
			throw new SystemException("Unable to serialize XML Node", e);
		}
		return s;
	}
	
}
