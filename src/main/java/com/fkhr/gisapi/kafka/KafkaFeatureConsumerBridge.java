/*
package com.fkhr.gisapi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fkhr.gisapi.model.Feature;
import com.fkhr.gisapi.service.FeatureServiceImpl;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaFeatureConsumerBridge {
    private final KafkaProducer kafkaProducer;
    private final FeatureServiceImpl featureService;

    public KafkaFeatureConsumerBridge(KafkaProducer kafkaProducer, FeatureServiceImpl featureService) {
        this.kafkaProducer = kafkaProducer;
        this.featureService = featureService;
    }

    @KafkaListener(topics = {Topics.FEATURE}, groupId = "gis-bridge")
    public void listen(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Feature feature = objectMapper.readValue(message, Feature.class);
       // featureService.get;
       */
/* String responseStr = String.format("Leaderboard updated with %s", player.toString());
        kafkaProducer.send(Topics.UPDATED_SCORE, responseStr);*//*

    }
}
*/
