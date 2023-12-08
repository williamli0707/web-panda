package com.github.williamli0707.webpanda.views;

import com.github.williamli0707.webpanda.api.Callback;
import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.github.williamli0707.webpanda.db.CodescanRecord;
import com.github.williamli0707.webpanda.db.MongoManager;
import com.github.williamli0707.webpanda.records.Attempt;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Route(value="analyze", layout = MainLayout.class)
@RouteAlias(value="", layout = MainLayout.class)
public class MainView extends VerticalLayout {
    public MainView() {
        Hashtable<String, String[]> problemsets = RunestoneAPI.getProblems();

        ComboBox<String> problemsetSelector = new ComboBox<>("Problem Set");
        problemsetSelector.setItems(problemsets.keySet());
        problemsetSelector.setPlaceholder("Select a problem set");
        problemsetSelector.setWidth("500px");

        MultiSelectComboBox<String> problemSelector = new MultiSelectComboBox<>("Problems");
        problemSelector.setEnabled(false);
        problemSelector.setPlaceholder("Select problems");
        problemSelector.setWidth("500px");

        Checkbox selectAll = new Checkbox("Select All");
        selectAll.setEnabled(false);

        Button analyze = new Button("Analyze");
        analyze.setEnabled(false);

        NativeLabel status1Label = new NativeLabel("Getting data...");
        NativeLabel status1 = new NativeLabel("status");
        NativeLabel status2Label = new NativeLabel("Calculating min times...");
        NativeLabel status2 = new NativeLabel("status2");
        NativeLabel status3Label = new NativeLabel("Getting large edits...");
        NativeLabel status3 = new NativeLabel("status3");

        CodeView results = new CodeView(RunestoneAPI.getNames());
        AtomicBoolean resultsShown = new AtomicBoolean(false);


        problemsetSelector.addValueChangeListener(e -> {
            String selected = e.getValue();
            problemSelector.setItems(problemsets.get(selected));
            problemSelector.setEnabled(true);
            selectAll.setEnabled(true);
            selectAll.setValue(false);
            analyze.setEnabled(false);
        });

        problemSelector.addValueChangeListener(e -> {
            Set<String> selectedCountriesText = e.getValue();
            analyze.setEnabled(selectedCountriesText.size() > 0);
        });

        selectAll.addValueChangeListener(e -> {
            if (e.getValue()) {
                problemSelector.select(problemsets.get(problemsetSelector.getValue()));
            } else {
                problemSelector.deselectAll();
            }
        });

        analyze.addClickListener(e -> {
            new Thread(() -> {
                CodescanRecord record = new CodescanRecord();
                Set<String> selectedProblems = problemSelector.getValue();
                record.setPids(selectedProblems.toArray(new String[0]));
                if(resultsShown.get()) {
                    getUI().ifPresent(ui -> {
                        if(ui.isAttached()) ui.access(() -> remove(results));
                    });
                }
                //create a new api in case the cookie is expired
//                RunestoneAPI currApi = new RunestoneAPI();
                getUI().ifPresent(ui -> {
                    if(ui.isAttached()) ui.access(() -> add(status1Label, status1));
                });
                Callback callback1 = new AnalyzeCallback(status1), callback2 = new AnalyzeCallback(status2), callback3 = new AnalyzeCallback(status3);
                HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data = RunestoneAPI.getAllCodeMultiple(callback1, selectedProblems);

//                System.out.println("done getting data");

                getUI().ifPresent(ui -> {
                    if(ui.isAttached()) ui.access(() -> remove(status1Label, status1));
                });

                ArrayList<DiffBetweenProblems> minTimes = null;
                if(selectedProblems.size() > 1) {
                    getUI().ifPresent(ui -> {
                        if(ui.isAttached()) ui.access(() -> add(status2Label, status2));
                    });
                    long time = System.currentTimeMillis();
//                    minTimes = currApi.minTimeDiff(data, selectedProblems.size(), callback2);
                    minTimes = RunestoneAPI.minTimeDiff(data, record.getPids(), callback2); //TODO change back
                    System.out.println("Done calculating min times - Execution time: " + (System.currentTimeMillis() - time) + "ms");

                    getUI().ifPresent(ui -> {
                        if(ui.isAttached()) ui.access(() -> remove(status2Label, status2));
                    });
                    record.setTimeDiffs(minTimes);
                }
                getUI().ifPresent(ui -> {
                    if(ui.isAttached()) ui.access(() -> add(status3Label, status3));
                });
                long time = System.currentTimeMillis();
                ArrayList<Diff> largeEdits = RunestoneAPI.findLargeEdits(data, selectedProblems.size(), callback3);
                record.setLargeDiffs(largeEdits);
                System.out.println("Done getting large edits - Execution time: " + (System.currentTimeMillis() - time) + "ms");
//                System.out.println(largeEdits);

                getUI().ifPresent(ui -> {
                    if(ui.isAttached()) ui.access(() -> remove(status3Label, status3));
                });
                resultsShown.set(true);

                getUI().ifPresent(ui -> {
                    if(ui.isAttached()) ui.access(() -> {
                        results.set(record);
                        add(results);
                    });
                });
                MongoManager.save(record);
            }).start();
        });

        add(problemsetSelector);
        add(problemSelector);
        add(selectAll);
        add(analyze);
        setHeight("100%");
    }

    class AnalyzeCallback implements Callback {
        NativeLabel label;
        public AnalyzeCallback (NativeLabel label) {
            this.label = label;
        }

        @Override
        public void call(int percent, String message) {
            getUI().ifPresent(ui -> {
                    if(ui.isAttached()) ui.access(() -> label.setText(percent + "% done - " + message));
            });
        }
    }
}

/*
List most (highest scoring?) students, show a difference between iterations and what was weird about it
 */