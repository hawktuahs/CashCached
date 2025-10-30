package com.bt.accounts.config;

import com.bt.accounts.event.*;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin admin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    // Topic creation beans
    @Bean
    public NewTopic customerValidationRequestTopic() {
        return TopicBuilder.name(KafkaTopics.CUSTOMER_VALIDATION_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic customerValidationResponseTopic() {
        return TopicBuilder.name(KafkaTopics.CUSTOMER_VALIDATION_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDetailsRequestTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCT_DETAILS_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDetailsResponseTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCT_DETAILS_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fdCalculationRequestTopic() {
        return TopicBuilder.name(KafkaTopics.FD_CALCULATION_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fdCalculationResponseTopic() {
        return TopicBuilder.name(KafkaTopics.FD_CALCULATION_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fdHistoryRequestTopic() {
        return TopicBuilder.name(KafkaTopics.FD_HISTORY_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fdHistoryResponseTopic() {
        return TopicBuilder.name(KafkaTopics.FD_HISTORY_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.ACCOUNT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountUpdatedTopic() {
        return TopicBuilder.name(KafkaTopics.ACCOUNT_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Producer configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate() {
        return new org.springframework.kafka.core.KafkaTemplate<>(producerFactory());
    }

    // Consumer configuration for customer validation responses
    @Bean
    public ConsumerFactory<String, CustomerValidationResponse> customerValidationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CustomerValidationResponse.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, CustomerValidationResponse>> customerValidationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CustomerValidationResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(customerValidationConsumerFactory());
        return factory;
    }

    // Consumer configuration for product details responses
    @Bean
    public ConsumerFactory<String, ProductDetailsResponse> productDetailsConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ProductDetailsResponse.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ProductDetailsResponse>> productDetailsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProductDetailsResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productDetailsConsumerFactory());
        return factory;
    }

    // Consumer configuration for FD calculation responses
    @Bean
    public ConsumerFactory<String, FdCalculationResponseEvent> fdCalculationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, FdCalculationResponseEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, FdCalculationResponseEvent>> fdCalculationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FdCalculationResponseEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fdCalculationConsumerFactory());
        return factory;
    }

    // Generic consumer factory (for backward compatibility)
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}
