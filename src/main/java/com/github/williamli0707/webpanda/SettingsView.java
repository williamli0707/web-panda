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
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.lang.annotation.Native;

@PageTitle("Settings Viewer")
@Route(value = "settings", layout = MainLayout.class)
@PreserveOnRefresh
public class SettingsView extends VerticalLayout {
    public SettingsView() {
        NativeLabel label = new NativeLabel("test");
        Button resetButton = new Button("Reset Caches");
        NativeLabel resetLabel = new NativeLabel("Resetting caches will re-download all data from Runestone. Useful for when course roster changes or for when problem list changes. ");

        resetButton.addClickListener(e -> {
            resetLabel.setText("Resetting caches...");
            RunestoneAPI.reset();
            resetLabel.setText("Caches reset.");
        });

        add(resetButton, resetLabel);
    }
}
