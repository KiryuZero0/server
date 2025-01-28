// CloudSyncService.java - Sincronizare securizată cu serverul cloud
package com.example.demo3;

import okhttp3.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudSyncService {
    private static final String BASE_URL = "https://your-cloud-server.com/api/";
    private final OkHttpClient client;
    private final Gson gson;
    private static final Logger logger = Logger.getLogger(CloudSyncService.class.getName());

    public CloudSyncService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    // Upload date utilizator cu validare și logging
    public void uploadUserData(String username, Map<String, List<String>> videos, List<String> messages) {
        if (!validateUserData(username, videos, messages)) {
            logger.log(Level.WARNING, "Datele utilizatorului sunt invalide!");
            return;
        }

        UserData userData = new UserData(username, videos, messages, LocalDateTime.now().toString());
        String jsonData = gson.toJson(userData);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), jsonData
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "upload")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.log(Level.SEVERE, "Eroare la încărcarea datelor utilizatorului: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    logger.log(Level.INFO, "Datele utilizatorului au fost încărcate cu succes!");
                } else {
                    logger.log(Level.WARNING, "Eșec la încărcarea datelor utilizatorului: " + response.message());
                }
            }
        });
    }

    // Descărcare date utilizator cu logging
    public void downloadUserData(String username, Callback callback) {
        if (username == null || username.trim().isEmpty()) {
            logger.log(Level.WARNING, "Numele utilizatorului este invalid pentru descărcare!");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "download?username=" + username)
                .get()
                .build();

        client.newCall(request).enqueue(callback);
        logger.log(Level.INFO, "Cerere de descărcare trimisă pentru utilizator: " + username);
    }

    // Validare date utilizator înainte de sincronizare
    private boolean validateUserData(String username, Map<String, List<String>> videos, List<String> messages) {
        if (username == null || username.trim().isEmpty()) {
            logger.log(Level.WARNING, "Numele utilizatorului este gol sau invalid.");
            return false;
        }
        if (videos == null || videos.isEmpty()) {
            logger.log(Level.WARNING, "Lista de videoclipuri este goală sau invalidă.");
            return false;
        }
        if (messages == null) {
            logger.log(Level.WARNING, "Lista de mesaje este null.");
            return false;
        }
        return true;
    }
}
