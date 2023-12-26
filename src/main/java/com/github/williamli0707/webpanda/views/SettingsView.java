package com.github.williamli0707.webpanda.views;

import com.github.williamli0707.webpanda.WebPandaApplication;
import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.addons.componentfactory.PaperSlider;


//import java.awt.*;

@PermitAll
@PageTitle("Settings Viewer")
@Route(value = "settings", layout = MainLayout.class)


//@PreserveOnRefresh
public class SettingsView extends VerticalLayout {
    public SettingsView() {
        Button resetButton = new Button("Reset Caches");
        NativeLabel resetLabel = new NativeLabel("Resetting caches will re-download all data from Runestone. Useful for when course roster changes or for when problem list changes. ");

        resetButton.addClickListener(e -> {
            resetLabel.setText("Resetting caches...");
            RunestoneAPI.reset();
            resetLabel.setText("Caches reset.");
        });

        NativeLabel timeDiffSettings = new NativeLabel("Time Difference Analyze Settings");
        timeDiffSettings.getStyle().set("font-weight", "bold");
        NativeLabel timeDiffSensitivity = new NativeLabel("Sensitivity: " + RunestoneAPI.timeDiffSensitivity + "%");
        PaperSlider timeDiffSensitivitySlider = new PaperSlider();
        timeDiffSensitivitySlider.setWidth("500px");
        timeDiffSensitivitySlider.setMin(0);
        timeDiffSensitivitySlider.setMax(100);
        timeDiffSensitivitySlider.setValue(RunestoneAPI.timeDiffSensitivity);

        timeDiffSensitivitySlider.addValueChangeListener(e -> {
            RunestoneAPI.timeDiffSensitivity = e.getValue();
            WebPandaApplication.preferences.put("timeDiffSensitivity", String.valueOf(e.getValue()));
            timeDiffSensitivity.setText("Sensitivity: " + RunestoneAPI.timeDiffSensitivity + "%");
        });

        NativeLabel largeEditSettings = new NativeLabel("Large Edit Analyze Settings");
        largeEditSettings.getStyle().set("font-weight", "bold");
        NativeLabel largeEditSensitivity = new NativeLabel("Sensitivity: " + RunestoneAPI.largeEditSensitivity + "%");
        PaperSlider largeEditSensitivitySlider = new PaperSlider();
        largeEditSensitivitySlider.setWidth("500px");
        largeEditSensitivitySlider.setMin(0);
        largeEditSensitivitySlider.setMax(100);
        largeEditSensitivitySlider.setValue(RunestoneAPI.largeEditSensitivity);

        largeEditSensitivitySlider.addValueChangeListener(e -> {
            RunestoneAPI.largeEditSensitivity = e.getValue();
            WebPandaApplication.preferences.put("largeEditSensitivity", String.valueOf(e.getValue()));
            largeEditSensitivity.setText("Sensitivity: " + RunestoneAPI.largeEditSensitivity + "%");
        });

        Button resetSettings = new Button("Reset to Default Settings");
        resetSettings.addClickListener(e -> {
            RunestoneAPI.timeDiffSensitivity = 50;
            RunestoneAPI.largeEditSensitivity = 50;
            WebPandaApplication.preferences.put("timeDiffSensitivity", String.valueOf(10));
            WebPandaApplication.preferences.put("largeEditSensitivity", String.valueOf(10));
            timeDiffSensitivitySlider.setValue(50);
            largeEditSensitivitySlider.setValue(50);
            timeDiffSensitivity.setText("Sensitivity: " + RunestoneAPI.timeDiffSensitivity + "%");
            largeEditSensitivity.setText("Sensitivity: " + RunestoneAPI.largeEditSensitivity + "%");
        });

        TextField userField = new TextField("Runestone Username");
        userField.setValue((WebPandaApplication.preferences.get("user", "")));
        PasswordField passField = new PasswordField("Runestone Password");
        passField.setValue(WebPandaApplication.preferences.get("password", ""));

        PasswordField websitePassField = new PasswordField("Website Passcode (default is \"password\")");
        websitePassField.setValue(WebPandaApplication.passcode);

        Button save = new Button("Save Username, Password, and Website Passcode");
        NativeLabel errorText = new NativeLabel("");
        errorText.getStyle().setColor("red");
        save.addClickListener(e -> {
            String utemp = userField.getValue();
            String ptemp = passField.getValue();
            String wptemp = websitePassField.getValue();

            WebPandaApplication.preferences.put("user", userField.getValue());
            WebPandaApplication.preferences.put("password", passField.getValue());
            WebPandaApplication.preferences.put("passcode", websitePassField.getValue());

            WebPandaApplication.passcode = websitePassField.getValue();

            RunestoneAPI.user = userField.getValue();
            RunestoneAPI.password = passField.getValue();
            try {
                RunestoneAPI.reset();
                errorText.setText("Saved. If the website passcode was changed, the server will need to be restarted. ");
            } catch (Exception error) {
                errorText.setText("Invalid credentials, reverted to previous settings.");
                WebPandaApplication.preferences.put("user", utemp);
                WebPandaApplication.preferences.put("password", ptemp);
                WebPandaApplication.preferences.put("passcode", wptemp);

                WebPandaApplication.passcode = wptemp;

                RunestoneAPI.user = utemp;
                RunestoneAPI.password = ptemp;

                userField.setValue(utemp);
                passField.setValue(ptemp);
                websitePassField.setValue(wptemp);
            }
        });

        add(timeDiffSettings, timeDiffSensitivity, timeDiffSensitivitySlider,
                new Hr(),
                largeEditSettings, largeEditSensitivity, largeEditSensitivitySlider,
                new Hr(),
                resetButton, resetLabel, resetSettings,
                new Hr(),
                userField, passField, websitePassField, save, errorText
        );
    }
}
