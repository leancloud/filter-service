package cn.leancloud.filter.service;

import cn.leancloud.filter.service.metrics.MetricsService;
import org.junit.Test;

public class ConfigurationTest {
    @Test
    public void testA() throws Exception{
        Configuration.initConfiguration("/Users/guorui/projects/mine/filter-service/filter-service-core/src/main/resources/configuration.yaml");
        System.out.println("asdf " + Configuration.spec());

        System.out.println(MetricsService.class.getName());
    }
}