package com.fkhr.gisapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("gis")
public class GisProperties {
    int srid;

    public GisProperties() {
    }

    public GisProperties(int srid) {
        this.srid = srid;
    }

    public int getSrid() {
        return srid;
    }

    public void setSrid(int srid) {
        this.srid = srid;
    }
}
