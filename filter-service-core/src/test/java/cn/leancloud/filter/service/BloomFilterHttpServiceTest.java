package cn.leancloud.filter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
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
    public void testCreateFilter() throws Exception {
        final var validPeriod = 1000;
        final var expectedInsertions = 1000000;
        final var fpp = 0.0001;
        final var request = mapper.createObjectNode();
        request.put("validPeriod", validPeriod);
        request.put("fpp", fpp);
        request.put("expectedInsertions", expectedInsertions);
        final var expectConfig = new ExpirableBloomFilterConfig(testingFilterName)
                .setValidPeriod(validPeriod)
                .setExpectedInsertions(expectedInsertions)
                .setFpp(fpp);
        final var expectedFilter = factory.createFilter(expectConfig);

        when(mockedManager.createFilter(expectConfig)).thenReturn(expectedFilter);

        final var response = service.create(testingFilterName, request).aggregate().get();
        assertThat(response.status().code()).isEqualTo(HttpStatus.CREATED.code());
        final var filter = new ObjectMapper()
                .readerFor(GuavaBloomFilter.class)
                .readValue(response.content(StandardCharsets.UTF_8));
        assertThat(filter).isNotNull().isInstanceOf(GuavaBloomFilter.class).isEqualTo(expectedFilter);
    }

    @Test
    public void testGetFilterInfo() throws Exception {
        final var expectedFilter = factory.createFilter(new ExpirableBloomFilterConfig(testingFilterName));
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(expectedFilter);
        final var filter = mapper.convertValue(service.getFilterInfo(testingFilterName), GuavaBloomFilter.class);
        assertThat(filter).isEqualTo(expectedFilter);
    }

    @Test
    public void testList() {
        final var expectedNames = List.of("Filter1", "Filter2", "Filter3");
        when(mockedManager.getAllFilterNames()).thenReturn(expectedNames);

        final var res = service.list();
        for (int i = 0; i < res.size(); i++) {
            assertThat(res.get(i).textValue()).isEqualTo(expectedNames.get(i));
        }
    }

    @Test
    public void testCheckAndSetValueIsNull() {
        final var param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.checkAndset(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testCheckAndSetValueIsNotString() {
        final var param = mapper.createObjectNode();
        param.put("value", 12345);
        assertThatThrownBy(() -> service.checkAndset(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testCheckAndSet() throws Exception {
        final var testingValue = "testing-value";
        final var testingFilter = factory.createFilter(new ExpirableBloomFilterConfig(testingFilterName));
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final var param = mapper.createObjectNode();
        param.put("value", testingValue);
        final var res = service.checkAndset(testingFilterName, param);
        assertThat(res.isBoolean()).isTrue();
        assertThat(res.asBoolean()).isFalse();
        assertThat(testingFilter.mightContain(testingValue)).isTrue();
    }

    @Test
    public void testMultiSetValueIsNull() {
        final var param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.multiCheckAndSet(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testMultiSetValueIsNotArray() {
        final var param = mapper.createObjectNode();
        param.put("values", "12345");
        assertThatThrownBy(() -> service.multiCheckAndSet(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testMultiSet() throws Exception {
        final var testingValues = List.of("testing-value", "testing-value2", "testing-value3");
        final var testingFilter = factory.createFilter(new ExpirableBloomFilterConfig(testingFilterName));
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final var values = mapper.createArrayNode();
        testingValues.forEach(values::add);
        final var param = mapper.createObjectNode();
        param.set("values", values);
        final var res = service.multiCheckAndSet(testingFilterName, param);
        assertThat(res.isArray()).isTrue();
        for (final var mightContain : res) {
            assertThat(mightContain.isBoolean()).isTrue();
            assertThat(mightContain.asBoolean()).isFalse();
        }

        for (final var value : testingValues) {
            assertThat(testingFilter.mightContain(value)).isTrue();
        }
    }

    @Test
    public void testCheckValueIsNull() {
        final var param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.check(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testCheckValueIsNotString() {
        final var param = mapper.createObjectNode();
        param.put("value", 12345);
        assertThatThrownBy(() -> service.check(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testCheck() throws Exception {
        final var testingValue = "testing-value";
        final var testingFilter = factory.createFilter(new ExpirableBloomFilterConfig(testingFilterName));
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final var param = mapper.createObjectNode();
        param.put("value", testingValue);
        final var res = service.check(testingFilterName, param);
        assertThat(res.asBoolean()).isFalse();
        assertThat(testingFilter.mightContain(testingValue)).isFalse();
    }

    @Test
    public void testMultiCheckAndSetValueIsNull() {
        final var param = mapper.createObjectNode();
        assertThatThrownBy(() -> service.multiCheck(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("required parameter");
    }

    @Test
    public void testMultiCheckAndSetValueIsNotArray() {
        final var param = mapper.createObjectNode();
        param.put("values", "12345");
        assertThatThrownBy(() -> service.multiCheck(testingFilterName, param))
                .isInstanceOf(BadParameterException.class)
                .hasMessageContaining("invalid parameter");
    }

    @Test
    public void testMultiCheckAndSet() throws Exception {
        final var testingValue = List.of("testing-value", "testing-value2", "testing-value3");
        final var testingFilter = factory.createFilter(new ExpirableBloomFilterConfig(testingFilterName));
        when(mockedManager.safeGetFilter(testingFilterName)).thenReturn(testingFilter);
        final var values = mapper.createArrayNode();
        testingValue.forEach(values::add);
        final var param = mapper.createObjectNode();
        param.set("values", values);
        final var res = service.multiCheck(testingFilterName, param);
        for (final var node : res) {
            assertThat(node.asBoolean()).isFalse();
        }
    }

    @Test
    public void testRemove() throws Exception{
        final var res = service.remove(testingFilterName).aggregate().get();
        assertThat(res.status()).isEqualTo(HttpStatus.OK);
        verify(mockedManager).remove(testingFilterName);
    }
}