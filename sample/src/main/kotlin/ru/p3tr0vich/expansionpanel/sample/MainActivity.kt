package ru.p3tr0vich.expansionpanel.sample

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.TextView

import java.util.Random

import ru.p3tr0vich.widget.ExpansionPanel
import ru.p3tr0vich.widget.ExpansionPanelListenerAdapter

class MainActivity : AppCompatActivity() {

    private var dialogValue: String? = null
        set(value) {
            field = value

            findViewById<TextView>(R.id.text_value_collapsed).text =
                if (TextUtils.isEmpty(value)) "empty" else value

            findViewById<TextView>(R.id.edit_value_expanded).text = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        val expansionPanel = findViewById<ExpansionPanel>(R.id.expansion_panel)
        val expansionPanelDialog = findViewById<ExpansionPanel>(R.id.expansion_panel_dialog)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val random = Random()

            findViewById<TextView>(R.id.text_collapsed).text =
                random.nextInt(Integer.MAX_VALUE).toString()
            findViewById<TextView>(R.id.text_expanded).text =
                random.nextInt(Integer.MAX_VALUE).toString()

            var s = random.nextInt().toString()
            if (random.nextBoolean()) s += '\n' + random.nextInt().toString()
            if (random.nextBoolean()) s += '\n' + random.nextInt().toString()
            if (random.nextBoolean()) s += '\n' + random.nextInt().toString()

            findViewById<TextView>(R.id.text_expanded_2).text = s
        }

        findViewById<View>(R.id.btn_collapse).setOnClickListener { expansionPanel!!.collapse() }
        findViewById<View>(R.id.btn_expand).setOnClickListener { expansionPanel!!.expand() }
        findViewById<View>(R.id.btn_toggle).setOnClickListener { expansionPanel!!.toggle() }

        expansionPanelDialog.findViewById<View>(R.id.btn_cancel)
            .setOnClickListener { expansionPanelDialog.collapse() }
        expansionPanelDialog.findViewById<View>(R.id.btn_save)?.setOnClickListener {
            dialogValue = findViewById<EditText>(R.id.edit_value_expanded).text.toString()

            expansionPanelDialog.collapse()
        }

        findViewById<View>(R.id.switch_slow_mo).setOnClickListener { v ->
            with(expansionPanel) {
                if ((v as SwitchCompat).isChecked) {
                    durationToggle *= 5
                    delayCollapsedViewHiding *= 5
                    durationCollapsedViewChangeVisibility *= 5
                    durationContentHeightChanged *= 5
                } else {
                    durationToggle = -1
                    delayCollapsedViewHiding = -1
                    durationCollapsedViewChangeVisibility = -1
                    durationContentHeightChanged = -1
                }
            }
        }

        dialogValue = if (savedInstanceState != null)
            savedInstanceState.getString("value")
        else
            Random().nextInt(Integer.MAX_VALUE).toString()

        expansionPanelDialog.setListener(object : ExpansionPanelListenerAdapter() {
            override fun onCollapsed(panel: ExpansionPanel) {
                dialogValue = dialogValue
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("value", dialogValue)
    }
}
