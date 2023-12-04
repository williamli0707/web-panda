package com.github.williamli0707.webpanda.views;

import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.github.williamli0707.webpanda.db.CodescanRecord;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.Hashtable;

public class CodeView extends TabSheet {
    private Div div1, div2;
    private Hashtable<String, String> names;
    public CodeView(Hashtable<String, String> names) {
        this.names = names;
        div1 = new Div();
        div2 = new Div();
        div1.setHeight("100%");
        div2.setHeight("100%");
        add("Time Differences", div1);
        add("Large Edits", div2);
        setWidth("100%");
        setHeight("100%");
    }

    public void set(CodescanRecord record) {
        remove(0);
        remove(0);
        div1 = new Div();
        div2 = new Div();

        if(record.getPids().length == 1) {
            div1.add(new NativeLabel("Skipped because only one problem was selected"));
        }
        else {
            setupTimeDiffs(record);
        }
        setupLargeEdits(record);

        div1.setHeight("100%");
        div2.setHeight("100%");

        add("Time Differences", div1);
        add("Large Edits", div2);
    }

    public void setupLargeEdits(CodescanRecord record) {
        Grid<Diff> edits = new Grid<>(Diff.class, false);
        edits.setWidth("100%");
        edits.setHeight("100%");
        edits.addColumn(Diff::sid).setHeader("ID").setSortable(true);
        edits.addColumn(a -> names.get(a.sid())).setHeader("Name").setSortable(true);
        edits.addColumn(a -> a.pid() + " submission " + a.num()).setHeader("Submission");
        edits.addColumn(Diff::score).setHeader("Difference").setSortable(true);

        edits.setMultiSort(true, Grid.MultiSortPriority.APPEND);
        edits.setItems(record.getLargeEdits());

        edits.setItemDetailsRenderer(new ComponentRenderer<ProblemViewer, Diff>(ProblemViewer::new, (viewer, diff) -> {
//            viewer.setCode(record.getData().get(diff.sid()).get(diff.pid()), diff.num() - 2);

            viewer.setCode(RunestoneAPI.requestHistory(diff.sid(), diff.pid()), diff.num() - 2);
        }));

        div2.add(edits);
    }

    private void setupTimeDiffs(CodescanRecord record) {
        Grid<DiffBetweenProblems> timeDiff = new Grid<>(DiffBetweenProblems.class, false);
        timeDiff.setWidth("100%");
        timeDiff.setHeight("100%");
        timeDiff.addColumn(DiffBetweenProblems::sid).setHeader("ID").setSortable(true);
        timeDiff.addColumn(a -> names.get(a.sid())).setHeader("Name").setSortable(true);
        timeDiff.addColumn(a -> a.pid1() + " submission " + a.a1()).setHeader("Submission for Problem 1");
        timeDiff.addColumn(a -> a.pid2() + " submission " + a.a2()).setHeader("Submission for Problem 2");
        timeDiff.addColumn(DiffBetweenProblems::time).setHeader("Time Difference").setSortable(true);
        timeDiff.addColumn(a -> String.format("%.2f", a.score())).setHeader("Score").setSortable(true);

        timeDiff.setMultiSort(true, Grid.MultiSortPriority.APPEND);
        timeDiff.setItems(record.getTimeDiffs());

        timeDiff.setItemDetailsRenderer(new ComponentRenderer<>(DoubleProblemViewer::new, (viewer, diff) -> {
//            viewer.setCode(record.getData().get(diff.sid()).get(diff.pid1()), diff.a1() - 2, record.getData().get(diff.sid()).get(diff.pid2()), diff.a2() - 2);
            viewer.setCode(RunestoneAPI.requestHistory(diff.sid(), diff.pid1()), diff.a1() - 2, RunestoneAPI.requestHistory(diff.sid(), diff.pid2()), diff.a2() - 2);
        }));

        div1.add(timeDiff);
    }

}
