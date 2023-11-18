package com.github.williamli0707.webpanda;

import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Test Viewer")
@Route(value = "test", layout = MainLayout.class)
public class TestView extends VerticalLayout {
    public TestView() {
        Button resetButton = new Button("Reset Caches");
        NativeLabel resetLabel = new NativeLabel("refksldfjlkdsf");
        add(resetButton, resetLabel);
    }
}
