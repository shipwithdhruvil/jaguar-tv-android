package org.screenlite.webkiosk.components

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.screenlite.webkiosk.app.TvPower
import org.screenlite.webkiosk.data.KioskSettingsFactory
import org.screenlite.webkiosk.data.Rotation
import org.screenlite.webkiosk.data.ServerChoice
import org.screenlite.webkiosk.data.ServerConfig
import org.screenlite.webkiosk.data.TvBreak
import org.screenlite.webkiosk.service.StayOnTopService
import org.screenlite.webkiosk.ui.theme.*
import java.text.DateFormat
import java.util.Date

private val GroupShape = RoundedCornerShape(20.dp)
private val PillShape = RoundedCornerShape(50)

/**
 * The staff-only settings behind five presses of OK. Built like the CRM's grouped
 * rows: label on the left, value on the right, hairlines between, one paper-white
 * primary button.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val kioskSettings = remember { KioskSettingsFactory.get(context) }

    // Debug only: which server, and the address typed under Custom.
    var serverChoice by remember { mutableStateOf(ServerChoice.LIVE) }
    var kioskUrl by remember { mutableStateOf("") }
    var kioskUrlError by remember { mutableStateOf<String?>(null) }
    var checkIntervalSeconds by remember { mutableStateOf("") }
    var rotation by remember { mutableStateOf(Rotation.ROTATION_0) }
    var idleTimeout by remember { mutableStateOf("") }
    var idleBrightness by remember { mutableStateOf("") }
    var activeBrightness by remember { mutableStateOf("") }

    var checkIntervalError by remember { mutableStateOf<String?>(null) }
    var idleTimeoutError by remember { mutableStateOf<String?>(null) }
    var idleBrightnessError by remember { mutableStateOf<String?>(null) }
    var activeBrightnessError by remember { mutableStateOf<String?>(null) }

    // Off at closing: whether the TV may be switched off (TvPower), checked again after Android's "allow" screen.
    var canSleep by remember { mutableStateOf(TvPower.canSleep(context)) }
    var powerNote by remember { mutableStateOf<String?>(null) }
    val allowLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        canSleep = TvPower.canSleep(context)
    }
    val togglePower: () -> Unit = {
        try {
            if (canSleep) {
                // A device admin is given back at once; the accessibility way is switched off on the TV's own screen.
                TvPower.stopSleeping(context)?.let { allowLauncher.launch(it) }
                canSleep = TvPower.canSleep(context)
            } else {
                allowLauncher.launch(TvPower.allowIntent(context))
            }
        } catch (e: ActivityNotFoundException) {
            powerNote = "This TV has no screen for it. Allow it from a computer: see the install notes."
        }
    }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    // Take a break: the choices replace the settings until one is picked or Back is pressed.
    var choosingBreak by remember { mutableStateOf(false) }
    val takeBreak: (Long) -> Unit = { until ->
        TvBreak.start(context, until)
        // To the TV's own home screen; nothing brings Jaguar TV back until the break is over.
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        (context as? ComponentActivity)?.finish()
    }

    LaunchedEffect(Unit) {
        val stored = kioskSettings.getStartUrl().first()
        serverChoice = ServerConfig.choiceFor(stored)
        kioskUrl = if (serverChoice == ServerChoice.CUSTOM) stored else "https://"
        checkIntervalSeconds = (kioskSettings.getCheckInterval().first() / 1000).toString()
        rotation = kioskSettings.getRotation().first()
        idleTimeout = kioskSettings.getIdleTimeout().first().toString()
        idleBrightness = kioskSettings.getIdleBrightness().first().toString()
        activeBrightness = kioskSettings.getActiveBrightness().first().toString()
    }

    fun digits(value: String, set: (String) -> Unit, clearError: () -> Unit) {
        if (value.matches(Regex("\\d*"))) { set(value); clearError() }
    }

    val save: () -> Unit = save@{
        var hasError = false

        val startUrl = when {
            !ServerConfig.SERVER_CHOICE -> ServerConfig.LIVE_URL
            serverChoice == ServerChoice.LIVE -> ServerConfig.LIVE_URL
            serverChoice == ServerChoice.MAC -> ServerConfig.MAC_URL
            else -> kioskUrl.trim()
        }
        if (ServerConfig.SERVER_CHOICE && serverChoice == ServerChoice.CUSTOM && !ServerConfig.isValidCustom(startUrl)) {
            kioskUrlError = "Address: a full http:// or https:// address"; hasError = true
        }

        val checkIntervalValue = checkIntervalSeconds.toLongOrNull()
        if (checkIntervalValue == null || checkIntervalValue !in 1..99999) { checkIntervalError = "Check every: 1 to 99999 seconds"; hasError = true }

        val idleTimeoutValue = idleTimeout.toLongOrNull()
        if (idleTimeoutValue == null || idleTimeoutValue < 0) { idleTimeoutError = "Dim after: 0 or more seconds"; hasError = true }

        val idleBrightnessValue = idleBrightness.toIntOrNull()
        if (idleBrightnessValue == null || idleBrightnessValue !in 0..100) { idleBrightnessError = "Dim to: 0 to 100 %"; hasError = true }

        val activeBrightnessValue = activeBrightness.toIntOrNull()
        if (activeBrightnessValue == null || activeBrightnessValue !in 0..100) { activeBrightnessError = "Brightness: 0 to 100 %"; hasError = true }

        if (hasError) return@save

        (context as? ComponentActivity)?.lifecycleScope?.launch {
            kioskSettings.setCheckInterval(checkIntervalValue!! * 1000L)
            kioskSettings.setStartUrl(startUrl)
            kioskSettings.setRotation(rotation)
            kioskSettings.setIdleTimeout(idleTimeoutValue!!)
            kioskSettings.setIdleBrightness(idleBrightnessValue!!)
            kioskSettings.setActiveBrightness(activeBrightnessValue!!)

            StayOnTopService.restart(context)
        }
        (context as? ComponentActivity)?.finish()
    }

    val screenGroup: @Composable () -> Unit = {
        SettingsGroup("Screen", errors = listOfNotNull(kioskUrlError)) {
            if (ServerConfig.SERVER_CHOICE) {
                ChoiceRow("Server", ServerChoice.entries, serverChoice, {
                    when (it) {
                        ServerChoice.LIVE -> "Live"
                        ServerChoice.MAC -> "This Mac"
                        ServerChoice.CUSTOM -> "Custom"
                    }
                }) { serverChoice = it; kioskUrlError = null }
                Separator()
                if (serverChoice == ServerChoice.CUSTOM) {
                    TextRow("Address", kioskUrl, { kioskUrl = it; kioskUrlError = null }, KeyboardType.Uri, isError = kioskUrlError != null)
                    Separator()
                }
            } else {
                ReadOnlyRow("Server", "Live")
                Separator()
            }
            ChoiceRow("Rotation", Rotation.entries, rotation, { "${it.degrees}°" }) { rotation = it }
        }
    }
    val runningGroup: @Composable () -> Unit = {
        SettingsGroup(
            "Keep running",
            errors = listOfNotNull(checkIntervalError, powerNote),
            note = if (canSleep) "Off at the gym's closing time. Switch it on in the morning." else "Off at closing needs your OK once.",
        ) {
            TextRow("Check every", checkIntervalSeconds, { digits(it, { v -> checkIntervalSeconds = v }) { checkIntervalError = null } }, KeyboardType.Number, suffix = "s", isError = checkIntervalError != null)
            Separator()
            ActionRow("Off at closing", if (canSleep) "On" else "Allow", onClick = togglePower)
        }
    }
    val dimmingGroup: @Composable () -> Unit = {
        SettingsGroup("Dimming", errors = listOfNotNull(idleTimeoutError, idleBrightnessError, activeBrightnessError), note = "0 s keeps the screen on.") {
            TextRow("Dim after", idleTimeout, { digits(it, { v -> idleTimeout = v }) { idleTimeoutError = null } }, KeyboardType.Number, suffix = "s", isError = idleTimeoutError != null)
            Separator()
            TextRow("Dim to", idleBrightness, { digits(it, { v -> idleBrightness = v }) { idleBrightnessError = null } }, KeyboardType.Number, suffix = "%", isError = idleBrightnessError != null)
            Separator()
            TextRow("Brightness", activeBrightness, { digits(it, { v -> activeBrightness = v }) { activeBrightnessError = null } }, KeyboardType.Number, suffix = "%", isError = activeBrightnessError != null)
        }
    }

    if (choosingBreak) {
        BreakPicker(onPick = takeBreak, onBack = { choosingBreak = false })
        return
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Frame)
    ) {
        val wide = maxWidth >= 720.dp
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide) 48.dp else 20.dp, vertical = if (wide) 32.dp else 24.dp)
        ) {
            Text("TV settings", style = TitleLarge)
            Text(listOfNotNull("Jaguar TV", version?.let { "v$it" }).joinToString(" · "), style = Caption, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(24.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Each column scrolls: on a TV the remote's focus brings a row below the fold into view.
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            screenGroup()
                            runningGroup()
                        }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { dimmingGroup() }
                    }
                } else {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        screenGroup()
                        runningGroup()
                        dimmingGroup()
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillButton("Take a break", primary = false) { choosingBreak = true }
                Spacer(Modifier.weight(1f))
                PillButton("Cancel", primary = false) { (context as? ComponentActivity)?.finish() }
                PillButton("Save", primary = true, onClick = save)
            }
        }
    }
}

private val Breaks = listOf(30L to "30 minutes", 60L to "1 hour", 120L to "2 hours", 0L to "Until opened")

/**
 * Take a break: four rows, each saying when Jaguar TV comes back. Picking one leaves for the TV's home screen;
 * Back returns to the settings.
 */
