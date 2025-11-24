package com.fkhr.gisapi.utils;

import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GeometryConverter {

    private final GeometryFactory geometryFactory = new GeometryFactory();

    // ================= Proto -> JTS =================
    public Geometry fromProto(com.fkhr.gisapi.Geometry proto) {
        String type = proto.getType();
        ListValue coords = proto.getCoordinates();

        switch (type) {
            case "Point":
                return geometryFactory.createPoint(listToCoordinate(coords));
            case "LineString":
                return geometryFactory.createLineString(listToCoordinates(coords));
            case "Polygon":
                return listValueToPolygon(coords);
            default:
                throw new IllegalArgumentException("Unsupported geometry type: " + type);
        }
    }

    private Coordinate listToCoordinate(ListValue coords) {
        return new Coordinate(
                coords.getValues(0).getNumberValue(),
                coords.getValues(1).getNumberValue()
        );
    }

    private Coordinate[] listToCoordinates(ListValue listValue) {
        return listValue.getValuesList().stream()
                .map(v -> listToCoordinate(v.getListValue()))
                .toArray(Coordinate[]::new);
    }

    private Polygon listValueToPolygon(ListValue coords) {
        ListValue outerRing = coords.getValues(0).getListValue();
        LinearRing shell = geometryFactory.createLinearRing(listToCoordinates(outerRing));

        LinearRing[] holes = new LinearRing[coords.getValuesCount() - 1];
        for (int i = 1; i < coords.getValuesCount(); i++) {
            holes[i - 1] = geometryFactory.createLinearRing(listToCoordinates(coords.getValues(i).getListValue()));
        }

        return geometryFactory.createPolygon(shell, holes);
    }

    // ================= JTS -> Proto =================
    public com.fkhr.gisapi.Geometry toProto(Geometry geom) {
        if (geom instanceof Point point) {
            return com.fkhr.gisapi.Geometry.newBuilder()
                    .setType("Point")
                    .setCoordinates(pointCoordinates(point))
                    .build();
        } else if (geom instanceof LineString line) {
            return com.fkhr.gisapi.Geometry.newBuilder()
                    .setType("LineString")
                    .setCoordinates(lineCoordinates(line))
                    .build();
        } else if (geom instanceof Polygon polygon) {
            return com.fkhr.gisapi.Geometry.newBuilder()
                    .setType("Polygon")
                    .setCoordinates(polygonCoordinates(polygon))
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported geometry type: " + geom.getGeometryType());
        }
    }

    private ListValue pointCoordinates(Point point) {
        return ListValue.newBuilder()
                .addValues(Value.newBuilder().setNumberValue(point.getX()))
                .addValues(Value.newBuilder().setNumberValue(point.getY()))
                .build();
    }

    private ListValue lineCoordinates(LineString line) {
        List<Value> coords = java.util.Arrays.stream(line.getCoordinates())
                .map(c -> Value.newBuilder()
                        .setListValue(ListValue.newBuilder()
                                .addValues(Value.newBuilder().setNumberValue(c.x))
                                .addValues(Value.newBuilder().setNumberValue(c.y))
                                .build())
                        .build())
                .collect(Collectors.toList());
        return ListValue.newBuilder().addAllValues(coords).build();
    }

    private ListValue polygonCoordinates(Polygon polygon) {
        List<Value> rings = new java.util.ArrayList<>();
        rings.add(ringCoordinates(polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            rings.add(ringCoordinates(polygon.getInteriorRingN(i)));
        }
        return ListValue.newBuilder().addAllValues(rings).build();
    }

    private Value ringCoordinates(LineString ring) {
        List<Value> coords = java.util.Arrays.stream(ring.getCoordinates())
                .map(c -> Value.newBuilder()
                        .setListValue(ListValue.newBuilder()
                                .addValues(Value.newBuilder().setNumberValue(c.x))
                                .addValues(Value.newBuilder().setNumberValue(c.y))
                                .build())
                        .build())
                .collect(Collectors.toList());
        return Value.newBuilder().setListValue(ListValue.newBuilder().addAllValues(coords)).build();
    }
}

