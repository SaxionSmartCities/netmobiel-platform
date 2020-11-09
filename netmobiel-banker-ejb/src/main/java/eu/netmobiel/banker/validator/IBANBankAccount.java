package eu.netmobiel.banker.validator;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR })
@Constraint(validatedBy = IBANValidator.class )
public @interface IBANBankAccount {
	String message() default "Not a valid IBAN";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
