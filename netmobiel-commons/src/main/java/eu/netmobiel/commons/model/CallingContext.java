package eu.netmobiel.commons.model;

public class CallingContext<T extends User> {
	private T callingUser;
	private T effectiveUser;
	
	public CallingContext(T aCallingUser, T anEffectiveUser) {
		this.callingUser = aCallingUser;
		this.effectiveUser = anEffectiveUser;
	}

	public T getCallingUser() {
		return callingUser;
	}

	public T getEffectiveUser() {
		return effectiveUser;
	}
}
