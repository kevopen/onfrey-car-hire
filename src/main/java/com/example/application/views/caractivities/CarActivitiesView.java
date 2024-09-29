package com.example.application.views.caractivities;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Car Activities")
@Route(value = "car-activities", layout = MainLayout.class)
public class CarActivitiesView extends Div {

    public CarActivitiesView() {
        setText("Car Activities View - Placeholder content");
    }
}
