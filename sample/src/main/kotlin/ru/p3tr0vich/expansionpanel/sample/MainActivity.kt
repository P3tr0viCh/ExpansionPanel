package ru.p3tr0vich.expansionpanel.sample

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.TextView

import java.util.Random

import ru.p3tr0vich.widget.ExpansionPanel
import ru.p3tr0vich.widget.ExpansionPanelListenerAdapter

class MainActivity : AppCompatActivity() {

    private var mExpansionPanel: ExpansionPanel? = null
    private var mExpansionPanelDialog: ExpansionPanel? = null

    private var mValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mExpansionPanel = findViewById(R.id.expansion_panel)
        mExpansionPanelDialog = findViewById(R.id.expansion_panel_dialog)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val random = Random()

            (findViewById<View>(R.id.text_collapsed) as TextView).text = random.nextInt(Integer.MAX_VALUE).toString()
            (findViewById<View>(R.id.text_expanded) as TextView).text = random.nextInt(Integer.MAX_VALUE).toString()

            var s = random.nextInt().toString()
            if (random.nextBoolean()) s = s + '\n' + random.nextInt()
            if (random.nextBoolean()) s = s + '\n' + random.nextInt()
            if (random.nextBoolean()) s = s + '\n' + random.nextInt()

            (findViewById<View>(R.id.text_expanded_2) as TextView).text = s
        }

        findViewById<View>(R.id.btn_collapse).setOnClickListener { mExpansionPanel!!.collapse() }
        findViewById<View>(R.id.btn_expand).setOnClickListener { mExpansionPanel!!.expand() }
        findViewById<View>(R.id.btn_toggle).setOnClickListener { mExpansionPanel!!.toggle() }

        findViewById<View>(R.id.btn_cancel).setOnClickListener { mExpansionPanelDialog!!.collapse() }
        findViewById<View>(R.id.btn_save).setOnClickListener {
            setValue((findViewById<View>(R.id.edit_value_expanded) as EditText).text.toString())

            mExpansionPanelDialog!!.collapse()
        }

        findViewById<View>(R.id.switch_slow_mo).setOnClickListener { v ->
            with(mExpansionPanel!!) {
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

        val value: String? = if (savedInstanceState != null)
            savedInstanceState.getString("value")
        else
            Random().nextInt(Integer.MAX_VALUE).toString()

        setValue(value)

        mExpansionPanelDialog!!.setListener(object : ExpansionPanelListenerAdapter() {
            override fun onCollapsed(panel: ExpansionPanel) {
                setValue(mValue)
            }
        })
    }

    private fun setValue(value: String?) {
        mValue = value

        (findViewById<View>(R.id.text_value_collapsed) as TextView).text = if (TextUtils.isEmpty(value)) "empty" else value
        (findViewById<View>(R.id.edit_value_expanded) as EditText).setText(value)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("value", mValue)
    }
}
