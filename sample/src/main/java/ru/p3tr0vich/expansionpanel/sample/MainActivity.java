package ru.p3tr0vich.expansionpanel.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Random;

import ru.p3tr0vich.widget.ExpansionPanel;
import ru.p3tr0vich.widget.ExpansionPanelListenerAdapter;

public class MainActivity extends AppCompatActivity {

    private ExpansionPanel mExpansionPanel;
    private ExpansionPanel mExpansionPanelDialog;

    private String mValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mExpansionPanel = (ExpansionPanel) findViewById(R.id.expansion_panel);
        mExpansionPanelDialog = (ExpansionPanel) findViewById(R.id.expansion_panel_dialog);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random random = new Random();

                ((TextView) findViewById(R.id.text_collapsed)).setText(String.valueOf(random.nextInt(Integer.MAX_VALUE)));
                ((TextView) findViewById(R.id.text_expanded)).setText(String.valueOf(random.nextInt(Integer.MAX_VALUE)));

                String s = String.valueOf(random.nextInt());
                if (random.nextBoolean()) s = s + '\n' + String.valueOf(random.nextInt());
                if (random.nextBoolean()) s = s + '\n' + String.valueOf(random.nextInt());
                if (random.nextBoolean()) s = s + '\n' + String.valueOf(random.nextInt());

                ((TextView) findViewById(R.id.text_expanded_2)).setText(s);
            }
        });

        findViewById(R.id.btn_collapse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpansionPanel.collapse();
            }
        });
        findViewById(R.id.btn_expand).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpansionPanel.expand();
            }
        });
        findViewById(R.id.btn_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpansionPanel.toggle();
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpansionPanelDialog.collapse();
            }
        });
        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setValue(((EditText) findViewById(R.id.edit_value_expanded)).getText().toString());

                mExpansionPanelDialog.collapse();
            }
        });
        findViewById(R.id.switch_slow_mo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((SwitchCompat) v).isChecked();

                mExpansionPanel.setDurationToggle(isChecked ?
                        mExpansionPanel.getDurationToggle() * 5 : -1);
                mExpansionPanel.setDelayCollapsedViewHiding(isChecked ?
                        mExpansionPanel.getDelayCollapsedViewHiding() * 5 : -1);
                mExpansionPanel.setDurationCollapsedViewChangeVisibility(isChecked ?
                        mExpansionPanel.getDurationCollapsedViewChangeVisibility() * 5 : -1);
                mExpansionPanel.setDurationContentHeightChanged(isChecked ?
                        mExpansionPanel.getDurationContentHeightChanged() * 5 : -1);
            }
        });

        String value;

        if (savedInstanceState != null)
            value = savedInstanceState.getString("value");
        else
            value = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));

        setValue(value);

        mExpansionPanelDialog.setListener(new ExpansionPanelListenerAdapter() {
            @Override
            public void onCollapsed(ExpansionPanel panel) {
                setValue(mValue);
            }
        });
    }

    private void setValue(String value) {
        mValue = value;

        ((TextView) findViewById(R.id.text_value_collapsed)).setText(
                TextUtils.isEmpty(value) ? "empty" : value);
        ((EditText) findViewById(R.id.edit_value_expanded)).setText(value);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("value", mValue);
    }
}
