package com.example.demo3;

import okhttp3.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CloudSyncService {
    private static final String BASE_URL = "https://your-cloud-server.com/api/";
    private final OkHttpClient client;
    private final Gson gson;

    public CloudSyncService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    // Upload date utilizator
    public void uploadUserData(String username, Map<String, List<String>> videos, List<String> messages) {
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
                System.out.println("Failed to upload user data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("User data uploaded successfully!");
                } else {
                    System.out.println("Failed to upload user data: " + response.message());
                }
            }
        });
    }

    // Download date utilizator
    public void downloadUserData(String username, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "download?username=" + username)
                .get()
                .build();

        client.newCall(request).enqueue(callback);
    }
}
