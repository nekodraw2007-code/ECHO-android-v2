package com.echo.paranormal

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), SensorEventListener {
    private val bgColor = Color.rgb(7, 11, 19)
    private val surface = Color.rgb(16, 24, 37)
    private val surfaceAlt = Color.rgb(22, 33, 49)
    private val cyan = Color.rgb(83, 220, 237)
    private val white = Color.rgb(242, 247, 252)
    private val muted = Color.rgb(145, 163, 184)
    private val green = Color.rgb(91, 220, 164)
    private val orange = Color.rgb(255, 174, 92)

    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var magnetic = floatArrayOf(0f, 0f, 0f)
    private var acceleration = floatArrayOf(0f, 0f, 0f)
    private var rotation = floatArrayOf(0f, 0f, 0f)
    private var fieldStrength = 0f
    private var baseline = 0f
    private var delta = 0f
    private var smoothedDelta = 0f
    private var calibrated = false
    private var calibrating = false
    private var calibrationStarted = 0L
    private var calibrationSum = 0.0
    private var calibrationCount = 0
    private var sessionActive = false
    private var threshold = 2.0f
    private var lastTrigger = 0L
    private var aboveThresholdSamples = 0
    private var movementAt = 0L
    private var lastWord = ""

    private val cooldownMs = 8500L
    private val words = listOf(
        "OUI", "NON", "ICI", "LÀ", "PROCHE", "LOIN", "ATTENDS", "ÉCOUTE",
        "AIDE", "STOP", "PARTIR", "RESTE", "MAISON", "PORTE", "PIÈCE",
        "LUMIÈRE", "OMBRE", "FAMILLE", "AMI", "ENFANT", "HOMME", "FEMME",
        "QUI ?", "POURQUOI ?", "QUAND ?", "COMMENT ?", "MAINTENANT",
        "AVANT", "APRÈS", "BIENTÔT", "CALME", "PEUR", "FROID", "CHAUD",
        "DERRIÈRE", "DEVANT", "GAUCHE", "DROITE", "UN", "DEUX", "TROIS",
        "QUATRE", "CINQ", "MERCI", "BONJOUR", "AU REVOIR", "ENTENDS",
        "SOUVIENS", "NOM", "SILENCE"
    )
    private val history = mutableListOf<String>()

    private lateinit var statusLabel: TextView
    private lateinit var fieldLabel: TextView
    private lateinit var deltaLabel: TextView
    private lateinit var axisLabel: TextView
    private lateinit var wordLabel: TextView
    private lateinit var historyLabel: TextView
    private lateinit var calibrationButton: Button
    private lateinit var sessionButton: Button
    private lateinit var thresholdLabel: TextView
    private lateinit var sensorLabel: TextView
    private val ui = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        history.addAll(getSharedPreferences("echo_basic", MODE_PRIVATE)
            .getString("history", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList())
        buildInterface()
        updateReadings()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(30))
        }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        page.addView(label("E C H O", 27, white, true))
        page.addView(label("COMMUNICATION MAGNÉTIQUE", 11, cyan, true))
        page.addView(label("EXPÉRIENCE SENSORIELLE · HORS LIGNE", 10, muted))
        page.addView(space(18))

        val sensorCard = card()
        sensorCard.addView(label("CHAMP MAGNÉTIQUE", 11, cyan, true))
        fieldLabel = label("--.-- µT", 42, white, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
        }
        sensorCard.addView(fieldLabel)
        deltaLabel = label("Variation par rapport à la référence : -- µT", 13, muted).apply {
            gravity = Gravity.CENTER
        }
        sensorCard.addView(deltaLabel)
        axisLabel = label("X : --    Y : --    Z : --", 12, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
        }
        sensorCard.addView(axisLabel)
        page.addView(sensorCard)

        page.addView(space(12))
        statusLabel = label("Recherche des capteurs…", 12, orange, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = rounded(surfaceAlt, 12)
        }
        page.addView(statusLabel, matchWidth())

        page.addView(space(12))
        val calibrationCard = card()
        calibrationCard.addView(label("1 · CALIBRATION", 12, cyan, true))
        calibrationCard.addView(label(
            "Pose le téléphone sur une surface stable et lance la calibration. ECHO calcule une référence à partir de mesures réelles.",
            13, muted
        ))
        calibrationButton = actionButton("CALIBRER LE CAPTEUR") { startCalibration() }
        calibrationCard.addView(calibrationButton, matchWidth())
        page.addView(calibrationCard)

        page.addView(space(10))
        val talkCard = card()
        talkCard.addView(label("2 · SESSION DE COMMUNICATION", 12, cyan, true))
        wordLabel = label("—", 34, white, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(22))
        }
        talkCard.addView(wordLabel)
        talkCard.addView(label(
            "Un mot est choisi uniquement lorsqu'une variation magnétique mesurée dépasse le seuil pendant plusieurs échantillons.",
            12, muted
        ))
        sessionButton = actionButton("DÉMARRER LA SESSION") { toggleSession() }
        talkCard.addView(sessionButton, matchWidth())
        page.addView(talkCard)

        page.addView(space(10))
        val settingsCard = card()
        settingsCard.addView(label("SENSIBILITÉ", 12, cyan, true))
        thresholdLabel = label("Seuil de variation : %.1f µT".format(Locale.FRANCE, threshold), 13, white)
        settingsCard.addView(thresholdLabel)
        val seek = SeekBar(this).apply {
            max = 149
            progress = 10
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    threshold = (progress + 1) / 5f
                    thresholdLabel.text = "Seuil de variation : %.1f µT".format(Locale.FRANCE, threshold)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        settingsCard.addView(seek)
        settingsCard.addView(label(
            "Une sensibilité élevée déclenche moins souvent. Les appareils électriques, aimants et objets métalliques peuvent modifier le champ mesuré.",
            12, muted
        ))
        page.addView(settingsCard)

        page.addView(space(10))
        val infoCard = card()
        infoCard.addView(label("ÉTAT DES CAPTEURS", 12, cyan, true))
        sensorLabel = label("", 12, muted)
        infoCard.addView(sensorLabel)
        page.addView(infoCard)

        page.addView(space(10))
        val historyCard = card()
        historyCard.addView(label("JOURNAL DE SESSION", 12, cyan, true))
        historyLabel = label("", 12, muted)
        historyCard.addView(historyLabel)
        historyCard.addView(actionButton("EFFACER L'HISTORIQUE") {
            history.clear()
            persistHistory()
            updateHistory()
        }, matchWidth())
        page.addView(historyCard)

        page.addView(space(12))
        page.addView(label(
            "ECHO mesure des phénomènes physiques. Les variations et les mots affichés ne permettent pas d'établir la présence d'une entité ou d'un esprit.",
            11, muted
        ))
        setContentView(root)
    }

    private fun startCalibration() {
        if (magneticSensor == null) {
            Toast.makeText(this, "Ce téléphone ne possède pas de magnétomètre.", Toast.LENGTH_LONG).show()
            return
        }
        sessionActive = false
        calibrating = true
        calibrated = false
        calibrationStarted = System.currentTimeMillis()
        calibrationSum = 0.0
        calibrationCount = 0
        aboveThresholdSamples = 0
        sessionButton.text = "DÉMARRER LA SESSION"
        calibrationButton.text = "CALIBRATION EN COURS…"
        statusLabel.text = "Calibration : garde le téléphone immobile"
        statusLabel.setTextColor(orange)
    }

    private fun toggleSession() {
        if (sessionActive) {
            sessionActive = false
            sessionButton.text = "DÉMARRER LA SESSION"
            statusLabel.text = "Session arrêtée"
            statusLabel.setTextColor(muted)
            addHistory("Session arrêtée")
            return
        }
        if (magneticSensor == null) {
            Toast.makeText(this, "Magnétomètre indisponible sur cet appareil.", Toast.LENGTH_LONG).show()
            return
        }
        if (!calibrated) {
            Toast.makeText(this, "Effectue d'abord une calibration.", Toast.LENGTH_SHORT).show()
            return
        }
        sessionActive = true
        lastTrigger = System.currentTimeMillis()
        aboveThresholdSamples = 0
        sessionButton.text = "ARRÊTER LA SESSION"
        statusLabel.text = "SESSION ACTIVE · analyse des variations"
        statusLabel.setTextColor(green)
        addHistory("Session démarrée · référence %.2f µT".format(Locale.FRANCE, baseline))
    }

    private fun registerSensors() {
        listOfNotNull(magneticSensor, accelerometer, gyroscope).forEach {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onResume() {
        super.onResume()
        registerSensors()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        if (sessionActive) {
            sessionActive = false
            if (::sessionButton.isInitialized) sessionButton.text = "DÉMARRER LA SESSION"
            if (::statusLabel.isInitialized) {
                statusLabel.text = "Session mise en pause"
                statusLabel.setTextColor(muted)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetic = event.values.copyOf()
                fieldStrength = sqrt(
                    magnetic[0] * magnetic[0] +
                    magnetic[1] * magnetic[1] +
                    magnetic[2] * magnetic[2]
                )
                if (calibrating) collectCalibrationSample()
                if (calibrated) {
                    delta = abs(fieldStrength - baseline)
                    smoothedDelta = if (smoothedDelta == 0f) delta else smoothedDelta * 0.78f + delta * 0.22f
                    detectVariation()
                }
                updateReadings()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                acceleration = event.values.copyOf()
                val magnitude = sqrt(acceleration.sumOf { (it * it).toDouble() }).toFloat()
                if (abs(magnitude - SensorManager.GRAVITY_EARTH) > 0.75f) movementAt = System.currentTimeMillis()
            }
            Sensor.TYPE_GYROSCOPE -> {
                rotation = event.values.copyOf()
                val speed = sqrt(rotation.sumOf { (it * it).toDouble() }).toFloat()
                if (speed > 0.35f) movementAt = System.currentTimeMillis()
            }
        }
    }

    private fun collectCalibrationSample() {
        if (fieldStrength <= 0f) return
        calibrationSum += fieldStrength
        calibrationCount++
        val elapsed = System.currentTimeMillis() - calibrationStarted
        if (calibrationCount >= 35 || elapsed >= 4000L) {
            calibrating = false
            if (calibrationCount >= 8) {
                baseline = (calibrationSum / calibrationCount).toFloat()
                calibrated = true
                smoothedDelta = 0f
                calibrationButton.text = "RECALIBRER"
                statusLabel.text = "Référence calibrée · %.2f µT".format(Locale.FRANCE, baseline)
                statusLabel.setTextColor(green)
                addHistory("Calibration · %.2f µT (%d mesures)".format(Locale.FRANCE, baseline, calibrationCount))
            } else {
                statusLabel.text = "Calibration insuffisante · réessaie"
                statusLabel.setTextColor(orange)
            }
        } else {
            statusLabel.text = "Calibration · $calibrationCount mesures"
        }
    }

    private fun detectVariation() {
        if (!sessionActive || !calibrated) return
        val moving = System.currentTimeMillis() - movementAt < 900L
        if (moving) {
            aboveThresholdSamples = 0
            return
        }
        if (smoothedDelta >= threshold) {
            aboveThresholdSamples++
        } else {
            aboveThresholdSamples = 0
        }
        val now = System.currentTimeMillis()
        if (aboveThresholdSamples >= 5 && now - lastTrigger >= cooldownMs) {
            lastTrigger = now
            aboveThresholdSamples = 0
            val word = chooseWord()
            wordLabel.text = word
            addHistory("Mot : $word · variation %.2f µT · champ %.2f µT".format(
                Locale.FRANCE, delta, fieldStrength
            ))
            vibrate()
        }
    }

    private fun chooseWord(): String {
        val candidates = words.filter { it != lastWord }
        val selected = candidates[Random.nextInt(candidates.size)]
        lastWord = selected
        return selected
    }

    private fun updateReadings() {
        if (!::fieldLabel.isInitialized) return
        fieldLabel.text = if (magneticSensor == null) "N/D" else "%.2f µT".format(Locale.FRANCE, fieldStrength)
        deltaLabel.text = if (calibrated) {
            "Variation par rapport à la référence : %.2f µT".format(Locale.FRANCE, delta)
        } else {
            "Variation par rapport à la référence : non calibrée"
        }
        axisLabel.text = "X : %.1f    Y : %.1f    Z : %.1f µT".format(
            Locale.FRANCE, magnetic[0], magnetic[1], magnetic[2]
        )
        val available = buildList {
            add(if (magneticSensor != null) "Magnétomètre : disponible" else "Magnétomètre : absent")
            add(if (accelerometer != null) "Accéléromètre : disponible" else "Accéléromètre : absent")
            add(if (gyroscope != null) "Gyroscope : disponible" else "Gyroscope : absent")
        }
        if (::sensorLabel.isInitialized) sensorLabel.text = available.joinToString("\n")
    }

    private fun addHistory(message: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date())
        history.add(0, "$stamp  ·  $message")
        while (history.size > 60) history.removeAt(history.lastIndex)
        persistHistory()
        updateHistory()
    }

    private fun updateHistory() {
        if (::historyLabel.isInitialized) {
            historyLabel.text = if (history.isEmpty()) "Aucun événement pour le moment." else history.take(12).joinToString("\n\n")
        }
    }

    private fun persistHistory() {
        getSharedPreferences("echo_basic", MODE_PRIVATE).edit()
            .putString("history", history.joinToString("\n"))
            .apply()
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(120)
                }
            }
        } catch (_: Exception) { }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(surface, 18)
        elevation = dp(2).toFloat()
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(3), 0, dp(4))
        }

    private fun actionButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 12f
        isAllCaps = false
        setTextColor(white)
        background = rounded(Color.rgb(27, 65, 82), 12)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun matchWidth() = LinearLayout.LayoutParams(-1, -2)

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
