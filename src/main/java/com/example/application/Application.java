package com.example.application;

import io.github.cdimascio.dotenv.Dotenv;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@SpringBootApplication
@Theme(value = "my-app")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        initializeFirebase();
        SpringApplication.run(Application.class, args);
    }

    private static void initializeFirebase() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                // Load environment variables from .env file
                Dotenv dotenv = Dotenv.load();
                String serviceAccountKey = dotenv.get("FIREBASE_SERVICE_ACCOUNT");

                System.out.println("FIREBASE_SERVICE_ACCOUNT: " + serviceAccountKey); // For debugging

                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountKey.getBytes())))
                        .setDatabaseUrl(dotenv.get("FIREBASE_DATABASE_URL"))
                        .setStorageBucket(dotenv.get("FIREBASE_STORAGE_BUCKET"))
                        .build();
                FirebaseApp.initializeApp(options);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }
}
