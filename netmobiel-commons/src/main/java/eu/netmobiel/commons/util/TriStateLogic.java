package eu.netmobiel.commons.util;

import java.util.function.BinaryOperator;

public class TriStateLogic {
    public static Boolean and(Boolean acc, Boolean value) {
    	// Acc/Value null	F		T
    	// null		null	F		T	
    	// F		F		F		F
    	// T		T		F		T
    	// The following expression gives an NPE. Why?
//    	return value == null ? acc : (!Boolean.FALSE.equals(acc) && !Boolean.FALSE.equals(value));
    	
    	Boolean result = acc;
    	if (value != null) {
    		result = !Boolean.FALSE.equals(acc) && !Boolean.FALSE.equals(value);
    	}
    	return result;
    }

    public static Boolean or(Boolean acc, Boolean value) {
    	// Acc/Value null	F		T
    	// null		null	F		T	
    	// F		F		F		T
    	// T		T		T		T
    	Boolean result = acc;
    	if (value != null) {
    		 result = Boolean.TRUE.equals(acc) || Boolean.TRUE.equals(value);
    	}
    	return result;
    }

    public static BinaryOperator<Boolean> reduceAnd() {
        return (acc, v) -> and(acc, v);
    }

    public static BinaryOperator<Boolean> reduceOr() {
        return (acc, v) -> or(acc, v);
    }
}
