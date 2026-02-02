package com.fkhr.gisapi.service;


import com.fkhr.gisapi.*;
import com.fkhr.gisapi.config.GisProperties;
import com.fkhr.gisapi.kafka.GroupIds;
import com.fkhr.gisapi.kafka.KafkaProducer;
import com.fkhr.gisapi.kafka.KafkaStreamFactory;
import com.fkhr.gisapi.kafka.Topics;
import com.fkhr.gisapi.model.Feature;
import com.fkhr.gisapi.repository.FeatureRepository;
import com.fkhr.gisapi.utils.CustomError;
import com.fkhr.gisapi.utils.CustomException;
import com.fkhr.gisapi.utils.GeometryConverter;
import com.fkhr.gisapi.utils.ProtoUtils;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.boot.info.InfoProperties;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@GrpcService
public class FeatureServiceImpl extends FeatureServiceGrpc.FeatureServiceImplBase {
    private final GeometryConverter geometryConverter;
    private final FeatureRepository featureRepository;
    private final KafkaProducer kafkaProducer;
    private final KafkaStreamFactory kafkaSTreamFactory;
    private final GisProperties gisProperties;

    public FeatureServiceImpl(GeometryConverter geometryConverter, FeatureRepository featureRepository,
                              KafkaProducer kafkaProducer, KafkaStreamFactory kafkaSTreamFactory, GisProperties gisProperties) {
        this.geometryConverter = geometryConverter;
        this.featureRepository = featureRepository;
        this.kafkaProducer = kafkaProducer;
        this.kafkaSTreamFactory = kafkaSTreamFactory;
        this.gisProperties = gisProperties;
    }

    @Override
    public void createFeature(CreateFeatureRequestDto request, StreamObserver<FeatureResponseDto> responseObserver) {
        try {
            Feature feature = convertCreateFeatureRequestDtoToFeature(request);
            feature = featureRepository.save(feature);
            FeatureResponseDto featureResponseDto = convertFeatureToFeatureResponseDto(feature);
            responseObserver.onNext(featureResponseDto);
            sendFeatureToKafka(featureResponseDto);
        } catch (Exception exception) {
            responseObserver.onError(exception);
        } finally {
            responseObserver.onCompleted();
        }
    }

    private void sendFeatureToKafka(FeatureResponseDto feature) {
        byte[] featureBytes = feature.toByteArray();
        kafkaProducer.send(Topics.FEATURE, feature.getOwner(), featureBytes);
    }

