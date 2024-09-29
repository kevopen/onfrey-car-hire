package com.example.application.views.dashboard;

import com.example.application.views.MainLayout;
import com.example.application.views.clientdetails.ClientDetailsView;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.example.application.Application.getFirestore;

@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
public class DashboardView extends Composite<VerticalLayout> {

    private final Grid<Client> clientGrid = new Grid<>(Client.class);
    private final List<Client> clientList = new ArrayList<>();
    private final List<Car> carList = new ArrayList<>();

    public DashboardView() {
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");

        // Create the card items
        VerticalLayout clientOnboardingCard = createCard(
                VaadinIcon.USER.create(),
                "Client Onboarding",
                "Add new clients to the system"
        );

        VerticalLayout manageCarsCard = createCard(
                VaadinIcon.CAR.create(),
                "Manage Cars",
                "Add, edit, or remove cars from the fleet"
        );

        VerticalLayout carActivitiesCard = createCard(
                VaadinIcon.CLIPBOARD_TEXT.create(),
                "Car Activities",
                "View car activities and logs"
        );

        VerticalLayout assignClientCard = createCard(
                VaadinIcon.CONNECT.create(),
                "Assign Client",
                "Assign a client to a specific car"
        );

        // Arrange the cards horizontally
        HorizontalLayout cardLayout = new HorizontalLayout(clientOnboardingCard, manageCarsCard, carActivitiesCard, assignClientCard);
        cardLayout.setWidthFull();
        cardLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // Add card layout to the view content
        getContent().add(cardLayout);
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search by Full Name");
        searchField.addValueChangeListener(event -> filterClients(event.getValue()));
        getContent().add(searchField);

        // Initialize and configure the Grid to show clients data
        configureClientGrid();

        // Fetch and display client data
        fetchClientData();
        fetchCarData();

        // Add click listener to the Assign Client card
        assignClientCard.addClickListener(event -> openAssignClientDialog());
    }

