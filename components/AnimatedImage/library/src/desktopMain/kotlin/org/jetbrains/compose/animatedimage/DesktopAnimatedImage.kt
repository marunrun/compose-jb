package org.jetbrains.compose.animatedimage

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import java.net.MalformedURLException
import java.net.URL

private const val DEFAULT_FRAME_DURATION = 50

actual class AnimatedImage(val codec: Codec)

actual suspend fun loadAnimatedImage(path: String): AnimatedImage {
    val loader = getAnimatedImageLoaderByPath(path)
    return loader.loadAnimatedImage()
}

actual suspend fun loadResourceAnimatedImage(path: String): AnimatedImage {
    val loader = ResourceAnimatedImageLoader(path)
    return loader.loadAnimatedImage()
}

@Composable
actual fun AnimatedImage.animate(): ImageBitmap {
    val transition = rememberInfiniteTransition()
    val frameIndex by transition.animateValue(
        initialValue = 0,
        targetValue = codec.frameCount - 1,
        Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 0
                for ((index, frame) in codec.framesInfo.withIndex()) {
                    index at durationMillis
                    val frameDuration = calcFrameDuration(frame)

                    durationMillis += frameDuration
                }
            }
        )
    )

    val bitmap = remember(codec) { Bitmap().apply { allocPixels(codec.imageInfo) } }

    remember(bitmap, frameIndex) {
        codec.readPixels(bitmap, frameIndex)
    }

    return bitmap.asComposeImageBitmap()
}

private fun calcFrameDuration(frame: AnimationFrameInfo): Int {
    var frameDuration = frame.duration

    // If the frame does not contain information about a duration, set a reasonable constant duration
    if (frameDuration == 0) {
        frameDuration = DEFAULT_FRAME_DURATION
    }

    return frameDuration
}

/**
 * Depending on the [path], provide a specific implementation of [AnimatedImageLoader]
 * @return [NetworkAnimatedImageLoader] if it is a network URL, [LocalAnimatedImageLoader] otherwise
 */
private fun getAnimatedImageLoaderByPath(path: String): AnimatedImageLoader {
    return if (isNetworkPath(path)) {
        NetworkAnimatedImageLoader(path)
    } else {
        LocalAnimatedImageLoader(path)
    }
}

private fun isNetworkPath(path: String): Boolean {
    return try {
        URL(path)
        true
    } catch (e: MalformedURLException) {
        false
    }
}