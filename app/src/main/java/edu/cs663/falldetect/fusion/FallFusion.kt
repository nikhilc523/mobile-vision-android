package edu.cs663.falldetect.fusion

import edu.cs663.falldetect.util.Log

/**
 * Fuses LSTM probability with rule-based score to produce final fall probability.
 * Uses a weighted combination approach.
 */
class FallFusion(
    private val lstmWeight: Float = 0.7f,
    private val ruleWeight: Float = 0.3f,
    private val threshold: Float = 0.5f
) {
    
    init {
        require(lstmWeight + ruleWeight == 1.0f) {
            "Weights must sum to 1.0"
        }
    }
    
    /**
     * Combine LSTM probability and rule-based score.
     * 
     * @param pLstm LSTM model probability (0.0 to 1.0)
     * @param ruleScore Rule-based score (0.0 to 1.0)
     * @return Final fused probability (0.0 to 1.0)
     */
    fun fuse(pLstm: Float, ruleScore: Float): Float {
        val pFinal = (lstmWeight * pLstm) + (ruleWeight * ruleScore)
        
        Log.d("Fusion: LSTM=$pLstm, Rule=$ruleScore, Final=$pFinal")
        
        return pFinal.coerceIn(0f, 1f)
    }
    
    /**
     * Check if fused probability exceeds threshold.
     */
    fun isFallDetected(pFinal: Float): Boolean {
        return pFinal >= threshold
    }
    
    /**
     * Convenience method to fuse and check threshold in one call.
     */
    fun detectFall(pLstm: Float, ruleScore: Float): Boolean {
        val pFinal = fuse(pLstm, ruleScore)
        return isFallDetected(pFinal)
    }
}

