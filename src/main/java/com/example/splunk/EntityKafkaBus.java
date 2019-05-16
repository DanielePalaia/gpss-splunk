package com.example.splunk;

import java.lang.*;
import java.util.*;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import java.time.*;


public class EntityKafkaBus {

    private Properties props = null;
    private Producer<Integer, String> producer = null;
    private String topic;

    public EntityKafkaBus(String ip, String port, String topic)   {

        props = new Properties();
        props.put("bootstrap.servers", ip+":"+port);
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.topic = topic;
        //create producer
        try {
            producer = new KafkaProducer<Integer, String>(props);
        }
        catch(Exception e)  {
            e.printStackTrace();
        }


    }

    public void sendJson(String splunkLog)   {

        System.out.println("sending: " + splunkLog + " to topic: " + topic);

        try {
            ProducerRecord producerRecord = new ProducerRecord<String, String>(this.topic, Instant.now().toString(), splunkLog);
            producer.send(producerRecord);
        }
        catch(Exception e)  {
            e.printStackTrace();
        }

    }

}
