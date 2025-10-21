package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.random.Random

data class CalculationHistory(
    val expression: String,
    val result: String,
)

@Composable
fun CalculatorPage(
    onBackClicked: () -> Unit,
) {
    var currentInput by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<CalculationHistory>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    var isTextSelected by remember { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf("") }
    var cursorPosition by remember { mutableStateOf(0) }

    val displayListState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            displayListState.animateScrollToItem(history.size)
        }
    }

    LaunchedEffect(currentInput, cursorPosition) {
        if (cursorPosition > currentInput.length) {
            cursorPosition = currentInput.length
        }
        suggestion = if (cursorPosition == currentInput.length) {
            getSuggestion(currentInput)
        } else {
            ""
        }
    }

    fun extractPreviousOperand(): Pair<String, String> {
        if (cursorPosition == 0) return Pair("", "")

        var end = cursorPosition
        var depth = 0
        var start = end - 1

        // 如果光标前是右括号，需要找到匹配的左括号
        if (start >= 0 && currentInput[start] == ')') {
            depth = 1
            start--
            while (start >= 0 && depth > 0) {
                when (currentInput[start]) {
                    ')' -> depth++
                    '(' -> depth--
                }
                start--
            }
            start++
        } else {
            // 否则提取数字
            while (start >= 0 && (currentInput[start].isDigit() || currentInput[start] == '.')) {
                start--
            }
            start++
        }

        val prefix = currentInput.substring(0, start)
        val operand = currentInput.substring(start, end)

        return Pair(prefix, operand)
    }

    fun handleInput(input: String) {
        when (input) {
            "C" -> {
                if (isTextSelected) {
                    isTextSelected = false
                } else {
                    currentInput = ""
                    cursorPosition = 0
                    historyIndex = -1
                }
            }

            "⌫" -> {
                if (isTextSelected) {
                    currentInput = ""
                    cursorPosition = 0
                    isTextSelected = false
                } else if (cursorPosition > 0) {
                    currentInput = currentInput.substring(0, cursorPosition - 1) +
                            currentInput.substring(cursorPosition)
                    cursorPosition--
                }
                historyIndex = -1
            }

            "=" -> {
                if (currentInput.isNotEmpty()) {
                    try {
                        val result = evaluateExpression(currentInput)
                        val formattedResult = formatResult(result)
                        history = history + CalculationHistory(currentInput, formattedResult)
                        currentInput = ""
                        cursorPosition = 0
                        historyIndex = -1
                    } catch (e: Exception) {
                        history = history + CalculationHistory(currentInput, "错误: ${e.message}")
                        currentInput = ""
                        cursorPosition = 0
                        historyIndex = -1
                    }
                }
            }

            "→" -> {
                if (suggestion.isNotEmpty()) {
                    currentInput += suggestion
                    cursorPosition = currentInput.length
                    isTextSelected = false
                }
            }

            "rand" -> {
                val randomValue = Random.nextDouble()
                val randomStr = String.format("%.10f", randomValue).trimEnd('0').trimEnd('.')
                if (isTextSelected) {
                    currentInput = randomStr
                    cursorPosition = randomStr.length
                    isTextSelected = false
                } else {
                    currentInput = currentInput.substring(0, cursorPosition) +
                            randomStr +
                            currentInput.substring(cursorPosition)
                    cursorPosition += randomStr.length
                }
                historyIndex = -1
            }
            // 处理需要前置操作数的运算符
            "x²", "x³", "x!", "1/x", "10^x", "e^x" -> {
                val (prefix, operand) = extractPreviousOperand()
                val (replacement, newCursorOffset) = when (input) {
                    "x²" -> if (operand.isNotEmpty()) {
                        Pair("($operand)^2", "($operand)^2".length)
                    } else {
                        Pair("()^2", 1)
                    }

                    "x³" -> if (operand.isNotEmpty()) {
                        Pair("($operand)^3", "($operand)^3".length)
                    } else {
                        Pair("()^3", 1)
                    }

                    "x!" -> if (operand.isNotEmpty()) {
                        Pair("($operand)!", "($operand)!".length)
                    } else {
                        Pair("()!", 1)
                    }

                    "1/x" -> if (operand.isNotEmpty()) {
                        Pair("1/($operand)", "1/($operand)".length)
                    } else {
                        Pair("1/()", 3)
                    }

                    "10^x" -> if (operand.isNotEmpty()) {
                        Pair("10^($operand)", "10^($operand)".length)
                    } else {
                        Pair("10^()", 4)
                    }

                    "e^x" -> if (operand.isNotEmpty()) {
                        Pair("e^($operand)", "e^($operand)".length)
                    } else {
                        Pair("e^()", 3)
                    }

                    else -> Pair(operand, operand.length)
                }
                currentInput = prefix + replacement + currentInput.substring(cursorPosition)
                cursorPosition = prefix.length + newCursorOffset
                historyIndex = -1
            }
            // 处理需要自动添加括号的函数
            "sin", "cos", "tan", "asin", "acos", "atan",
            "sinh", "cosh", "tanh", "ln", "log", "√",
            "abs", "ceil", "floor", "round",
                -> {
                if (isTextSelected) {
                    currentInput = "$input()"
                    cursorPosition = input.length + 1
                    isTextSelected = false
                } else {
                    currentInput = currentInput.substring(0, cursorPosition) +
                            "$input()" +
                            currentInput.substring(cursorPosition)
                    cursorPosition += input.length + 1
                }
                historyIndex = -1
            }

            else -> {
                if (isTextSelected) {
                    currentInput = input
                    cursorPosition = input.length
                    isTextSelected = false
                } else {
                    currentInput = currentInput.substring(0, cursorPosition) +
                            input +
                            currentInput.substring(cursorPosition)
                    cursorPosition += input.length
                }
                historyIndex = -1
            }
        }
    }

    fun navigateHistory(direction: Int) {
        if (history.isEmpty()) return

        if (direction > 0) {
            if (historyIndex < history.size - 1) {
                historyIndex++
                currentInput = history[history.size - 1 - historyIndex].expression
                cursorPosition = currentInput.length
            }
        } else {
            if (historyIndex > 0) {
                historyIndex--
                currentInput = history[history.size - 1 - historyIndex].expression
                cursorPosition = currentInput.length
            } else if (historyIndex == 0) {
                historyIndex = -1
                currentInput = ""
                cursorPosition = 0
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val isShift = event.isShiftPressed
                    val isCtrl = event.isCtrlPressed

                    when {
                        isCtrl && event.key == Key.A -> {
                            if (currentInput.isNotEmpty()) {
                                isTextSelected = true
                            }
                            true
                        }

                        event.key == Key.DirectionUp -> {
                            navigateHistory(1)
                            true
                        }

                        event.key == Key.DirectionDown -> {
                            navigateHistory(-1)
                            true
                        }

                        event.key == Key.DirectionLeft -> {
                            if (cursorPosition > 0) {
                                cursorPosition--
                            }
                            true
                        }

                        event.key == Key.DirectionRight -> {
                            if (suggestion.isNotEmpty() && cursorPosition == currentInput.length) {
                                handleInput("→")
                            } else if (cursorPosition < currentInput.length) {
                                cursorPosition++
                            }
                            true
                        }

                        event.key == Key.Home -> {
                            cursorPosition = 0
                            true
                        }

                        event.key == Key.MoveEnd -> {
                            cursorPosition = currentInput.length
                            true
                        }

                        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                            handleInput("=")
                            true
                        }

                        event.key == Key.Backspace -> {
                            handleInput("⌫")
                            true
                        }

                        isShift && event.key == Key.Nine -> {
                            handleInput("("); true
                        }

                        isShift && event.key == Key.Zero -> {
                            handleInput(")"); true
                        }

                        isShift && event.key == Key.Eight -> {
                            handleInput("×"); true
                        }

                        isShift && event.key == Key.Equals -> {
                            handleInput("+"); true
                        }

                        isShift && event.key == Key.Six -> {
                            handleInput("^"); true
                        }

                        isShift && event.key == Key.One -> {
                            handleInput("x!"); true
                        }

                        isShift && event.key == Key.Five -> {
                            handleInput("%"); true
                        }

                        event.key == Key.Zero -> {
                            handleInput("0"); true
                        }

                        event.key == Key.One -> {
                            handleInput("1"); true
                        }

                        event.key == Key.Two -> {
                            handleInput("2"); true
                        }

                        event.key == Key.Three -> {
                            handleInput("3"); true
                        }

                        event.key == Key.Four -> {
                            handleInput("4"); true
                        }

                        event.key == Key.Five -> {
                            handleInput("5"); true
                        }

                        event.key == Key.Six -> {
                            handleInput("6"); true
                        }

                        event.key == Key.Seven -> {
                            handleInput("7"); true
                        }

                        event.key == Key.Eight -> {
                            handleInput("8"); true
                        }

                        event.key == Key.Nine -> {
                            handleInput("9"); true
                        }

                        event.key == Key.Plus -> {
                            handleInput("+"); true
                        }

                        event.key == Key.Minus -> {
                            handleInput("-"); true
                        }

                        event.key == Key.Multiply || event.key == Key.NumPadMultiply -> {
                            handleInput("×"); true
                        }

                        event.key == Key.Slash || event.key == Key.NumPadDivide -> {
                            handleInput("÷"); true
                        }

                        event.key == Key.Period || event.key == Key.NumPadDot -> {
                            handleInput("."); true
                        }

                        event.key == Key.Equals && !isShift -> {
                            handleInput("="); true
                        }

                        event.key == Key.C -> {
                            handleInput("c"); true
                        }

                        event.key == Key.S -> {
                            handleInput("s"); true
                        }

                        event.key == Key.T -> {
                            handleInput("t"); true
                        }

                        event.key == Key.L -> {
                            handleInput("l"); true
                        }

                        event.key == Key.A && !isCtrl -> {
                            handleInput("a"); true
                        }

                        event.key == Key.E && !isCtrl -> {
                            handleInput("e"); true
                        }

                        event.key == Key.P -> {
                            handleInput("p"); true
                        }

                        event.key == Key.I -> {
                            handleInput("i"); true
                        }

                        event.key == Key.N -> {
                            handleInput("n"); true
                        }

                        event.key == Key.O -> {
                            handleInput("o"); true
                        }

                        event.key == Key.R -> {
                            handleInput("r"); true
                        }

                        event.key == Key.H -> {
                            handleInput("h"); true
                        }

                        event.key == Key.F -> {
                            handleInput("f"); true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier.size(36.dp),
                    enabled = true
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Pages.FunctionPage.Back.getLang(),
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Pages.AddSitePage.Calculator.getLang(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }

        // 显示屏
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(bottom = 12.dp),
            elevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            LazyColumn(
                state = displayListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 历史记录
                items(history) { item ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.expression,
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "= ${item.result}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }

                // 当前输入
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cursorPosition > 0) {
                                    Text(
                                        text = getHighlightedText(
                                            currentInput.substring(0, cursorPosition),
                                            false
                                        ),
                                        fontSize = 24.sp,
                                        textAlign = TextAlign.End
                                    )
                                }

                                if (!isTextSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(28.dp)
                                            .background(MaterialTheme.colors.primary)
                                    )
                                }

                                Row {
                                    if (cursorPosition < currentInput.length) {
                                        Text(
                                            text = getHighlightedText(
                                                currentInput.substring(cursorPosition),
                                                isTextSelected
                                            ),
                                            fontSize = 24.sp,
                                            textAlign = TextAlign.Start
                                        )
                                    } else if (isTextSelected && currentInput.isNotEmpty()) {
                                        Text(
                                            text = getHighlightedText(currentInput, true),
                                            fontSize = 24.sp,
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    if (suggestion.isNotEmpty() && !isTextSelected && cursorPosition == currentInput.length) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        }

                        if (suggestion.isNotEmpty() && !isTextSelected && cursorPosition == currentInput.length) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Pages.SchedulePage.AcceptSuggestion.getLang(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colors.primary.copy(alpha = 0.6f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(start = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 按钮网格
        val allButtons = listOf(
            listOf("C", "⌫", "(", ")"),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "%", "+"),
            listOf("π", "e", "^", "="),
            listOf("sin", "cos", "tan", "√"),
            listOf("asin", "acos", "atan", "rand"),
            listOf("ln", "log", "e^x", "10^x"),
            listOf("x²", "x³", "x!", "1/x"),
            listOf("abs", "ceil", "floor", "round"),
            listOf("sinh", "cosh", "tanh", "→")
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(allButtons.size * 4) { index ->
                val row = index / 4
                val col = index % 4
                if (row < allButtons.size && col < allButtons[row].size) {
                    CalculatorButton(
                        text = allButtons[row][col],
                        onClick = {
                            handleInput(allButtons[row][col])
                            focusRequester.requestFocus()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
) {
    val isOperator = text in listOf("+", "-", "×", "÷", "=", "^", "%")
    val isFunction = text in listOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "ln", "log", "√",
        "abs", "ceil", "floor", "round",
        "x²", "x³", "x!", "1/x", "10^x", "e^x",
        "(", ")"
    )
    val isClear = text in listOf("C", "⌫")
    val isConstant = text in listOf("π", "e")
    val isSpecial = text in listOf("→", "rand")

    val backgroundColor = when {
        text == "=" -> MaterialTheme.colors.primary
        isOperator -> MaterialTheme.colors.primary.copy(alpha = 0.2f)
        isFunction -> MaterialTheme.colors.secondary.copy(alpha = 0.2f)
        isClear -> Color(0xFFFF6B6B).copy(alpha = 0.2f)
        isConstant -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        isSpecial -> Color(0xFF2196F3).copy(alpha = 0.2f)
        else -> MaterialTheme.colors.surface
    }

    val textColor = when {
        text == "=" -> Color.White
        isOperator -> MaterialTheme.colors.primary
        isFunction -> MaterialTheme.colors.secondary
        isClear -> Color(0xFFFF6B6B)
        isConstant -> Color(0xFF4CAF50)
        isSpecial -> Color(0xFF2196F3)
        else -> MaterialTheme.colors.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(onClick = onClick),
        elevation = if (text == "=") 4.dp else 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (text.length > 4) 10.sp else if (text.length > 2) 12.sp else 16.sp,
                fontWeight = if (text == "=") FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

fun getSuggestion(input: String): String {
    if (input.isEmpty()) return ""

    val keywords = listOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "sqrt", "abs", "ceil", "floor", "round",
        "ln", "log", "exp"
    )

    var lastWordStart = input.length - 1
    while (lastWordStart >= 0 && input[lastWordStart].isLetter()) {
        lastWordStart--
    }
    lastWordStart++

    if (lastWordStart < input.length) {
        val lastWord = input.substring(lastWordStart).lowercase()
        for (keyword in keywords) {
            if (keyword.startsWith(lastWord) && keyword != lastWord) {
                return keyword.substring(lastWord.length)
            }
        }
    }

    return ""
}

@Composable
fun getHighlightedText(input: String, isSelected: Boolean): AnnotatedString {
    return buildAnnotatedString {
        if (isSelected) {
            withStyle(
                style = SpanStyle(
                    background = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                    color = MaterialTheme.colors.onSurface
                )
            ) {
                append(input)
            }
        } else {
            val errors = findSyntaxErrors(input)
            var currentIndex = 0

            for ((start, end) in errors) {
                if (currentIndex < start) {
                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                        append(input.substring(currentIndex, start))
                    }
                }
                withStyle(
                    style = SpanStyle(
                        color = Color.Red,
                        background = Color.Red.copy(alpha = 0.2f)
                    )
                ) {
                    append(input.substring(start, end))
                }
                currentIndex = end
            }

            if (currentIndex < input.length) {
                withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                    append(input.substring(currentIndex))
                }
            }
        }
    }
}

fun findSyntaxErrors(input: String): List<Pair<Int, Int>> {
    val errors = mutableListOf<Pair<Int, Int>>()
    val validTokens = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "sqrt", "abs", "ceil", "floor", "round",
        "ln", "log", "exp", "pi", "e"
    )

    var i = 0
    while (i < input.length) {
        if (input[i].isLetter()) {
            val start = i
            while (i < input.length && input[i].isLetter()) {
                i++
            }
            val token = input.substring(start, i).lowercase()
            if (token !in validTokens) {
                errors.add(Pair(start, i))
            }
        } else {
            i++
        }
    }

    return errors
}

fun evaluateExpression(expression: String): Double {
    if (expression.isEmpty()) return 0.0

    var expr = expression.trim()
        .replace("×", "*")
        .replace("÷", "/")
        .replace("π", PI.toString())
        .replace(" ", "")

    expr = expr.replace(Regex("(?<![0-9.])e(?![0-9+-])"), E.toString())
    expr = handleAdvancedFunctions(expr)

    return Parser(expr).parse()
}

fun handleAdvancedFunctions(expr: String): String {
    var result = expr

    // 处理阶乘
    while (result.contains("!")) {
        val index = result.indexOf("!")
        var startIndex = index - 1
        var parenDepth = 0

        // 如果!前面是右括号，找匹配的左括号
        if (startIndex >= 0 && result[startIndex] == ')') {
            parenDepth = 1
            startIndex--
            while (startIndex >= 0 && parenDepth > 0) {
                when (result[startIndex]) {
                    ')' -> parenDepth++
                    '(' -> parenDepth--
                }
                startIndex--
            }
            startIndex++
        } else {
            // 否则提取数字
            while (startIndex >= 0 && (result[startIndex].isDigit() || result[startIndex] == '.')) {
                startIndex--
            }
            startIndex++
        }

        val numberStr = result.substring(startIndex, index)
        val number = evaluateExpression(numberStr)
        val factorial = factorial(number.toInt())
        result = result.substring(0, startIndex) + factorial + result.substring(index + 1)
    }

    // 处理函数调用
    val functions = mapOf(
        "asin" to "asin", "acos" to "acos", "atan" to "atan",
        "sinh" to "sinh", "cosh" to "cosh", "tanh" to "tanh",
        "sin" to "sin", "cos" to "cos", "tan" to "tan",
        "ln" to "ln", "log" to "log",
        "abs" to "abs", "ceil" to "ceil", "floor" to "floor", "round" to "round",
        "sqrt" to "sqrt", "√" to "sqrt"
    )

    for ((pattern, func) in functions.entries.sortedByDescending { it.key.length }) {
        while (result.contains(pattern + "(")) {
            val index = result.indexOf(pattern + "(")
            val startIndex = index + pattern.length + 1
            var endIndex = startIndex
            var parenCount = 1

            while (endIndex < result.length && parenCount > 0) {
                if (result[endIndex] == '(') parenCount++
                if (result[endIndex] == ')') parenCount--
                endIndex++
            }

            val arg = result.substring(startIndex, endIndex - 1)
            val argValue = evaluateExpression(arg)

            val funcResult = when (func) {
                "sin" -> sin(Math.toRadians(argValue))
                "cos" -> cos(Math.toRadians(argValue))
                "tan" -> tan(Math.toRadians(argValue))
                "asin" -> Math.toDegrees(asin(argValue))
                "acos" -> Math.toDegrees(acos(argValue))
                "atan" -> Math.toDegrees(atan(argValue))
                "sinh" -> sinh(argValue)
                "cosh" -> cosh(argValue)
                "tanh" -> tanh(argValue)
                "ln" -> ln(argValue)
                "log" -> log10(argValue)
                "abs" -> abs(argValue)
                "ceil" -> ceil(argValue)
                "floor" -> floor(argValue)
                "round" -> round(argValue)
                "sqrt" -> sqrt(argValue)
                else -> argValue
            }

            result = result.substring(0, index) + funcResult + result.substring(endIndex)
        }
    }

    return result
}

fun factorial(n: Int): Double {
    if (n < 0) throw IllegalArgumentException("负数没有阶乘")
    if (n > 170) throw IllegalArgumentException("数字太大")
    var result = 1.0
    for (i in 2..n) {
        result *= i
    }
    return result
}

fun formatResult(result: Double): String {
    return when {
        result.isNaN() -> "未定义"
        result.isInfinite() -> if (result > 0) "∞" else "-∞"
        abs(result) < 1e-10 -> "0"
        result % 1.0 == 0.0 && abs(result) < 1e15 -> result.toLong().toString()
        abs(result) >= 1e10 || abs(result) < 1e-4 -> String.format("%.6e", result)
        else -> {
            val formatted = String.format("%.10f", result).trimEnd('0').trimEnd('.')
            if (formatted.length > 15) {
                String.format("%.6e", result)
            } else {
                formatted
            }
        }
    }
}

class Parser(private val expression: String) {
    private var pos = 0

    fun parse(): Double {
        val result = parseExpression()
        if (pos < expression.length) {
            throw IllegalArgumentException("意外字符: ${expression[pos]}")
        }
        return result
    }

    private fun parseExpression(): Double {
        var result = parseTerm()

        while (pos < expression.length) {
            when (expression[pos]) {
                '+' -> {
                    pos++
                    result += parseTerm()
                }

                '-' -> {
                    pos++
                    result -= parseTerm()
                }

                else -> break
            }
        }

        return result
    }

    private fun parseTerm(): Double {
        var result = parsePower()

        while (pos < expression.length) {
            when {
                expression[pos] == '*' -> {
                    pos++
                    result *= parsePower()
                }

                expression[pos] == '/' -> {
                    pos++
                    val divisor = parsePower()
                    if (divisor == 0.0) throw ArithmeticException("除数为零")
                    result /= divisor
                }

                expression[pos] == '%' -> {
                    pos++
                    val divisor = parsePower()
                    if (divisor == 0.0) throw ArithmeticException("除数为零")
                    result %= divisor
                }

                else -> break
            }
        }

        return result
    }

    private fun parsePower(): Double {
        var result = parseFactor()

        while (pos < expression.length && expression[pos] == '^') {
            pos++
            result = result.pow(parseFactor())
        }

        return result
    }

    private fun parseFactor(): Double {
        skipWhitespace()

        if (pos < expression.length && expression[pos] == '(') {
            pos++
            val result = parseExpression()
            if (pos >= expression.length || expression[pos] != ')') {
                throw IllegalArgumentException("缺少右括号")
            }
            pos++
            return result
        }

        if (pos < expression.length && expression[pos] == '-') {
            pos++
            return -parseFactor()
        }

        if (pos < expression.length && expression[pos] == '+') {
            pos++
            return parseFactor()
        }

        return parseNumber()
    }

    private fun parseNumber(): Double {
        skipWhitespace()
        val start = pos

        while (pos < expression.length &&
            (expression[pos].isDigit() || expression[pos] == '.' ||
                    expression[pos] == 'e' || expression[pos] == 'E')
        ) {
            if ((expression[pos] == 'e' || expression[pos] == 'E') &&
                pos + 1 < expression.length &&
                (expression[pos + 1] == '+' || expression[pos + 1] == '-')
            ) {
                pos += 2
            } else {
                pos++
            }
        }

        if (start == pos) {
            throw IllegalArgumentException("期望数字在位置 $pos")
        }

        return expression.substring(start, pos).toDoubleOrNull()
            ?: throw IllegalArgumentException("无效数字: ${expression.substring(start, pos)}")
    }

    private fun skipWhitespace() {
        while (pos < expression.length && expression[pos].isWhitespace()) {
            pos++
        }
    }
}
