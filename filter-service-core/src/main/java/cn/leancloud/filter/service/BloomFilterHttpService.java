package cn.leancloud.filter.service;

import cn.leancloud.filter.service.BloomFilterManager.CreateFilterResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.annotation.*;

import java.time.Duration;

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
        final JsonNode validPeriodAfterCreate = req.get("validPeriod") == null ?
                req.get("validPeriodAfterCreate") : req.get("validPeriod");
        final JsonNode extendValidPeriodAfterAccess = req.get("extendValidPeriodAfterAccess");
        final JsonNode overwrite = req.get("overwrite");
        final ExpirableBloomFilterConfig config = new ExpirableBloomFilterConfig();

        if (expectedInsertions != null) {
            config.setExpectedInsertions(expectedInsertions.intValue());
        }
        if (fpp != null) {
            config.setFpp(fpp.doubleValue());
        }

        if (validPeriodAfterCreate != null) {
            config.setValidPeriodAfterCreate(Duration.ofSeconds(validPeriodAfterCreate.intValue()));
        }

        if (extendValidPeriodAfterAccess != null) {
            config.setExtendValidPeriodAfterAccess(Duration.ofSeconds(extendValidPeriodAfterAccess.intValue()));
        }

        final CreateFilterResult<?> createResult;
        if (overwrite != null && overwrite.isBoolean() && overwrite.asBoolean()) {
            createResult = bloomFilterManager.createFilter(name, config, true);
        } else {
            createResult = bloomFilterManager.createFilter(name, config);
        }
        return HttpResponse.of(
                createResult.isCreated() ? HttpStatus.CREATED : HttpStatus.OK,
                MediaType.JSON_UTF_8,
                MAPPER.valueToTree(createResult.getFilter()).toString());
    }

    @Get("/{name}")
    public JsonNode getFilterInfo(@Param String name) throws FilterNotFoundException {
        final BloomFilter filter = bloomFilterManager.safeGetFilter(name);
        return MAPPER.valueToTree(filter);
    }

    @Get("/list")
    public JsonNode list() {
        final ArrayNode response = MAPPER.createArrayNode();

        for (final String name : bloomFilterManager.getAllFilterNames()) {
            response.add(name);
        }

        return response;
    }

    @Post("/{name}/check")
    public JsonNode check(@Param String name,
                          @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final JsonNode testingValue = checkNotNull("value", req.get("value"));
        checkParameter("value", testingValue.isTextual(), "expect string type");

        final BloomFilter filter = bloomFilterManager.safeGetFilter(name);
        final boolean contain = filter.mightContain(testingValue.textValue());
        return BooleanNode.valueOf(contain);
    }

    @Post("/{name}/multi-check")
    public JsonNode multiCheck(@Param String name,
                               @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final JsonNode values = checkNotNull("values", req.get("values"));
        checkParameter("values", values.isArray(), "expect Json array");

        final BloomFilter filter = bloomFilterManager.safeGetFilter(name);
        final ArrayNode response = MAPPER.createArrayNode();
        for (final JsonNode value : values) {
            response.add(value.isTextual() && filter.mightContain(value.textValue()));
        }
        return response;
    }

    @Post("/{name}/check-and-set")
    public JsonNode checkAndSet(@Param String name,
                                @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final JsonNode testingValue = checkNotNull("value", req.get("value"));
        checkParameter("value", testingValue.isTextual(), "expect string type");

        final BloomFilter filter = bloomFilterManager.safeGetFilter(name);
        final boolean contain = !filter.set(testingValue.textValue());
        return BooleanNode.valueOf(contain);
    }

    @Post("/{name}/multi-check-and-set")
    public JsonNode multiCheckAndSet(@Param String name,
                                     @RequestObject JsonNode req)
            throws FilterNotFoundException {
        final JsonNode values = checkNotNull("values", req.get("values"));
        checkParameter("values", values.isArray(), "expect Json array");

        final BloomFilter filter = bloomFilterManager.safeGetFilter(name);
        final ArrayNode response = MAPPER.createArrayNode();
        for (final JsonNode value : values) {
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
