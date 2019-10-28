package cn.leancloud.filter.service;

/**
 * The subtype of this interface can be listened by a listener of type L.
 *
 * @param <L> the type of the listener
 */
public interface Listenable<L> {
    void addListener(L listener);

    boolean removeListener(L listener);
}
