package com.fkhr.gisapi.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fkhr.gisapi.*;
import com.fkhr.gisapi.kafka.KafkaProducer;
import com.fkhr.gisapi.kafka.Topics;
import com.fkhr.gisapi.model.Feature;
import com.fkhr.gisapi.repository.FeatureRepository;
import com.fkhr.gisapi.utils.CustomError;
import com.fkhr.gisapi.utils.CustomException;
import com.fkhr.gisapi.utils.GeometryConverter;
import com.fkhr.gisapi.utils.ProtoUtils;
import com.google.protobuf.Struct;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@GrpcService
public class FeatureServiceImpl extends FeatureServiceGrpc.FeatureServiceImplBase {
    private final GeometryConverter geometryConverter;
    private final FeatureRepository featureRepository;
    private final KafkaProducer kafkaProducer;

    public FeatureServiceImpl(GeometryConverter geometryConverter, FeatureRepository featureRepository, KafkaProducer kafkaProducer) {
        this.geometryConverter = geometryConverter;
        this.featureRepository = featureRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void createFeature(CreateFeatureRequestDto request, StreamObserver<FeatureResponseDto> responseObserver) {
        try {
            Feature feature = convertCreateFeatureRequestDtoToFeature(request);
            feature = featureRepository.save(feature);
            FeatureResponseDto featureResponseDto = convertFeatureToFeatureResponseDto(feature);
            responseObserver.onNext(featureResponseDto);
            sendFeatureToKafka(feature);
        } catch (Exception exception) {
            responseObserver.onError(exception);
        }
        finally {
            responseObserver.onCompleted();
        }
    }

    private void sendFeatureToKafka(Feature feature) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String featureString = objectMapper.writeValueAsString(feature);
        kafkaProducer.send(Topics.FEATURE, featureString);
    }

    @Override
    public void getFeature(FeatureId request, StreamObserver<FeatureResponseDto> responseObserver){
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
        }
        finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getFeatureLocationStream(FeatureId request, StreamObserver<FeatureResponseDto> responseObserver) {

    }

    private Feature convertCreateFeatureRequestDtoToFeature(CreateFeatureRequestDto createFeatureRequestDto){
        org.locationtech.jts.geom.Geometry geometry = geometryConverter.fromProto(createFeatureRequestDto.getGeometry());
        Map<String, Object> properties = ProtoUtils.structToMap(createFeatureRequestDto.getProperties());
        Feature feature = new Feature(UUID.randomUUID(), createFeatureRequestDto.getOwner(),
                createFeatureRequestDto.getDescription(), geometry,
                LocalDateTime.now(), properties);
        return feature;
    }

    private Feature convertFeatureRequestDtoToFeature(FeatureRequestDto featureRequestDto){
        org.locationtech.jts.geom.Geometry geometry = geometryConverter.fromProto(featureRequestDto.getGeometry());
        Map<String, Object> properties = ProtoUtils.structToMap(featureRequestDto.getProperties());
        Feature feature = new Feature(UUID.fromString(featureRequestDto.getId()), featureRequestDto.getOwner(),
                featureRequestDto.getDescription(), geometry,
                LocalDateTime.now(), properties);
        return feature;
    }

    private FeatureResponseDto convertFeatureToFeatureResponseDto(Feature feature){
        Geometry geometry = geometryConverter.toProto(feature.getGeometry());
        Struct properties = ProtoUtils.mapToStruct(feature.getProperties());

        FeatureResponseDto featureResponseDto = FeatureResponseDto.newBuilder()
                .setId(feature.getId().toString())
                .setOwner(feature.getOwner())
                .setDescription(feature.getDescription())
                .setGeometry(geometry)
                .setTimestamp(feature.getTimestamp().toString())
                .setProperties(properties).build();
        return featureResponseDto;
    }
}



