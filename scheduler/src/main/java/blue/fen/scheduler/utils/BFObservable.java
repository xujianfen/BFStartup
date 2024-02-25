package blue.fen.scheduler.utils;

import java.util.List;
import android.database.Observable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>创建时间：2024/03/03 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public class BFObservable<T> extends Observable<T> {

    /**
     * The list of observers.  An observer can be in the list at most
     * once and will never be null.
     */
    protected final List<T> mObservers = new CopyOnWriteArrayList<>();

    /**
     * Adds an observer to the list. The observer cannot be null and it must not already
     * be registered.
     * @param observer the observer to register
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is already registered
     */
    public void registerObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }
    }

    /**
     * Removes a previously registered observer. The observer must not be null and it
     * must already have been registered.
     * @param observer the observer to unregister
     * @throws IllegalArgumentException the observer is null
     * @throws IllegalStateException the observer is not yet registered
     */
    public void unregisterObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            int index = mObservers.indexOf(observer);
            if (index == -1) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
            mObservers.remove(index);
        }
    }

    /**
     * Remove all registered observers.
     */
    public void unregisterAll() {
        synchronized(mObservers) {
            mObservers.clear();
        }
    }
}
