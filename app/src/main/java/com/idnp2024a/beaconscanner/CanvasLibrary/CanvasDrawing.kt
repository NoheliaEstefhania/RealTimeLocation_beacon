package com.idnp2024a.beaconscanner.CanvasLibrary
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.idnp2024a.beaconscanner.CanvasLibrary.Models.Person
import com.idnp2024a.beaconscanner.CanvasLibrary.ui.theme.PolygonoTheme
import com.idnp2024a.beaconscanner.CanvasLibrary.Models.Picture
import com.idnp2024a.beaconscanner.CanvasLibrary.Models.Room
import com.idnp2024a.beaconscanner.CanvasLibrary.Utils.Point


@Composable
fun DrawCanvas(){
    PolygonoTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Room(
                data = loadData(),
                modifier = Modifier.padding(innerPadding)
            )
            Person(
                modifier = Modifier,
                positionX = 174,
                positionY = 415
            )
        }
    }
}

@Composable
fun loadData(): ArrayList<Point>{
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = with(density){configuration.screenHeightDp.dp.roundToPx()}
    val factor = screenHeight*(0.05).toFloat()

    val points = ArrayList<Point>()
    points.add(Point(1 * factor, 1 * factor))
    points.add(Point(8 * factor, 1 * factor))
    points.add(Point(8 * factor, 18 * factor))
    points.add(Point(1 * factor, 18 * factor))
    points.add(Point(1 * factor, 1 * factor))

    return points
}