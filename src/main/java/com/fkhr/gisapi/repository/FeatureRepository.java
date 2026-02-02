package com.fkhr.gisapi.repository;


import com.fkhr.gisapi.model.Feature;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface FeatureRepository extends JpaRepository<Feature, UUID> {
    Optional<Feature> findTopByOwnerOrderByTimestampDesc(String owner);

    @Query(value = "SELECT DISTINCT ON (f.owner) * FROM feature f WHERE ST_Intersects(f.geometry, :area) " +
            "ORDER BY f.owner, f.timestamp DESC, f.id DESC", nativeQuery = true)
    Stream<Feature> streamFeaturesIntersecting(@Param("area") Geometry area);
}
