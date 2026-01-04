package com.bank.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bank.accounts.*;
import com.bank.exceptions.InsufficientFundsException;
import com.bank.utils.FileManager;

public class BankGUI extends JFrame {

    // UI Components
    private JTextField nameField, initialAmountField, txnAmountField;
    private JTextArea outputArea;
    private JComboBox<String> accountTypeBox;

    // Summary labels
    private JLabel summaryName, summaryType, summaryBalance, summaryStatus;

    // Buttons
    private JButton createBtn, depositBtn, withdrawBtn,
            interestBtn, saveBtn, loadBtn, balanceBtn;

    // Model
    private Account account;
    private final List<String> transactions = new ArrayList<>();

    // Formatter
    private final DecimalFormat df = new DecimalFormat("₹#,##0.00");

    // Font & Colors
    private final Font uiFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Color buttonColor = new Color(180, 210, 255);
    private final Color headerColor = new Color(200, 220, 255);

    /* ================= ADDED STATE ================= */
    private double avgTransaction = 0;
    private int transactionCount = 0;

    private int failedWithdrawals = 0;
    private long lockUntil = 0;

    private long lastActivityTime = System.currentTimeMillis();

    public BankGUI() {
        setTitle("Bank Account Management System");
        setSize(750, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(createHeader(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        add(createOutputPanel(), BorderLayout.SOUTH);

        disableActionButtons();

        enableHiddenAuditShortcut();
        startSessionWatchdog();

        setVisible(true);
    }

    /* ---------------- HEADER ---------------- */
    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setBackground(headerColor);
        JLabel title = new JLabel("Bank Account Management System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.add(title);
        return header;
    }

    /* ---------------- MAIN PANEL ---------------- */
    private JPanel createMainPanel() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.add(createAccountPanel(), BorderLayout.NORTH);
        main.add(createButtonPanel(), BorderLayout.CENTER);
        main.add(createSummaryPanel(), BorderLayout.EAST);
        return main;
    }

    /* ---------------- ACCOUNT DETAILS ---------------- */
    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Account Details"));

        nameField = new JTextField();
        initialAmountField = new JTextField();
        txnAmountField = new JTextField();

        accountTypeBox = new JComboBox<>(new String[]{
                "Savings Account", "Checking Account"
        });

        applyFont(nameField, initialAmountField, txnAmountField, accountTypeBox);

        panel.add(new JLabel("Account Holder Name:"));
        panel.add(nameField);

        panel.add(new JLabel("Initial Amount:"));
        panel.add(initialAmountField);

        panel.add(new JLabel("Transaction Amount:"));
        panel.add(txnAmountField);

        panel.add(new JLabel("Account Type:"));
        panel.add(accountTypeBox);

        return panel;
    }

    /* ---------------- BUTTON PANEL ---------------- */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Operations"));

        createBtn = createButton("Create");
        depositBtn = createButton("Deposit");
        withdrawBtn = createButton("Withdraw");
        balanceBtn = createButton("Balance");
        interestBtn = createButton("Interest / Fee");
        saveBtn = createButton("Save");
        loadBtn = createButton("Load");

        panel.add(createBtn);
        panel.add(depositBtn);
        panel.add(withdrawBtn);
        panel.add(balanceBtn);
        panel.add(interestBtn);
        panel.add(saveBtn);
        panel.add(loadBtn);

        attachButtonLogic();
        return panel;
    }

    /* ---------------- SUMMARY PANEL ---------------- */
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Account Summary"));

        summaryName = new JLabel("Name: -");
        summaryType = new JLabel("Type: -");
        summaryBalance = new JLabel("Balance: -");
        summaryStatus = new JLabel("Status: Inactive");

        applyFont(summaryName, summaryType, summaryBalance, summaryStatus);

        panel.add(summaryName);
        panel.add(summaryType);
        panel.add(summaryBalance);
        panel.add(summaryStatus);

