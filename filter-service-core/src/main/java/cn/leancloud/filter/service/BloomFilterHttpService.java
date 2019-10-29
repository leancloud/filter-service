package cn.leancloud.filter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.annotation.*;

import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkNotNull;
import static cn.leancloud.filter.service.ServiceParameterPreconditions.checkParameter;

/**
 * An http service powered by armeria to expose RESTFul APIs for Bloom filter operations.
 */
@ExceptionHandler(GlobalExceptionHandler.class)
public final class BloomFilterHttpService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BloomFilterManager<?, ? super ExpirableBloomFilterConfig> bloomFilterManager;

    public BloomFilterHttpService(BloomFilterManager<?, ? super ExpirableBloomFilterConfig> bloomFilterManager) {
        this.bloomFilterManager = bloomFilterManager;
    }

    @Put("/{name}")
    public HttpResponse create(@Param String name,
                               @RequestObject JsonNode req) {
        final JsonNode expectedInsertions = req.get("expectedInsertions");
        final JsonNode fpp = req.get("fpp");
        final JsonNode validPeriod = req.get("validPeriod");
        final var config = new ExpirableBloomFilterConfig(name);

        if (expectedInsertions != null) {
            config.setExpectedInsertions(expectedInsertions.intValue());
        }
        if (fpp != null) {
            config.setFpp(fpp.doubleValue());
        }

        if (validPeriod != null) {
            config.setValidPeriod(validPeriod.longValue());
        }

        final BloomFilter filter = bloomFilterManager.createFilter(config);
        return HttpResponse.of(HttpStatus.CREATED,
                MediaType.JSON_UTF_8,
                MAPPER.valueToTree(filter).toString());
    }

    @Get("/{name}")
    public JsonNode getFilterInfo(@Param String name) throws FilterNotFoundException {
        var filter = bloomFilterManager.safeGetFilter(name);
        return MAPPER.valueToTree(filter);
    }

    @Get("/list")
    public JsonNode list() {
        final var response = MAPPER.createArrayNode();

        for (final var name : bloomFilterManager.getAllFilterNames()) {
            response.add(name);
        }

        return response;
    }

    @Post("/{name}/check")
    public JsonNode check(@Param String name,
                          @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final var testingValue = checkNotNull("value", req.get("value"));
        checkParameter("value", testingValue.isTextual(), "expect string type");

        final var filter = bloomFilterManager.safeGetFilter(name);
        final var contain = filter.mightContain(testingValue.textValue());
        return BooleanNode.valueOf(contain);
    }

    @Post("/{name}/multi-check")
    public JsonNode multiCheck(@Param String name,
                               @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final var values = checkNotNull("values", req.get("values"));
        checkParameter("values", values.isArray(), "expect Json array");

        final var filter = bloomFilterManager.safeGetFilter(name);
        final var response = MAPPER.createArrayNode();
        for (final var value : values) {
            response.add(value.isTextual() && filter.mightContain(value.textValue()));
        }
        return response;
    }

    @Post("/{name}/check-and-checkAndSet")
    public JsonNode checkAndSet(@Param String name,
                                @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final var testingValue = checkNotNull("value", req.get("value"));
        checkParameter("value", testingValue.isTextual(), "expect string type");

        final var filter = bloomFilterManager.safeGetFilter(name);
        final var contain = !filter.set(testingValue.textValue());
        return BooleanNode.valueOf(contain);
    }

    @Post("/{name}/multi-check-and-checkAndSet")
    public JsonNode multiCheckAndSet(@Param String name,
                                     @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final var values = checkNotNull("values", req.get("values"));
        checkParameter("values", values.isArray(), "expect Json array");

        final var filter = bloomFilterManager.safeGetFilter(name);
        final var response = MAPPER.createArrayNode();
        for (final var value : values) {
            if (value.isTextual()) {
                response.add(BooleanNode.valueOf(!filter.set(value.textValue())));
            } else {
                response.add(BooleanNode.FALSE);
            }
        }
        return response;
    }

    @Delete("/{name}")
    public HttpResponse remove(@Param String name) {
        bloomFilterManager.remove(name);
        return HttpResponse.of(HttpStatus.OK);
    }
}
