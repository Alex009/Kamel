package io.kamel.image

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import io.kamel.core.*
import io.kamel.core.config.ResourceConfig
import io.kamel.core.config.ResourceConfigBuilder
import io.kamel.image.config.LocalKamelConfig
import io.ktor.http.*

/**
 * Loads a [Painter] resource asynchronously.
 * @param data Can be anything such as [String], [Url] or a [File].
 * @param key That is used in [remember] during composition, usually it's just [data].
 * @param filterQuality That is used by [BitmapPainter].
 * @param block Configuration for [ResourceConfig].
 * @param onLoadingPainter A [Painter] that is used when the resource is in [Resource.Loading] state.
 * Note that, supplying a [Painter] object here will take precedence over [KamelImage] or [KamelImageBox]
 * [onLoading] parameter.
 * @param onFailurePainter A [Painter] that is used when the resource is in [Resource.Failure] state.
 * Note that, supplying a [Painter] object here will take precedence over [KamelImage] or [KamelImageBox]
 * [onFailure] parameter.
 * @return [Resource] Which contains a [Painter] that can be used to display an image using [KamelImage] or [KamelImageBox].
 * @see LocalKamelConfig
 */
@ExperimentalKamelApi
@Composable
public inline fun lazyPainterResource(
    data: Any,
    key: Any? = data,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    noinline onLoadingPainter: @Composable (Float) -> Painter? = { null },
    noinline onFailurePainter: @Composable (Throwable) -> Painter? = { null },
    block: ResourceConfigBuilder.() -> Unit = {},
): Resource<Painter> {

    val kamelConfig = LocalKamelConfig.current
    val density = LocalDensity.current
    val resourceConfig = remember(key, density) {
        ResourceConfigBuilder()
            .apply { this.density = density }
            .apply(block)
            .build()
    }

    val painterResource by remember(key, resourceConfig) {
        when (data.toString().substringAfterLast(".")) {
            "svg" -> kamelConfig.loadSvgResource(data, resourceConfig)
            "xml" -> kamelConfig.loadImageVectorResource(data, resourceConfig)
            else -> kamelConfig.loadImageBitmapResource(data, resourceConfig)
        }
    }.collectAsState(Resource.Loading(0F), resourceConfig.coroutineContext)

    val painterResourceWithFallbacks = when (painterResource) {
        is Resource.Loading -> {
            val resource = painterResource as Resource.Loading
            val painter = onLoadingPainter(resource.progress)
            if (painter != null) Resource.Success(painter) else painterResource
        }
        is Resource.Success -> painterResource
        is Resource.Failure -> {
            val resource = painterResource as Resource.Failure
            val painter = onFailurePainter(resource.exception)
            if (painter != null) Resource.Success(painter) else painterResource
        }
    }

    return painterResourceWithFallbacks.map { value ->
        when (value) {
            is ImageVector -> rememberVectorPainter(value)
            is ImageBitmap -> remember(value) {
                BitmapPainter(value, filterQuality = filterQuality)
            }
            else -> remember(value) { value as Painter }
        }
    }
}

/**
 * Loads a [Painter] resource asynchronously.
 * @param data Can be anything such as [String], [Url] or a [File].
 * @param key That is used in [remember] during composition, usually it's just [data].
 * @param filterQuality That is used by [BitmapPainter].
 * @param block Configuration for [ResourceConfig].
 * @return [Resource] Which contains a [Painter] that can be used to display an image using [KamelImage] or [KamelImageBox].
 * @see LocalKamelConfig
 */
@OptIn(ExperimentalKamelApi::class)
@Composable
public inline fun lazyPainterResource(
    data: Any,
    key: Any? = data,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    block: ResourceConfigBuilder.() -> Unit = {},
): Resource<Painter> = lazyPainterResource(
    data,
    key,
    filterQuality,
    onLoadingPainter = { null },
    onFailurePainter = { null },
    block
)