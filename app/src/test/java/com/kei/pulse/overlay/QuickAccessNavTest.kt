package com.kei.pulse.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure cursor math for the Quick Access bar's explicit controller navigation. */
class QuickAccessNavTest {

    @Test
    fun `moveItem clamps at both ends and never wraps`() {
        assertEquals(1, QuickAccessNav.moveItem(item = 0, delta = 1, itemCount = 4))
        assertEquals(0, QuickAccessNav.moveItem(item = 0, delta = -1, itemCount = 4)) // can't go below 0
        assertEquals(3, QuickAccessNav.moveItem(item = 3, delta = 1, itemCount = 4)) // can't go past the end
        assertEquals(2, QuickAccessNav.moveItem(item = 3, delta = -1, itemCount = 4))
    }

    @Test
    fun `moveItem pins to 0 for an empty list`() {
        assertEquals(0, QuickAccessNav.moveItem(item = 0, delta = 1, itemCount = 0))
        assertEquals(0, QuickAccessNav.moveItem(item = 5, delta = -1, itemCount = 0))
    }

    @Test
    fun `moveTab wraps in both directions`() {
        assertEquals(1, QuickAccessNav.moveTab(tabIndex = 0, delta = 1, tabCount = 4))
        assertEquals(0, QuickAccessNav.moveTab(tabIndex = 3, delta = 1, tabCount = 4)) // wrap forward
        assertEquals(3, QuickAccessNav.moveTab(tabIndex = 0, delta = -1, tabCount = 4)) // wrap backward
    }

    @Test
    fun `clampItem re-clamps after the list shrinks`() {
        assertEquals(1, QuickAccessNav.clampItem(item = 4, itemCount = 2)) // mode switch hid sub-controls
        assertEquals(2, QuickAccessNav.clampItem(item = 2, itemCount = 5)) // still valid → unchanged
        assertEquals(0, QuickAccessNav.clampItem(item = 3, itemCount = 0))
    }
}

class QuickAccessNavGroupTest {
    private val starts = listOf(0, 3, 7)

    @org.junit.Test
    fun nextGroupFromInsideAGroupGoesToTheFollowingStart() {
        org.junit.Assert.assertEquals(3, QuickAccessNav.moveGroup(1, 1, starts))
        org.junit.Assert.assertEquals(7, QuickAccessNav.moveGroup(3, 1, starts))
    }

    @org.junit.Test
    fun previousGroupFromInsideAGroupGoesToThePriorStart() {
        org.junit.Assert.assertEquals(0, QuickAccessNav.moveGroup(5, -1, starts))
    }

    @org.junit.Test
    fun wrapsAtBothEnds() {
        org.junit.Assert.assertEquals(0, QuickAccessNav.moveGroup(8, 1, starts))
        org.junit.Assert.assertEquals(7, QuickAccessNav.moveGroup(0, -1, starts))
    }

    @org.junit.Test
    fun emptyGroupsPinZero() {
        org.junit.Assert.assertEquals(0, QuickAccessNav.moveGroup(4, 1, emptyList()))
    }
}
