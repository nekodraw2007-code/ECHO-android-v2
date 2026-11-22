package com.echo.paranormal

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.*
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), SensorEventListener {
    private val bg = Color.rgb(7, 10, 18)
    private val panel = Color.rgb(17, 25, 39)
    private val accent = Color.rgb(91, 220, 235)
    private val ink = Color.rgb(241, 246, 252)
    private val dim = Color.rgb(145, 161, 182)
    private val green = Color.rgb(89, 220, 164)
    private lateinit var sm: SensorManager
    private var mag: Sensor? = null
    private var acc: Sensor? = null
    private var gyro: Sensor? = null
    private var linear: Sensor? = null
    private var mode = 0 // 0 auto, 1 motion, 2 magnetic
    private var running = false
    private var calibrating = false
    private var calibrated = false
    private var threshold = 2.0f
    private var cooldown = 9
    private var baseMag = 0f
    private var baseMotion = 0f
    private var magnetic = 0f
    private var acceleration = 0f
    private var rotation = 0f
    private var motion = 0f
    private var smooth = 0f
    private var samples = 0
    private var total = 0.0
    private var calibrationStart = 0L
    private var lastWord = ""
    private var lastEvent = 0L
    private var consecutive = 0
    private val history = mutableListOf<String>()
    private val enabled = mutableSetOf<String>()
    private lateinit var status: TextView
    private lateinit var measure: TextView
    private lateinit var word: TextView
    private lateinit var sensorsText: TextView
    private lateinit var logText: TextView
    private lateinit var start: Button
    private lateinit var modeButton: Button
    private lateinit var categoryButton: Button
    private lateinit var sensitivityText: TextView
    private lateinit var delayText: TextView

    private val dictionary = linkedMapOf(
        "Réponses" to listOf("OUI","NON","PEUT-ÊTRE","JE NE SAIS PAS","POURQUOI","COMMENT","QUI","QUAND","OÙ","MERCI","PARDON","ÉCOUTE","PARLE","RÉPÈTE","ATTENDS","STOP"),
        "Personnes" to listOf("MÈRE","PÈRE","ENFANT","FAMILLE","AMI","SŒUR","FRÈRE","FEMME","HOMME","GRAND-MÈRE","GRAND-PÈRE","INCONNU","VISITEUR","PRÉNOM","NOM"),
        "Émotions" to listOf("AMOUR","PEUR","JOIE","TRISTESSE","COLÈRE","CALME","SOLITUDE","PARDON","REGRET","BONHEUR","INQUIÉTUDE","CONFIANCE","DOULEUR","PAIX","MANQUE","ESPOIR"),
        "Lieux" to listOf("MAISON","CHAMBRE","CUISINE","JARDIN","PORTE","FENÊTRE","GRENIER","CAVE","DEHORS","DEDANS","ICI","LÀ-BAS","PRÈS","LOIN","ÉCOLE","ÉGLISE"),
        "Temps et souvenirs" to listOf("HIER","AUJOURD'HUI","DEMAIN","AVANT","APRÈS","MAINTENANT","BIENTÔT","LONGTEMPS","ENFANCE","SOUVENIR","DATE","ANNÉE","MATIN","SOIR","NUIT","JOUR"),
        "Objets" to listOf("PHOTO","LIVRE","TABLE","CHAISE","LIT","MONTRE","BIJOU","CLÉ","TÉLÉPHONE","MIROIR","LAMPE","BOÎTE","LETTRE","VÊTEMENT","JOUET","MUSIQUE"),
        "Sensations" to listOf("FROID","CHAUD","LUMIÈRE","OMBRE","BRUIT","SILENCE","ODEUR","VENT","PRESSION","MOUVEMENT","VIBRATION","PROCHE","DERRIÈRE","DEVANT"),
        "Actions" to listOf("VIENS","RESTE","PARTIR","REVIENS","AIDE","SUIVRE","OUVRIR","FERMER","MONTRER","ATTENDRE","AVANCER","RECULER","REGARDER","ENTENDRE","DORMIR","RÉVEILLER"),
        "Nombres" to listOf("UN","DEUX","TROIS","QUATRE","CINQ","SIX","SEPT","HUIT","NEUF","DIX","BEAUCOUP","PEU","PREMIER","DERNIER")
    )
    private val categoryNames get() = dictionary.keys.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val prefs = getSharedPreferences("echos", MODE_PRIVATE)
        threshold = prefs.getFloat("threshold", 2f)
        cooldown = prefs.getInt("cooldown", 9)
        history.addAll(prefs.getString("history", "")!!.split("\n").filter { it.isNotBlank() }.take(60))
        enabled.addAll(categoryNames)
        createUi()
        updateSensors()
        refreshLog()
    }

    private fun createUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(28)) }
        scroll.addView(page)
        page.addView(label("E C H O S", 28, ink, true))
        page.addView(label("EXPÉRIENCE SENSORIELLE · MODE HORS LIGNE", 11, accent, true))
        page.addView(gap(18))
        val meter = card()
        meter.addView(label("ACTIVITÉ DES CAPTEURS", 11, accent, true))
        measure = label("EN ATTENTE", 28, ink, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(8)) }
        meter.addView(measure)
        meter.addView(label("Les valeurs affichées proviennent des capteurs de l'appareil.", 12, dim))
        page.addView(meter)
        page.addView(gap(10))
        status = label("Initialisation…", 12, green, true).apply { gravity = Gravity.CENTER; background = shape(panel, 12); setPadding(dp(10), dp(12), dp(10), dp(12)) }
        page.addView(status, full())
        page.addView(gap(10))
        val session = card()
        session.addView(label("MOT SÉLECTIONNÉ", 11, accent, true))
        word = label("—", 40, ink, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(22), 0, dp(22)) }
        session.addView(word)
        session.addView(label("La sélection est algorithmique : elle ne prouve pas une communication avec un défunt.", 12, dim))
        start = actionButton("DÉMARRER LA SESSION") { toggle() }
        session.addView(start, full())
        page.addView(session)
        page.addView(gap(10))
        val calibration = card()
        calibration.addView(label("CALIBRATION", 12, accent, true))
        calibration.addView(label("Pose l'appareil immobile pendant quelques secondes pour établir une référence.", 12, dim))
        calibration.addView(actionButton("CALIBRER") { calibrate() }, full())
        page.addView(calibration)
        page.addView(gap(10))
        val settings = card()
        settings.addView(label("RÉGLAGES", 12, accent, true))
        modeButton = actionButton(modeName()) { chooseMode() }
        settings.addView(modeButton, full())
        categoryButton = actionButton("Toutes les catégories") { chooseCategories() }
        settings.addView(categoryButton, full())
        sensitivityText = label("", 12, ink)
        settings.addView(sensitivityText)
        settings.addView(SeekBar(this).apply {
            max = 98; progress = ((threshold - 0.2f) * 10).toInt().coerceIn(0, 98)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                    threshold = 0.2f + p / 10f
                    sensitivityText.text = "Sensibilité : %.1f".format(Locale.FRANCE, threshold)
                    getSharedPreferences("echos", MODE_PRIVATE).edit().putFloat("threshold", threshold).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        delayText = label("", 12, ink)
        settings.addView(delayText)
        settings.addView(SeekBar(this).apply {
            max = 29; progress = cooldown - 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                    cooldown = p + 1
                    delayText.text = "Délai entre les mots : " + cooldown + " s"
                    getSharedPreferences("echos", MODE_PRIVATE).edit().putInt("cooldown", cooldown).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        sensitivityText.text = "Sensibilité : %.1f".format(Locale.FRANCE, threshold)
        delayText.text = "Délai entre les mots : " + cooldown + " s"
        page.addView(settings)
        page.addView(gap(10))
        val hardware = card()
        hardware.addView(label("CAPTEURS DISPONIBLES", 12, accent, true))
        sensorsText = label("", 12, dim)
        hardware.addView(sensorsText)
        page.addView(hardware)
        page.addView(gap(10))
        val journal = card()
        journal.addView(label("JOURNAL DE SESSION", 12, accent, true))
        logText = label("Aucun événement.", 12, dim)
        journal.addView(logText)
        journal.addView(actionButton("EFFACER L'HISTORIQUE") {
            history.clear(); saveHistory(); refreshLog()
        }, full())
        page.addView(journal)
        page.addView(gap(14))
        page.addView(label("Les capteurs mesurent des phénomènes physiques ordinaires. Les mots affichés sont des associations produites par le logiciel et ne constituent pas des messages vérifiés.", 11, dim))
        setContentView(scroll)
    }

    private fun modeName() = when (mode) {
        1 -> "Mode mouvement (accéléromètre + gyroscope)"
        2 -> "Magnétomètre uniquement"
        else -> "Automatique (magnétomètre si présent)"
    }
    private fun actualMode() = if (mode == 1) 1 else if (mode == 2) 2 else if (mag != null) 2 else 1
    private fun hasSensor() = if (actualMode() == 2) mag != null else acc != null || gyro != null || linear != null

    private fun chooseMode() {
        val choices = arrayOf("Automatique", "Mouvement (accéléromètre + gyroscope)", "Magnétomètre uniquement")
        AlertDialog.Builder(this).setTitle("Mode de détection")
            .setSingleChoiceItems(choices, mode) { dialog, which ->
                mode = which; calibrated = false; modeButton.text = modeName(); updateSensors(); dialog.dismiss()
            }.setNegativeButton("Annuler", null).show()
    }

    private fun chooseCategories() {
        val boxes = categoryNames.map { name -> CheckBox(this).apply {
            text = name; setTextColor(ink); isChecked = name in enabled; setPadding(dp(16), dp(7), dp(16), dp(7))
        } }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; boxes.forEach { addView(it) } }
        AlertDialog.Builder(this).setTitle("Catégories actives")
            .setView(ScrollView(this).apply { addView(list) })
            .setPositiveButton("Appliquer") { _, _ ->
                enabled.clear(); boxes.filter { it.isChecked }.forEach { enabled.add(it.text.toString()) }
                if (enabled.isEmpty()) enabled.addAll(categoryNames)
                categoryButton.text = if (enabled.size == categoryNames.size) "Toutes les catégories" else enabled.size.toString() + " catégories actives"
            }.setNegativeButton("Annuler", null).show()
    }

    private fun calibrate() {
        if (!hasSensor()) { toast("Aucun capteur disponible dans ce mode."); return }
        running = false; start.text = "DÉMARRER LA SESSION"
        calibrating = true; calibrated = false; samples = 0; total = 0.0
        calibrationStart = System.currentTimeMillis(); smooth = 0f; consecutive = 0
        status.text = "Calibration en cours · garde l'appareil immobile"
        status.setTextColor(Color.rgb(255, 190, 100))
    }

    private fun toggle() {
        if (running) {
            running = false; start.text = "DÉMARRER LA SESSION"; status.text = "Session arrêtée"; status.setTextColor(dim)
            addLog("Session arrêtée")
        } else if (!hasSensor()) toast("Aucun capteur compatible avec ce mode.")
        else if (!calibrated) toast("Effectue d'abord une calibration.")
        else {
            running = true; start.text = "ARRÊTER LA SESSION"; lastEvent = System.currentTimeMillis()
            status.text = "SESSION ACTIVE · analyse des mesures"; status.setTextColor(green); addLog("Session démarrée · " + modeName())
        }
    }

    override fun onResume() {
        super.onResume()
        listOfNotNull(mag, acc, gyro, linear).distinctBy { it.type }.forEach { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        updateSensors()
    }
    override fun onPause() {
        super.onPause(); sm.unregisterListener(this)
        if (running) { running = false; if (::start.isInitialized) start.text = "DÉMARRER LA SESSION" }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val v = event.values
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetic = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
                if (actualMode() == 2) reading(abs(magnetic - baseMag), magnetic)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                acceleration = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
                if (actualMode() == 1) motionReading()
            }
            Sensor.TYPE_ACCELEROMETER -> if (linear == null) {
                acceleration = abs(sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]) - SensorManager.GRAVITY_EARTH)
                if (actualMode() == 1) motionReading()
            }
            Sensor.TYPE_GYROSCOPE -> {
                rotation = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
                if (actualMode() == 1) motionReading()
            }
        }
        if (::measure.isInitialized) measure.text = if (actualMode() == 2) "%.1f µT".format(Locale.FRANCE, magnetic) else "MOUVEMENT"
    }

    private fun motionReading() {
        motion = acceleration * 0.65f + rotation * 1.8f
        reading(abs(motion - baseMotion), motion)
    }

    private fun reading(delta: Float, raw: Float) {
        if (calibrating) {
            if (raw.isFinite()) { total += raw.toDouble(); samples++ }
            if (samples >= 45 || System.currentTimeMillis() - calibrationStart > 5000L) {
                calibrating = false
                if (samples >= 10) {
                    val baseline = (total / samples).toFloat()
                    if (actualMode() == 2) baseMag = baseline else baseMotion = baseline
                    calibrated = true; status.text = "Calibration terminée · " + samples + " mesures"; status.setTextColor(green)
                    addLog("Calibration terminée")
                } else { status.text = "Mesures insuffisantes · recommence"; status.setTextColor(Color.YELLOW) }
            } else status.text = "Calibration · " + samples + " mesures"
            return
        }
        if (!calibrated) return
        smooth = if (smooth == 0f) delta else smooth * 0.78f + delta * 0.22f
        if (!running) return
        consecutive = if (smooth >= threshold) consecutive + 1 else 0
        val now = System.currentTimeMillis()
        if (consecutive >= 5 && now - lastEvent >= cooldown * 1000L) {
            consecutive = 0; lastEvent = now
            val next = chooseWord()
            word.text = next
            addLog("Mot : " + next + " · variation " + "%.2f".format(Locale.FRANCE, smooth))
            try {
                val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (vib.hasVibrator()) vib.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: Exception) {}
        }
    }

    private fun chooseWord(): String {
        val pool = dictionary.filterKeys { it in enabled }.values.flatten().filter { it != lastWord }
        val options = if (pool.isNotEmpty()) pool else dictionary.values.flatten()
        return options[Random.nextInt(options.size)].also { lastWord = it }
    }

    private fun updateSensors() {
        if (!::sensorsText.isInitialized) return
        sensorsText.text = "Magnétomètre : " + if (mag != null) "disponible" else "absent"
        sensorsText.append("\nAccéléromètre : " + if (acc != null || linear != null) "disponible" else "absent")
        sensorsText.append("\nGyroscope : " + if (gyro != null) "disponible" else "absent")
        sensorsText.append("\nMode actif : " + modeName())
        status.text = when {
            !hasSensor() -> "Aucun capteur compatible avec ce mode"
            actualMode() == 1 -> "Mode mouvement prêt · calibration nécessaire"
            else -> "Magnétomètre disponible · calibration nécessaire"
        }
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRANCE).format(Date())
        history.add(0, time + " · " + message)
        while (history.size > 60) history.removeAt(history.lastIndex)
        saveHistory(); refreshLog()
    }
    private fun saveHistory() = getSharedPreferences("echos", MODE_PRIVATE).edit().putString("history", history.joinToString("\n")).apply()
    private fun refreshLog() { if (::logText.isInitialized) logText.text = if (history.isEmpty()) "Aucun événement." else history.take(15).joinToString("\n\n") }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15)); background = shape(panel, 18) }
    private fun label(s: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size.toFloat(); setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(3), 0, dp(4))
    }
    private fun actionButton(s: String, fn: () -> Unit) = Button(this).apply {
        text = s; textSize = 12f; isAllCaps = false; setTextColor(ink); background = shape(Color.rgb(28, 65, 82), 12)
        setOnClickListener { fn() }; layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) }
    }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun gap(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun full() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
