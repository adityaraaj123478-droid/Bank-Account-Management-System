package com.bank.accounts;

public class CheckingAccount extends Account {

    private double fee = 100;

    public CheckingAccount(int accNo, String name, double balance) {
        super(accNo, name, balance);
    }

    @Override
    public double calculateInterest() {
        return 0;
    }

    @Override
    public void applyFee() {
        balance -= fee;
    }
}
