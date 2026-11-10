package com.springboot.razorpay.common.config;

import com.springboot.razorpay.common.enums.EventAggregateType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties (
        Map<String, String> topics
){
    public String topicFor(EventAggregateType aggregateType) {
        String topic = topics.get(aggregateType.name().toLowerCase());
        if(topic == null){
            throw new IllegalStateException("No Kafka topic found for aggregate type " + aggregateType.name());
        }
        return topic;
    }
}
