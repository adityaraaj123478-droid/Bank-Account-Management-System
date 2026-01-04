package com.bank.utils;

import java.io.*;
import com.bank.accounts.Account;

public class FileManager {

    public static void save(Account acc) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream("account.dat"))) {
            oos.writeObject(acc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Account load() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream("account.dat"))) {
            return (Account) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
