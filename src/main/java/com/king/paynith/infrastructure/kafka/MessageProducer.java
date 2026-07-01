package com.king.paynith.infrastructure.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.event.KafkaEvent;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public <V> void sendMessage(String topic, V message) {
        this.kafkaTemplate.send(topic, message).whenComplete((result, ex)->{
            if (ex != null){
                System.out.println("Failed to send message: " + ex.getMessage());
            } else {
                System.out.println("Message sent successfully");
            }
        });
    }
}
