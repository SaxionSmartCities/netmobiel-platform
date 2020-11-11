package eu.netmobiel.banker.rest.sepa;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SepaCreditTransferDocumentTest {
    private static final Logger log = LoggerFactory.getLogger(SepaCreditTransferDocumentTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private static final Validator VALIDATOR =
			  Validation.byDefaultProvider()
			    .configure()
			    .messageInterpolator(new ParameterMessageInterpolator())
			    .buildValidatorFactory()
			    .getValidator();
	@Test
	public void testToXml() {
		List<SepaTransaction> transactions = new ArrayList<>();
		transactions.add(new SepaTransaction.Builder("NL69INGB0123456789")
				.withAmount(BigDecimal.valueOf(150, 2))
				.withEnd2EndId("End2End-21")
				.withName("P. Pietersen")
				.withRemittance("Rit van A naar B met Henk |||||")
				.build()
				);
		transactions.add(new SepaTransaction.Builder("NL44 RABO 0123 4567 89")
				.withAmount(BigDecimal.valueOf(305, 2))
				.withEnd2EndId("End2End-22")
				.withName("J. Jansen")
				.withRemittance("Rit van C naar D met Jän")
				.build()
				);
		SepaGroupHeader header = new SepaGroupHeader.Builder("Batch-01 0123456789012345678901234567890")
				.of(transactions)
				.withInitiatingParty("NetMobiel")
				.build();
		SepaPaymentInformation payinfo = new SepaPaymentInformation.Builder("Batch-01")
				.of(transactions)
				.withAccount("NL02ABNA0123456789")
				.withAccountHolder("Stichting Netmobiel")
				.build();
		SepaCreditTransferDocument doc = new SepaCreditTransferDocument.Builder()
				.with(header)
				.with(payinfo)
				.with(transactions)
				.build();
//		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//		Validator validator = factory.getValidator();
//		Set<ConstraintViolation<SepaCreditTransferDocument>> violations = validator.validate(doc);
		Set<ConstraintViolation<SepaCreditTransferDocument>> violations = VALIDATOR.validate(doc);
		for (ConstraintViolation<SepaCreditTransferDocument> violation : violations) {
		    log.error(violation.getPropertyPath() + " " + violation.getMessage()); 
		}
		if (violations.size() == 0) {
			log.info("No violations");
			XMLNode xml = doc.toXml();
			log.debug("\n"+ xml.toString());
		} else {
			fail("Constraint violations");
		}
	}

}
