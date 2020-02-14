# Filter Service

[![Build Status](https://api.travis-ci.org/leancloud/filter-service.svg?branch=master)](https://travis-ci.org/leancloud/filter-service)
[![Coverage Status](https://codecov.io/gh/leancloud/filter-service/branch/master/graph/badge.svg)](https://codecov.io/gh/leancloud/filter-service)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven](https://img.shields.io/github/release/leancloud/filter-service.svg)](https://github.com/leancloud/filter-service/releases)

Filter-Service is a daemon network service which is used to expose bloom filters and operations by RESTFul APIs. There's already a great daemon like bloom filter lib called [bloomd](https://github.com/armon/bloomd) which has great performance results, but we still write our own because performance is not our first priority but monitorability and client dependency free is (Yes, HTTP still needs client but it is far more common and versatile). So here is this lib.

## Features

* Scalable non-blocking core allows for many connected clients and concurrent operations
* Implements scalable bloom filters, allowing dynamic filter sizes
* All the bloom filters are persisted on local disk and can be recovered on process rebooting
* All the bloom filters can have a expiration time and can be removed automatically after expired
* Generate metrics by [micrometer](https://github.com/micrometer-metrics/micrometer) which can bridge to many popular monitoring tools
* Provide RESTFul APIs, which is convient to use and test
* Provide a health check API which could be used by orchestration service like Kubernetes to monitor the health of this service

## Usage

Filter-Service requires Java 8 or newer to build and run. So please ensure that the `JAVA_HOME` environment variable points to a valid JDK then do the following steps.

1. Download the latest `filter-service.tar.gz` file from the releases list [here](https://github.com/leancloud/filter-service/releases), then uncompress this package to your local directory;
2. Under the extracted directory `filter-service`, execute `./bin/filter-service` to run Filter-Service on port 8080 by default;
3. Please use the `-h` option and check `./config/configuration.yaml` file to browse all the available configurable options;
4. For all the available APIs please refer to [docs](https://leancloud.github.io/filter-service/);
5. To change the GC policy, log path, JVM heap size, please refer to script `./bin/filter-service` and export corresponding environment variables before running Filter-Service. Such as `export FILTER_SERVICE_HEAP_OPTS='-Xmx2G -Xms2G' ; ./bin/filter-service` to change the JVM heap size to 2G. Usually `FILTER_SERVICE_HEAP_OPTS` is the only environment variable you may would like to use.

## Persistence

Filter-Service uses a file named `snapshot.db`in a dedicated directory given in `config/configuration.yaml` to save all the filters every several seconds. It will lock this directory so only one Filter-Service can use the same directory on the same time. During persistence operation, firstly Filter-Service will save filters to a file named `snapshot.tmp`. After that, if everything is OK, it will rename `snapshot.tmp` to `snapshot.db` in an atomic operation. 

You can configurate when to save filters to a file by both of number of seconds and number of update operations occurred. In the example below, the behaviour will be to save:

* after 900 sec if at least 1 filter update operations occurred
* after 300 sec if at least 100 filter update operations occurred
* after 60 sec if at least 10000 filter update operations occurred

```yaml
triggerPersistenceCriteria:
  - periodInSeconds: 900
    updatesMoreThan: 1
  - periodInSeconds: 300
    updatesMoreThan: 100
  - periodInSeconds: 60
    updatesMoreThan: 10000
```

For the recovering process, on startup, after Filter-Service have locked the working directory, it tries to find the `snapshot.db` file and recover filters from that file. It recovers filters in a one-by-one process. Reading all the bytes needed to recover a filter, checking these bytes to see if they are valid, then it deserialize filter from these bytes. It repeats this process to recover the rest filters until all valid filters in the file have been deserialized. If the bytes for a filter is not valid, like checksum unmatched, magic unmached, no enough bytes to do deserialization etc, it will stop the recovering process immediately in that all the bytes afterwards is definitely corrupted. In this circumstance, you can configurate Filter-Service to stop working by throwing an exception or just tolerates the corrupted file and only recovers as many filters as it can, then continue working normally.

Sometimes before you start a Filter-Service, you can see a `snapshot.tmp` file stands by a `snapshot.db` file. It means the last persistence filter operation was not fully completed. Maybe the process crashed unintentionly, or even the host machine is down when writing filters to `snapshot.tmp`. You can leave `snapshot.tmp` file as it is. Filter-Service will recover from `snapshot.tmp` after it have read all filters from `snapshot.db`. If the `snapshot.tmp` file is corrupted, Filter-Service will not throw any exception no matter what your configurations are. It just tries it best to recover filters from `snapshot.tmp` and leaves those corrupted ones. You can tune the persistence interval to reduce the posibility of non-recoverable filters occur.

## Metrics

Filter-Service generates a lot of metrics like QPS of all the APIs, current connections, active worker threads size, requests queue size, etc. We are using [Micrometer](https://github.com/micrometer-metrics/micrometer), a metrics facade for many popular monitoring tools, to collect these metrics. Please check the docunment on [Micrometer Document](https://micrometer.io/docs) for more informations.

For simplicity, we are taking [DefaultMetricsService](https://github.com/leancloud/filter-service/blob/master/filter-service-core/src/main/java/cn/leancloud/filter/service/DefaultMetricsService.java) as a default implemntation for [MetricsService](https://github.com/leancloud/filter-service/blob/master/filter-service-metrics/src/main/java/cn/leancloud/filter/service/metrics/MetricsService.java) to handle metrics. It only use [LoggingMeterRegistry](https://static.javadoc.io/io.micrometer/micrometer-core/1.1.3/io/micrometer/core/instrument/logging/LoggingMeterRegistry.html) , from `Micrometer`, to log all the available metrics to a local file. If you think it's not enough, you can implrement your own `MetricsService`, then package it as a SPI implementation and put the packaged Jar file to the classpath of Filter-Service. Filter-Service will use [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the `MetricsService` you implemented and use it to handle metrics.

For example, assuming that you are using [Prometheus](https://prometheus.io/) to collect metrcis and we are taking [the codes from Micrometer](https://micrometer.io/docs/registry/prometheus) as an example.

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

and put this file under path `resources/META-INFO/services`.  So your project structure would be like:

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

After you uncompressed the `filter-service.tar.gz` file from the project release list, the benchmark scripts are right at your hand under `./filter-service/bin`, the same path with the script to run Filter-Service daemon mentioned above. Those benchmark scripts cover all the crucial parts of Filter-Service. You can test Filter-Service with them on your local machine.

At first, you need to install [wrk](https://github.com/wg/wrk) which is used by all our benchmark scripts.

Then, taking `check-and-set` benchmark as an example, you can run the benchmark script like this:

```
 ./bin/check-and-set-benchmark.sh
```

The test results will show after 30s like:

```
Filter: "check-set-bench" created.
Running 30s test @ http://127.0.0.1:8080/
  4 threads and 20 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   290.18us  498.51us  33.72ms   97.63%
    Req/Sec    18.82k     1.84k   30.96k    72.59%
  Latency Distribution
     50%  225.00us
     75%  258.00us
     90%  376.00us
     99%    1.72ms
  2254716 requests in 30.10s, 191.96MB read
Requests/sec:  74903.30
Transfer/sec:      6.38MB
Filter: "check-set-bench" deleted.
```

This is tested on my machine with java `1.8.0_181`, 2.3 GHz Intel Core i5 cpu and 16G mem. Please remember to run several times to warm up JVM before your real test. You can see from the result aforementioned that Filter-Service can process almost 75k requests per seconds. I think it's good enough in most cases.

## Doc service

`DocService` is a feature powered by [Armeria](https://line.github.io/armeria/index.html). It is a single-page web application by which we can browse or invoke any of the available APIs on Filter-Service. It's a convienent tool for testing.

By default, the `DocService` is disabled. To enable it, please run Filter-Service with `-d` option. Asuming that we are running Filter-Service on port 8080:

```
./bin/filter-service -d -p 8080
```

After Filter-Service start up, we can open `http://localhost:8080/v1/docs` on the web browser and see the following screen:

![08D3D3A6-A4CC-4121-BD62-3AD88001B2E0](https://user-images.githubusercontent.com/1115061/67844505-cd24eb80-fb38-11e9-925c-97a1cef78251.png)

On the left side of the screen shows all the available APIs of Filter-Service, you can use them to play with Filter-Service. Please refer [this doc](https://line.github.io/armeria/server-docservice.html) for more information about the `DocService` on Armeria.

## License

Copyright 2020 LeanCloud. Released under the [MIT License](https://github.com/leancloud/filter-service/blob/master/LICENSE.md) .

