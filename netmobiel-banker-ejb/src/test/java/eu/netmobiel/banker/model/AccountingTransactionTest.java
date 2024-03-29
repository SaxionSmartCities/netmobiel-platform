package eu.netmobiel.banker.model;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.banker.exception.BalanceInsufficientException;

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
        BankerUser user1 = new BankerUser("U1", "A", "Family U1", null);
        BankerUser user2 = new BankerUser("U2", "B", "Family U2", null);
        BankerUser user3 = new BankerUser("U3", "C", "Family U3", null);
    	account1 = Account.newInstant("account-1", user1.createAccountName(), AccountType.LIABILITY, AccountPurposeType.CURRENT);
    	account2 = Account.newInstant("account-2", user2.createAccountName(), AccountType.LIABILITY, AccountPurposeType.CURRENT); 
    	account3 = Account.newInstant("account-3", user3.createAccountName(), AccountType.LIABILITY, AccountPurposeType.CURRENT); 
    	assetAccount = Account.newInstant("bank", "Bank", AccountType.ASSET, AccountPurposeType.SYSTEM); 
        balance1 = new Balance(ledger, account1, 100); 
        balance2 = new Balance(ledger, account2, 200); 
        balance3 = new Balance(ledger, account3, 0); 
        assetBalance = new Balance(ledger, assetAccount, 0); 
	}

	@Test
	public void testTransaction_Balanced() throws BalanceInsufficientException {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		AccountingTransaction tr = ledger.createStartTransaction(description, reference, acctime, trtime)
				.debit(balance1, 10, TransactionType.PAYMENT, balance2.getAccount())
				.credit(balance2, 10, TransactionType.PAYMENT, balance1.getAccount())
				.build();
		assertNotNull(tr);
		assertEquals(description, tr.getDescription());
		assertEquals(acctime, tr.getAccountingTime());
		assertEquals(trtime, tr.getTransactionTime());
		assertEquals(2, tr.getAccountingEntries().size());
	}

	@Test
	public void testTransaction_LessThan2() throws BalanceInsufficientException {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		try {
			ledger.createStartTransaction(description, reference, acctime, trtime)
					.debit(balance1, 10, TransactionType.PAYMENT, null)
					.build();
			fail("Expected IllegalStateException");
		} catch (IllegalStateException ex) {
			// Anticipated
		}
	}
	
	@Test
	public void testTransaction_NotBalanced() throws BalanceInsufficientException {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		try {
			ledger.createStartTransaction(description, reference, acctime, trtime)
					.debit(balance1, 10, TransactionType.PAYMENT, balance2.getAccount())
					.credit(balance2, 100, TransactionType.PAYMENT, balance1.getAccount())
					.build();
			fail("Expected IllegalStateException");
		} catch (IllegalStateException ex) {
			// Anticipated
		}
	}

	@Test
	public void testTransaction_BalancedThree() throws BalanceInsufficientException {
		String description = "description-1";
		String reference = "reference-1";
		Instant acctime = Instant.parse("2020-01-01T01:00:00Z");
		Instant trtime = Instant.now();
		int oldAmount1 = balance1.getEndAmount();
		int oldAmount2 = balance2.getEndAmount();
		int oldAmount3 = balance3.getEndAmount();
		// Counterparty does not match well with multi-leg transactions.
		ledger.createStartTransaction(description, reference, acctime, trtime)
					.debit(balance1, 10, TransactionType.PAYMENT, balance3.getAccount())
					.debit(balance2, 10, TransactionType.PAYMENT, balance3.getAccount())
					.credit(balance3, 20, TransactionType.PAYMENT, balance1.getAccount())
					.build();
		assertEquals(oldAmount1 - 10, balance1.getEndAmount());
		assertEquals(oldAmount2 - 10, balance2.getEndAmount());
		assertEquals(oldAmount3 + 20, balance3.getEndAmount());
	}
	
	@Test
	public void testTransaction_Deposit() throws BalanceInsufficientException {
		String description = "description-1";
		String reference = null;
		Instant acctime = Instant.parse("2020-08-01T01:00:00Z");
		Instant trtime = Instant.now();
		int oldAmount = balance1.getEndAmount();
		ledger.createStartTransaction(description, reference, acctime, trtime)
					.debit(assetBalance, 100, TransactionType.DEPOSIT, balance1.getAccount())
					.credit(balance1, 100, TransactionType.DEPOSIT, assetBalance.getAccount())
					.build();
		assertEquals(100, assetBalance.getEndAmount());
		assertEquals(100, balance1.getEndAmount() - oldAmount);
	}
}
