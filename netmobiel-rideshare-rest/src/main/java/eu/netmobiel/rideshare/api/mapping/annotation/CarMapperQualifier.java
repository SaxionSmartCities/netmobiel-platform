/**
 * 
 */
package eu.netmobiel.rideshare.api.mapping.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Qualifier;

@Qualifier
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
/**
 * @author Jaap Reitsma
 *
 */
public @interface CarMapperQualifier {

}
