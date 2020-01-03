package cn.leancloud.filter.service;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorsTest {
    @Test
    public void testUnknownError() {
        assertThat(Errors.UNKNOWN_ERROR.buildErrorInfoInJson().toString())
                .isEqualTo("{\"error\":\"unknown error\",\"code\":-1}");

        assertThat(Errors.UNKNOWN_ERROR.buildErrorInfoInJson("error msg").toString())
                .isEqualTo("{\"error\":\"error msg\",\"code\":-1}");
    }

    @Test
    public void testBadParameter() {
        assertThat(Errors.BAD_PARAMETER.buildErrorInfoInJson().toString())
                .isEqualTo("{\"error\":\"invalid parameter\",\"code\":1}");

        assertThat(Errors.BAD_PARAMETER.buildErrorInfoInJson("error msg").toString())
                .isEqualTo("{\"error\":\"error msg\",\"code\":1}");
    }

    @Test
    public void testFilterNotFound() {
        assertThat(Errors.FILTER_NOT_FOUND.buildErrorInfoInJson().toString())
                .isEqualTo("{\"error\":\"filter not found\",\"code\":2}");

        assertThat(Errors.FILTER_NOT_FOUND.buildErrorInfoInJson("error msg").toString())
                .isEqualTo("{\"error\":\"error msg\",\"code\":2}");
    }
}