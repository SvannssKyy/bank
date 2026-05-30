
package com.chkaduuu.mcbank.models;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class BankAccount
{
    private final String accountNumber;
    private double balance;
    private double loan;
    private long loanTakenDate;
    private boolean active;
    private final long created;
    private List<String> history;
    
    public BankAccount(final String accountNumber, final double balance, final boolean active) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.loan = 0.0;
        this.loanTakenDate = 0L;
        this.active = active;
        this.created = System.currentTimeMillis();
        this.history = new ArrayList<String>();
    }
    
    public BankAccount(final String accountNumber, final double balance, final double loan, final long loanTakenDate, final boolean active, final long created, final List<String> history) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.loan = loan;
        this.loanTakenDate = loanTakenDate;
        this.active = active;
        this.created = created;
        this.history = ((history != null) ? new ArrayList<String>(history) : new ArrayList<String>());
    }
    
    public String getAccountNumber() {
        return this.accountNumber;
    }
    
    public double getBalance() {
        return this.balance;
    }
    
    public void setBalance(final double balance) {
        this.balance = balance;
    }
    
    public double getLoan() {
        return this.loan;
    }
    
    public void setLoan(final double loan) {
        this.loan = loan;
    }
    
    public long getLoanTakenDate() {
        return this.loanTakenDate;
    }
    
    public void setLoanTakenDate(final long loanTakenDate) {
        this.loanTakenDate = loanTakenDate;
    }
    
    public boolean isActive() {
        return this.active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
    }
    
    public long getCreated() {
        return this.created;
    }
    
    public List<String> getHistory() {
        return this.history;
    }
    
    public void addHistory(final String entry) {
        this.history.add(0, entry);
        if (this.history.size() > 10) {
            this.history = new ArrayList<String>(this.history.subList(0, 10));
        }
    }
}
