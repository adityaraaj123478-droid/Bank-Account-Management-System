package com.bank.accounts;

public class SavingsAccount extends Account {

    private double interestRate = 0.04;

    public SavingsAccount(int accNo, String name, double balance) {
        super(accNo, name, balance);
    }

    @Override
    public double calculateInterest() {
        return balance * interestRate;
    }

    @Override
    public void applyFee() {
       
    }
}
