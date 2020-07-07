package com.min60.flowers.ui.uni;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.min60.flowers.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;


public class MoneySpinner extends LinearLayout {
    private float initialValue = 0;
    private float maxValue = Float.MAX_VALUE;
    private float minValue = Float.MIN_VALUE;
    private float value;
    private float increment = 1;
    private MaterialDialog dialog;
    private boolean isManualChanged = false;

    DecimalFormat df = new DecimalFormat("€#.##");

    TextView tvValue;
    OnValueChangedListener listener;

    private void closeDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public void setListener(OnValueChangedListener listener) {
        this.listener = listener;
    }

    public MoneySpinner(Context context) {
        super(context);
        init(null, 0);

    }

    public MoneySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public MoneySpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        df.setMinimumFractionDigits(2);
        df.setMinimumIntegerDigits(1);
        df.setMaximumFractionDigits(2);
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(getLocale(getContext())));
        final TypedArray taParams = getContext().obtainStyledAttributes(
                attrs, R.styleable.MoneySpinner, defStyle, 0);

        minValue = taParams.getFloat(R.styleable.MoneySpinner_minFloatValue, minValue);
        initialValue = taParams.getFloat(R.styleable.MoneySpinner_initialFloatValue, 1);
        value = initialValue;
        increment = taParams.getFloat(R.styleable.MoneySpinner_incremental, 1);
        maxValue = taParams.getFloat(R.styleable.MoneySpinner_maxFloatValue, maxValue);

        taParams.recycle();

        inflate(getContext(), R.layout.number_spinner, this);

        tvValue = findViewById(R.id.tvValue);
        tvValue.setEnabled(isEnabled());
        tvValue.setOnClickListener(view -> {
            dialog = new MaterialDialog.Builder(getContext())
                    .title(getContext().getString(R.string.money_amount))
                    .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                    .input(getContext().getString(R.string.single_price), "" + value, false,
                            (dialog1, input) -> {

                            })
                    .positiveText(R.string._save)
                    .negativeText(R.string._cancel)
                    .onPositive((dialog12, which) -> {
                        try {
                            isManualChanged = true;
                            setValue(Float.parseFloat(dialog12.getInputEditText().getText().toString()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    })

                    .show();

        });
        findViewById(R.id.btnPlus).setOnClickListener(view -> {
            if (value <= maxValue - increment) {
                isManualChanged = true;
                value += increment;
                showValue();
            }
        });
        findViewById(R.id.btnPlus).setEnabled(isEnabled());
        findViewById(R.id.btnMinus).setOnClickListener(view -> {
            if (value >= minValue + increment) {
                isManualChanged = true;
                value -= increment;
                showValue();
            }
        });
        findViewById(R.id.btnMinus).setEnabled(isEnabled());
        showValue();
    }

    public boolean isManualChanged() {
        return isManualChanged;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        findViewById(R.id.btnMinus).setVisibility(enabled ? View.VISIBLE : INVISIBLE);
        findViewById(R.id.btnPlus).setVisibility(enabled ? View.VISIBLE : INVISIBLE);
        tvValue.setEnabled(enabled);

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeDialog();
    }

    private void showValue() {
        tvValue.setText(df.format(value));
        if (listener != null) listener.onChange(value);
    }


    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
        showValue();
    }

    public interface OnValueChangedListener {
        void onChange(float value);
    }

    private Locale getLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }
}
