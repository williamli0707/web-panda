package com.github.williamli0707.webpanda;

import com.github.williamli0707.webpanda.api.Callback;
import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.github.williamli0707.webpanda.db.CodescanRecord;
import com.github.williamli0707.webpanda.db.MongoManager;
import com.github.williamli0707.webpanda.records.Attempt;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
import com.github.williamli0707.webpanda.records.Problem;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

//@Uses(TextArea.class)
//@PageTitle("Runestone Analyzer")
@Route(value="analyze", layout = MainLayout.class)
@RouteAlias(value="", layout = MainLayout.class)
public class MainView extends VerticalLayout {
//    private HorizontalLayout layout = new HorizontalLayout();
//    private TextArea codeArea = new TextArea();
    private RunestoneAPI api;
    public MainView() {
//        System.out.println(this.getClass().getPackage().getImplementationVersion());
//        System.out.println("start");
        api = new RunestoneAPI();
//        System.out.println("initialized api");
        Hashtable<String, String[]> problemsets = api.getProblems();
//        System.out.println("got problem sets");

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

        CodeView results = new CodeView(api.getNames());
        AtomicBoolean resultsShown = new AtomicBoolean(false);

//        System.out.println("init ui");

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
                    getUI().get().access(() -> {
                        remove(results);
                    });
                }
                //create a new api in case the cookie is expired
                RunestoneAPI currApi = new RunestoneAPI();
                Hashtable<String, String> names = currApi.getNames();
                getUI().get().access(() -> add(status1Label, status1));
                Callback callback1 = new AnalyzeCallback(status1), callback2 = new AnalyzeCallback(status2), callback3 = new AnalyzeCallback(status3);
                HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data = currApi.getAllCodeMultiple(callback1, selectedProblems);
                record.setData(data);

                System.out.println("done getting data");

                getUI().get().access(() -> {
                    remove(status1Label);
                    remove(status1);
                });

                ArrayList<DiffBetweenProblems> minTimes = null;
                if(selectedProblems.size() > 1) {
                    getUI().get().access(() -> add(status2Label, status2));
                    long time = System.currentTimeMillis();
                    minTimes = currApi.minTimeDiff(data, selectedProblems.size(), callback2);
                    System.out.println("Done calculating min times - Execution time: " + (System.currentTimeMillis() - time) + "ms");

                    getUI().get().access(() -> {
                        remove(status2Label);
                        remove(status2);
                    });
                    record.setTimeDiffs(minTimes);
                }
                getUI().get().access(() -> add(status3Label, status3));
                long time = System.currentTimeMillis();
                ArrayList<Diff> largeEdits = currApi.findLargeEdits(data, selectedProblems.size(), callback3);
                record.setLargeDiffs(largeEdits);
                System.out.println("Done getting large edits - Execution time: " + (System.currentTimeMillis() - time) + "ms");
                System.out.println(largeEdits);

                getUI().get().access(() -> {
                    remove(status3Label);
                    remove(status3);
                });
                resultsShown.set(true);

                getUI().get().access(() -> results.set(record));
                getUI().get().access(() -> add(results));
                MongoManager.save(record);
            }).start();
        });

//        System.out.println("event listeners");

        add(problemsetSelector);
        add(problemSelector);
        add(selectAll);
        add(analyze);
        setHeight("100%");
    }

//    private static ComponentRenderer<ProblemViewer, Diff> createProblemViewerRenderer() {
//        return new ComponentRenderer<>(problem -> {
//            ProblemViewer viewer = new ProblemViewer();
//            viewer.setCode(problem.code());
//            return viewer;
//        });
//    }

    class AnalyzeCallback implements Callback {
        NativeLabel label;
        public AnalyzeCallback (NativeLabel label) {
            this.label = label;
        }

        @Override
        public void call(int percent, String message) {
            getUI().get().access(() -> label.setText(percent + "% done - " + message));
//            System.out.println("callback: " + percent + "% done - " + message);
        }
    }
}

/*
List most (highest scoring?) students, show a difference between iterations and what was weird about it
 */