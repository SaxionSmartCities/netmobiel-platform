package eu.netmobiel.banker.api.mapping.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mapstruct.Qualifier;
/**
 * Qualifier to show the charity details, without roles.
 * @author Jaap Reitsma
 *
 */
@Qualifier
@Retention(CLASS)
@Target(METHOD)
public @interface CharityDetails {

}
