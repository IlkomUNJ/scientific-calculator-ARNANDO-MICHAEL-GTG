package com.example.mycalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mycalculator.ui.theme.MycalculatorTheme
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import java.text.DecimalFormat
import kotlin.math.asin
import kotlin.math.acos
import kotlin.math.atan

// Warna Baru Sesuai Tampilan Gambar
val ButtonLightGray = Color(0xFFD6D6D6)
val ButtonDarkGray = Color(0xFF2E2E2E)
val PrimaryBackground = Color(0xFFF5F5F5)
val AccentOperator = Color(0xFF00B3A6)
val AccentFunction = Color(0xFF4A4A4A)

// Mengganti definisi warna lama untuk menghindari kebingungan
val DarkGray = Color(0xFF2E2E2E)
val LightGray = Color(0xFFA2BBCF)
val Blue = Color(0xFF205B7A)
val White = Color.White
val Black = Color.Black

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MycalculatorTheme {
                CalculatorApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// --- Komponen Navigasi (Disederhanakan) ---

@Composable
fun MyApp() {
    var currentScreen by rememberSaveable { mutableStateOf("calculator") }

    Surface(Modifier.fillMaxSize()) {
        when (currentScreen) {
            "calculator" -> CalculatorApp(
                onBack = { currentScreen = "menu" }
            )
            else -> MainMenu(onSelectApp = { selected -> currentScreen = selected })
        }
    }
}

@Composable
fun MainMenu(onSelectApp: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(PrimaryBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onSelectApp("calculator") }) {
            Text("Buka Kalkulator")
        }
    }
}

// --- Kalkulator Ilmiah dan Logika ---

val factorial = object : Function("fact", 1) {
    override fun apply(vararg args: Double): Double {
        val n = args[0].toInt()
        if (n < 0 || n != args[0].toInt()) {
            throw IllegalArgumentException("Argument for factorial must be a non-negative integer")
        }
        var result = 1.0
        for (i in 2..n) {
            result *= i
        }
        return result
    }
}

val myAsin = object : Function("asin", 1) { override fun apply(vararg args: Double) = asin(args[0]) }
val myAcos = object : Function("acos", 1) { override fun apply(vararg args: Double) = acos(args[0]) }
val myAtan = object : Function("atan", 1) { override fun apply(vararg args: Double) = atan(args[0]) }

@Composable
fun CalculatorApp(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    var display by rememberSaveable { mutableStateOf("0") }
    var expression by rememberSaveable { mutableStateOf("") }
    var isInverse by rememberSaveable { mutableStateOf(false) }
    var isScientific by rememberSaveable { mutableStateOf(false) }

    fun evaluateExpression(exp: String): String {
        return try {
            val sanitizedExp = exp
                .replace("×", "*")
                .replace("÷", "/")
                .replace("√", "sqrt")
                .replace("π", Math.PI.toString())
                .replace("e", Math.E.toString())
                .replace("xʸ", "^")
                .replace("sin⁻¹", "asin")
                .replace("cos⁻¹", "acos")
                .replace("tan⁻¹", "atan")
                .replace("C", "")

            val expressionBuilder = ExpressionBuilder(sanitizedExp)
                .function(factorial)
                .function(myAsin)
                .function(myAcos)
                .function(myAtan)
                .build()

            val result = expressionBuilder.evaluate()

            val df = DecimalFormat("#.#######")
            df.format(result)
        } catch (e: Exception) {
            "Error"
        }
    }

    // KOREKSI: Menggunakan label eksplisit 'buttonHandler'
    val onButtonClick: (String) -> Unit = buttonHandler@ { buttonText ->
        var currentText = buttonText

        // Handle tombol Scientific/Standard toggle
        if (buttonText == "SCIENTIFIC_TOGGLE") {
            isScientific = !isScientific
            return@buttonHandler // KOREKSI: Menggunakan label yang benar
        }

        // Handle tombol inverse
        if (isInverse) {
            currentText = when (buttonText) {
                "sin" -> "sin⁻¹"
                "cos" -> "cos⁻¹"
                "tan" -> "tan⁻¹"
                else -> buttonText
            }
        }

        // Sesuaikan nama tombol untuk logika evaluasi
        val logicText = when (currentText) {
            "C" -> "AC"
            "⌫" -> "⌫"
            "," -> "." // Koma menjadi titik
            "00" -> "00"
            "sin⁻¹", "cos⁻¹", "tan⁻¹" -> currentText
            else -> currentText
        }

        when (logicText) {
            "AC" -> {
                expression = ""
                display = "0"
            }
            "⌫" -> { // Backspace
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                    display = if (expression.isEmpty()) "0" else expression
                }
            }
            "=" -> {
                if (expression.isNotEmpty()) {
                    val result = evaluateExpression(expression)
                    display = result
                    expression = if (result != "Error") result else ""
                }
            }
            "inv" -> {
                isInverse = !isInverse
            }
            "π", "e" -> {
                if (display == "0" || expression == "Error" || expression.isEmpty()) {
                    expression = logicText
                } else {
                    expression += logicText
                }
                display = expression
            }
            "sin", "cos", "tan", "log", "ln", "√", "!", "sin⁻¹", "cos⁻¹", "tan⁻¹" -> {
                if (expression == "0" || expression == "Error" || expression.isEmpty()) {
                    expression = "$logicText("
                } else {
                    expression += "$logicText("
                }
                display = expression
            }
            "xʸ" -> {
                expression += "^"
                display = expression
            }
            "." -> { // LOGIKA KOMPONEN DESIMAL YANG DIPERBAIKI (Mengatasi 'Unresolved reference')
                val lastChar = expression.lastOrNull()

                // Jika expression kosong atau karakter terakhir adalah operator, tambahkan "0."
                if (lastChar == null || !lastChar.isDigit()) {
                    expression += "0."
                } else {
                    // Cek apakah angka terakhir sudah memiliki titik
                    // Mencari indeks operator terakhir
                    val lastOperatorIndex = expression.lastIndexOfAny(charArrayOf('+', '-', '×', '÷', '^', '(', ')'))
                    val currentNumber = if (lastOperatorIndex != -1) expression.substring(lastOperatorIndex + 1) else expression

                    if (!currentNumber.contains('.')) {
                        expression += "."
                    }
                }
                display = expression
            }
            else -> {
                // Logika angka dan "00"
                if (display == "0" || expression == "Error" || expression.isEmpty()) {
                    expression = logicText
                } else {
                    expression += logicText
                }
                display = expression
            }
        }
    }

    // --- TAMPILAN ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrimaryBackground)
    ) {
        CalculatorTabs(
            currentTab = "Kalkulator",
            onTabSelected = { /* Logika pemilihan tab */ }
        )

        // Layar Input/Hasil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = display,
                modifier = Modifier.fillMaxWidth(),
                fontSize = if (display.length > 9) 48.sp else 72.sp,
                fontWeight = FontWeight.Light,
                color = Black,
                textAlign = TextAlign.End,
                maxLines = 2,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        // --- Area Tombol ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isScientific) {
                ScientificButtonsRow(onButtonClick, isInverse)
                ScientificButtonsRow2(onButtonClick, isInverse)
            }

            StandardButtonsGrid(onButtonClick, isScientific)
        }
    }
}

