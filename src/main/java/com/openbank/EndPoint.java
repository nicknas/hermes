package com.openbank;

import com.openbank.util.VariablesExpander;

public enum EndPoint {
    BASE(getBase()),

    // customer endpoints
    CUSTOMERS(BASE, "/customers"),
    CUSTOMER(CUSTOMERS, "/:customerId"),
    AMEND_CUSTOMER(CUSTOMER, "/update"),
    INVALIDCUSTOMERS(BASE, "/customerss"),
    AMEND_INVAIDURL(BASE, "/updates"),

    // account endpoints
    ACCOUNTS(BASE, "/accounts"),
    INVALIDLISTALLACCOUNTS(ACCOUNTS,"?customerd="),
    ACCOUNT(ACCOUNTS, "/:accountId"),
    LISTALLACCOUNTS(ACCOUNTS,"?customerId=:customerId"),
    INVALIDACCOUNT(INVALIDLISTALLACCOUNTS, ":customerId"),
    ACCOUNT_UPDATE_ACCOUNT(ACCOUNT, "/update-account"),
    ACCOUNT_UPDATE_PRODUCT(ACCOUNT, "/update-product"),
    ACCOUNT_UPDATE_PARTICIPANTS(ACCOUNT, "/update-participants"),
    INVALID_ACCOUNT_UPDATE_PARTICIPANTS(ACCOUNT, "/update-participan"),
    ACCOUNT_UPDATE_LIMIT(ACCOUNT,"/update-limit"),
  	TRANSACTIONS_SEARCH_FROM_TO_AMOUNTS(BASE, "/transactions?fromAmount=:FromAmount&toAmount=:ToAmount&accountId=:AccountId"),
  	TRANSACTIONS_SEARCH_TXNID(BASE,"/transactions?accountId=:AccountId&transactionTypeTCode=:txnId"),
  	TRANSACTIONS_SEARCH_FROM_TO_DATE(BASE, "/transactions?accountId=:accountId&fromDate=:fromDate&toDate=:toDate"),
	ACCOUNT_SEARCH_SETTLEMENT(ACCOUNT, "/settlements"),
  	ACCOUNT_SETTLEMENT_PERIOD(ACCOUNT, "/settlement-periods"),
  	ACCOUNT_LOCKED_AMOUNT(ACCOUNT, "/locked-amounts"),
  	BIC(BASE, "/bics"),
  	BICID(BIC,"/:BICId"),
    
    //chequebooks
    CHEQUEBOOKS_LIST(ACCOUNTS, "/:account_customerId/chequebooks/:accountId"),
    CHEQUEBOOKS(ACCOUNT, "/chequebooks"),
    INVALID_CHEQUEBOOKS(ACCOUNT, "/chequeboo"),
    CHEQUEBOOKS_SPEC(ACCOUNT, "/chequeBooks"),
    IBANGENERATOR(ACCOUNTS,"/generate-next-IBAN"),
    VALIDATEPARTICIPANTS(ACCOUNT,"/participants"),
    CHEQUEBOOKS_CANCEL(CHEQUEBOOKS,"/cancel"),
    INVALIDCHEQUEBOOKS_CANCEL(INVALID_CHEQUEBOOKS,"/cancel"),
	
	    //Teller
    TELLER(BASE,"/tellers"),
    
    //Teller Transactions
    TELLERTRANSACTION(BASE,"/teller-transactions"),
    CASHTRANSACTION(TELLERTRANSACTION,"/:tellerId/cash-transaction"),
  	TELLERTRANSACTIONS_CASH(TELLERTRANSACTION,"/:accountId/cash-transaction"),

    
    //Teller Transactions
  	TELLERTRANSACTIONS(BASE,"/teller-transactions/:accountId/cash-transaction"),
  	// Transactions
  	LISTACCOUNTTRANSACTIONS(BASE,"/transactions?accountId=:accountId"),
  	VIEWSPECIFICTRANSACTION(BASE,"/transactions/:recordID"),
    VIEWTRANSACTION(BASE,"/transactions"),
    UPDATECATEGORYTRANSACTION(BASE,"/transactions/:accountId/update-category"),
    
