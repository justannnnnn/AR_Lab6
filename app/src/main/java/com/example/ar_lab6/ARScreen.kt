package com.example.ar_lab6

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberOnGestureListener

private enum class ObjectSearchState {
    SEARCHING,
    OUT_OF_SCREEN,
    FOUND
}

@Composable
fun ARScreen() {

    var anchor by remember {
        mutableStateOf<Anchor?>(null)
    }

    var latestFrame by remember {
        mutableStateOf<Frame?>(null)
    }

    var searchState by remember {
        mutableStateOf(ObjectSearchState.SEARCHING)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // --------------------------------
        // AR
        // --------------------------------

        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            planeRenderer = true,

            onSessionUpdated = { _, frame ->
                latestFrame = frame
            },

            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, _ ->

                    val frame = latestFrame
                        ?: return@rememberOnGestureListener

                    val hit = frame
                        .hitTest(motionEvent)
                        .firstOrNull { hitResult ->

                            val trackable = hitResult.trackable

                            trackable is Plane &&
                                    trackable.trackingState == TrackingState.TRACKING &&
                                    trackable.isPoseInPolygon(
                                        hitResult.hitPose
                                    )
                        }

                    hit?.createAnchor()?.let { newAnchor ->

                        anchor?.detach()

                        anchor = newAnchor

                        // Якорь появился,
                        // но пользователь ещё не нашёл объект глазами.
                        searchState = ObjectSearchState.OUT_OF_SCREEN
                    }
                }
            )
        ) {

            anchor?.let { currentAnchor ->

                AnchorNode(
                    anchor = currentAnchor
                ) {

                    SphereNode(
                        radius = 0.15f
                    )
                }
            }
        }

        // --------------------------------
        // Helper UI
        // --------------------------------

        when (searchState) {

            // --------------------------------
            // Поиск поверхности
            // --------------------------------

            ObjectSearchState.SEARCHING -> {

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Поиск объекта...",
                        color = Color.White,
                        fontSize = 18.sp
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Наведите камеру на поверхность",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }

                // Рамка для объекта
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(220.dp)
                        .border(
                            width = 3.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            // --------------------------------
            // Якорь есть, но объект вне экрана
            // --------------------------------

            ObjectSearchState.OUT_OF_SCREEN -> {

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Color.Black.copy(alpha = 0.65f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "↑",
                        color = Color.White,
                        fontSize = 48.sp
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Найдите объект",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }

            // --------------------------------
            // Объект найден
            // --------------------------------

            ObjectSearchState.FOUND -> {

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Button(
                        onClick = {
                            // TODO:
                            // поворот объекта
                        }
                    ) {
                        Text("Повернуть")
                    }

                    Button(
                        onClick = {

                            anchor?.detach()
                            anchor = null

                            searchState =
                                ObjectSearchState.SEARCHING
                        }
                    ) {
                        Text("Скрыть")
                    }
                }
            }
        }
    }
}