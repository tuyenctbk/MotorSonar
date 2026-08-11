package com.soloprono.motorsonar.audio

import kotlin.math.PI
import kotlin.math.abs

/**
 * Digital Acoustic Noise Filter for Engine Diagnostic Sound Processing.
 * Applies a Band-Pass Filter (60 Hz to 4,500 Hz) and Adaptive Noise Gate to isolate
 * internal combustion / electric motor acoustic signatures from background wind, ambient noise, and mic hiss.
 */
class AcousticNoiseFilter(
    private val sampleRate: Int = 16000,
    lowCutoffHz: Double = 60.0,
    highCutoffHz: Double = 4500.0,
    private val noiseGateThreshold: Short = 150
) {
    // High-pass filter variables (RC alpha)
    private var hpX1 = 0.0
    private var hpY1 = 0.0
    private val hpAlpha: Double

    // Low-pass filter variables (RC alpha)
    private var lpY1 = 0.0
    private val lpAlpha: Double

    init {
        val dt = 1.0 / sampleRate
        val hpRc = 1.0 / (2.0 * PI * lowCutoffHz)
        hpAlpha = hpRc / (hpRc + dt)

        val lpRc = 1.0 / (2.0 * PI * highCutoffHz)
        lpAlpha = dt / (lpRc + dt)
    }

    /**
     * Processes PCM 16-bit short samples applying band-pass filtering and noise gating.
     */
    fun processBuffer(buffer: ShortArray, readSize: Int, isFilterEnabled: Boolean = true): ShortArray {
        if (!isFilterEnabled) return buffer

        val filtered = ShortArray(readSize)
        for (i in 0 until readSize) {
            val input = buffer[i].toDouble()

            // 1. High-Pass Filter (60 Hz cutoff: cuts low frequency handling noise & wind rumble)
            val hpOutput = hpAlpha * (hpY1 + input - hpX1)
            hpX1 = input
            hpY1 = hpOutput

            // 2. Low-Pass Filter (4,500 Hz cutoff: cuts high frequency white noise & mic hiss)
            val lpOutput = lpY1 + lpAlpha * (hpOutput - lpY1)
            lpY1 = lpOutput

            // 3. Noise Gate (attenuates noise floor when engine sound is faint or silent)
            var outputSample = lpOutput
            if (abs(outputSample) < noiseGateThreshold) {
                outputSample *= 0.15 // 16dB noise floor attenuation
            }

            filtered[i] = outputSample.toInt().coerceIn(-32768, 32767).toShort()
        }
        return filtered
    }

    fun reset() {
        hpX1 = 0.0
        hpY1 = 0.0
        lpY1 = 0.0
    }
}
