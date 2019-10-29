package cn.leancloud.filter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public enum Errors {
    UNKNOWN_ERROR(-1, "unknown error"),
    NONE(0, "none"),
    BAD_PARAMETER(1, "invalid parameter"),
    FILTER_NOT_FOUND(2, "filter not found");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final short code;
    private final String defaultErrorMsg;

    Errors(int code, String defaultErrorMsg) {
        this.code = (short) code;
        this.defaultErrorMsg = defaultErrorMsg;
    }

    public JsonNode buildErrorInfoInJson() {
        return buildErrorInfoInJson(defaultErrorMsg);
    }

    public JsonNode buildErrorInfoInJson(String errorMsg) {
        final ObjectNode error = MAPPER.createObjectNode();
        error.put("error", errorMsg);
        error.put("code", code);
        return error;
    }
}