  	//Global Positions
  	GLOBALPOSITION(BASE,"/global-position?customerId="),
  	GLOBALPOSITION_CUSTOMER(GLOBALPOSITION,":customerId"),
	//CHEQUEBOOKS_SPEC(ACCOUNTS, "/chequeBooks");
	
	//Deposits
	DEPOSITS(BASE,"/deposits"),
	DEPOSIT(DEPOSITS, "/:depositId"),
	DEPOSIT_REDEEM(DEPOSIT,"/redeem"),
	DEPOSIT_UPDATE_PRODUCT(DEPOSIT, "/change-product"),
	DEPOSIT_UPDATE_DEPOSIT(DEPOSIT, "/update-deposit"),
	DEPOSIT_REDEEM_DEPOSIT(DEPOSIT, "/redeem"),
	DEPOSIT_UPDATE_PARTICIPANTS(DEPOSIT, "/update-participants"),
	GLOBAL_POSITION(BASE,"/global-position"),
	DEPOSIT_UPDATE_SETTLEMENT(DEPOSIT,"/update-settlement"),
	DEPOSIT_UPDATE_PARTICIPANT(DEPOSIT,"/update-participants"),
	DEPOSIT_SEARCH_PARTICIPANT(DEPOSIT, "/participants"),
	DEPOSIT_SEARCH_SETTLEMENT(DEPOSIT, "/settlements"),
	DEPOSIT_SETTLEMENT_PERIOD(DEPOSIT, "/settlement-periods"),
		//globalPosition endpoints
	
	GLOBALPOSITION_DEPOSIT(BASE,"/global-position?customerId=:customerId"),

	// loans endpoints
    LOANS(BASE, "/loans"),
    LOANSSIMULATION(LOANS, "/simulations"),
    LISTALLLOANSSIMULATION(LOANSSIMULATION,"?customerId=:param"),
    LISTLOANSIMULATIONDETAIL(LOANSSIMULATION,"/:param"),
    LISTLOANSIMULATIONSCHEDULE(LOANSSIMULATION,"/:param/schedules"),
    
    EXECUTELOANSIMULATION(LOANSSIMULATION,"/:param/execute"),
    LIVELOANDETAIL(LOANS,"/:param"),
    LIVELOANSCHEDULE(LOANS,"/:param/schedules"),
    LISTALLLOANS(LOANS,"?customerId=:param"),
    
    COLLATERALS(BASE,"/collaterals"),
    
    // rates endpoints
    CONVERSIONRATES(BASE, "/miscellaneous/convert-rate/?fromCurrencyTCode=:fromCurrency&toCurrencyTCode=:toCurrency&amount=:amnt"),
    EXCHANGERATES(BASE, "/currencies/exchange-rate/?currencyTCode=:param"),
    CONVERSIONRATES_(BASE, "/miscellaneous/convert-rate/?fromCurrencyTCode=:fromCurrency&amount=:amnt"),
    
	//Beneficiary
    BENEFICIARY(BASE, "/beneficiaries"),
    BENEFICIARIES(BENEFICIARY, "/:beneficiaryId"),
	AMEND_BENEFICIARY(BENEFICIARIES, "/update"),
	DELETE_BENEFICIARY(BENEFICIARIES, "/delete"),
	DELETE_BENEFICIARY1(BENEFICIARIES, "delete"),
	
	// user endpoints
    USER(BASE, "/users"),
    USERUPDATE(USER, "/:param/update"),
    USERSEARCH(USER, "/:param"),
    USERDELETE(USER, "/:param/delete"),
    
