# Filter Service

[![Build Status](https://api.travis-ci.org/leancloud/filter-service.svg?branch=master)](https://travis-ci.org/leancloud/filter-service)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Filter-Service is a daemon network service which is used to expose bloom filters and operations by RESTFul API. There's already a great daemon like bloom filter lib called [bloomd](https://github.com/armon/bloomd) which has great performance results, but we still write our own because performance is not our first priority but monitorability and client dependency free is (Yes, HTTP still needs client but it is far more common and versatile). So here is Filter-Service.  

## Features

* Scalable non-blocking core allows for many connected clients and concurrent operations
* Implements scalable bloom filters, allowing dynamic filter sizes
* All the bloom filters are persistent on local disk periodically and can be recovered on process rebooting
* All the bloom filters can have a expiration time and can be removed automatically after expire
* Spit metrics by [micrometer](https://github.com/micrometer-metrics/micrometer) which can bridge to many popular monitoring tools
* Provide RESTFul API, which is convient to use and test

## Usage

Filter-Service requires Java 8 or  newer  to build and run. So please ensure that the `JAVA_HOME` environment variable points to a valid JDK then do the following steps.

1. Download the latest `filter-service.tar.gz` file from the releases list [here](https://github.com/leancloud/filter-service/releases), and uncompressed this package to your local directory;
2. Under the extracted directory `filter-service`, execute `./bin/filter-service` to run Filter-Service on port 8080 by default;
3. Please using the `-h` option and checking `./config/configuration.xml` file to browse all the available configurable options for Filter-Service;
4. The doc for all the available API is written in [Swagger](https://swagger.io/) and is put under the `doc` directory [at here](https://github.com/leancloud/filter-service/blob/master/doc/bloom-filter-swagger.yaml). Please use a swagger rendering tool to check those API. If you don't have a swagger rendering tool available, please consider import the [bloom-filter-swagger.yaml](https://github.com/leancloud/filter-service/blob/master/doc/bloom-filter-swagger.yaml) file to [Swagger Editor](https://editor.swagger.io/) to browse.

## Persistence

Filter-Service uses a file named `snapshot.db`in a dedicated directory given in `config/configuration.xml` to save all the filters every several seconds. It will lock this directory so only one Filter-Service can use the same directory on the same time. During persistence operation, firstly Filter-Service will save filters to a file named `snapshot.tmp`. After that, if everything is OK, it will rename `snapshot.tmp` to `snapshot.db` in an atomic operation. 

On startup, after Filter-Service have locked the working directory, it tries to find the `snapshot.db` file and recover filters from that file. It recovers in a one-by-one process. Reading all the bytes needed to recover a filter, checking some stuff, then it deserialize filter from these bytes. It repeats this process to recover the rest filters until all the bytes in file have read. If the bytes for a filter is not correct, like checksum unmatched, magic unmached, not enough bytes etc. It will stop the recovering process immediately because all the bytes afterwards is definitely corrupted. You can use configuration to let Filter-Service throws a exception and does not recover anything on this situation or let Filter-Service just tolerates the corrupted file and tries to recover as many filters as it can from it. 

Sometimes before you start a Filter-Service, you can see the `snapshot.tmp` stand by a `snapshot.db`. It means the last persistence filter operation was not fully completed. Maybe the process crashed unintentionly, or even the host machine is down when writing filters to `snapshot.tmp`. You can delete `snapshot.tmp` file or leave it as it is. Latter in the future, we may support to recover from either `snapshot.tmp` or `snapshot.db` to reduce the lost data.  

## Metrics

Filter-Service spits a lot of metrics like QPS of all the APIs, current connections, active worker threads size, requests queue size, etc. We are using [Micrometer](https://github.com/micrometer-metrics/micrometer), a metrics facade for many popular monitoring tools, to collect these metrics. Please check the docunment on [Micrometer Document](https://micrometer.io/docs) for more informations.

For simplicity, we are taking [DefaultMetricsService](https://github.com/leancloud/filter-service/blob/master/filter-service-core/src/main/java/cn/leancloud/filter/service/DefaultMetricsService.java) as a default implemntation for [MetricsService](https://github.com/leancloud/filter-service/blob/master/filter-service-metrics/src/main/java/cn/leancloud/filter/service/metrics/MetricsService.java) to handle metrics. It only use [LoggingMeterRegistry](https://static.javadoc.io/io.micrometer/micrometer-core/1.1.3/io/micrometer/core/instrument/logging/LoggingMeterRegistry.html) , from `Micrometer`, to log all the available metrics to a file. If you think it's not enough, you can implrement your own `MetricsService`, then package it as a SPI implementation and put it to the extracted directory `filter-service`. Filter-Service will use [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the `MetricsService`you implemented from the provided Jar file and use it to handle metrics.

For example, assume that you are using [Prometheus](https://prometheus.io/) to collect metrcis and we are taking [the codes from Micrometer](https://micrometer.io/docs/registry/prometheus) as an example.

1. You can implement `MetricsService` like this:

```java
package cn.leancloud.example;

...

public final class PrometheusMetricsService implements MetricsService {
    @Override
    public MeterRegistry createMeterRegistry() throws Exception {
        final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/prometheus", httpExchange -> {
                final String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
            return prometheusRegistry;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

2. Write a file with name `cn.leancloud.filter.service.metrics.MetricsService` and with content:

```
cn.leancloud.example.PrometheusMetricsService
```

and put this file in path `resources/META-INFO/services`.  So your project structure would be like:

```
src
 `-- main.
      |--java
      |   `--cn.leancloud.example
      |           `-- PrometheusMetricsService.java
      `--resources
          `-- META-INFO
               `--services
					 `-- cn.leancloud.filter.service.metrics.MetricsService
```

3. Package your project into a Jar;
4. Put your Jar file and all your dependent Jar files to `./filter-service`;
5. Run Filter-Service with `./filter-service/bin/filter-service` and wait metrics be collected to `Prometheus` using `PrometheusMeterRegistry`;

## Performance

When you uncompress the `filter-service.tar.gz` file from the release list, the benchmakr tools is right at your hand under `./filter-service/bin`, the same path with the script to run Filter-Service daemon mentioned above. Those benchmark tools cover all the crucial parts of Filter-Service. You can test Filter-Service with them on your local machine.

At first, you need to install [wrk](https://github.com/wg/wrk) which is used by all our benchmark tools.

Then, taking `check-and-set` benchmark as example, you can run the benchmark like this:

```
 ./bin/check-and-set-benchmark.sh
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

This is tested on my machine with java 1.8.0_181, 2.3 GHz Intel Core i5 cpu and 16G mem. Please remember to run several times to warm up JVM before your real test. You can see from the result above that Filter-Service can process more than 60k requests per seconds. I think it's good enough in most cases.

## Doc service

`DocService` is a feature powered by [Armeria](https://line.github.io/armeria/index.html). It is a single-page web application by which we can browse or invoke any of the available operations on Filter-Service. It's a convienent tool for testing.

By default, the `DocService` is disabled. To enable it, please run Filter-Service with `-d` option. Asume we are running Filter-Service on port 8080:

```
./bin/filter-service -d -p 8080
```

After Filter-Service start up, we can open `http://localhost:8080/docs` on the web browser and see the following screen:

![08D3D3A6-A4CC-4121-BD62-3AD88001B2E0](https://user-images.githubusercontent.com/1115061/67844505-cd24eb80-fb38-11e9-925c-97a1cef78251.png)

On the left side of the screen shows all the available operations of Filter-Service, you can use them to play with Filter-Service. Please refer [this doc](https://line.github.io/armeria/server-docservice.html) for more information about the `DocService` on Armeria.

## License

Copyright 2019 LeanCloud. Released under the [MIT License](https://github.com/leancloud/filter-service/blob/master/LICENSE.md) .

