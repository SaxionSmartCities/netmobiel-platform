package eu.netmobiel.banker.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountPurposeType;
import eu.netmobiel.banker.model.AccountType;

public class LedgerServiceTest {

	private Account bankingReserve;
	private Account reservations;
	private Account premiums;
	private Account user1;
	private Account user1P;
	private Account user2;
	
	@Before
	public void setUp() throws Exception {
		bankingReserve = Account.newInstant(LedgerService.ACC_REF_BANKING_RESERVE, LedgerService.ACC_NAME_BANKING_RESERVE, AccountType.ASSET, AccountPurposeType.SYSTEM);
		reservations = Account.newInstant(LedgerService.ACC_REF_RESERVATIONS, LedgerService.ACC_NAME_RESERVATIONS, AccountType.LIABILITY, AccountPurposeType.SYSTEM);
		premiums = Account.newInstant(LedgerService.ACC_REF_PREMIUMS, LedgerService.ACC_NAME_PREMIUMS, AccountType.LIABILITY, AccountPurposeType.SYSTEM);
		user1 = Account.newInstant("user1", "User 1", AccountType.LIABILITY, AccountPurposeType.CURRENT);
		user2 = Account.newInstant("user2", "User 2", AccountType.LIABILITY, AccountPurposeType.CURRENT);
		user1P = Account.newInstant("user1P", "User 1 P", AccountType.LIABILITY, AccountPurposeType.PREMIUM);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSortinbgAccounts() {
		List<Account> accounts = new ArrayList<>(List.of(user1P, user1, user2, premiums, reservations, bankingReserve));
		LedgerService.sortAccountsForLocking(accounts);
		assertEquals(accounts.get(0), bankingReserve);
		assertEquals(accounts.get(1), reservations);
		assertEquals(accounts.get(2), premiums);
		assertEquals(accounts.get(3), user1);
		assertEquals(accounts.get(4), user2);
		assertEquals(accounts.get(5), user1P);
	}

}
