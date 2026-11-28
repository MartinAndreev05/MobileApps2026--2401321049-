package bg.uni_plovdiv.fmi.mobileapps.testutil

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Observes the LiveData (which starts builders such as `liveData {}` and `asLiveData()`)
 * and returns its current value. Needs InstantTaskExecutorRule and MainDispatcherRule.
 */
fun <T> LiveData<T>.observedValue(): T? {
    val observer = Observer<T> { }
    observeForever(observer)
    try {
        return value
    } finally {
        removeObserver(observer)
    }
}
