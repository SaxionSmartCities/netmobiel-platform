package eu.netmobiel.banker.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import nl.garvelink.iban.IBAN;
import nl.garvelink.iban.Modulo97;

public class IBANValidator implements ConstraintValidator<IBANBankAccount, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		IBAN iban = null; 
		 // You can use the Modulo97 class directly to compute or verify the check digits on an input.
		boolean valid = Modulo97.verifyCheckDigits(value);
		if (valid) {
			iban = IBAN.valueOf(value);
		}
	    // You can query whether an IBAN is of a SEPA-participating country
	    // You can query whether an IBAN is in the SWIFT Registry
		return valid && iban.isSEPA() && iban.isInSwiftRegistry();
	}

}
