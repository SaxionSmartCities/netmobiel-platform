package eu.netmobiel.banker.test;

import java.time.Instant;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.commons.model.GeoLocation;

public class Fixture {

	public static final GeoLocation placeHengeloStation = GeoLocation.fromString("Hengelo NS Station::52.260977,6.7931087");// Bij metropool
	public static final GeoLocation placeZieuwent = GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835");
	public static final GeoLocation placeSlingeland = GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741");
	public static final GeoLocation placeRaboZutphen = GeoLocation.fromString("Rabobank Zutphen::52.148125, 6.196966");
	public static final GeoLocation placeRozenkwekerijZutphen = GeoLocation.fromString("Hoveniersweg 9-5 Zutphen::52.146734,6.174644");
	public static final GeoLocation placeZieuwentRKKerk = GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542");
	public static final GeoLocation placeThuisLichtenvoorde  = GeoLocation.fromString("Rapenburgsestraat Lichtenvoorde::51.987757,6.564012");
	public static final GeoLocation placeCentrumDoetinchem = GeoLocation.fromString("Catharina Parkeergarage Doetinchem::51.9670528,6.2894002");

	private Fixture() {
		// No instances allowed
	}

	public static BankerUser createUser(String identity, String givenName, String familyName, String email) {
		return new BankerUser(identity, givenName, familyName, email);
	}
	
	public static BankerUser createUser(LoginContext loginContext) {
        Subject subject = loginContext.getSubject();
        @SuppressWarnings("rawtypes")
		Set<KeycloakPrincipal> ps = subject.getPrincipals(KeycloakPrincipal.class);
        @SuppressWarnings("unchecked")
		KeycloakPrincipal<KeycloakSecurityContext> p = ps.iterator().next();
        return createUser(p.getKeycloakSecurityContext().getToken());
	}

	public static BankerUser createUser(AccessToken token) {
		return new BankerUser(token.getSubject(), token.getGivenName(), token.getFamilyName(), token.getEmail());
	}

	public static BankerUser createDriver1() {
		return createUser("ID1", "Carla1", "Netmobiel", null);
	}
	
	public static BankerUser createDriver2() {
		return createUser("ID2", "Carla2", "Netmobiel", null);
	}
	public static BankerUser createDriver3() {
		return createUser("ID3", "Carla3", "Netmobiel", null);
	}

	public static BankerUser createPassenger1() {
		return createUser("IP1", "Simon1", "Netmobiel", null);
	}
	
	public static BankerUser createPassenger2() {
		return createUser("IP2", "Simon2", "Netmobiel", null);
	}

    public static Ledger createLedger(String name, String startTimeIso, String endTimeIso) {
    	Instant startPeriod = Instant.parse(startTimeIso);
    	Instant endPeriod = endTimeIso != null ? Instant.parse(endTimeIso) : null;
    	Ledger ledger = new Ledger();
    	ledger.setName(name);
    	ledger.setStartPeriod(startPeriod);
    	ledger.setEndPeriod(endPeriod);
    	return ledger;
    }

    public static Account createLiabilityAccount(String ncan, String name, Instant creationTime) {
    	return Account.newInstant(ncan, name, AccountType.LIABILITY, creationTime);
    }

	public static Charity createCharity(Account account, String name, String description, int donatedAmount, int goalAmount, GeoLocation location, String imageUrl) {
		Charity ch = new Charity();
    	ch.setAccount(account);
    	ch.setName(name);
    	ch.setDescription(description);
    	ch.setDonatedAmount(donatedAmount);
    	ch.setGoalAmount(goalAmount);
    	ch.setLocation(location);
    	ch.setImageUrl(imageUrl);
    	ch.setCampaignStartTime(account.getCreatedTime().plusSeconds(3600));
    	return ch;
	}
	
	public static Donation createDonation(Charity charity, BankerUser user, String description, int amount, Instant donationTime, boolean anonymous) {
		Donation d = new Donation();
		d.setAmount(amount);
		d.setAnonymous(anonymous);
		d.setCharity(charity);
		d.setDescription(description);
		d.setDonationTime(donationTime);
		d.setUser(user);
		return d;
	}
	
	public static WithdrawalRequest createWithdrawalRequest(Account account, BankerUser requestor, String description, int amount, AccountingTransaction transaction) {
		WithdrawalRequest wr = new WithdrawalRequest();
		wr.setAccount(account);
		wr.setAmountCredits(amount);
		wr.setAmountEurocents(amount * 19);
		wr.setCreationTime(Instant.now());
		wr.setDescription(description);
		wr.setCreatedBy(requestor);
		wr.setStatus(PaymentStatus.ACTIVE);
		wr.setTransaction(transaction);
		return wr;
	}

	public static PaymentBatch createPaymentBatch(BankerUser requestor) {
    	PaymentBatch pb = new PaymentBatch();
    	pb.setCreatedBy(requestor);
    	pb.setCreationTime(Instant.now());
    	return pb;
	}

}
