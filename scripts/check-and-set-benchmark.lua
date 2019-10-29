
unique_thread_id = tostring( {} ):sub(8)
testing_value = 0
request = function()
    testing_value = testing_value + 1
    path = "/bloomfilter/check-set-bench/check-and-set"
    wrk.headers["content-type"] = "application/json; charset=utf-8"
    return wrk.format("POST", path, nil, "{\"value\": \"" .. testing_value .. "\"}")
end
