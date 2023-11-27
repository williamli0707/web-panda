package com.github.williamli0707.webpanda.db;

import com.github.williamli0707.webpanda.records.Attempt;
import com.github.williamli0707.webpanda.records.DiffBetweenProblems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MongoManager {
    public static ItemRepository repository;

    public static void save(CodescanRecord record) {
        repository.save(record);
    }
}
