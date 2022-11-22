package eu.netmobiel.banker.model;

/**
 * Generalized model of account types. For Netmobiel we need only Assets and Liability accounts.
 * Definitions of debit/credit for the different types of accounts
 * Account 	To Increase Balance 	To Decrease Balance
 *  Assets 	   		Debit 				Credit
 *  Liabilities 	Credit 				Debit
 *	Revenue 		Credit 				Debit
 *	Expenses 		Debit 				Credit
 *	Equity 			Credit 				Debit
 * 
 * @author Jaap Reitsma
 *
 */
public enum AccountType {
	ASSET("A"),
	LIABILITY("L");
//	REVENUE("R"),
//	EXPENSE("X"),
//	EQUITY("E");
	
	private String code;
	 
    private AccountType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
