package com.github.williamli0707.webpanda;

import com.github.williamli0707.webpanda.api.RunestoneAPI;
import com.github.williamli0707.webpanda.db.CodescanRecord;
import com.github.williamli0707.webpanda.db.MongoManager;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@PageTitle("History Viewer")
@Route(value = "history", layout = MainLayout.class)
public class HistoryView extends VerticalLayout {
    private static SimpleDateFormat sdf = new SimpleDateFormat("E MMMM d hh:mm:ss a z y");
    private RunestoneAPI api;
    public HistoryView() {
        setHeight("100%");
        setWidth("100%");
        api = new RunestoneAPI();
        System.out.println(MongoManager.repository.count());
        List<CodescanRecord> res = MongoManager.repository.findAll();
        Collections.sort(res);
        Accordion accordion = new Accordion();
        accordion.setHeight("100%");;
        accordion.setWidth("100%");
        for(CodescanRecord i: res) {
            VerticalLayout div = new VerticalLayout();
            AccordionPanel panel = accordion.add("Scan on " + sdf.format(new Date(i.getTime())), div);
            panel.setOpened(false);
            div.add(new NativeLabel("Problems scanned: " + Arrays.toString(i.getPids())));
            CodeView view = new CodeView(api.getNames());
            div.add(view);
            view.setHeight("500px");
            panel.addOpenedChangeListener(e -> {
                if(e.isOpened()) {
                    view.set(i);
                }
            });
        }
        accordion.close();
        add(accordion);
    }
}