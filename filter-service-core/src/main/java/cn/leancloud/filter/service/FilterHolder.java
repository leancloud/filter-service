package cn.leancloud.filter.service;

public final class FilterHolder<F extends BloomFilter> {
    private String name;
    private F filter;

    public FilterHolder(String name, F filter) {
        this.name = name;
        this.filter = filter;
    }

    public String name() {
        return name;
    }

    public F filter() {
        return filter;
    }
}

