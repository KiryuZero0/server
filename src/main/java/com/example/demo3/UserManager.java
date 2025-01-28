
package com.example.demo3;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String USERS_FILE = "src/main/resources/users.txt";
    private Map<String, String> users = new HashMap<>();

    public UserManager() {
        loadUsers();
    }

    // Înregistrează un utilizator nou cu parolă criptată
    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false; // Utilizatorul există deja
        }
        users.put(username, hashPassword(password));
        saveUsers();
        return true;
    }

    // Autentificare utilizator cu verificarea parolei criptate
    public boolean authenticateUser(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(hashPassword(password));
    }

    // Încărcare utilizatori din fișier
    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("Nu s-au găsit date despre utilizatori: " + e.getMessage());
        }
    }

    // Salvare utilizatori în fișier
    private void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (Map.Entry<String, String> entry : users.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Eroare la salvarea datelor utilizatorilor: " + e.getMessage());
        }
    }

    // Criptare parolă folosind SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 nu este suportat pe această platformă!", e);
        }
    }
}
