/**
 * 
 */
package eu.netmobiel.commons.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
/**
 * Annotation used with event generation for CRUD like changes in the system.
 *  
 * @author Jaap Reitsma
 *
 */
public @interface Removed {

}
