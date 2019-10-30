# Filter Service
Filter-Service is a daemon network service which is used to expose bloom filters and operations by RESTFul API. There's already a great daemon like bloom filter lib called [bloomd](https://github.com/armon/bloomd) which has great performance results, but we still write our own because performance is not our first priority but monitorability and client dependency free is (Yes, HTTP still needs client but it is far more common and versatile). So here is Filter-Service.  

## Features

* Scalable non-blocking core allows for many connected clients and concurrent operations
* Implements scalable bloom filters, allowing dynamic filter sizes
* All the bloom filter can have a expiration time and can be removed automatically after expire
* Spit metrics by [micrometer](https://github.com/micrometer-metrics/micrometer) which can bridge to many popular monitoring tools
* Provide RESTFul API, which is convient to use and test

## Usage

Filter-Service requires Java 8 or  newer  to build and run. So please ensure that the `JAVA_HOME` environment variable points to a valid JDK then do the following steps.

1. Download the latest `filter-service.tar.gz` file from the releases list [here](https://github.com/leancloud/filter-service/releases), and uncompressed this package to your local directory;
2. Under the extracted directory `filter-service`, execute `./bin/filter-service` to run Filter-Service on port 8080 by default;
3. Please using the `-h` option to browse other configurable options for Filter-Service;
4. The doc for all the available API is written in [Swagger](https://swagger.io/) and is put under the `doc` directory [at here](https://github.com/leancloud/filter-service/blob/master/doc/bloom-filter-swagger.yaml). Please use a swagger rendering tool to check those API. If you don't have a swagger rendering tool available, please consider import the [bloom-filter-swagger.yaml](https://github.com/leancloud/filter-service/blob/master/doc/bloom-filter-swagger.yaml) file to [Swagger Editor](https://editor.swagger.io/) to browse.

## Metrics

Filter-Service spit a lot of metrics like QPS of all the APIs, current connections, active worker threads size, requests queue size etc. We are using [micrometer](https://github.com/micrometer-metrics/micrometer), a metrics facade for many popular monitoring tools, to collect these metrics. Please check the docunment on [Micrometer Document](https://micrometer.io/docs) for how to bridge micrometer to your monitoring system.

## Performance

We provide benchmark tools under directory: `filter-service-core/bin/`. It covers all the crucial parts of Filter-Service. You can test Filter-Service with them on your local machine.

At first, you need to install [wrk](https://github.com/wg/wrk) which is used by all the benchmark tools.

Then, taking `check-and-set` benchmark as example, you can run the script like this:
```
 ./filter-service-core/bin/check-and-set-benchmark.sh
```

The test results will show after 30s like:
```
Filter: "check-set-bench" created.
Running 30s test @ http://127.0.0.1:8080/
  4 threads and 20 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   605.86us    2.98ms 107.85ms   98.39%
    Req/Sec    15.57k     4.66k   31.76k    64.87%
  Latency Distribution
     50%  203.00us
     75%  426.00us
     90%  785.00us
     99%    5.83ms
  1865253 requests in 30.10s, 268.61MB read
Requests/sec:  61964.80
Transfer/sec:      8.92MB
Filter: "check-set-bench" deleted.
```

This is test on my machine with java 1.8.0_181, 2.3 GHz Intel Core i5 cpu and16G mem. Please remember to warm up before your real test. You can see from the result above that Filter-Service can process more than 60k requests per seconds. I think it's good enough in most cases.

## Doc service

`DocService` is a feature powered by [Armeria](https://line.github.io/armeria/index.html). It is a single-page web application by which we can browse even invoke all the available operations on Filter-Service. It's a convienent tool for testing.

By default, the `DocService` is disabled. To enable it, please run Filter-Service with `-d` option. Asume we are running Filter-Service on port 8080:
```
./filter-service-core/bin/filter-service -d -p 8080
```

After Filter-Service start up, we can open  `http://localhost:8080/docs` on the web browser and see the following screen:

![08D3D3A6-A4CC-4121-BD62-3AD88001B2E0](https://user-images.githubusercontent.com/1115061/67844505-cd24eb80-fb38-11e9-925c-97a1cef78251.png)

On the left side of the screen shows all the available operations of Filter-Service, you can use them to play with Filter-Service. Please refer [this doc](https://line.github.io/armeria/server-docservice.html) for more information about the `DocService` on Armeria.

## License

Copyright 2019 LeanCloud. Released under the  [MIT License](https://github.com/leancloud/filter-service/blob/master/LICENSE.md) .

