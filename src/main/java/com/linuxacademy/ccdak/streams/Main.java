package com.linuxacademy.ccdak.streams;

import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "working-stream-processing-lab");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_DOC, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        //Get the source stream
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> source = builder.stream("inventory_purchases");
        final KStream<String, Integer> integerValuesSource = source.mapValues(value ->{
           try{
               return Integer.parseInt(value);
           } catch (NumberFormatException e) {
               System.out.println("Unable to parse integer value: " + value);
               return 0;
           }
        });

        KTable<String, Integer> productCounts = integerValuesSource.groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
                .reduce((total, newQuantity) -> total + newQuantity);
            productCounts.toStream().to("total_purchases", Produced.with(Serdes.String(), Serdes.Integer()));

        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        System.out.println("Starting Kafka Streams " +  topology.describe());
        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}