// --- Komponen Tampilan (Tidak Ada Perubahan Signifikan) ---

@Composable
fun CalculatorTabs(currentTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("Kalkulator", "Nilai Tukar", "Pengonversi satuan")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        tabs.forEach { tab ->
            Text(
                text = tab,
                color = if (tab == currentTab) AccentOperator else Color.Gray,
                fontWeight = if (tab == currentTab) FontWeight.Bold else FontWeight.Normal,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(end = 24.dp)
                    .clickable { onTabSelected(tab) }
            )
        }
    }
}

@Composable
fun ScientificButtonsRow(onButtonClick: (String) -> Unit, isInverse: Boolean) {
    val row1 = listOf("sin", "cos", "tan", "log", "ln")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ButtonDarkGray)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        row1.forEach { text ->
            val displayText = when (text) {
                "sin" -> if (isInverse) "sin⁻¹" else "sin"
                "cos" -> if (isInverse) "cos⁻¹" else "cos"
                "tan" -> if (isInverse) "tan⁻¹" else "tan"
                else -> text
            }
            ScientificButton(text = displayText, onClick = { onButtonClick(text) }, isInverse = isInverse)
        }
    }
}

@Composable
fun ScientificButtonsRow2(onButtonClick: (String) -> Unit, isInverse: Boolean) {
    val row2 = listOf("π", "e", "inv", "√", "!", "xʸ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ButtonDarkGray)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        row2.forEach { text ->
            ScientificButton(text = text, onClick = { onButtonClick(text) }, isInverse = isInverse)
        }
        ScientificButton(text = "Std", onClick = { onButtonClick("SCIENTIFIC_TOGGLE") }, isInverse = false)
    }
}

@Composable
fun StandardButtonsGrid(onButtonClick: (String) -> Unit, isScientific: Boolean) {
    val buttonRows = listOf(
        listOf("C", "%", "⌫", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("00", "0", ",", "=")
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = if (isScientific) "Std" else "Sci",
            color = AccentOperator,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(end = 16.dp)
                .clickable { onButtonClick("SCIENTIFIC_TOGGLE") }
        )
    }


    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttonRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { buttonText ->
                    CalculatorButton(
                        text = buttonText,
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        onClick = { onButtonClick(buttonText) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (backgroundColor, textColor) = when (text) {
        "C", "%", "⌫", "÷", "×", "-", "+" -> AccentOperator.copy(alpha = 0.1f) to AccentOperator
        "=" -> AccentOperator to White
        else -> ButtonLightGray to Black
    }

    val shape = CircleShape

    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, fontSize = 24.sp, fontWeight = FontWeight.Normal)
    }
}

@Composable
fun ScientificButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isInverse: Boolean
) {
    val displayColor = when {
        text == "inv" && isInverse -> Color.White
        text.contains("⁻¹") -> Color.White
        else -> Color.White.copy(alpha = 0.7f)
    }

    Text(
        text = text,
        color = displayColor,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .width(IntrinsicSize.Min)
    )
}

@Preview(showBackground = true)
@Composable
fun CalculatorAppPreview() {
    MycalculatorTheme {
        CalculatorApp(modifier = Modifier.fillMaxSize())
    }
}