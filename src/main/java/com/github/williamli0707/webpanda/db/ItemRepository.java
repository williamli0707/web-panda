package com.github.williamli0707.webpanda.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@org.springframework.stereotype.Repository
public interface ItemRepository extends MongoRepository<CodescanRecord, String> {

    @Query("{id:'?0'}")
        CodescanRecord findItemByName(String name);

    public long count();

}