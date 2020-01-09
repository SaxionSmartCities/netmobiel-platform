package eu.netmobiel.opentripplanner.api.model;

import javax.ws.rs.core.Response;

public enum Message {
    // id field is loosely based on HTTP error codes http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
    PLAN_OK(200, Response.Status.OK),
    SYSTEM_ERROR(500, Response.Status.INTERNAL_SERVER_ERROR),
    GRAPH_UNAVAILABLE(503, Response.Status.SERVICE_UNAVAILABLE),

	OUTSIDE_BOUNDS(400, Response.Status.BAD_REQUEST),
	PATH_NOT_FOUND(404, Response.Status.NOT_FOUND),
	NO_TRANSIT_TIMES(406, Response.Status.NOT_ACCEPTABLE),
	REQUEST_TIMEOUT(408, Response.Status.REQUEST_TIMEOUT),
	BOGUS_PARAMETER(413, Response.Status.BAD_REQUEST),
	GEOCODE_FROM_NOT_FOUND(440, Response.Status.BAD_REQUEST),
	GEOCODE_TO_NOT_FOUND(450, Response.Status.BAD_REQUEST),
	GEOCODE_FROM_TO_NOT_FOUND(460, Response.Status.BAD_REQUEST),
    TOO_CLOSE(409, Response.Status.CONFLICT),
    LOCATION_NOT_ACCESSIBLE(470, Response.Status.BAD_REQUEST),

    GEOCODE_FROM_AMBIGUOUS(340, Response.Status.BAD_REQUEST),
    GEOCODE_TO_AMBIGUOUS(350, Response.Status.BAD_REQUEST),
    GEOCODE_FROM_TO_AMBIGUOUS(360, Response.Status.BAD_REQUEST), 
    
    UNDERSPECIFIED_TRIANGLE(370, Response.Status.BAD_REQUEST),
    TRIANGLE_NOT_AFFINE(371, Response.Status.BAD_REQUEST),
    TRIANGLE_OPTIMIZE_TYPE_NOT_SET(372, Response.Status.BAD_REQUEST),
    TRIANGLE_VALUES_NOT_SET(373, Response.Status.BAD_REQUEST),
    ;

    private final int m_id;
    private final Response.Status status;

    /** enum constructors are private -- see values above */
    private Message(int id, Response.Status status) {
        this.m_id = id;
        this.status = status;
    }

    public int getId() {
        return m_id;
    }

    public Response.Status getStatus() {
        return status;
    }
}
