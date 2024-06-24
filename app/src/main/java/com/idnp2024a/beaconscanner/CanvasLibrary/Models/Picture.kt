package com.idnp2024a.beaconscanner.CanvasLibrary.Models

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun Picture(
    modifier: Modifier = Modifier,
    image: ImageBitmap,
    positionX: Int,
    positionY: Int,
){
    val imageScale = remember {
        mutableStateOf(0.40f)
    }
    val posX = remember {
        mutableStateOf(positionX.dp)
    }
    val posY = remember {
        mutableStateOf(positionY.dp)
    }
    val isPressed = remember {
        mutableStateOf(false)
    }
    Canvas(
        modifier = modifier
            .size(100.dp, 100.dp)
            .offset(posX.value, posY.value)
            .border(0.5.dp, Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed.value = true
                            imageScale.value = 1.70f
                            posX.value = 174.dp
                            posY.value = 415.dp
                            awaitRelease()
                        } finally {
                            isPressed.value = false
                            imageScale.value = 0.40f
                            posX.value = positionX.dp
                            posY.value = positionY.dp
                        }
                    },
                )
            },
    ) {
        withTransform({
            scale(imageScale.value)
            clipRect(450f, right = -160f,bottom = 500f, top = -650f)
        }) {
            drawImage(image, topLeft = Offset(-575f,-160f))
        }

    }
}