package com.github.williamli0707.webpanda.db;

//import org.jetbrains.annotations.NotNull;

import com.github.williamli0707.webpanda.WebPandaApplication;
import com.github.williamli0707.webpanda.records.Attempt;
import com.github.williamli0707.webpanda.records.Diff;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;
import com.google.gson.Gson;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Document("codescantasks")
public class CodescanRecord implements Comparable<CodescanRecord> {
    private static Gson gson = new Gson();
    @Id
    private String id;
    private String[] pids;
    private ArrayList<Diff> largeDiffs;
    private ArrayList<DiffBetweenProblems> timeDiffs;
    private HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data;
    private String dataString;
    private String version;

    public CodescanRecord(ArrayList<Diff> largeDiffs, ArrayList<DiffBetweenProblems> timeDiffs, String[] pids, HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.largeDiffs = largeDiffs;
        this.timeDiffs = timeDiffs;
        this.pids = pids;
        this.data = data;
        dataString = gson.toJson(data);
        this.version = WebPandaApplication.version;
    }

    public CodescanRecord() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.largeDiffs = new ArrayList<>();
        this.timeDiffs = new ArrayList<>();
        this.pids = new String[0];
        data = new HashMap<>();
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

    public HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> getData() {
        return data;
    }

    public String getDataString() {
        return dataString;
    }

    public void setDataString(String dataString) {
        this.dataString = dataString;
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

    public void setData(HashMap<String, LinkedHashMap<String, ArrayList<Attempt>>> data) {
        this.data = data;
        dataString = gson.toJson(data);
    }

    public void setData(String data) {
        this.data = new Gson().fromJson(data, HashMap.class); //weird cast but it should work
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
