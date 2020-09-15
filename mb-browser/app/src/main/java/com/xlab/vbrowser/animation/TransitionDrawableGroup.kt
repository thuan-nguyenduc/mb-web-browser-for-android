/*Copyright by MonnyLab*/

package com.xlab.vbrowser.animation

import android.graphics.drawable.TransitionDrawable

/**
 * A class to allow [TransitionDrawable]'s animations to play together: similar to [android.animation.AnimatorSet].
 */
class TransitionDrawableGroup(private vararg val transitionDrawables: TransitionDrawable) {
    fun startTransition(durationMillis: Int) {
        // In theory, there are no guarantees these will play together.
        // In practice, I haven't noticed any problems.
        for (transitionDrawable in transitionDrawables) {
            transitionDrawable.startTransition(durationMillis)
        }
    }

    fun resetTransition() {
        for (transitionDrawable in transitionDrawables) {
            transitionDrawable.resetTransition()
        }
    }
}
