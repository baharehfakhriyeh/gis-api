package com.fkhr.gisapi.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fkhr.gisapi.FeatureResponseDto;
import io.grpc.stub.ServerCallStreamObserver;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KafkaStreamFactory {
    private final ConsumerFactory<String, byte[]> consumerFactory;
    private final Map<String, ConcurrentMessageListenerContainer<String, byte[]>> activeContainers = new ConcurrentHashMap<>();

    public KafkaStreamFactory(ConsumerFactory<String, byte[]> consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    public ConcurrentMessageListenerContainer<String, byte[]> createFilteredContainer(
            String topic, String groupId, String owner,
            ServerCallStreamObserver<FeatureResponseDto> observer, boolean consumeHistory
    ) {
        Map<String, Object> factoryProps = new HashMap<>(consumerFactory.getConfigurationProperties());
        factoryProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumeHistory ? "earliest" : "latest");//todo: convert to enum
        consumerFactory.updateConfigs(factoryProps);

        final String tempGroupId = groupId + "-" + UUID.randomUUID();
        ContainerProperties properties = new ContainerProperties(topic);
        properties.setGroupId(tempGroupId);
        properties.setAckMode(ContainerProperties.AckMode.RECORD);//todo: what does it do?

        properties.setMessageListener((MessageListener<String, byte[]>) record -> {
            try {
                FeatureResponseDto feature = FeatureResponseDto.parseFrom(record.value());
                if (!feature.getOwner().equals((owner))) {
                    return;
                }
                if (observer.isCancelled()) {
                    return;
                }

                observer.onNext(feature);

            } catch (Exception ex) {
                observer.onError(ex);
            }
        });
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, properties);
        container.setAutoStartup(false);
        return container;
    }

    public void stopAndRemoveContainer(String groupId) {
        ConcurrentMessageListenerContainer<String, byte[]> container = activeContainers.remove(groupId);
        if (container != null) {
            try {
                container.stop();
            } catch (Exception e) {
                System.out.println(String.format("Error stopping container /s: /s", groupId, e.getMessage()));
            }
        }
    }


}
