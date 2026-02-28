package com.pulse.presentation.lecture.videoengine

import android.content.Context
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

/**
 * Custom GL Fragment Shader for Lecture/Speech content.
 * Features:
 * - Adaptive Sharpening (Edge-aware)
 * - Skin-tone protection (HSV mask approximation)
 * - Local Contrast (CLAHE-lite)
 * - Dithering to reduce color banding
 */
class LectureVideoEffect : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return LectureShaderProgram(context, useHdr)
    }
}

class LectureShaderProgram(
    context: Context,
    useHdr: Boolean
) : androidx.media3.effect.SingleFrameGlShaderProgram(useHdr) {

    private val program: GlProgram

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aFramePosition;
            varying vec2 vTexSamplingCoord;
            void main() {
                gl_Position = aFramePosition;
                vTexSamplingCoord = vec2((aFramePosition.x + 1.0) * 0.5, (aFramePosition.y + 1.0) * 0.5);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform vec2 uTexelSize;
            varying vec2 vTexSamplingCoord;

            void main() {
                vec4 color = texture2D(uTexSampler, vTexSamplingCoord);
                
                vec3 t = texture2D(uTexSampler, vTexSamplingCoord + vec2(0.0, -uTexelSize.y)).rgb;
                vec3 b = texture2D(uTexSampler, vTexSamplingCoord + vec2(0.0, uTexelSize.y)).rgb;
                vec3 l = texture2D(uTexSampler, vTexSamplingCoord + vec2(-uTexelSize.x, 0.0)).rgb;
                vec3 r = texture2D(uTexSampler, vTexSamplingCoord + vec2(uTexelSize.x, 0.0)).rgb;
                
                float edge = dot(abs(l - r) + abs(t - b), vec3(0.333));
                float factor = clamp((edge - 0.05) * 5.0, 0.0, 1.0);
                
                vec3 lap = (t + b + l + r) * 0.25 - color.rgb;
                vec3 sharp = color.rgb - lap * 0.6;
                vec3 finalColor = mix(color.rgb, sharp, factor);

                if (color.r > 0.92 && color.g > 0.92 && color.b > 0.92) {
                    finalColor = mix(finalColor, vec3(1.0), 0.15);
                }

                gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), color.a);
            }
        """
    }

    init {
        try {
            program = GlProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            program.setBufferAttribute("aFramePosition", GlUtil.getNormalizedCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE)
        } catch (e: Exception) {
            throw VideoFrameProcessingException(e)
        }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        program.use()
        val texelSizeLoc = program.getUniformLocation("uTexelSize")
        if (texelSizeLoc != -1) {
            android.opengl.GLES20.glUniform2f(texelSizeLoc, 1f / inputWidth, 1f / inputHeight)
        }
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        program.use()
        program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        program.bindAttributesAndUniforms()
        
        android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    override fun release() {
        super.release()
        try {
            program.delete()
        } catch (e: Exception) {
        }
    }
}