    private void filterClients(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            clientGrid.setItems(clientList); // Show all clients if search term is empty
        } else {
            // Filter client list based on the search term
            List<Client> filteredClients = new ArrayList<>();
            for (Client client : clientList) {
                if (client.getFullName().toLowerCase().contains(searchTerm.toLowerCase())) {
                    filteredClients.add(client);
                }
            }
            clientGrid.setItems(filteredClients);
        }
    }

    private VerticalLayout createCard(Icon icon, String title, String description) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("border", "1px solid #E0E0E0");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("padding", "16px");
        card.getStyle().set("box-shadow", "0 4px 8px rgba(0,0,0,0.1)");

        // Add icon
        icon.setSize("50px");
        card.add(icon);

        // Add title
        Div titleDiv = new Div();
        titleDiv.setText(title);
        titleDiv.getStyle().set("font-weight", "bold");
        titleDiv.getStyle().set("font-size", "16px");
        card.add(titleDiv);

        // Add description
        Div descriptionDiv = new Div();
        descriptionDiv.setText(description);
        descriptionDiv.getStyle().set("color", "#6B6B6B");
        descriptionDiv.getStyle().set("font-size", "14px");
        card.add(descriptionDiv);

        return card;
    }

    private void configureClientGrid() {
        // Re-arranging columns in the specified order
        clientGrid.removeAllColumns();  // Clear any pre-existing columns

        // Add columns in order: Full Name, Phone Number, Email, Number of Cars
        clientGrid.addColumn(Client::getFullName).setHeader("Full Name");
        clientGrid.addColumn(Client::getPhone).setHeader("Phone Number");
        clientGrid.addColumn(Client::getEmail).setHeader("Email");
        clientGrid.addColumn(Client::getNumberOfCars).setHeader("Number of Cars");

        // Add dropdown for Edit and Status Log actions
        clientGrid.addComponentColumn(client -> {
            // Creating a dropdown with an additional non-selectable first item
            Select<String> actionsDropdown = new Select<>();

            // Create a non-selectable "More actions" item
            actionsDropdown.setItems("More actions", "Edit", "Status Log");

            // Set the value to the non-actionable default
            actionsDropdown.setValue("More actions");

            // Disable the first item from being selectable
            actionsDropdown.addValueChangeListener(event -> {
                if (!"More actions".equals(event.getValue())) {
                    if ("Edit".equals(event.getValue())) {
                        editClient(client);
                    } else if ("Status Log".equals(event.getValue())) {
                        getUI().ifPresent(ui -> {
                            // Use QueryParameters instead of RouteParameters
                            QueryParameters params = new QueryParameters(
                                    Map.of(
                                            "name", List.of(client.getFullName()),
                                            "phone", List.of(client.getPhone())
                                    )
                            );
                            // Navigate to ClientDetailsView with query parameters
                            ui.navigate(ClientDetailsView.class, params);
                        });
                    }
                    actionsDropdown.setValue("More actions");
                }
            });

            return actionsDropdown;
        }).setHeader("Actions");

        clientGrid.setWidthFull();
        getContent().add(clientGrid);
    }

    private void editClient(Client client) {
        // Create a dialog for editing client information
        Dialog editDialog = new Dialog();
        editDialog.setCloseOnOutsideClick(true);

        // Create fields for editing client information
        TextField fullNameField = new TextField("Full Name");
        fullNameField.setValue(client.getFullName());

        TextField emailField = new TextField("Email");
        emailField.setValue(client.getEmail());

        TextField phoneField = new TextField("Phone Number");
        phoneField.setValue(client.getPhone());

        TextField numberOfCarsField = new TextField("Number of Cars");
        numberOfCarsField.setValue(String.valueOf(client.getNumberOfCars()));

        // Create a button to save changes
        Button saveButton = new Button("Save", event -> {
            // Update client information in Firestore
            updateClientInFirestore(client.getEmail(), fullNameField.getValue(), emailField.getValue(), phoneField.getValue(), numberOfCarsField.getValue());

            // Close the dialog
            editDialog.close();

            // Refresh client data after edit
            UI.getCurrent().access(() -> {
                fetchClientData(); // Refresh client data after edit
            });
        });

        // Add fields and button to the dialog layout
        VerticalLayout dialogLayout = new VerticalLayout(fullNameField, emailField, phoneField, numberOfCarsField, saveButton);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        editDialog.add(dialogLayout);

        // Open the dialog
        editDialog.open();
    }

    private void updateClientInFirestore(String originalEmail, String fullName, String email, String phone, String numberOfCars) {
        Firestore db = getFirestore();
        CollectionReference clientsRef = db.collection("clients");

        // Get the client document based on the original email
        ApiFuture<QuerySnapshot> future = clientsRef.whereEqualTo("email", originalEmail).get();
        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                for (QueryDocumentSnapshot document : documents) {
                    // Update the client document in Firestore
                    clientsRef.document(document.getId()).update("full-name", fullName, "email", email, "phone", phone, "numberOfCars", Integer.parseInt(numberOfCars));
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }, Runnable::run);
    }

    private void fetchClientData() {
        Firestore db = getFirestore();
        CollectionReference clientsRef = db.collection("clients");

        // Fetch clients asynchronously
        ApiFuture<QuerySnapshot> future = clientsRef.get();
        try {
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            Set<String> uniqueEmails = new HashSet<>();  // Track unique emails to avoid duplicates
            List<Client> clients = new ArrayList<>();

            // Loop through documents and add to client list, ensuring no duplicates
            for (QueryDocumentSnapshot document : documents) {
                Map<String, Object> data = document.getData();
                String fullName = (String) data.get("full-name");
                String email = (String) data.get("email");
                String phone = (String) data.get("phone");
                int numberOfCars = Integer.parseInt(("2"));

                if (!uniqueEmails.contains(email)) {  // Check if email is already added
                    Client client = new Client(fullName, email, phone, numberOfCars);
                    clients.add(client);
                    uniqueEmails.add(email);  // Mark email as added
                }
            }

            // Set the client data to the Grid and also update clientList
            clientList.clear(); // Clear existing data in clientList
            clientList.addAll(clients); // Populate clientList with fetched clients
            clientGrid.setItems(clients); // Set items to the grid

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void fetchCarData() {
        Firestore db = getFirestore();
        CollectionReference carsRef = db.collection("cars");

        ApiFuture<QuerySnapshot> future = carsRef.get();
        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                carList.clear();
                for (QueryDocumentSnapshot document : documents) {
                    String plateNumber = document.getString("plateNumber");
                    if (plateNumber != null) {
                        carList.add(new Car(plateNumber));
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }, Runnable::run);
    }

    private void openAssignClientDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        Select<String> carSelect = new Select<>();
        carSelect.setLabel("Select Car");
        carSelect.setItems(carList.stream().map(Car::getPlateNumber).collect(Collectors.toList()));

        Select<String> clientSelect = new Select<>();
        clientSelect.setLabel("Select Client");
        clientSelect.setItems(clientList.stream().map(Client::getFullName).collect(Collectors.toList()));

        Button submitButton = new Button("Assign", event -> {
            String selectedCar = carSelect.getValue();
            String selectedClient = clientSelect.getValue();
            if (selectedCar != null && selectedClient != null) {
                assignClientToCar(selectedCar, selectedClient);
                dialog.close();
            }
        });

        VerticalLayout layout = new VerticalLayout(carSelect, clientSelect, submitButton);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialog.add(layout);

        dialog.open();
    }

    private void assignClientToCar(String plateNumber, String clientName) {
        Firestore db = getFirestore();
        CollectionReference carsRef = db.collection("cars");

        // Find the client's phone number
        String clientPhone = clientList.stream()
                .filter(client -> client.getFullName().equals(clientName))
                .findFirst()
                .map(Client::getPhone)
                .orElse(null);

        if (clientPhone == null) {
            Notification.show("Client not found", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        // Update the car document with the owner's phone number
        ApiFuture<QuerySnapshot> future = carsRef.whereEqualTo("plateNumber", plateNumber).get();
        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                for (QueryDocumentSnapshot document : documents) {
                    document.getReference().update("ownerNumber", clientPhone);
                }
                Notification.show("Client assigned successfully", 3000, Notification.Position.TOP_CENTER);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                Notification.show("Error assigning client: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
            }
        }, Runnable::run);
    }

    // Client class to represent the data
    public static class Client {
        private String fullName;
        private String email;
        private String phone;
        private int numberOfCars;

        public Client(String fullName, String email, String phone, int numberOfCars) {
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.numberOfCars = numberOfCars;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public int getNumberOfCars() {
            return numberOfCars;
        }
    }

    // Inner class to represent a Car
    private static class Car {
        private String plateNumber;

        public Car(String plateNumber) {
            this.plateNumber = plateNumber;
        }

        public String getPlateNumber() {
            return plateNumber;
        }
    }
}