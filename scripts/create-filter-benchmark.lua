
unique_thread_id = tostring( {} ):sub(8)
counter = 0
request = function()
    counter = counter + 1
    filter_name = "" .. unique_thread_id .. counter
    path = "/bloomfilter/" .. filter_name
    wrk.headers["content-type"] = "application/json; charset=utf-8"
    return wrk.format("PUT", path, nil, "{\"expectedInsertions\": 10, \"fpp\": 0.1, \"validPeriod\": 5}")
end
