/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 * adapted to My Expenses by Michael Totschnig:
 * - added support for small screen height
 * - retain state on orientation change
 ******************************************************************************/
package org.totschnig.myexpenses.activity;

import android.content.Intent;
import android.os.Bundle;
//import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Stack;

//TODO move to DialogFragment in order to have material styled ok and cancel buttons
public class CalculatorInput extends ProtectedFragmentActivity implements OnClickListener {
    public static final String EXTRA_KEY_INPUT_ID = "input_id";
    public static final BigDecimal HUNDRED = new BigDecimal(100);
    public static final int[] buttons = {R.id.b0, R.id.b1, R.id.b2, R.id.b3,
            R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.bAdd,
            R.id.bSubtract, R.id.bDivide, R.id.bMultiply, R.id.bPercent,
            R.id.bPlusMinus, R.id.bDot, R.id.bResult, R.id.bClear, R.id.bDelete};

    private static final BigDecimal nullValue = new BigDecimal(0);

    private TextView tvResult;
    private TextView tvOp;

    //private Vibrator vibrator;

    private Stack<String> stack = new Stack<String>();
    private String result = "0";
    private boolean isRestart = true;
    private boolean isInEquals = false;
    private char lastOp = '\0';

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyApplication.getThemeType().equals(MyApplication.ThemeType.dark)?
            R.style.Theme_AppCompat_Dialog_NoTitle : R.style.Theme_AppCompat_Light_Dialog_NoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calculator);

        //vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        for (int id : buttons) {
            Button b = (Button) findViewById(id);
            b.setOnClickListener(this);
            if (b.getTag()!=null) {
                char c = ((String) b.getTag()).charAt(0);
                if (Character.isDigit(c)) {
                    b.setText(String.format("%d",Character.getNumericValue(c)));
                }
                else if (c == '.') {
                    b.setText(String.valueOf(Utils.getDefaultDecimalSeparator()));
                }
            }
        }

        tvResult = (TextView) findViewById(R.id.result);
        setDisplay("0");
        tvOp = (TextView) findViewById(R.id.op);

        Button b = (Button) findViewById(R.id.bOK);

        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!isInEquals) {
                    doEqualsChar();
                }
                close();
            }
        });
        b = (Button) findViewById(R.id.bCancel);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
          BigDecimal amount = (BigDecimal) intent.getSerializableExtra(KEY_AMOUNT);
            if (amount != null) {
                setDisplay(amount.toPlainString());
            }
        }

    }

    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        char c = (v.getTag() == null ? b.getText() : ((String) b.getTag())).charAt(0);
        onButtonClick(c);
    }

    private void setDisplay(String s) {
        if (isNotEmpty(s)) {
            s = s.replaceAll(",", ".");
            result = s;
            tvResult.setText(localize(result));
        }
    }

    private String localize(String in) {
        StringBuilder out = new StringBuilder();
        for (char c : in.toCharArray()) {
            if (Character.isDigit(c)) {
                out.append(String.format("%d",Character.getNumericValue(c)));
            } else if (c == '.') {
                out.append(String.valueOf(Utils.getDefaultDecimalSeparator()));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static boolean isNotEmpty(String s) {
      return s != null && s.length() > 0;
}

    private void onButtonClick(char c) {
/*        if (vibrator != null) {
            vibrator.vibrate(20);
        }*/
        switch (c) {
            case 'C':
                resetAll();
                break;
            case '<':
                doBackspace();
                break;
            default:
                doButton(c);
                break;
        }
    }

    private void resetAll() {
        setDisplay("0");
        tvOp.setText("");
        lastOp = '\0';
        isRestart = true;
        stack.clear();
    }

    private void doBackspace() {
        String s = result;
        if ("0".equals(s) || isRestart) {
            return;
        }
        String newDisplay = s.length() > 1 ? s.substring(0, s.length() - 1) : "0";
        if ("-".equals(newDisplay)) {
            newDisplay = "0";
        }
        setDisplay(newDisplay);
    }

    private void doButton(char c) {
        if (Character.isDigit(c) || c == '.') {
            addChar(c);
        } else {
            switch (c) {
                case '+':
                case '-':
                case '/':
                case '*':
                    doOpChar(c);
                    break;
                case '%':
                    doPercentChar();
                    break;
                case '=':
                case '\r':
                    doEqualsChar();
                    break;
                case '\u00B1':
                    setDisplay(new BigDecimal(result).negate().toPlainString());
                    break;
            }
        }
    }

    private void addChar(char c) {
        String s = result;
        if (c == '.' && s.indexOf('.') != -1 && !isRestart) {
            return;
        }
        if (isRestart) {
            setDisplay(c == '.' ? "0." : String.valueOf(c));
            isRestart = false;
        } else {
          if ("0".equals(s) && c != '.') {
            s = String.valueOf(c);
          } else {
            s += c;
          }
          setDisplay(s);
        }
    }

    private void doOpChar(char op) {
        if (isInEquals) {
            stack.clear();
            isInEquals = false;
        }
        stack.push(result);
        doLastOp();
        lastOp = op;
        tvOp.setText(String.valueOf(lastOp));
    }

    private void doLastOp() {
        isRestart = true;
        if (lastOp == '\0' || stack.size() == 1) {
            return;
        }

        String valTwo = stack.pop();
        String valOne = stack.pop();
        switch (lastOp) {
            case '+':
                stack.push(new BigDecimal(valOne).add(new BigDecimal(valTwo)).toPlainString());
                break;
            case '-':
                stack.push(new BigDecimal(valOne).subtract(new BigDecimal(valTwo)).toPlainString());
                break;
            case '*':
                stack.push(new BigDecimal(valOne).multiply(new BigDecimal(valTwo)).toPlainString());
                break;
            case '/':
                BigDecimal d2 = new BigDecimal(valTwo);
                if (d2.compareTo(nullValue) == 0) {
                    stack.push("0.0");
                } else {
                    stack.push(new BigDecimal(valOne).divide(d2, MathContext.DECIMAL64).toPlainString());
                }
                break;
            default:
                break;
        }
        setDisplay(stack.peek());
        if (isInEquals) {
            stack.push(valTwo);
        }
    }

    private void doPercentChar() {
        if (stack.size() == 0)
            return;
        setDisplay(new BigDecimal(result).divide(HUNDRED).multiply(new BigDecimal(stack.peek())).toPlainString());
        tvOp.setText("");
    }

    private void doEqualsChar() {
        if (lastOp == '\0') {
            return;
        }
        if (!isInEquals) {
            isInEquals = true;
            stack.push(result);
        }
        doLastOp();
        tvOp.setText("");
    }

    private void close() {
        Intent data = new Intent();
        data.putExtra(KEY_AMOUNT, result);
        data.putExtra(EXTRA_KEY_INPUT_ID,getIntent().getIntExtra(EXTRA_KEY_INPUT_ID,0));
        setResult(RESULT_OK, data);
        finish();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putString("result", result);
      outState.putChar("lastOp", lastOp);
      outState.putBoolean("isInEquals", isInEquals);
      outState.putSerializable("stack", stack.toArray(new String[stack.size()]));
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      result = savedInstanceState.getString("result");
      lastOp = savedInstanceState.getChar("lastOp");
      isInEquals = savedInstanceState.getBoolean("isInEquals");
      stack = new Stack<String>();
      stack.addAll(Arrays.asList((String[])savedInstanceState.getSerializable("stack")));
      if (lastOp != '\0' && !isInEquals)
        tvOp.setText(String.valueOf(lastOp));
      setDisplay(result);
    }
}
