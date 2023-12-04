package com.github.williamli0707.webpanda.db;

//import org.jetbrains.annotations.NotNull;

import com.github.williamli0707.webpanda.WebPandaApplication;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
import com.google.gson.Gson;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document("codescantasks")
public class CodescanRecord implements Comparable<CodescanRecord> {
    private static Gson gson = new Gson();
    @Id
    private String id;
    private String[] pids;
    private ArrayList<Diff> largeDiffs;
    private ArrayList<DiffBetweenProblems> timeDiffs;
    private String version;

    public CodescanRecord(ArrayList<Diff> largeDiffs, ArrayList<DiffBetweenProblems> timeDiffs, String[] pids) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.largeDiffs = largeDiffs;
        this.timeDiffs = timeDiffs;
        this.pids = pids;
        this.version = WebPandaApplication.version;
    }

    public CodescanRecord() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.largeDiffs = new ArrayList<>();
        this.timeDiffs = new ArrayList<>();
        this.pids = new String[0];
        this.version = WebPandaApplication.version;
    }

    public ArrayList<Diff> getLargeEdits() {
        return largeDiffs;
    }
    public ArrayList<DiffBetweenProblems> getTimeDiffs() {
        return timeDiffs;
    }

    public String[] getPids() {
        return pids;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLargeDiffs(ArrayList<Diff> largeDiffs) {
        this.largeDiffs.clear();
        this.largeDiffs.addAll(largeDiffs);
    }

    public void setTimeDiffs(ArrayList<DiffBetweenProblems> timeDiffs) {
        this.timeDiffs.clear();
        this.timeDiffs.addAll(timeDiffs);
    }

    public void setPids(String[] pids) {
        this.pids = pids;
    }

    //reversed so that the newest ones are first
    @Override
    public int compareTo(CodescanRecord o) {
        return (int) (Long.parseLong(o.id) - Long.parseLong(id));
    }

    public long getTime() {
        return Long.parseLong(id);
    }
}
