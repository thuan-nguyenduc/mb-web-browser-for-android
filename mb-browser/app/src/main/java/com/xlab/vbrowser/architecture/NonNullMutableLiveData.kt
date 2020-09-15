/*Copyright by MonnyLab*/

package com.xlab.vbrowser.architecture

class NonNullMutableLiveData<T>(initialValue: T) : NonNullLiveData<T>(initialValue) {
    /**
     * Posts a task to a main thread to set the given (non-null) value.
     */
    public override fun postValue(value: T?) {
        super.postValue(value)
    }

    /**
     * Sets the (non-null) value. If there are active observers, the value will be dispatched to them.
     */
    public override fun setValue(value: T?) {
        super.setValue(value)
    }
}
