package eu.netmobiel.banker.model;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.banker.test.Fixture;

public class AccountingTransactionTest {

	private Ledger ledger;
    private Account account1;
    private Account account2;
    private Account account3;
    private Account assetAccount;
    private Balance balance1;
    private Balance balance2;
    private Balance balance3;
    private Balance assetBalance;

	@Before
	public void createLedger() {
		String startTime = "2020-01-01T01:00:00Z";
		ledger = new Ledger();
    	ledger.setStartPeriod(Instant.parse(startTime));
    	ledger.setName(String.format("%d", ledger.getStartPeriod().atOffset(ZoneOffset.UTC).getYear()));
        User user1 = new User("U1", "A", "Family U1", null);
        User user2 = new User("U2", "B", "Family U2", null);
        User user3 = new User("U3", "C", "Family U3", null);
    	account1 = Fixture.createAccount("account-1", user1.createAccountName(), AccountType.LIABILITY);
    	account2 = Fixture.createAccount("account-2", user2.createAccountName(), AccountType.LIABILITY); 
    	account3 = Fixture.createAccount("account-3", user3.createAccountName(), AccountType.LIABILITY); 
    	assetAccount = Fixture.createAccount("bank", "Bank", AccountType.ASSET); 
        balance1 = new Balance(ledger, account1, 100); 
        balance2 = new Balance(ledger, account2, 200); 
        balance3 = new Balance(ledger, account3, 0); 
        assetBalance = new Balance(ledger, assetAccount, 0); 
	}

	@Test
	public void testTransaction_Balanced() {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		AccountingTransaction tr = ledger.createTransaction(TransactionType.PAYMENT, description, reference, acctime, trtime)
				.debit(balance1, 10, balance2.getAccount().getName())
				.credit(balance2, 10, balance1.getAccount().getName())
				.build();
		assertNotNull(tr);
		assertEquals(description, tr.getDescription());
		assertEquals(acctime, tr.getAccountingTime());
		assertEquals(trtime, tr.getTransactionTime());
		assertEquals(2, tr.getAccountingEntries().size());
	}

	@Test
	public void testTransaction_LessThan2() {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		try {
			ledger.createTransaction(TransactionType.PAYMENT, description, reference, acctime, trtime)
					.debit(balance1, 10, null)
					.build();
			fail("Expected IllegalStateException");
		} catch (IllegalStateException ex) {
		}
	}
	
	@Test
	public void testTransaction_NotBalanced() {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		try {
			ledger.createTransaction(TransactionType.PAYMENT, description, reference, acctime, trtime)
					.debit(balance1, 10, balance2.getAccount().getName())
					.credit(balance2, 100, balance1.getAccount().getName())
					.build();
			fail("Expected IllegalStateException");
		} catch (IllegalStateException ex) {
		}
	}

	@Test
	public void testTransaction_BalancedThree() {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		// Counterparty does not match well wirh multi-leg transactions.
		ledger.createTransaction(TransactionType.PAYMENT, description, reference, acctime, trtime)
					.debit(balance1, 10, balance3.getAccount().getName())
					.debit(balance2, 10, balance3.getAccount().getName())
					.credit(balance3, 20, null)
					.build();
	}
	
	@Test
	public void testTransaction_Deposit() {
		String description = "description-1";
		String reference = null;
		Instant acctime = Instant.parse("2020-08-01T01:00:00Z");
		Instant trtime = Instant.now();
		int oldAmount = balance1.getEndAmount();
		ledger.createTransaction(TransactionType.DEPOSIT, description, reference, acctime, trtime)
					.debit(assetBalance, 100, balance1.getAccount().getName())
					.credit(balance1, 100, null)
					.build();
		assertEquals(100, assetBalance.getEndAmount());
		assertEquals(100, balance1.getEndAmount() - oldAmount);
	}
}
