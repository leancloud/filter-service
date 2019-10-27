package cn.leancloud.filter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlobalExceptionHandler implements ExceptionHandlerFunction {
    private static final Logger logger = LoggerFactory.getLogger(BloomFilterHttpService.class);

    private static final String FILTER_NOT_FOUND_RESPONSE;
    private static final String INVALID_JSON_RESPONSE;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        FILTER_NOT_FOUND_RESPONSE = errorJsonResponse("Filter not found.");
        INVALID_JSON_RESPONSE = errorJsonResponse("Invalid Json");
    }

    private static String errorJsonResponse(String errorMsg) {
        final ObjectNode error = MAPPER.createObjectNode();
        error.put("error", errorMsg);
        return error.toString();
    }

    @Override
    public HttpResponse handleException(ServiceRequestContext ctx, HttpRequest req, Throwable cause) {
        if (cause instanceof FilterNotFoundException) {
            return HttpResponse.of(HttpStatus.NOT_FOUND,
                    MediaType.JSON_UTF_8,
                    FILTER_NOT_FOUND_RESPONSE);
        } else if (cause instanceof BadParameterException) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST,
                    MediaType.JSON_UTF_8,
                    errorJsonResponse(cause.getMessage()));
        } else if (cause instanceof JsonProcessingException) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST,
                    MediaType.JSON_UTF_8,
                    INVALID_JSON_RESPONSE);
        }

        logger.error("Got unexpected exception on req {}", req, cause);
        return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
