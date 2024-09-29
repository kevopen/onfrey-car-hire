package com.example.application.views.managecars;

import com.example.application.views.MainLayout;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.example.application.Application.getFirestore;

@PageTitle("Manage Cars")
@Route(value = "manage-cars", layout = MainLayout.class)
public class ManageCarsView extends VerticalLayout {
    private static final String BUCKET_NAME = "booking-portal-bf962.appspot.com";
    private final TextField plateNumber = new TextField("Plate Number");
    private final TextField engineNumber = new TextField("Engine Number");
    private final TextField chassisNumber = new TextField("Chassis Number");
    private final TextField yearOfManufacture = new TextField("Year of Manufacture");
    private final TextField carModel = new TextField("Car Model");
    private final Select<String> inspectionStatus = new Select<>();
    private final DatePicker inspectionDate = new DatePicker("Inspection Date");
    private final DatePicker nextDueInspectionDate = new DatePicker("Next Due Inspection Date");
    private final TextField insuranceCompany = new TextField("Insurance Company");
    private final TextField insuranceBroker = new TextField("Insurance Broker");
    private final Select<String> insuranceType = new Select<>();
    private final TextField commissionStructure = new TextField("Commission Structure");
    private final Select<String> paymentMethod = new Select<>();
    private final TextField mpesaNumber = new TextField("MPESA Number");
    private final TextField bankName = new TextField("Bank Name");
    private final TextField bankAccountNumber = new TextField("Bank Account Number");
    private final TextArea salesDescription = new TextArea("About the Car (Sales Description)");
    private final TextArea originalInspectionNotes = new TextArea("Original Inspection Notes");

    private final MemoryBuffer logbookBuffer = new MemoryBuffer();
    private final MemoryBuffer insuranceBuffer = new MemoryBuffer();
    private final MemoryBuffer agreementBuffer = new MemoryBuffer();

    private final Upload logbookUpload = new Upload(logbookBuffer);
    private final Upload insuranceUpload = new Upload(insuranceBuffer);
    private final Upload agreementUpload = new Upload(agreementBuffer);

    private final ProgressBar uploadProgress = new ProgressBar();
    private final Button submitButton = new Button("Submit", event -> submitCarDetails());

    public ManageCarsView() {
        add(new H2("Add New Car"));

        configureForm();

        uploadProgress.setWidth("100%");
        uploadProgress.setVisible(false);

        add(uploadProgress, submitButton);
    }

    private void configureForm() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        inspectionStatus.setLabel("Inspection Status");
        inspectionStatus.setItems("Yes", "No");
        inspectionStatus.addValueChangeListener(event -> {
            boolean isInspected = "Yes".equals(event.getValue());
            inspectionDate.setVisible(isInspected);
            nextDueInspectionDate.setVisible(true);
        });

        salesDescription.setHeight("100px");
        originalInspectionNotes.setHeight("100px");

        insuranceType.setLabel("Insurance Type");
        insuranceType.setItems("Private", "Commercial");

        paymentMethod.setLabel("Payment Method");
        paymentMethod.setItems("MPESA", "Bank");
        paymentMethod.addValueChangeListener(event -> {
            boolean isMpesa = "MPESA".equals(event.getValue());
            mpesaNumber.setVisible(isMpesa);
            bankName.setVisible(!isMpesa);
            bankAccountNumber.setVisible(!isMpesa);
        });

        // Hide conditional fields initially
        mpesaNumber.setVisible(false);
        bankName.setVisible(false);
        bankAccountNumber.setVisible(false);

        configureUpload(logbookUpload, "Upload Logbook");
        configureUpload(insuranceUpload, "Upload Insurance");
        configureUpload(agreementUpload, "Upload Agreement Paperwork");

        formLayout.add(
                plateNumber, engineNumber, chassisNumber,
                yearOfManufacture, carModel, inspectionStatus,
                inspectionDate, nextDueInspectionDate, insuranceCompany, insuranceBroker,
                insuranceType, commissionStructure, paymentMethod,
                mpesaNumber, bankName, bankAccountNumber,
                salesDescription, originalInspectionNotes,
                logbookUpload, insuranceUpload, agreementUpload
        );

