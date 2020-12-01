package eu.netmobiel.here.places.model;

public class ErrorResponse {
	public Integer status;
	public String message; 
	public String incidentId;
	
	@Override
	public String toString() {
		return String.format("HERE: %s", message);
	}
}
