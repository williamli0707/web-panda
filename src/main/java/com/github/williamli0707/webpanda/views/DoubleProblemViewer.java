package com.github.williamli0707.webpanda.views;

import com.github.williamli0707.webpanda.records.Attempt;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;

public class DoubleProblemViewer extends HorizontalLayout {
    private ProblemViewer p1, p2;

    public DoubleProblemViewer() {
        p1 = new ProblemViewer();
        p2 = new ProblemViewer();
        add(p1, p2);
    }

    public void setCode(ArrayList<Attempt> code1, ArrayList<Attempt> code2) {
        setCode(code1, code1.size() - 1, code2, code2.size() - 1);
    }

    public void setCode(ArrayList<Attempt> code1, int start1, ArrayList<Attempt> code2, int start2) {
        p1.setCode(code1, start1);
        p2.setCode(code2, start2);
    }
}
