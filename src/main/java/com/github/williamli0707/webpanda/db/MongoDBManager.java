package com.github.williamli0707.webpanda.db;

public class MongoDBManager {
    public static ItemRepository repository;

    /**
     * Save a record to the database
     * @param record record to be saved
     */
    public static void save(CodescanRecord record) {
        repository.save(record);
    }

    /**
     * Delete a record from the database
     * @param record record to be deleted
     */
    public static void delete(CodescanRecord record) {
        repository.delete(record);
    }
}
