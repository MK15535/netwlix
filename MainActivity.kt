package com.netwlax.app

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val Red = Color(0xFFE5173F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestHighRefreshRate()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Color.Black, surface = Color(0xFF141414))) { App() }
        }
    }

    /** Ask the system for the highest refresh rate (90/120Hz) at the current resolution. */
    @Suppress("DEPRECATION")
    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT < 23) return
        val d = windowManager.defaultDisplay
        val cur = d.mode
        val best = d.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = best.modeId
            preferredRefreshRate = best.refreshRate
        }
    }
}

sealed interface Screen {
    object Home : Screen
    data class Detail(val t: Title) : Screen
    data class Play(val url: String, val name: String) : Screen
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var splash by remember { mutableStateOf(true) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var titles by remember { mutableStateOf<List<Title>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { titles = withContext(Dispatchers.IO) { Catalog.load(ctx) } }
    LaunchedEffect(Unit) { delay(1600); splash = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { (fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.94f)) togetherWith fadeOut(tween(200)) },
            label = "nav"
        ) { s ->
            when (s) {
                Screen.Home -> Home(
                    titles,
                    onOpen = { screen = Screen.Detail(it) },
                    onPlay = { screen = Screen.Play(it.url, it.name) },
                    onUrl = { screen = Screen.Play(it, "Stream") },
                    onCatalogChanged = { reload++ }
                )
                is Screen.Detail -> {
                    BackHandler { screen = Screen.Home }
                    Detail(s.t, onBack = { screen = Screen.Home }) { screen = Screen.Play(s.t.url, s.t.name) }
                }
                is Screen.Play -> {
                    BackHandler { screen = Screen.Home }
                    PlayerScreen(s.url, s.name) { screen = Screen.Home }
                }
            }
        }
        AnimatedVisibility(splash, exit = fadeOut(tween(500))) { Splash() }
    }
}

@Composable
fun Logo(size: Int = 28) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(size.dp).clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.width(8.dp))
        Text("NETWLAX", color = Red, fontWeight = FontWeight.Black, fontSize = (size * 0.7f).sp, letterSpacing = 2.sp)
    }
}

@Composable
fun Splash() {
    val sc = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { sc.animateTo(1.15f, tween(1300, easing = FastOutSlowInEasing)) }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Box(Modifier.scale(sc.value).alpha(((sc.value - 0.6f) / 0.35f).coerceIn(0f, 1f))) { Logo(56) }
    }
}

fun Title.tint(): Color = Color.hsv((name.hashCode() and 0x7fffffff) % 360f, 0.6f, 0.45f)

