package com.example.ar_lab6

import android.graphics.BitmapFactory
import android.opengl.Matrix
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView

private enum class ObjectSearchState {
    SEARCHING,
    OBJECT_OUT_OF_SCREEN,
    OBJECT_FOUND
}

@Composable
fun ARScreen() {

    val context = LocalContext.current

    var detectedImages by remember {
        mutableStateOf<List<AugmentedImage>>(emptyList())
    }
    var modelRotation by remember { mutableFloatStateOf(0f) }
    var searchState by remember {
        mutableStateOf(ObjectSearchState.SEARCHING)
    }
    var targetScreenPosition by remember { mutableStateOf<Offset?>(null) }
    var arrowRotation by remember { mutableFloatStateOf(0f) }

    var waitingForImageToDisappear by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            planeRenderer = false,
            sessionConfiguration = { session, config ->
                val bitmap = BitmapFactory.decodeStream(
                    context.assets.open("1_number.jpg")
                )

                config.augmentedImageDatabase = AugmentedImageDatabase(session).apply {
                    addImage("1_number", bitmap, 0.15f)
                }
            },

            onSessionUpdated = { _, frame ->
                val images = frame
                    .getUpdatedTrackables(
                        AugmentedImage::class.java
                    )
                    .filter { it.trackingState == TrackingState.TRACKING }
                    .toList()

                if (waitingForImageToDisappear) {
                    if (images.isEmpty()) {
                        waitingForImageToDisappear = false
                        detectedImages = emptyList()
                        targetScreenPosition = null
                        searchState = ObjectSearchState.SEARCHING
                    }
                    return@ARSceneView
                }

                if (images.isEmpty()) {
                    detectedImages = emptyList()
                    targetScreenPosition = null
                    if (searchState != ObjectSearchState.SEARCHING) {
                        searchState = ObjectSearchState.SEARCHING
                    }

                    return@ARSceneView
                }

                detectedImages = images

                val image = images.first()
                val screenPosition = calculateScreenPosition(
                    frame = frame,
                    augmentedImage = image
                )

                targetScreenPosition = screenPosition

                if (screenPosition == null) return@ARSceneView

                val imageWidth = frame.camera
                    .imageIntrinsics
                    .imageDimensions[0]
                    .toFloat()

                val imageHeight = frame.camera
                    .imageIntrinsics
                    .imageDimensions[1]
                    .toFloat()

                val isOnScreen =
                    screenPosition.x >= 0f &&
                            screenPosition.x <= imageWidth &&
                            screenPosition.y >= 0f &&
                            screenPosition.y <= imageHeight


                if (isOnScreen) {
                    searchState = ObjectSearchState.OBJECT_FOUND
                    arrowRotation = 0f
                } else {
                    searchState = ObjectSearchState.OBJECT_OUT_OF_SCREEN
                    arrowRotation = calculateArrowRotation(
                        screenPosition = screenPosition,
                        screenWidth = imageWidth,
                        screenHeight = imageHeight
                    )
                }
            },
            onTouchEvent = { event, _ ->
                if (event.actionMasked == MotionEvent.ACTION_UP && detectedImages.isNotEmpty() &&
                    searchState == ObjectSearchState.OBJECT_FOUND
                ) {
                    modelRotation += 45f
                }
                true
            }
        ) {
            detectedImages.forEach { augmentedImage ->
                val modelInstance =
                    remember(augmentedImage.index) {
                        modelLoader.createModelInstance(assetFileLocation = "squirrel.glb")
                    }

                AugmentedImageNode(
                    augmentedImage = augmentedImage,
                    applyImageScale = true
                ) {
                    ModelNode(
                        modelInstance = modelInstance,
                        scaleToUnits = 0.2f,
                        rotation = io.github.sceneview.math.Rotation(y = modelRotation)
                    )
                }
            }
        }

        when (searchState) {
            ObjectSearchState.SEARCHING -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Поиск метки...",
                        color = Color.White,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Наведите камеру на картинку",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                        .border(
                            width = 3.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            ObjectSearchState.OBJECT_OUT_OF_SCREEN -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "➤",
                        color = Color.White,
                        fontSize = 52.sp,
                        modifier = Modifier
                            .padding(top = 80.dp)
                            .rotate(arrowRotation)
                    )

                    Text(
                        text = "Найдите метку",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .background(
                                Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Button(
                        onClick = {
                            modelRotation = 0f
                            detectedImages = emptyList()
                            targetScreenPosition = null
                            waitingForImageToDisappear = true
                            searchState = ObjectSearchState.SEARCHING
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text("Сбросить")
                    }
                }
            }

            ObjectSearchState.OBJECT_FOUND -> {
                Button(
                    onClick = { modelRotation += 45f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Text("Повернуть")
                }
            }
        }
    }
}

private fun calculateScreenPosition(
    frame: Frame,
    augmentedImage: AugmentedImage
): Offset? {
    return try {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) return null

        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100f)

        val imagePose = augmentedImage.centerPose
        val imageMatrix = FloatArray(16)

        imagePose.toMatrix(imageMatrix, 0)
        val viewModelMatrix = FloatArray(16)
        Matrix.multiplyMM(
            viewModelMatrix,
            0,
            viewMatrix,
            0,
            imageMatrix,
            0
        )

        val clipMatrix = FloatArray(16)
        Matrix.multiplyMM(
            clipMatrix,
            0,
            projectionMatrix,
            0,
            viewModelMatrix,
            0
        )

        val point = floatArrayOf(
            0f,
            0f,
            0f,
            1f
        )

        val result = FloatArray(4)

        Matrix.multiplyMV(
            result,
            0,
            clipMatrix,
            0,
            point,
            0
        )

        if (result[3] <= 0f) return null

        val ndcX = result[0] / result[3]
        val ndcY = result[1] / result[3]

        val imageWidth = camera.imageIntrinsics
            .imageDimensions[0]
            .toFloat()
        val imageHeight = camera.imageIntrinsics
            .imageDimensions[1]
            .toFloat()

        val screenX = (ndcX + 1f) / 2f * imageWidth
        val screenY = (1f - ndcY) / 2f * imageHeight

        Offset(x = screenX, y = screenY)
    } catch (_: Exception) {
        null
    }
}

private fun calculateArrowRotation(
    screenPosition: Offset,
    screenWidth: Float,
    screenHeight: Float
): Float {
    val centerX = screenWidth / 2f
    val centerY = screenHeight / 2f

    val dx = screenPosition.x - centerX
    val dy = screenPosition.y - centerY

    return Math.toDegrees(
        kotlin.math.atan2(
            dx.toDouble(),
            -dy.toDouble()
        )
    ).toFloat() - 90f
}