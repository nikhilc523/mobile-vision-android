package edu.cs663.falldetect.util

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FpsMeter.
 * Tests FPS calculation with synthetic timings.
 */
class FpsMeterTest {
    
    private lateinit var fpsMeter: FpsMeter
    
    @Before
    fun setup() {
        fpsMeter = FpsMeter()
    }
    
    @After
    fun teardown() {
        fpsMeter.stopUpdates()
    }
    
    @Test
    fun testInitialFpsIsZero() {
        assertEquals(0f, fpsMeter.fps(), 0.01f)
    }
    
    @Test
    fun testTickIncrementsFrameCount() {
        // Initial FPS should be 0
        assertEquals(0f, fpsMeter.fps(), 0.01f)
        
        // Tick some frames
        repeat(10) {
            fpsMeter.tick()
        }
        
        // FPS should still be 0 until update is called
        // (update happens automatically every 1 second)
        assertEquals(0f, fpsMeter.fps(), 0.01f)
    }
    
    @Test
    fun testResetClearsFps() {
        // Tick some frames
        repeat(30) {
            fpsMeter.tick()
        }
        
        // Reset
        fpsMeter.reset()
        
        // FPS should be 0 again
        assertEquals(0f, fpsMeter.fps(), 0.01f)
    }
    
    @Test
    fun testFpsCalculationWithCallback() {
        var callbackFps = 0f
        var callbackCount = 0
        
        fpsMeter.startUpdates { fps ->
            callbackFps = fps
            callbackCount++
        }
        
        // Simulate 30 frames
        repeat(30) {
            fpsMeter.tick()
        }
        
        // Wait for update (1 second + buffer)
        Thread.sleep(1200)
        
        // Callback should have been invoked at least once
        assertTrue("Callback should be invoked", callbackCount > 0)
        
        // FPS should be approximately 30 (may vary due to timing)
        // We use a wide range because of test timing variability
        assertTrue("FPS should be > 0", callbackFps > 0f)
        assertTrue("FPS should be reasonable", callbackFps < 100f)
    }
    
    @Test
    fun testStopUpdatesStopsCallback() {
        var callbackCount = 0
        
        fpsMeter.startUpdates { 
            callbackCount++
        }
        
        // Wait for first update
        Thread.sleep(1200)
        val firstCount = callbackCount
        
        // Stop updates
        fpsMeter.stopUpdates()
        
        // Wait again
        Thread.sleep(1200)
        
        // Callback count should not have increased
        assertEquals("Callback should not be invoked after stop", firstCount, callbackCount)
    }
    
    @Test
    fun testMultipleTicksProduceReasonableFps() {
        var latestFps = 0f
        
        fpsMeter.startUpdates { fps ->
            latestFps = fps
        }
        
        // Simulate 60 FPS for 2 seconds
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 2000) {
            fpsMeter.tick()
            Thread.sleep(16) // ~60 FPS
        }
        
        // Wait for final update
        Thread.sleep(200)
        
        // FPS should be in a reasonable range (40-70 FPS)
        // Accounting for timing variability in tests
        assertTrue("FPS should be > 30", latestFps > 30f)
        assertTrue("FPS should be < 80", latestFps < 80f)
    }
}

