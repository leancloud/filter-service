package cn.leancloud.filter.service;

import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractIteratorTest {
    @Test
    public void testIterator() {
        int max = 10;
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < max; i++)
            l.add(i);
        ListIterator<Integer> iter = new ListIterator<Integer>(l);
        for (int i = 0; i < max; i++) {
            Integer value = i;
            assertThat(iter.peek()).isEqualTo(value);
            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.next()).isEqualTo(value);
        }
        assertThat(iter.hasNext()).isFalse();
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmptyIterator() {
        Iterator<Object> iter = new ListIterator<Object>(Collections.emptyList());
        iter.next();
    }

    static class ListIterator<T> extends AbstractIterator<T> {
        private List<T> list;
        private int position = 0;

        public ListIterator(List<T> l) {
            this.list = l;
        }

        public T makeNext() {
            if (position < list.size())
                return list.get(position++);
            else
                return allDone();
        }
    }

}