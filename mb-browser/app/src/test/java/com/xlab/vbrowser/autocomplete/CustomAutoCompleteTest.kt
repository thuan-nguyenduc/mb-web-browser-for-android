/*Copyright by MonnyLab*/

package com.xlab.vbrowser.autocomplete

import android.content.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CustomAutoCompleteTest {
    @Before
    fun setUp() {
        RuntimeEnvironment.application
                .getSharedPreferences("custom_autocomplete", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
    }

    @Test
    fun testCustomListIsEmptyByDefault() {
        val domains = runBlocking {
            CustomAutoComplete.loadCustomAutoCompleteDomains(RuntimeEnvironment.application)
        }

        assertEquals(0, domains.size)
    }

    @Test
    fun testSavingAndLoadingDomains() = runBlocking {
        CustomAutoComplete.saveDomains(RuntimeEnvironment.application, setOf(
                "mozilla.org",
                "example.org",
                "example.com"
        ))

        val domains = CustomAutoComplete.loadCustomAutoCompleteDomains(RuntimeEnvironment.application)

        assertEquals(3, domains.size)
        assertEquals("mozilla.org", domains.elementAt(0))
        assertEquals("example.org", domains.elementAt(1))
        assertEquals("example.com", domains.elementAt(2))
    }
}