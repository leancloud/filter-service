package cn.leancloud.filter.service;

import cn.leancloud.filter.service.BloomFilterManager.CreateFilterResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BloomFilterHttpServiceTest {
    private static final String testingFilterName = "TestingFilter";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final GuavaBloomFilterFactory factory = new GuavaBloomFilterFactory();

    private BloomFilterManager<GuavaBloomFilter, ExpirableBloomFilterConfig> mockedManager;
    private BloomFilterHttpService service;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockedManager = Mockito.mock(BloomFilterManager.class);
        service = new BloomFilterHttpService(mockedManager);
    }

    @Test
    public void testForceCreateFilter() throws Exception {
        final int validPeriodAfterCreate = 1000;
        final int validPeriodAfterAccess = 100;
        final int expectedInsertions = 1000000;
        final double fpp = 0.0001;
        final ObjectNode request = mapper.createObjectNode();
        request.put("validPeriodAfterCreate", validPeriodAfterCreate);
        request.put("validPeriodAfterAccess", validPeriodAfterAccess);
        request.put("fpp", fpp);
        request.put("expectedInsertions", expectedInsertions);
        request.put("overwrite", true);
        final ExpirableBloomFilterConfig expectConfig = new ExpirableBloomFilterConfig(expectedInsertions, fpp);
        expectConfig.setValidPeriodAfterCreate(Duration.ofSeconds(validPeriodAfterCreate));
        expectConfig.setValidPeriodAfterAccess(Duration.ofSeconds(validPeriodAfterAccess));
        final GuavaBloomFilter expectedFilter = factory.createFilter(expectConfig);
        final CreateFilterResult<GuavaBloomFilter> result = new CreateFilterResult<>(expectedFilter, true);

        when(mockedManager.createFilter(testingFilterName, expectConfig, true)).thenReturn(result);

        final AggregatedHttpResponse response = service.create(testingFilterName, request).aggregate().get();
        assertThat(response.status().code()).isEqualTo(HttpStatus.CREATED.code());
        final JsonNode responseInJson = mapper.readTree(response.content(StandardCharsets.UTF_8));
        final GuavaBloomFilter filter = new ObjectMapper()
                .readerFor(GuavaBloomFilter.class)
                .readValue(responseInJson);
        assertThat(filter).isNotNull().isEqualTo(expectedFilter);
    }

    @Test
    public void testCreateAlreadyExistsFilter() throws Exception {
        final ObjectNode request = mapper.createObjectNode();
        final ExpirableBloomFilterConfig expectConfig = new ExpirableBloomFilterConfig();
        final GuavaBloomFilter expectedFilter = factory.createFilter(expectConfig);
        final CreateFilterResult<GuavaBloomFilter> result = new CreateFilterResult<>(expectedFilter, false);

        when(mockedManager.createFilter(testingFilterName, expectConfig)).thenReturn(result);

        final AggregatedHttpResponse response = service.create(testingFilterName, request).aggregate().get();
        assertThat(response.status().code()).isEqualTo(HttpStatus.OK.code());
        final JsonNode responseInJson = mapper.readTree(response.content(StandardCharsets.UTF_8));
        final GuavaBloomFilter filter = new ObjectMapper()
                .readerFor(GuavaBloomFilter.class)
                .readValue(responseInJson);
        assertThat(filter).isNotNull().isEqualTo(expectedFilter);
    }

    @Test
    public void testGetFilterInfo() throws Exception {
        final GuavaBloomFilter expectedFilter = factory.createFilter(new ExpirableBloomFilterConfig());
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(expectedFilter);
        final GuavaBloomFilter filter = mapper.convertValue(service.getFilterInfo(testingFilterName), GuavaBloomFilter.class);
        assertThat(filter).isEqualTo(expectedFilter);
    }

    @Test
    public void testList() {
        final List<String> expectedNames = new ArrayList<>();
        expectedNames.add("Filter1");
        expectedNames.add("Filter2");
        expectedNames.add("Filter3");
        when(mockedManager.getAllFilterNames()).thenReturn(expectedNames);

        final JsonNode res = service.list();
        for (int i = 0; i < res.size(); i++) {
            assertThat(res.get(i).textValue()).isEqualTo(expectedNames.get(i));
        }
    }

    @Test
    public void testCheckAndSetValueIsNull() {
        final ObjectNode param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.checkAndSet(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testCheckAndSetValueIsNotString() {
        final ObjectNode param = mapper.createObjectNode();
        param.put("value", 12345);
        assertThatThrownBy(() -> service.checkAndSet(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testCheckAndSet() throws Exception {
        final String testingValue = "testing-value";
        final GuavaBloomFilter testingFilter = factory.createFilter(new ExpirableBloomFilterConfig());
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final ObjectNode param = mapper.createObjectNode();
        param.put("value", testingValue);
        final JsonNode res = service.checkAndSet(testingFilterName, param);
        assertThat(res.isBoolean()).isTrue();
        assertThat(res.asBoolean()).isFalse();
        assertThat(testingFilter.mightContain(testingValue)).isTrue();
    }

    @Test
    public void testMultiSetValueIsNull() {
        final ObjectNode param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.multiCheckAndSet(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testMultiSetValueIsNotArray() {
        final ObjectNode param = mapper.createObjectNode();
        param.put("values", "12345");
        assertThatThrownBy(() -> service.multiCheckAndSet(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testMultiSet() throws Exception {
        final List<String> testingValues = new ArrayList<>();
        testingValues.add("testing-value1");
        testingValues.add("testing-value2");
        testingValues.add("testing-value3");
        final GuavaBloomFilter testingFilter = factory.createFilter(new ExpirableBloomFilterConfig());
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final ArrayNode values = mapper.createArrayNode();
        testingValues.forEach(values::add);
        final ObjectNode param = mapper.createObjectNode();
        param.set("values", values);
        final JsonNode res = service.multiCheckAndSet(testingFilterName, param);
        assertThat(res.isArray()).isTrue();
        for (final JsonNode mightContain : res) {
            assertThat(mightContain.isBoolean()).isTrue();
            assertThat(mightContain.asBoolean()).isFalse();
        }

        for (final String value : testingValues) {
            assertThat(testingFilter.mightContain(value)).isTrue();
        }
    }

    @Test
    public void testCheckValueIsNull() {
        final ObjectNode param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.check(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testCheckValueIsNotString() {
        final ObjectNode param = mapper.createObjectNode();
        param.put("value", 12345);
        assertThatThrownBy(() -> service.check(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testCheck() throws Exception {
        final String testingValue = "testing-value";
        final GuavaBloomFilter testingFilter = factory.createFilter(new ExpirableBloomFilterConfig());
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final ObjectNode param = mapper.createObjectNode();
        param.put("value", testingValue);
        final JsonNode res = service.check(testingFilterName, param);
        assertThat(res.asBoolean()).isFalse();
        assertThat(testingFilter.mightContain(testingValue)).isFalse();
    }

    @Test
    public void testMultiCheckAndSetValueIsNull() {
        final ObjectNode param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.multiCheck(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testMultiCheckAndSetValueIsNotArray() {
        final ObjectNode param = mapper.createObjectNode();
        param.put("values", "12345");
        assertThatThrownBy(() -> service.multiCheck(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testMultiCheckAndSet() throws Exception {
        final List<String> testingValues = new ArrayList<>();
        testingValues.add("testing-value1");
        testingValues.add("testing-value2");
        testingValues.add("testing-value3");
        final GuavaBloomFilter testingFilter = factory.createFilter(new ExpirableBloomFilterConfig());
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final ArrayNode values = mapper.createArrayNode();
        testingValues.forEach(values::add);
        final ObjectNode param = mapper.createObjectNode();
        param.set("values", values);
        final JsonNode res = service.multiCheck(testingFilterName, param);
        for (final JsonNode node : res) {
            assertThat(node.asBoolean()).isFalse();
        }
    }

    @Test
    public void testRemove() throws Exception {
        final AggregatedHttpResponse res = service.remove(testingFilterName).aggregate().get();
        assertThat(res.status()).isEqualTo(HttpStatus.OK);
        verify(mockedManager).remove(testingFilterName);
    }
}