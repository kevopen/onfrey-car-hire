package com.example.application.views.onboarding;

import com.example.application.views.MainLayout;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@PageTitle("Client Onboarding")
@Route(value = "client-onboarding", layout = MainLayout.class)
public class ClientOnboardingView extends VerticalLayout {

    private final TextField fullName = new TextField("Full Name");
    private final TextField phone = new TextField("Phone");
    private final TextField physicalAddress = new TextField("Physical Address");
    private final EmailField email = new EmailField("Email Address");
    private final TextField id = new TextField("ID");
    private final TextField kinName = new TextField("Name of Kin");
    private final TextField kinPhone = new TextField("Phone of Kin");
    private final TextField kinId = new TextField("ID of Kin");
    private final TextField kinPhysicalAddress = new TextField("Physical Address of Kin");
    private final EmailField kinEmail = new EmailField("Email of Kin");

    private final Button submitButton = new Button("Submit", e -> submitClientDetails());
    private final ProgressBar uploadProgress = new ProgressBar();
    private final Div loadingIndicator = new Div();

    private UI ui;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = attachEvent.getUI();
    }

    public ClientOnboardingView() {
        add(new H2("Client Onboarding"));

        configureForm();
        configureLoadingIndicator();

        add(uploadProgress, loadingIndicator, submitButton);
    }

    private void configureForm() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(
                fullName, phone, physicalAddress, email, id,
                kinName, kinPhone, kinId, kinPhysicalAddress, kinEmail
        );

        add(formLayout);
    }

    private void configureLoadingIndicator() {
        uploadProgress.setIndeterminate(true);
        uploadProgress.setVisible(false);

        loadingIndicator.setText("Uploading client details...");
        loadingIndicator.getStyle().set("font-style", "italic");
        loadingIndicator.setVisible(false);
    }

    private void submitClientDetails() {
        if (!validateForm()) {
            Notification.show("Please fill in all fields", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        uploadProgress.setVisible(true);
        loadingIndicator.setVisible(true);
        submitButton.setEnabled(false);

        Map<String, Object> clientData = new HashMap<>();
        clientData.put("full-name", fullName.getValue());
        clientData.put("phone", phone.getValue());
        clientData.put("physicalAddress", physicalAddress.getValue());
        clientData.put("email", email.getValue());
        clientData.put("id", id.getValue());
        clientData.put("kinName", kinName.getValue());
        clientData.put("kinPhone", kinPhone.getValue());
        clientData.put("kinId", kinId.getValue());
        clientData.put("kinPhysicalAddress", kinPhysicalAddress.getValue());
        clientData.put("kinEmail", kinEmail.getValue());

        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<DocumentReference> future = db.collection("clients").add(clientData);

        new Thread(() -> {
            try {
                DocumentReference result = future.get();
                if (ui != null) {
                    ui.access(() -> {
                        uploadProgress.setVisible(false);
                        loadingIndicator.setVisible(false);
                        submitButton.setEnabled(true);
                        Notification.show("Client details uploaded successfully!", 3000, Notification.Position.TOP_CENTER);
                        clearForm();
                    });
                }
            } catch (InterruptedException | ExecutionException e) {
                if (ui != null) {
                    ui.access(() -> {
                        uploadProgress.setVisible(false);
                        loadingIndicator.setVisible(false);
                        submitButton.setEnabled(true);
                        Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    });
                }
                e.printStackTrace();
            }
        }).start();
    }
    private boolean validateForm() {
        return !fullName.isEmpty() && !phone.isEmpty() && !physicalAddress.isEmpty() &&
                !email.isEmpty() && !id.isEmpty() && !kinName.isEmpty() && !kinPhone.isEmpty() &&
                !kinId.isEmpty() && !kinPhysicalAddress.isEmpty() && !kinEmail.isEmpty();
    }

    private void clearForm() {
        fullName.clear();
        phone.clear();
        physicalAddress.clear();
        email.clear();
        id.clear();
        kinName.clear();
        kinPhone.clear();
        kinId.clear();
        kinPhysicalAddress.clear();
        kinEmail.clear();
    }
}