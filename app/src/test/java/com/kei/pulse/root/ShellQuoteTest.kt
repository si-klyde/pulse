package com.kei.pulse.root

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellQuoteTest {

    @Test
    fun plainValueIsSingleQuoted() {
        assertEquals("'#ff00ff00,#ff00ff00'", shellQuote("#ff00ff00,#ff00ff00"))
    }

    @Test
    fun embeddedSingleQuoteCannotBreakOut() {
        // '; rm -rf /; ' must stay inert: every ' becomes '\'' so the shell sees one literal string.
        assertEquals("""''\''; rm -rf /; '\'''""", shellQuote("'; rm -rf /; '"))
    }

    @Test
    fun metacharactersStayLiteral() {
        assertEquals("'a;b|c\$(x) `y` \"z\"'", shellQuote("a;b|c\$(x) `y` \"z\""))
    }

    @Test
    fun emptyStringIsEmptyQuotes() {
        assertEquals("''", shellQuote(""))
    }
}
