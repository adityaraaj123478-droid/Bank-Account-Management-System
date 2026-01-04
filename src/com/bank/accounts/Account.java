package com.bank.accounts;

import java.io.Serializable;
import com.bank.exceptions.InsufficientFundsException;

public abstract class Account implements Serializable {

    private final int accountNumber;
    private String holderName;
    protected double balance;

    public Account(int accountNumber, String holderName, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
    }

    public final int getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public double getBalance() {
        return balance;
    }

    // Overloaded methods
    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount > balance)
            throw new InsufficientFundsException("❌ Insufficient Balance");
        balance -= amount;
    }

    // Abstraction
    public abstract double calculateInterest();
    public abstract void applyFee();
}