    @Override
    public void getFeature(FeatureId request, StreamObserver<FeatureResponseDto> responseObserver) {
        try {
            Optional<Feature> featureOptional = featureRepository.findById(UUID.fromString(request.getId()));
            if (featureOptional.isPresent()) {
                Feature feature = featureOptional.get();
                FeatureResponseDto featureResponseDto = convertFeatureToFeatureResponseDto(feature);
                responseObserver.onNext(featureResponseDto);
                responseObserver.onCompleted();
            } else {
                throw new CustomException(CustomError.FEATURE_NOT_FOUND);
            }
        } catch (Exception exception) {
            responseObserver.onError(exception);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getFeatureByOwner(GetFeatureByOwnerRequestDto request, StreamObserver<FeatureResponseDto> responseObserver) {
        try {

            Optional<Feature> featureOptional = featureRepository.findTopByOwnerOrderByTimestampDesc(request.getOwner());
            if (featureOptional.isPresent()) {
                Feature feature = featureOptional.get();
                FeatureResponseDto featureResponseDto = convertFeatureToFeatureResponseDto(feature);
                responseObserver.onNext(featureResponseDto);
                responseObserver.onCompleted();
            } else {
                throw new CustomException(CustomError.FEATURE_NOT_FOUND);
            }
        } catch (Exception exception) {
            responseObserver.onError(exception);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void getFeaturesInArea(GetFeaturesInAreaRequestDto request, StreamObserver<FeatureResponseDto> responseObserver) {
        try {
            org.locationtech.jts.geom.Geometry area = geometryConverter.fromProto(request.getGeometry());
            area.setSRID(gisProperties.getSrid());
            try (Stream<Feature> featureStream = featureRepository.streamFeaturesIntersecting(area)) {
                featureStream.forEach((feature) -> {
                    FeatureResponseDto featureResponseDto = convertFeatureToFeatureResponseDto(feature);
                    responseObserver.onNext(featureResponseDto);
                });
            }
            responseObserver.onCompleted();
        }catch (Exception exeption){
            responseObserver.onError(exeption);
        }
    }

    @Override
    public void getFeatureLocationStream(GetFeatureLocationStreamRequestDto request, StreamObserver<FeatureResponseDto> responseObserver) {
        getFeatureLocationConsumerStream(request,
                (ServerCallStreamObserver<FeatureResponseDto>) responseObserver,
                false);
    }

    @Override
    public void getFeatureLocationHistoryStream(GetFeatureLocationStreamRequestDto request, StreamObserver<FeatureResponseDto> responseObserver) {
        getFeatureLocationConsumerStream(request,
                (ServerCallStreamObserver<FeatureResponseDto>) responseObserver,
                true);
    }


    private void getFeatureLocationConsumerStream(GetFeatureLocationStreamRequestDto request,
                                                  ServerCallStreamObserver<FeatureResponseDto> responseObserver,
                                                  boolean consumeHistory) {
        ServerCallStreamObserver<FeatureResponseDto> serverObserver =
                responseObserver;

        AtomicBoolean cancelled = new AtomicBoolean(false);

        serverObserver.setOnCancelHandler(() -> {
            cancelled.set(true);
            System.out.println("Client cancelled the stream.");
        });

        String owner = request.getOwner();

        ConcurrentMessageListenerContainer<String, byte[]> container = kafkaSTreamFactory.createFilteredContainer(
                Topics.FEATURE, GroupIds.GIS_BRIDGE, owner, serverObserver, consumeHistory
        );
        container.start();

        String containerGroupId = container.getContainerProperties().getGroupId();
        serverObserver.setOnCancelHandler(() -> {
            System.out.println(String.format("Client cancelled stream for owner {}. Stopping container {}", owner, containerGroupId));

            CompletableFuture.runAsync(() -> kafkaSTreamFactory.stopAndRemoveContainer(containerGroupId))
                    .whenComplete((v, ex) -> {
                        if (ex != null) {
                            System.out.println(String.format("Error stopping container {}: {}", containerGroupId, ex.getMessage()));
                        }
                        try {
                            serverObserver.onCompleted();
                            container.stop();
                        } catch (Exception ignore) {
                            System.out.println(ignore.getMessage());
                        }
                    });
        });
    }

    private Feature convertCreateFeatureRequestDtoToFeature(CreateFeatureRequestDto createFeatureRequestDto) {
        org.locationtech.jts.geom.Geometry geometry = geometryConverter.fromProto(createFeatureRequestDto.getGeometry());
        Map<String, Object> properties = ProtoUtils.structToMap(createFeatureRequestDto.getProperties());
        Feature feature = new Feature(UUID.randomUUID(), createFeatureRequestDto.getOwner(),
                createFeatureRequestDto.getDescription(), geometry,
                LocalDateTime.now(), properties);
        return feature;
    }

    private Feature convertFeatureRequestDtoToFeature(FeatureRequestDto featureRequestDto) {
        org.locationtech.jts.geom.Geometry geometry = geometryConverter.fromProto(featureRequestDto.getGeometry());
        Map<String, Object> properties = ProtoUtils.structToMap(featureRequestDto.getProperties());
        Feature feature = new Feature(UUID.fromString(featureRequestDto.getId()), featureRequestDto.getOwner(),
                featureRequestDto.getDescription(), geometry,
                LocalDateTime.now(), properties);
        return feature;
    }

    private FeatureResponseDto convertFeatureToFeatureResponseDto(Feature feature) {
        Geometry geometry = geometryConverter.toProto(feature.getGeometry());
        Struct properties = ProtoUtils.mapToStruct(feature.getProperties());

        FeatureResponseDto featureResponseDto = FeatureResponseDto.newBuilder()
                .setId(feature.getId().toString())
                .setOwner(feature.getOwner())
                .setDescription(feature.getDescription())
                .setGeometry(geometry)
                .setTimestamp(feature.getTimestamp() != null ? feature.getTimestamp().toString() : "")
                .setProperties(properties).build();
        return featureResponseDto;
    }
}