@Composable
private fun BreakPicker(onPick: (Long) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    val clock = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Frame)) {
        val wide = maxWidth >= 720.dp
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide) 48.dp else 20.dp, vertical = if (wide) 32.dp else 24.dp)
        ) {
            Text("Take a break", style = TitleLarge)
            Text("Use the TV for something else. Jaguar TV comes back by itself, or when you open it.", style = Caption, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(24.dp))
            Column(Modifier.weight(1f).widthIn(max = 640.dp)) {
                SettingsGroup("How long", errors = emptyList()) {
                    Breaks.forEachIndexed { i, (minutes, label) ->
                        if (i > 0) Separator()
                        val back = if (minutes == 0L) "Back when opened" else "Back at ${clock.format(Date(System.currentTimeMillis() + minutes * 60_000))}"
                        ActionRow(label, back, if (i == 0) Modifier.focusRequester(first) else Modifier) {
                            onPick(if (minutes == 0L) TvBreak.UNTIL_OPENED else System.currentTimeMillis() + minutes * 60_000)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                PillButton("Cancel", primary = false, onClick = onBack)
            }
        }
    }
}

/** A row that does something when OK is pressed: label on the left, what will happen on the right. */
@Composable
private fun ActionRow(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)) {
        RowFrame(focused) {
            Text(label, style = RowLabel, modifier = Modifier.weight(1f))
            Text(value, style = RowValue.copy(color = Muted), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, errors: List<String>, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = Caption, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(Graphite, GroupShape)
                .padding(vertical = 4.dp),
            content = content
        )
        if (note != null && errors.isEmpty()) {
            Text(note, style = Caption, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
        }
        errors.forEach {
            Text(it, style = Caption.copy(color = AccentText), modifier = Modifier.padding(start = 20.dp, top = 8.dp))
        }
    }
}

@Composable
private fun Separator() {
    Box(
        Modifier
            .padding(start = 20.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline)
    )
}

@Composable
private fun RowFrame(focused: Boolean, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .background(if (focused) Raised else Graphite, RoundedCornerShape(16.dp))
            .border(2.dp, if (focused) Accent else Graphite, RoundedCornerShape(16.dp))
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun TextRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Like the TV's own settings: a row is only selected until OK is pressed, so a
    // stray press (such as the one that opened this screen) never pops the keyboard.
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(focused) { if (!focused) editing = false }
    LaunchedEffect(editing) { if (editing) keyboard?.show() else keyboard?.hide() }
    // Back closes the keyboard without telling the field; leave edit mode then too,
    // so Left and Right go back to moving between rows.
    @OptIn(ExperimentalLayoutApi::class)
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) imeWasVisible = true
        else if (imeWasVisible) { imeWasVisible = false; editing = false }
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        readOnly = !editing,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { editing = false }),
        textStyle = RowValue.copy(color = if (isError) AccentText else Ink, textAlign = TextAlign.End),
        cursorBrush = SolidColor(Accent),
        interactionSource = interaction,
        // A single-line field would keep Up and Down for its cursor; on a remote they
        // must move between rows.
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (it.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (!editing) { editing = true; true } else false
                    Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                    Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                    Key.DirectionLeft -> if (!editing) focusManager.moveFocus(FocusDirection.Left) else false
                    Key.DirectionRight -> if (!editing) focusManager.moveFocus(FocusDirection.Right) else false
                    else -> false
                }
            },
        decorationBox = { field ->
            RowFrame(focused) {
                Text(label, style = RowLabel, modifier = Modifier.padding(end = 16.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { field() }
                if (suffix != null) Text(suffix, style = RowValue.copy(color = Muted), modifier = Modifier.padding(start = 6.dp))
            }
        }
    )
}

/** A value that cannot be edited; still reachable with the D-pad so the list reads in order. */
@Composable
private fun ReadOnlyRow(label: String, value: String) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(Modifier.focusable(interactionSource = interaction)) {
        RowFrame(focused) {
            Text(label, style = RowLabel, modifier = Modifier.weight(1f))
            Text(value, style = RowValue.copy(color = Muted), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun <T> ChoiceRow(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    RowFrame(focused = false) {
        Text(label, style = RowLabel, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val isSelected = option == selected
                Box(
                    Modifier
                        .background(if (isSelected) Paper else Chip, PillShape)
                        .border(2.dp, if (focused) Accent else androidx.compose.ui.graphics.Color.Transparent, PillShape)
                        .clickable(interactionSource = interaction, indication = null) { onSelect(option) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text(option), style = ButtonText.copy(fontSize = RowValue.fontSize, color = if (isSelected) PaperInk else Ink))
                }
            }
        }
    }
}

@Composable
private fun PillButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .background(if (primary) Paper else Raised, PillShape)
            .border(3.dp, if (focused) Accent else androidx.compose.ui.graphics.Color.Transparent, PillShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = ButtonText.copy(color = if (primary) PaperInk else Ink))
    }
}
