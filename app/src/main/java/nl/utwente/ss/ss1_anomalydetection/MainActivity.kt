package nl.utwente.ss.ss1_anomalydetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import com.google.android.gms.location.LocationRequest
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.core.app.ActivityCompat

import nl.utwente.ss.ss1_anomalydetection.ui.theme.SS1AnomalyDetectionTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            ActivityCompat.requestPermissions(this, permissions, 100)
        }

        setContent {
            SS1AnomalyDetectionTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    GPS(fusedLocationClient)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GPS(fusedLocationClient: FusedLocationProviderClient) {
    val lastLocation = remember { mutableStateOf<Location?>(Location("")) }
    var nextLocation = Location("")
    val list = remember { mutableStateOf(ArrayList<Float>()) }
    val meanValue = remember { mutableFloatStateOf(0.0f) }
    val maxListSize = 50

    val ctx = LocalContext.current
    val sensorManager = ctx.getSystemService(SENSOR_SERVICE) as SensorManager
    val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun parseLocation(location: Location?): String {
        val lat = location?.latitude.toString()
        val lon = location?.longitude.toString()
        return "($lat, $lon)"
    }

    val accelSensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                list.value.add(event.values[2])
                if (list.value.size > maxListSize) {
                    list.value.removeAt(0)
                }
                meanValue.floatValue = mean(list.value)
//                if (meanValue.floatValue > 5f) {
//                    println("acc: ${meanValue.floatValue}, loc: ${parseLocation(lastLocation.value)}")
//                }
            }
        }

        fun mean(arr: List<Float>): Float {
            return (arr.fold(0.0f) { acc, i -> acc + i }) / arr.size
        }
    }

    sensorManager.registerListener(
        accelSensorEventListener,
        accelSensor,
        SensorManager.SENSOR_DELAY_NORMAL
    )

    fun startGettingLocation() {
        val locationRequest = LocationRequest.Builder(100)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(location: LocationResult) {
                    super.onLocationResult(location)

                    if ((lastLocation.value)!!.distanceTo(nextLocation) > 1f) {
                        println(parseLocation(lastLocation.value))
                    }

                    lastLocation.value = nextLocation
                    nextLocation = (location.lastLocation)!!
                }
            },
            (Looper.myLooper())!!
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { startGettingLocation() }) {
            Text(text = "Show my location")
        }
        Text(
            text = "Current location:",
            fontSize = 20.sp
        )
        Text(
            text = parseLocation(lastLocation.value),
            color = Color.Red,
            fontSize = 25.sp
        )
    }
}