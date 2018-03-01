package roboy.context;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeMap;

/**
 * Sample implementation of a ValueHistory using timestamps (longs) as keys
 * and a TreeMap for data storage.
 *
 * The timestamps are equal or larger than the time when updateValue() was called.
 * Implementation does not guarantee perfect timestamp accuracy, but achieves key uniqueness.
 *
 */
public class TimestampedValueHistory<V> implements AbstractValueHistory<Long, V> {
    /**
     * Marks the last time a value was added to the history (or initialization).
     */
    private volatile long lastTime;
    private TreeMap<Long, V> data;
    /* When value count reaches MAX_LIMIT, it is reduced to REDUCE_BY. */
    private final int MAX_LIMIT = 50;
    private final int REDUCE_BY = 20;

    public TimestampedValueHistory() {
        data = new TreeMap<>();
        lastTime = System.nanoTime();
    }

    /**
     * @return The last element added to this history, or <code>null</code> if not found.
     */
    @Override
    public synchronized V getValue() {
        if (data.isEmpty()) return null;
        return data.lastEntry().getValue();
    }

    /**
     * Get a copy of the last n entries added to the history.
     * Less values may be returned if there are not enough values in this history.
     * In case of no values, an empty map is returned.
     *
     * Needs to be synchronized because data cannot be changed while working with an Iterator.
     */
    @Override
    public synchronized TreeMap<Long, V> getLastNValues(int n) {
        TreeMap<Long, V> map = new TreeMap<>();
        Iterator<Long> keyIterator = data.descendingKeySet().iterator();
        Long key;
        while (keyIterator.hasNext() && (n > 0)) {
            key = keyIterator.next();
            map.put(key, data.get(key));
            n--;
        }
        return map;
    }

    /**
     * Puts a value into the history in the last place.
     */
    @Override
    public synchronized void updateValue(V value) {
        reduce();
        data.put(generateKey(), value);
    }

    private synchronized void reduce() {
        if(data.size() < MAX_LIMIT) {
            return;
        }
        // Remove the oldest values.
        Iterator<Long> keySet = data.keySet().iterator();
        int leftToRemove = REDUCE_BY;
        while (leftToRemove > 0 && keySet.hasNext()) {
            keySet.next();
            keySet.remove();
            leftToRemove--;
        }
    }

    private synchronized long generateKey() {
        long currentTime = System.nanoTime();
        // Avoid duplicates (synchronized method, so no concurrent modifications to lastTime can happen).
        if (lastTime <= currentTime) {
            // Catch up with system time.
            lastTime = currentTime + 1;
        } else {
            // Continue with current counter.
            lastTime++;
        }
        return lastTime;
    }

    @Override
    public synchronized int valuesAddedSinceStart() {
        return data.size();
    }
}
