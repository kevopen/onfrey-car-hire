package com.example.application.views.clientdetails;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Client Details")
@Route(value = "client-details", layout = MainLayout.class)
public class ClientDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private Div title = new Div();
    private Div phoneDiv = new Div();

    public ClientDetailsView() {
        setWidth("100%");
        setAlignItems(Alignment.CENTER);

        add(title);
        add(phoneDiv);

        Button backButton = new Button("Back", event -> {
            getUI().ifPresent(ui -> ui.navigate(""));
        });
        add(backButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String name = event.getLocation().getQueryParameters().getParameters().getOrDefault("name", List.of("Unknown")).get(0);
        String phone = event.getLocation().getQueryParameters().getParameters().getOrDefault("phone", List.of("Unknown")).get(0);

        title.setText(name);
        title.getStyle().set("font-size", "24px");
        title.getStyle().set("font-weight", "bold");

        phoneDiv.setText("Phone Number: " + phone);
    }
}