package com.github.williamli0707.webpanda.db;

public class MongoManager {
    public static ItemRepository repository;

    public static void save(CodescanRecord record) {
        repository.save(record);
    }

    //TODO implement more crud methods when needed
}
