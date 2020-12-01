package eu.netmobiel.here.search.model;

public class ErrorResponse {
	public Integer status;
	public String title; 
	public String cause; 
	public String action;
	public String correlationId;
	public String requestId;
	
	@Override
	public String toString() {
		return String.format("HERE: %s caused by %s - %s", title, cause, action);
	}
}
