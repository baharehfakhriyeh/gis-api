package com.fkhr.gisapi.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.management.InstanceNotFoundException;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<Object> handleCustomException(CustomException customException){
        return buildResponseEntity(customException);
    }

    @ExceptionHandler(InstanceNotFoundException.class)
    protected ResponseEntity<Object> handleInstanceNotFoundException(InstanceNotFoundException exception){
        return buildResponseEntity(new CustomException(HttpStatus.NOT_FOUND, exception));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleException(Exception exception){
        return buildResponseEntity(new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, exception));
    }

    private ResponseEntity<Object> buildResponseEntity(CustomException exception) {
        exception.printStackTrace();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNodes = objectMapper.createObjectNode();
        jsonNodes.put("message", exception.getMessage());
        jsonNodes.put("code", exception.getCode());
        jsonNodes.put("cause", exception.getCause() != null ? exception.getCause().toString() : "");
        jsonNodes.put("time", exception.getTime().toString());
        return new ResponseEntity(jsonNodes, exception.getStatus());
    }
}
