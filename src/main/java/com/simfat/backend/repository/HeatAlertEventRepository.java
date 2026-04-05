package com.simfat.backend.repository;

import com.simfat.backend.model.HeatAlertEvent;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HeatAlertEventRepository extends MongoRepository<HeatAlertEvent, String> {

    List<HeatAlertEvent> findByRegionId(String regionId);
}