	//Cards
		CARDS(BASE, "/cards"),
		CARDS_CARDID(CARDS, "/:cardId"),
		CARDS_AMEND(CARDS_CARDID, "/updates"),
		CARDS_AMEND_INITIATE(CARDS_AMEND, "/initiate"),
		CARDS_AMEND_UPDATEID(CARDS_AMEND, "/:initiateId"),
		CARDS_AMEND_CONFIRM(CARDS_AMEND_UPDATEID, "/confirm"),
		CARDS_BLOCKREISSUE(CARDS_CARDID, "/block-reissues"),
		CARDS_BLOCKREISSUE_INITIATE(CARDS_BLOCKREISSUE, "/initiate"),
		CARDS_BLOCKREISSUE_INITIATEID(CARDS_BLOCKREISSUE,"/:initiateId"),
		CARDS_BLOCKREISSUE_CONFIRM(CARDS_BLOCKREISSUE_INITIATEID,"/confrim"),
		LOG_TRANSACTION(CARDS,"/:primaryCardID"),
		CARDCONTRACT(BASE, "/card-contracts"),
		INITIATE_ADDON_CARD(CARDCONTRACT,"/:cardContractId/additional-cards/initiate"),
		CONFIRM_ADDON_CARD(CARDCONTRACT,"/:cardContractId/additional-cards/:addOnCardId/confirm"),
		INITIATE_CONTROLS_CARD(CARDS,"/:primaryCardID/controls/initiate"),
		CONFIRM_CONTROLS_CARD(CARDS,"/:primaryCardID/controls/:controlUpdateId/confirm"),
		AMEND_CARD(CARDS,"/:primaryCardID/updates/initiate"),
		AMEND_CARD_CONFIRM(CARDS,"/:primaryCardID/updates/:activationId/confirm"),
	    CARDCREATE_INITIATE(BASE, "/card-contracts/initiate"),
	    CARDID(BASE, "/card-contracts/:cardId"),
	    CARDCREATE_CONFIRM(CARDID, "/confirm"),
	    GET_CARD_DETAILS(BASE, "/cards/:cardId"),
	    CARD_UPDATE(GET_CARD_DETAILS, "/updates"),
	    CARD_UPDATE_INITATE(CARD_UPDATE, "/:cardId/initiate"),
	    CARD_UPDATE_CONFRIM(CARD_UPDATE_INITATE, "/:cardId/confirm"),
	    CARDBASE(getCardBase()),
	    CARD(BASE,"/cards"),
		E2ECARDS(CARDBASE,"/cards"),
		CMSCONTRACT(CARDBASE,"/card-contracts"),
		CARDS_BLOCKREPLACE(CARDS_CARDID, "/block-replace"),
		CARDS_BLOCKREPLACES(CARDS_CARDID, "/block-replaces"),
		CARDS_BALANCETRANSFER(CARDS_CARDID, "/transfer"),
		CARDS_BALANCETRANSFERS(CARDS_CARDID, "/transfers"),
		LOAD_PREPAID_CARD(CARDS_CARDID, "/load"),
		LOAD_PREPAID_CARD_INVALIDURL(CARDS_CARDID, "/loads"),
	
	
	//Tabels
		TABLES(BASE, "/tables"),
		TABLE(TABLES, "/:tCode"),
		TABLES_PARAM(TABLES, "?page_size=:pageSize"),
		TABLE_PARAM(TABLES, "/:tCode?page_size=:pageSize"),
		
	//SSL
        BASESSL(getSSLBase()),
        BASEAUTHZSSL(getSSLAuthzBase()),
        LOGINACCESSTOKEN(BASESSL, "/auth/login"),
	    LOGINVALIDATETOKEN(BASESSL, "/auth/tokens/validate"),
	    LOGINPASSWORDPOSITION(BASESSL, "/auth/users/password-positions"),
	    INITIATECHALLANGE(BASEAUTHZSSL, "/authorize/init");
	
	
    private String url;

    
    private static String getSSLBase() {
        return exp("${" + exp("${country}.${env}.sslUrl") + "}");
	}
    
    private static String getSSLAuthzBase() {
        return exp("${" + exp("${country}.${env}.sslAuthUrl") + "}");
	}
    
    EndPoint(String url) {
        this.url = url;
    }

    EndPoint(EndPoint parent, String url) {
        this.url = parent.getUrl() + url;
    }

    private static String getBase() {
		String exp = exp("${" + exp("${country}.${env}.baseUrl") + "}");
        return exp;
    }

    private static String exp(String exp) {
        return VariablesExpander.get().replace(exp);
    }
    
    private static String getCardBase() {
        return exp("${" + exp("${country}.${env}.cardUrl") + "}");
	}

    public String getUrl() {
        return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
