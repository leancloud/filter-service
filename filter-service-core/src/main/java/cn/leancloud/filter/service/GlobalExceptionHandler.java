package cn.leancloud.filter.service;

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

    @Override
    public HttpResponse handleException(ServiceRequestContext ctx, HttpRequest req, Throwable cause) {
        if (cause instanceof FilterNotFoundException) {
            return HttpResponse.of(HttpStatus.NOT_FOUND,
                    MediaType.JSON_UTF_8,
                    Errors.FILTER_NOT_FOUND.buildErrorInfoInJson().toString());
        } else if (cause instanceof BadParameterException) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST,
                    MediaType.JSON_UTF_8,
                    Errors.BAD_PARAMETER.buildErrorInfoInJson(cause.getMessage()).toString());
        } else if (cause instanceof IllegalArgumentException) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST,
                    MediaType.JSON_UTF_8,
                    Errors.BAD_PARAMETER.buildErrorInfoInJson().toString());
        }

        logger.error("Got unexpected exception on req {}", req, cause);
        return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
