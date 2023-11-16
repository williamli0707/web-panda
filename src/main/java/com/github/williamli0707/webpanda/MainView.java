package com.github.williamli0707.webpanda;

import com.github.williamli0707.webpanda.api.Callback;
import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.github.williamli0707.webpanda.records.Attempt;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Uses(DrawerToggle.class)
@Uses(SideNav.class)
@Uses(SideNavItem.class)
@Uses(Scroller.class)
//@Uses(MainView.class)
@Uses(HistoryView.class)
@Uses(MainLayout.class)
@PageTitle("Runestone Analyzer")
@Route(value="analyze", layout = MainLayout.class)
@RouteAlias(value="", layout = MainLayout.class)
public class MainView extends VerticalLayout {
    private RunestoneAPI api;
    public MainView() {
        api = new RunestoneAPI();
        Hashtable<String, String[]> problemsets = api.getProblems();

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

        TabSheet results = new TabSheet();
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
                Set<String> selectedProblems = problemSelector.getValue();
                if(resultsShown.get()) remove(results);
                //create a new api in case the cookie is expired
                RunestoneAPI currApi = new RunestoneAPI();
                Hashtable<String, String> names = currApi.getNames();
                getUI().get().access(() -> add(status1Label, status1));
                Callback callback1 = new AnalyzeCallback(status1), callback2 = new AnalyzeCallback(status2), callback3 = new AnalyzeCallback(status3);
                HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data = currApi.getAllCodeMultiple(callback1, selectedProblems);
                System.out.println("done getting data");

                getUI().get().access(() -> {
                    remove(status1Label);
                    remove(status1);
                });

                ArrayList<DiffBetweenProblems> minTimes = null;
                if(selectedProblems.size() > 1) {
                    getUI().get().access(() -> add(status2Label, status2));
                    minTimes = currApi.minTimeDiff(data, selectedProblems.size(), callback2);
                    System.out.println("done calculating min times");

                    getUI().get().access(() -> {
                        remove(status2Label);
                        remove(status2);
                    });
                }
                getUI().get().access(() -> add(status3Label, status3));
                ArrayList<Diff> largeEdits = currApi.findLargeEdits(data, selectedProblems.size(), callback3);
                System.out.println("done getting large edits");
                System.out.println(largeEdits);

                getUI().get().access(() -> {
                    remove(status3Label);
                    remove(status3);
                });

                resultsShown.set(true);
                Div div1 = new Div(), div2 = new Div();
                results.add("Time Differences", div1);
                results.add("Large Edits", div2);
                results.setWidth("100%");

                if(selectedProblems.size() > 1) {
                    minTimes.sort(Comparator.comparingDouble(DiffBetweenProblems::time));

                    div1.add(new NativeLabel("Minimum time differences for each student: "));
                    Grid<DiffBetweenProblems> timeDiff = new Grid<>(DiffBetweenProblems.class, false);
                    timeDiff.setWidth("100%");
                    timeDiff.addColumn(DiffBetweenProblems::sid).setHeader("ID");
                    timeDiff.addColumn(a -> names.get(a.sid())).setHeader("Name");
                    timeDiff.addColumn(a -> a.pid1() + " submission " + a.a1()).setHeader("Submission for Problem 1");
                    timeDiff.addColumn(a -> a.pid2() + " submission " + a.a2()).setHeader("Submission for Problem 1");
                    timeDiff.addColumn(DiffBetweenProblems::time).setHeader("Time Difference");

                    timeDiff.setItems(minTimes);
                    div1.add(timeDiff);
                }
                else {
                    div1.add(new NativeLabel("Skipped because only one problem was selected"));
                }
                largeEdits.sort(Comparator.comparingDouble(Diff::score).reversed());

                Grid<Diff> edits = new Grid<>(Diff.class, false);
                edits.setWidth("100%");
                edits.addColumn(Diff::sid).setHeader("ID");
                edits.addColumn(a -> names.get(a.sid())).setHeader("Name");
                edits.addColumn(a -> a.pid() + " submission " + a.num()).setHeader("Submission");
                edits.addColumn(Diff::score).setHeader("Difference");

                edits.setItems(largeEdits);

                div2.add(edits);

                getUI().get().access(() -> add(results));
            }).start();
        });

        add(problemsetSelector);
        add(problemSelector);
        add(selectAll);
        add(analyze);

    }
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