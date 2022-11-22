package eu.netmobiel.banker.rest.sepa;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Size(max = SepaFormat.MAX_LENGTH_IDENTIFIER)
@Pattern(regexp = SepaFormat.REGEX_IDENTIFIER, message = "must not contain special characters")
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, ANNOTATION_TYPE, TYPE_USE })
@Constraint(validatedBy = { })
public @interface SepaIdentifier {

    String message() default "XXX";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
