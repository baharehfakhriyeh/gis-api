package com.fkhr.gisapi.repository;

import com.fkhr.gisapi.model.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeatureRepository extends JpaRepository<Feature, UUID> {
}
