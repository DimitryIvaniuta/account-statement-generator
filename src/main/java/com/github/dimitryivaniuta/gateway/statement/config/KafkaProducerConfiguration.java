package com.github.dimitryivaniuta.gateway.statement.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Configures Kafka producer infrastructure used by the outbox relay.
 */
@Configuration
public class KafkaProducerConfiguration {

    /**
     * Creates the producer factory.
     *
     * @param bootstrapServers Kafka bootstrap servers.
     * @return producer factory.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") final String bootstrapServers) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        properties.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd");
        return new DefaultKafkaProducerFactory<>(properties);
    }

    /**
     * Creates the Kafka template.
     *
     * @param producerFactory producer factory.
     * @return Kafka template.
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(final ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
