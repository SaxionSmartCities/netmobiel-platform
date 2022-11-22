package eu.netmobiel.opentripplanner.api.model;

import java.util.List;

/** 
 * This API response element represents an error in trip planning. 
 */
public class PlannerError {
    
    public int    id;
    public String msg;
    public Message message;
    public List<String> missing = null;
    public boolean noPath = false;

}