        add(formLayout);
    }

    private void configureUpload(Upload upload, String label) {
        upload.setAcceptedFileTypes("application/pdf", ".pdf");
        upload.setMaxFiles(1);
        upload.setDropLabel(new com.vaadin.flow.component.html.Label(label));
    }

    private void submitCarDetails() {
        uploadProgress.setVisible(true);
        submitButton.setEnabled(false);

        Map<String, Object> carData = new HashMap<>();
        carData.put("plateNumber", plateNumber.getValue());
        carData.put("engineNumber", engineNumber.getValue());
        carData.put("chassisNumber", chassisNumber.getValue());
        carData.put("yearOfManufacture", yearOfManufacture.getValue());
        carData.put("carModel", carModel.getValue());
        carData.put("inspectionStatus", inspectionStatus.getValue());
        carData.put("inspectionDate", inspectionDate.getValue() != null ? inspectionDate.getValue().toString() : null);
        carData.put("nextDueInspectionDate", nextDueInspectionDate.getValue() != null ? nextDueInspectionDate.getValue().toString() : null);
        carData.put("insuranceCompany", insuranceCompany.getValue());
        carData.put("insuranceBroker", insuranceBroker.getValue());
        carData.put("insuranceType", insuranceType.getValue());
        carData.put("commissionStructure", commissionStructure.getValue());
        carData.put("paymentMethod", paymentMethod.getValue());

        if ("MPESA".equals(paymentMethod.getValue())) {
            carData.put("mpesaNumber", mpesaNumber.getValue());
        } else if ("Bank".equals(paymentMethod.getValue())) {
            carData.put("bankName", bankName.getValue());
            carData.put("bankAccountNumber", bankAccountNumber.getValue());
        }

        carData.put("salesDescription", salesDescription.getValue());
        carData.put("originalInspectionNotes", originalInspectionNotes.getValue());

        uploadDocuments(carData);
    }

    private void uploadDocuments(Map<String, Object> carData) {
        StorageClient storage = StorageClient.getInstance(FirebaseApp.getInstance());

        uploadDocument(storage, "logbook", logbookBuffer)
                .thenCompose(logbookUrl -> {
                    carData.put("logbookUrl", logbookUrl);
                    return uploadDocument(storage, "insurance", insuranceBuffer);
                })
                .thenCompose(insuranceUrl -> {
                    carData.put("insuranceUrl", insuranceUrl);
                    return uploadDocument(storage, "agreement", agreementBuffer);
                })
                .thenCompose(agreementUrl -> {
                    carData.put("agreementUrl", agreementUrl);
                    return uploadToFirestore(carData);
                })
                .thenAccept(carId -> {
                    uploadProgress.setValue(1.0); // Complete progress
                    Notification.show("Car details successfully uploaded with ID: " + carId);
                    resetForm();
                })
                .exceptionally(e -> {
                    uploadProgress.setVisible(false);
                    submitButton.setEnabled(true);
                    Notification.show("Error: " + e.getMessage());
                    e.printStackTrace(); // Add this line for more detailed error logging
                    return null;
                });
    }

    private CompletableFuture<String> uploadDocument(StorageClient storage, String prefix, MemoryBuffer buffer) {
        if (buffer.getInputStream() == null) {
            return CompletableFuture.completedFuture(null);
        }

        String fileName = prefix + "_" + System.currentTimeMillis() + ".pdf";
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream is = buffer.getInputStream()) {
                storage.bucket(BUCKET_NAME).create(fileName, is, "application/pdf");
                return storage.bucket(BUCKET_NAME).get(fileName).signUrl(7, TimeUnit.DAYS).toString();
            } catch (Exception e) {
                e.printStackTrace(); // Add this line for more detailed error logging
                throw new CompletionException(e);
            }
        });
    }

    private CompletableFuture<String> uploadToFirestore(Map<String, Object> carData) {
        Firestore db = getFirestore();
        ApiFuture<DocumentReference> future = db.collection("cars").add(carData);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get().getId();
            } catch (InterruptedException | ExecutionException e) {
                throw new CompletionException(e);
            }
        });
    }

    private void resetForm() {
        plateNumber.clear();
        engineNumber.clear();
        chassisNumber.clear();
        yearOfManufacture.clear();
        carModel.clear();
        inspectionStatus.clear();
        inspectionDate.clear();
        nextDueInspectionDate.clear();
        insuranceCompany.clear();
        insuranceBroker.clear();
        insuranceType.clear();
        commissionStructure.clear();
        paymentMethod.clear();
        mpesaNumber.clear();
        bankName.clear();
        bankAccountNumber.clear();
        salesDescription.clear();
        originalInspectionNotes.clear();
        uploadProgress.setVisible(false);
        submitButton.setEnabled(true);
    }
}
