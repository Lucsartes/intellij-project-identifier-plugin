package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BranchChangeDetectorTest {

    @Test
    fun onChange_sameAsSeedDoesNotTrigger() {
        val detector = BranchChangeDetector("main")
        assertFalse(detector.onChange("main"))
    }

    @Test
    fun onChange_changedBranchTriggersOnceThenSettles() {
        val detector = BranchChangeDetector("main")
        assertTrue(detector.onChange("dev"))
        assertFalse(detector.onChange("dev"))
    }

    @Test
    fun onChange_backAndForthTriggersEachNetChange() {
        val detector = BranchChangeDetector("main")
        assertTrue(detector.onChange("dev"))
        assertTrue(detector.onChange("main"))
    }

    @Test
    fun onChange_handlesNullDetachedTransitions() {
        val detector = BranchChangeDetector("main")
        assertTrue(detector.onChange(null))
        assertFalse(detector.onChange(null))
        assertTrue(detector.onChange("main"))
    }
}
