package org.teslasoft.assistant.settings

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import org.teslasoft.assistant.AboutActivity
import org.teslasoft.assistant.DebugActivity
import org.teslasoft.assistant.R
import org.teslasoft.assistant.fragments.ActivationPromptDialog
import org.teslasoft.assistant.fragments.ModelDialogFragment
import org.teslasoft.assistant.onboarding.ActivationActivity

class SettingsActivity : FragmentActivity() {

    private var btnChangeApi: LinearLayout? = null
    private var btnChangeAccount: LinearLayout? = null
    private var btnSetAssisant: LinearLayout? = null
    private var silenceSwitch: MaterialSwitch? = null
    private var btnClearChat: MaterialButton? = null
    private var btnDebugMenu: MaterialButton? = null
    private var dalleResolutions: MaterialButtonToggleGroup? = null
    private var btnModel: LinearLayout? = null
    private var btnPrompt: LinearLayout? = null
    private var btnAbout: LinearLayout? = null
    private var r256: MaterialButton? = null
    private var r512: MaterialButton? = null
    private var r1024: MaterialButton? = null
    private var promptDesc: TextView? = null
    private var modelDesc: TextView? = null
    private var btnClassicView: LinearLayout? = null
    private var btnBubblesView: LinearLayout? = null

    private var chatId = ""

    private var model = ""

    private var activationPrompt = ""

    private var modelChangedListener: ModelDialogFragment.StateChangesListener = object : ModelDialogFragment.StateChangesListener {
        override fun onSelected(name: String) {
            model = name
            setModel(name)
            modelDesc?.text = model
        }

        override fun onCanceled() {
            /* unused */
        }
    }

    private var promptChangedListener: ActivationPromptDialog.StateChangesListener = object : ActivationPromptDialog.StateChangesListener {
        override fun onEdit(prompt: String) {
            val settings: SharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

            activationPrompt = prompt
            val editor: Editor = settings.edit()

            editor.putString("prompt", prompt)
            editor.apply()

            if (activationPrompt != "") {
                promptDesc?.text = activationPrompt
            } else {
                promptDesc?.text = resources.getString(R.string.activation_prompt_set_message)
            }
        }

        override fun onCanceled() {
            /* unused */
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnChangeApi = findViewById(R.id.btn_manage_api)
        btnChangeAccount = findViewById(R.id.btn_manage_account)
        btnSetAssisant = findViewById(R.id.btn_manage_assistant)
        silenceSwitch = findViewById(R.id.silent_switch)
        btnClearChat = findViewById(R.id.btn_clear_chat)
        btnDebugMenu = findViewById(R.id.btn_debug_menu)
        btnModel = findViewById(R.id.btn_model)
        btnPrompt = findViewById(R.id.btn_prompt)
        promptDesc = findViewById(R.id.prompt_desc)
        modelDesc = findViewById(R.id.model_desc)
        btnAbout = findViewById(R.id.btn_about)
        btnClassicView = findViewById(R.id.btn_classic_chat)
        btnBubblesView = findViewById(R.id.btn_bubbles_chat)


        val settings: SharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        activationPrompt = settings.getString("prompt", "").toString()

        if (settings.getString("layout", "classic").toString() == "bubbles") {
            btnBubblesView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v2)
            btnClassicView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v3)
        } else {
            btnBubblesView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v3)
            btnClassicView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v2)
        }

        btnClassicView?.setOnClickListener {
            settings.edit().putString("layout", "classic").apply()
            btnBubblesView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v3)
            btnClassicView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v2)
        }

        btnBubblesView?.setOnClickListener {
            settings.edit().putString("layout", "bubbles").apply()
            btnBubblesView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v2)
            btnClassicView?.setBackgroundResource(R.drawable.btn_accent_tonal_selector_v3)
        }

        if (activationPrompt != "") {
            promptDesc?.text = activationPrompt
        } else {
            promptDesc?.text = resources.getString(R.string.activation_prompt_set_message)
        }

        dalleResolutions = findViewById(R.id.model_for)
        r256 = findViewById(R.id.r256)
        r512 = findViewById(R.id.r512)
        r1024 = findViewById(R.id.r1024)

        loadResolution()
        loadModel()
        initChatId()

        modelDesc?.text = model

        r256?.setOnClickListener { saveResolution("256x256") }
        r512?.setOnClickListener { saveResolution("512x512") }
        r1024?.setOnClickListener { saveResolution("1024x1024") }

        btnChangeApi?.setOnClickListener {
            startActivity(Intent(this, ActivationActivity::class.java))
            finish()
        }

        btnChangeAccount?.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse("https://platform.openai.com/account")
            startActivity(intent)
        }

        btnSetAssisant?.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        btnAbout?.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        btnClearChat?.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                .setTitle("Confirm")
                .setMessage("Are you sure? This action can not be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    run {
                        val sharedPreferences: SharedPreferences = getSharedPreferences("chat_$chatId", MODE_PRIVATE)
                        val editor: Editor = sharedPreferences.edit()
                        editor.putString("chat", "[]")
                        editor.apply()
                        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }

        btnModel?.setOnClickListener {
            val modelDialogFragment: ModelDialogFragment = ModelDialogFragment.newInstance(model)
            modelDialogFragment.setStateChangedListener(modelChangedListener)
            modelDialogFragment.show(supportFragmentManager.beginTransaction(), "ModelDialog")
        }

        btnPrompt?.setOnClickListener {
            val promptDialog: ActivationPromptDialog = ActivationPromptDialog.newInstance(activationPrompt)
            promptDialog.setStateChangedListener(promptChangedListener)
            promptDialog.show(supportFragmentManager.beginTransaction(), "PromptDialog")
        }

        btnDebugMenu?.setOnClickListener {
            startActivity(Intent(this, DebugActivity::class.java))
        }

        val silenceSettings: SharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val silenceMode = silenceSettings.getBoolean("silence_mode", false)

        silenceSwitch?.isChecked = silenceMode

        silenceSwitch?.setOnCheckedChangeListener { _, isChecked ->
            run {
                val editor: Editor = silenceSettings.edit()
                if (isChecked) {
                    editor.putBoolean("silence_mode", true)
                } else {
                    editor.putBoolean("silence_mode", false)
                }

                editor.apply()
            }
        }


    }

    private fun initChatId() {
        val extras: Bundle? = intent.extras

        if (extras != null) {
            chatId = extras.getString("chatId", "")

            if (chatId == "") {
                btnClearChat?.visibility = View.GONE
            }
        } else {
            btnClearChat?.visibility = View.GONE
        }
    }

    private fun setModel(model: String) {
        val settings: SharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val editor = settings.edit()

        editor.putString("model", model)
        editor.apply()
    }

    private fun loadModel() {
        val settings: SharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        model = settings.getString("model", "gpt-3.5-turbo").toString()
    }

    private fun loadResolution() {
        val settings: SharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        when (settings.getString("resolution", "512x512")) {
            "256x256" -> r256?.isChecked = true
            "512x512" -> r512?.isChecked = true
            "1024x1024" -> r1024?.isChecked = true
            else -> r512?.isChecked = true
        }
    }

    private fun saveResolution(r: String) {
        val settings: SharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("resolution", r)
        editor.apply()
    }
}