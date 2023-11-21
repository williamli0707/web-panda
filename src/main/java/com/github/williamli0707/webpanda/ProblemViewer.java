package com.github.williamli0707.webpanda;

import com.github.williamli0707.webpanda.records.Attempt;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.ArrayList;

public class ProblemViewer extends VerticalLayout {
    private ArrayList<Attempt> code;
    private int numAttempts, currAttempt;
    Button prev, next;
    NativeLabel submissionLabel;
    TextArea codeArea;

    public ProblemViewer() {
        prev = new Button("Previous");
        next = new Button("Next");
        submissionLabel = new NativeLabel();
        codeArea = new TextArea();
        codeArea.setReadOnly(true);
        prev.addClickListener(e -> {
            if (currAttempt > 0) {
                if(--currAttempt == 0) {
                    prev.setEnabled(false);
                }
                codeArea.setValue(code.get(currAttempt).code());
                submissionLabel.setText("Submission " + (currAttempt + 2) + " out of " + numAttempts);
            }
        });
        next.addClickListener(e -> {
            if (currAttempt < numAttempts - 1) {
                if(++currAttempt == numAttempts - 1) {
                    next.setEnabled(false);
                }
                codeArea.setValue(code.get(currAttempt).code());
                submissionLabel.setText("Submission " + (currAttempt + 2) + " out of " + numAttempts);
            }
        });
        codeArea.setWidth("100%");
        codeArea.setHeight("100%");
        add(codeArea);
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        buttons.add(prev, submissionLabel, next);
        buttons.setWidth("100%");
        add(buttons);
//        setHeight("100%");
    }

    public void setCode(ArrayList<Attempt> code) {
        setCode(code, code.size() - 1);
    }

    public void setCode(ArrayList<Attempt> code, int start) {
        this.code = code;
        numAttempts = code.size() + 1;
        currAttempt = start;
        submissionLabel.setText("Submission " + (currAttempt + 2) + " out of " + numAttempts);
        codeArea.setValue(code.get(currAttempt).code());
    }
}
