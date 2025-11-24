package com.fkhr.gisapi.utils;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProtoUtils {

    // ================= Struct -> Map =================
    public static Map<String, Object> structToMap(Struct struct) {
        Map<String, Object> map = new HashMap<>();
        struct.getFieldsMap().forEach((k,v) -> map.put(k, valueToObject(v)));
        return map;
    }

    private static Object valueToObject(Value value) {
        switch (value.getKindCase()) {
            case BOOL_VALUE:
                return value.getBoolValue();
            case NUMBER_VALUE:
                return value.getNumberValue();
            case STRING_VALUE:
                return value.getStringValue();
            case STRUCT_VALUE:
                return structToMap(value.getStructValue());
            case LIST_VALUE:
                return listValueToList(value.getListValue());
            case KIND_NOT_SET:
            default:
                return null;
        }
    }

    private static List<Object> listValueToList(ListValue listValue) {
        return listValue.getValuesList().stream().map(ProtoUtils::valueToObject).collect(Collectors.toList());
    }

    // ================= Map -> Struct =================
    public static Struct mapToStruct(Map<String,Object> map) {
        Struct.Builder builder = Struct.newBuilder();
        map.forEach((k,v) -> builder.putFields(k, objectToValue(v)));
        return builder.build();
    }

    private static Value objectToValue(Object obj) {
        if (obj == null) return Value.newBuilder().setNullValueValue(0).build();
        else if (obj instanceof String s) return Value.newBuilder().setStringValue(s).build();
        else if (obj instanceof Boolean b) return Value.newBuilder().setBoolValue(b).build();
        else if (obj instanceof Number n) return Value.newBuilder().setNumberValue(n.doubleValue()).build();
        else if (obj instanceof Map<?,?> map) return Value.newBuilder().setStructValue(mapToStruct((Map<String,Object>)map)).build();
        else if (obj instanceof List<?> list) return Value.newBuilder().setListValue(listToListValue(list)).build();
        else throw new IllegalArgumentException("Unsupported type: " + obj.getClass());
    }

    private static ListValue listToListValue(List<?> list) {
        List<Value> values = list.stream().map(ProtoUtils::objectToValue).collect(Collectors.toList());
        return ListValue.newBuilder().addAllValues(values).build();
    }
}
