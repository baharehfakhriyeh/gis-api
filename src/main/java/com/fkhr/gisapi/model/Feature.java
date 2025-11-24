package com.fkhr.gisapi.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
public class Feature {
    @Id
    UUID id;
    String owner;
    String description;
    @Column(columnDefinition = "geometry(Geometry,4326)")
    Geometry geometry;
    LocalDateTime timestamp;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    Map<String, Object> properties;

    public Feature() {
    }

    public Feature(UUID id, String owner, String description, Geometry geometry, LocalDateTime timestamp,
                   Map<String, Object> properties) {
        this.id = id;
        this.owner = owner;
        this.description = description;
        this.geometry = geometry;
        this.timestamp = timestamp;
        this.properties = properties;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String ownerId) {
        this.owner = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String name) {
        this.description = name;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