@Composable
fun Art(t: Title, wide: Boolean, mod: Modifier) {
    Box(mod.background(Brush.linearGradient(listOf(t.tint(), Color.Black)))) {
        val img = if (wide && t.backdrop.isNotBlank()) t.backdrop else t.poster
        if (img.isNotBlank()) AsyncImage(img, t.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun PlayButton(label: String, onClick: () -> Unit) = Button(
    onClick, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
) {
    Icon(Icons.Default.PlayArrow, null)
    Spacer(Modifier.width(4.dp))
    Text(label, fontWeight = FontWeight.Bold)
}

@Composable
fun PosterCard(t: Title, delayMs: Int, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "press")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); shown = true }
    AnimatedVisibility(shown, enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 4 }) {
        Box(Modifier.width(120.dp).height(180.dp).scale(s).clip(RoundedCornerShape(6.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)) {
            Art(t, false, Modifier.fillMaxSize())
            if (t.poster.isBlank()) Text(t.name, Modifier.align(Alignment.BottomStart).padding(8.dp),
                fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TitleRow(cat: String, list: List<Title>, base: Int, onOpen: (Title) -> Unit) {
    Column {
        Text(cat, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(list) { i, t -> PosterCard(t, 80 * i + base * 60) { onOpen(t) } }
        }
    }
}

@Composable
fun Hero(t: Title, onOpen: (Title) -> Unit, onPlay: (Title) -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(Modifier.fillMaxWidth().height(480.dp)) {
        Art(t, true, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 400f)))
        AnimatedVisibility(shown, Modifier.align(Alignment.BottomCenter), enter = fadeIn(tween(700)) + scaleIn(tween(700), 0.9f)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(t.name, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(t.category, color = Color.LightGray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayButton("Play") { onPlay(t) }
                    Button({ onOpen(t) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0x88666666), contentColor = Color.White)) {
                        Icon(Icons.Default.Info, null); Spacer(Modifier.width(4.dp)); Text("Info")
                    }
                }
            }
        }
    }
}

@Composable
fun InputDialog(title: String, hint: String, initial: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(v, { v = it }, placeholder = { Text(hint) }, singleLine = true) },
        confirmButton = { TextButton({ onOk(v.trim()) }) { Text("OK") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun Home(titles: List<Title>, onOpen: (Title) -> Unit, onPlay: (Title) -> Unit, onUrl: (String) -> Unit, onCatalogChanged: () -> Unit) {
    val ctx = LocalContext.current
    var urlDlg by remember { mutableStateOf(false) }
    var srcDlg by remember { mutableStateOf(false) }
    val cats = titles.groupBy { it.category }.entries.toList()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { titles.firstOrNull()?.let { Hero(it, onOpen, onPlay) } }
            cats.forEachIndexed { i, e -> item(key = e.key) { TitleRow(e.key, e.value, i, onOpen) } }
        }
        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                .statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Logo()
            Spacer(Modifier.weight(1f))
            IconButton({ urlDlg = true }) { Icon(Icons.Default.Add, "Play URL") }
            IconButton({ srcDlg = true }) { Icon(Icons.Default.Settings, "Catalog source") }
        }
    }
    if (urlDlg) InputDialog("Play from URL", "https://.../video.mp4 or .m3u8 / .mpd", "", { urlDlg = false }) {
        urlDlg = false; if (it.isNotBlank()) onUrl(it)
    }
    if (srcDlg) InputDialog("Catalog JSON URL", "empty = bundled sample", Catalog.source(ctx), { srcDlg = false }) {
        srcDlg = false; Catalog.setSource(ctx, it); onCatalogChanged()
    }
}

@Composable
fun Detail(t: Title, onBack: () -> Unit, onPlay: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            Art(t, true, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
            IconButton(onBack, Modifier.statusBarsPadding().padding(start = 8.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
        }
        Column(Modifier.padding(16.dp)) {
            Text(t.name, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("${t.year}  •  ${t.category}", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            PlayButton("Play", onPlay)
            Spacer(Modifier.height(16.dp))
            Text(t.desc, color = Color.LightGray, lineHeight = 22.sp)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(url: String, name: String, onClose: () -> Unit) {
    val act = LocalContext.current as ComponentActivity
    var err by remember { mutableStateOf<String?>(null) }
    val exo = remember {
        ExoPlayer.Builder(
            act,
            DefaultRenderersFactory(act).setEnableDecoderFallback(true).setEnableAudioFloatOutput(true)
        )
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
                setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
                trackSelectionParameters = trackSelectionParameters.buildUpon().setForceHighestSupportedBitrate(true).build()
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
    }
    DisposableEffect(exo) {
        val l = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { err = "Can't play this link (${error.errorCodeName})" }
        }
        exo.addListener(l)
        val oldOrientation = act.requestedOrientation
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val c = WindowInsetsControllerCompat(act.window, act.window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) exo.pause() }
        act.lifecycle.addObserver(obs)
        onDispose {
            act.lifecycle.removeObserver(obs)
            exo.removeListener(l)
            exo.release()
            act.requestedOrientation = oldOrientation
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = true
                    keepScreenOn = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    controllerShowTimeoutMs = 3000
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Row(Modifier.statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(name, fontWeight = FontWeight.Bold)
        }
        err?.let { Text(it, Modifier.align(Alignment.Center), color = Color.White) }
    }
}
