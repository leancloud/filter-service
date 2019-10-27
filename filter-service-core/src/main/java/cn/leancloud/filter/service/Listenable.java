package cn.leancloud.filter.service;

public interface Listenable<L> {
    void addListener(L listener);

    boolean removeListener(L listener);
}
