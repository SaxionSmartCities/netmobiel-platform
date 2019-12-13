package eu.netmobiel.commons.util;

import org.apache.commons.lang3.tuple.MutablePair;

public class NameValuePair extends MutablePair<String, String> {

	private static final long serialVersionUID = 6345685313873909340L;

	public NameValuePair(String aName, String aValue) {
		super(aName, aValue);
	}

	public String getName() {
		return getLeft();
	}

	public void setName(String aName) {
		setLeft(aName);
	}

}
