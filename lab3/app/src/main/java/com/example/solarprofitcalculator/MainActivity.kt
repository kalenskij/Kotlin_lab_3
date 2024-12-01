package com.example.solarprofitcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SolarProfitApp(modifier = Modifier.padding(innerPadding), closeApp = { finish() })
                }
            }
        }
    }
}

@Composable
fun SolarProfitApp(modifier: Modifier = Modifier, closeApp: () -> Unit) {
    var inputPower by remember { mutableStateOf("") }
    var firstErrorMargin by remember { mutableStateOf("") }
    var secondErrorMargin by remember { mutableStateOf("") }
    var electricityRate by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    // Calculation logic
    val calculateGaussian: (Double, Double, Double) -> Double = { x, mean, deviation ->
        val coefficient = 1 / (deviation * sqrt(2 * Math.PI))
        val exponent = -((x - mean).pow(2)) / (2 * deviation.pow(2))
        coefficient * exp(exponent)
    }

    val approximateIntegral: (Double, Double, Int, Double, Double) -> Double = { start, end, intervals, mean, deviation ->
        var sum = 0.0
        val stepSize = (end - start) / intervals
        for (i in 0 until intervals) {
            val left = start + i * stepSize
            val right = start + (i + 1) * stepSize
            sum += (calculateGaussian(left, mean, deviation) + calculateGaussian(right, mean, deviation)) / 2 * stepSize
        }
        sum
    }

    // Helper functions for earnings and penalties
    fun calculateEarnings(power: Double, efficiency: Double, ratePerKWh: Double): Double {
        return power * 24 * efficiency * ratePerKWh * 1000
    }

    fun calculatePenalties(power: Double, efficiency: Double, ratePerKWh: Double): Double {
        return power * 24 * (1 - efficiency) * ratePerKWh * 1000
    }

    fun performCalculation() {
        val power = inputPower.toDoubleOrNull() ?: 0.0
        val initialError = firstErrorMargin.toDoubleOrNull() ?: 0.0
        val improvedError = secondErrorMargin.toDoubleOrNull() ?: 0.0
        val ratePerKWh = electricityRate.toDoubleOrNull() ?: 0.0

        val rangeStart = power - improvedError
        val rangeEnd = power + improvedError
        val divisions = 1000

        // Calculate the efficiency and profits before and after improvements
        val efficiencyBefore = approximateIntegral(rangeStart, rangeEnd, divisions, power, initialError)
        val earningsBefore = calculateEarnings(power, efficiencyBefore, ratePerKWh)
        val penaltiesBefore = calculatePenalties(power, efficiencyBefore, ratePerKWh)

        val efficiencyAfter = approximateIntegral(rangeStart, rangeEnd, divisions, power, improvedError)
        val earningsAfter = calculateEarnings(power, efficiencyAfter, ratePerKWh)
        val penaltiesAfter = calculatePenalties(power, efficiencyAfter, ratePerKWh)

        resultText = """
            Прибуток до вдосконалення: %.2f тис. грн
            Виручка до вдосконалення: %.2f тис. грн
            Штраф до вдосконалення: %.2f тис. грн
            Прибуток після вдосконалення: %.2f тис. грн
            Виручка після вдосконалення: %.2f тис. грн
            Штраф після вдосконалення: %.2f тис. грн
        """.trimIndent().format(
            earningsBefore / 1000,
            (earningsBefore - penaltiesBefore) / 1000,
            penaltiesBefore / 1000,
            earningsAfter / 1000,
            (earningsAfter - penaltiesAfter) / 1000,
            penaltiesAfter / 1000
        )
    }

    // UI components
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InputField(label = "Потужність (МВт)", value = inputPower) { inputPower = it }
        InputField(label = "Перше відхилення (МВт)", value = firstErrorMargin) { firstErrorMargin = it }
        InputField(label = "Друге відхилення (МВт)", value = secondErrorMargin) { secondErrorMargin = it }
        InputField(label = "Вартість електроенергії (грн/кВт·год)", value = electricityRate) { electricityRate = it }

        Button(onClick = { performCalculation() }, modifier = Modifier.fillMaxWidth()) {
            Text("Розрахувати прибуток")
        }

        Text(text = resultText, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun SolarProfitCalculatorPreview() {
    MaterialTheme {
        SolarProfitApp(closeApp = {})
    }
}