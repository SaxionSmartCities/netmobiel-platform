package eu.netmobiel.opentripplanner.api.model;

import java.util.HashMap;

/** 
 * Represents a trip planner response
 */
public class PlanResponse {

    /** A dictionary of the parameters provided in the request that triggered this response. */
    public HashMap<String, String> requestParameters;
    public TripPlan plan;
    public PlannerError error = null;
}