        return panel;
    }

    /* ---------------- OUTPUT PANEL ---------------- */
    private JScrollPane createOutputPanel() {
        outputArea = new JTextArea(6, 20);
        outputArea.setEditable(false);
        outputArea.setFont(uiFont);

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Transaction History"));
        return scroll;
    }

    /* ---------------- BUTTON LOGIC ---------------- */
    private void attachButtonLogic() {

        createBtn.addActionListener(e -> {
            lastActivityTime = System.currentTimeMillis();
            try {
                String name = nameField.getText();
                double initAmt = parseAmount(initialAmountField.getText());

                account = accountTypeBox.getSelectedIndex() == 0
                        ? new SavingsAccount(101, name, initAmt)
                        : new CheckingAccount(102, name, initAmt);

                transactions.clear();
                transactions.add("Account created with " + df.format(initAmt));

                enableActionButtons();
                updateSummary();
                logTransactions();
                clearCreateFields();

            } catch (Exception ex) {
                error("Enter valid account details");
            }
        });

        depositBtn.addActionListener(e -> {
            lastActivityTime = System.currentTimeMillis();

            validateAccountName(); // ✅ ADDED

            double amt = parseAmount(txnAmountField.getText());
            detectAnomalousTransaction(amt);

            account.deposit(amt);
            transactions.add("Deposited " + df.format(amt));

            updateSummary();
            logTransactions();
            clearTxnField();
        });

        withdrawBtn.addActionListener(e -> {
            lastActivityTime = System.currentTimeMillis();
            try {
                checkAccountLock();
                validateAccountName(); // ✅ ADDED

                double amt = parseAmount(txnAmountField.getText());

                if (amt > account.getBalance()) { // ✅ ADDED
                    throw new InsufficientFundsException(
                            "Insufficient balance.\nRequested: " +
                                    df.format(amt) +
                                    "\nAvailable: " +
                                    df.format(account.getBalance())
                    );
                }

                account.withdraw(amt);
                transactions.add("Withdrawn " + df.format(amt));

                updateSummary();
                logTransactions();
                clearTxnField();

            } catch (InsufficientFundsException ex) {
                failedWithdrawals++;

                if (failedWithdrawals >= 3) {
                    lockUntil = System.currentTimeMillis() + (2 * 60 * 1000);
                    transactions.add("🔒 Account locked for 2 minutes due to failures");
                }
                error(ex.getMessage());
            }
        });
    }

    /* ---------------- HELPERS ---------------- */
    private void validateAccountName() {
        if (!nameField.getText().equals(account.getHolderName())) {
            throw new RuntimeException("Account name mismatch");
        }
    }

    private double parseAmount(String text) {
        try {
            double val = Double.parseDouble(text);
            if (val <= 0) throw new Exception();
            return val;
        } catch (Exception e) {
            throw new RuntimeException("Invalid amount");
        }
    }

    private void updateSummary() {
        summaryName.setText("Name: " + account.getHolderName());
        summaryType.setText("Type: " + account.getClass().getSimpleName());
        summaryBalance.setText("Balance: " + df.format(account.getBalance()));
        summaryStatus.setText("Status: Active");

        withdrawBtn.setEnabled(account.getBalance() > 0);
        saveBtn.setEnabled(account != null);
    }

    private void logTransactions() {
        outputArea.setText("");
        for (String t : transactions) {
            outputArea.append(t + "\n");
        }
    }

    private void clearCreateFields() {
        nameField.setText("");
        initialAmountField.setText("");
    }

    private void clearTxnField() {
        txnAmountField.setText("");
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(uiFont);
        btn.setBackground(buttonColor);
        btn.setFocusPainted(false);
        return btn;
    }

    private void applyFont(JComponent... comps) {
        for (JComponent c : comps) c.setFont(uiFont);
    }

    private void disableActionButtons() {
        depositBtn.setEnabled(false);
        withdrawBtn.setEnabled(false);
        interestBtn.setEnabled(false);
        balanceBtn.setEnabled(false);
        saveBtn.setEnabled(false);
    }

    private void enableActionButtons() {
        depositBtn.setEnabled(true);
        withdrawBtn.setEnabled(true);
        interestBtn.setEnabled(true);
        balanceBtn.setEnabled(true);
        saveBtn.setEnabled(true);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    /* ================= ADDED FEATURES ================= */

    private void detectAnomalousTransaction(double amount) {
        if (transactionCount >= 3 && amount > avgTransaction * 4) {
            transactions.add("⚠ Anomalous transaction detected: " + df.format(amount));
        }
        avgTransaction = ((avgTransaction * transactionCount) + amount) / (++transactionCount);
    }

    private void checkAccountLock() {
        if (System.currentTimeMillis() < lockUntil) {
            throw new RuntimeException("Account temporarily locked");
        }
    }

    private void startSessionWatchdog() {
        Timer timer = new Timer(60000, e -> {
            if (System.currentTimeMillis() - lastActivityTime > 5 * 60 * 1000) {
                if (account != null) {
                    FileManager.save(account);
                }
                account = null;
                outputArea.setText("");
                disableActionButtons();

                JOptionPane.showMessageDialog(
                        this,
                        "Session expired due to inactivity.\nAccount auto-saved.",
                        "Session Timeout",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
        timer.start();
    }

    /* ================= UNIQUE FEATURE ================= */
    private void enableHiddenAuditShortcut() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED
                            && e.isControlDown()
                            && e.isShiftDown()
                            && e.getKeyCode() == KeyEvent.VK_A) {
                        showAuditDialog();
                        return true;
                    }
                    return false;
                });
    }

    private void showAuditDialog() {
        if (account == null) {
            JOptionPane.showMessageDialog(this,
                    "No account loaded for audit.",
                    "Audit",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String auditId = "AUD-" + System.currentTimeMillis();

        String report =
                "🔐 ADMIN AUDIT REPORT\n\n" +
                "Account Holder : " + account.getHolderName() + "\n" +
                "Account Type   : " + account.getClass().getSimpleName() + "\n" +
                "Balance        : " + df.format(account.getBalance()) + "\n" +
                "Transactions   : " + transactions.size() + "\n" +
                "Audit Time     : " + LocalDateTime.now() + "\n" +
                "Audit ID       : " + auditId;

        JOptionPane.showMessageDialog(this,
                report,
                "Secure Audit Mode",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
