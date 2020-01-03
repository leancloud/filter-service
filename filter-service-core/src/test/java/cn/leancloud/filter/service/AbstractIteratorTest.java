package cn.leancloud.filter.service;

import org.junit.Test;

import javax.annotation.Nullable;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AbstractIteratorTest {
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

    @Test
    public void testIterator() {
        final int max = 10;
        final List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < max; i++)
            l.add(i);
        final ListIterator<Integer> iter = new ListIterator<Integer>(l);
        for (int i = 0; i < max; i++) {
            Integer value = i;
            assertThat(iter.peek()).isEqualTo(value);
            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.next()).isEqualTo(value);
        }
        assertThat(iter.hasNext()).isFalse();
    }

    @Test
    public void testEmptyIterator() {
        final ListIterator<Object> iter = new ListIterator<Object>(Collections.emptyList());
        assertThatThrownBy(iter::next).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(iter::peek).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testMakeNextFailed() {
        final RuntimeException testingException = new RuntimeException("expected exception");
        final Iterator<Object> iter = new AbstractIterator<Object>() {
            @Nullable
            @Override
            protected Object makeNext() {
                throw testingException;
            }
        };

        assertThatThrownBy(iter::hasNext).isSameAs(testingException);
        assertThatThrownBy(iter::hasNext)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("iterator is in failed state");
    }

    @Test
    public void testMakeNextReturnsNull() {
        final Iterator<Object> iter = new AbstractIterator<Object>() {
            @Nullable
            @Override
            protected Object makeNext() {
                return null;
            }
        };

        assertThat(iter.hasNext()).isTrue();
        assertThatThrownBy(iter::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expected item but none found");
    }

    @Test
    public void testRemove() {
        final ListIterator<Object> iter = new ListIterator<Object>(Collections.emptyList());
        assertThatThrownBy(iter::remove)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("removal not supported");
    }
}