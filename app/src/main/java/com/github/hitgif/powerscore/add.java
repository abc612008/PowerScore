package com.github.hitgif.powerscore;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * Created by 123 on 02/14/2016.
 */
public class add extends Activity{

    private Button bd;
    private Button b1;
    private Button b2;
    private Button b3;
    private Button b4;
    private Button b5;
    private Button b6;
    private Button b7;
    private Button b8;
    private Button b9;
    private Button b0;
    private String showmonth;
    private String showday;
    private String reason_giveback = "";
    private String members_giveback="";
    private TextView score;
    private ImageView drop;
    private ImageView bplus;
    private ImageView bminus;
    private ImageButton backspace;
    private RelativeLayout ind;
    private LinearLayout inputscore;
    private boolean isplus = true;
    private boolean isdrop = false;
    private boolean caninput = true;
    private boolean onlyzero = true;
    private boolean caninputpoint = true;
    private boolean memchoosed = false;
    private ToastCommom toastCommom;
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            new AlertDialogios(add.this).builder()
                    .setTitle("提示")
                    .setMsg("确认返回吗? 所做的修改将不能被保存 :(")
                    .setPositiveButton("确认", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent iB = new Intent();
                            String[] result=new String[3];
                            result[0]="NULL";
                            result[1]="NULL";
                            result[2]="NULL";
                            iB.putExtra("data", result);
                            iB.setClass(add.this, MainActivity.class);
                            add.this.setResult(2, iB);
                            add.this.finish();
                            overridePendingTransition(R.anim.slide_in_froml, R.anim.slide_out_fromr);
                        }
                    })
                    .setNegativeButton("取消", null).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Util.setTranslucent(this);
        setContentView(R.layout.add);
        bd = (Button)findViewById(R.id.button4);
        b1 = (Button)findViewById(R.id.b1);
        b2 = (Button)findViewById(R.id.b2);
        b3 = (Button)findViewById(R.id.b3);
        b4 = (Button)findViewById(R.id.b4);
        b5 = (Button)findViewById(R.id.b5);
        b6 = (Button)findViewById(R.id.b6);
        b7 = (Button)findViewById(R.id.b7);
        b8 = (Button)findViewById(R.id.b8);
        b9 = (Button)findViewById(R.id.b9);
        b0 = (Button)findViewById(R.id.b0);
        score = (TextView)findViewById(R.id.score);
        drop = (ImageView)findViewById(R.id.droppp);
        bplus = (ImageView) findViewById(R.id.bplus);
        bminus = (ImageView) findViewById(R.id.bminus);
        backspace = (ImageButton) findViewById(R.id.backspace);
        inputscore = (LinearLayout) findViewById(R.id.inputscore);
        ind = (RelativeLayout)findViewById(R.id.relativeLayout15);
        toastCommom = ToastCommom.createToastConfig();
        SimpleDateFormat sdf=new SimpleDateFormat("yy-MM-dd");
        String date=sdf.format(new java.util.Date());
        ((TextView)findViewById(R.id.showdate)).setText(date);
        ViewTreeObserver vto3 = backspace.getViewTreeObserver();
        ViewTreeObserver vto2 = b9.getViewTreeObserver();
        ViewTreeObserver vto = bd.getViewTreeObserver();

        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                b9.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bd.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                bd.getLayoutParams().width = b9.getWidth();
            }
        });

        vto3.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                backspace.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                backspace.getLayoutParams().width = b9.getWidth();
            }
        });

        bplus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bplus.setImageResource(R.drawable.plus_white);
                bminus.setImageResource(R.drawable.minus_blue);
                isplus = true;
            }
        });

        bminus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bplus.setImageResource(R.drawable.plus_blue);
                bminus.setImageResource(R.drawable.minus_white);
                isplus = false;
            }
        });
        findViewById(R.id.ok).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((Button) findViewById(R.id.ok)).setTextColor(Color.parseColor("#7fffffff"));
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((Button) findViewById(R.id.ok)).setTextColor(Color.parseColor("#ffffff"));
                }
                return false;
            }
        });

        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (memchoosed) {
                    if (score.getText().toString().equals("0")||score.getText().toString().equals("0.")){
                        new AlertDialogios(add.this).builder()
                                .setTitle("提示")
                                .setMsg("分数不能为零 :)")
                                .setNegativeButton("好的", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                    }
                                }).show();
                    }else {
                        if (reason_giveback.isEmpty()) {
                            new AlertDialogios(add.this).builder()
                                    .setTitle("提示")
                                    .setMsg("没有设置理由，要继续吗?")
                                    .setPositiveButton("确定", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            //传值
                                            Intent i = new Intent();
                                            String[] result = new String[3];
                                            result[0] = "(未设置)";
                                            result[1] = members_giveback;
                                            result[2] = (!isplus ? "-" : "") + (score.getText().toString());
                                            i.putExtra("data", result);
                                            i.setClass(add.this, MainActivity.class);
                                            add.this.setResult(2, i);
                                            showToast("添加成功 :)");
                                            add.this.finish();
                                            overridePendingTransition(R.anim.slide_in_froml, R.anim.slide_out_fromr);
                                        }
                                    })

                                    .setNegativeButton("取消", null).show();

                        }else {
                            Intent i = new Intent();
                            String[] result = new String[3];
                            result[0] = reason_giveback;
                            result[1] = members_giveback;
                            result[2] = (!isplus ? "-" : "") + (score.getText().toString());
                            i.putExtra("data", result);
                            i.setClass(add.this, MainActivity.class);
                            add.this.setResult(2, i);
                            showToast("添加成功 :)");
                            add.this.finish();
                            overridePendingTransition(R.anim.slide_in_froml, R.anim.slide_out_fromr);
                        }

                    }
                }else {
                    new AlertDialogios(add.this).builder()
                            .setTitle("提示")
                            .setMsg("请选择成员 :)")
                            .setNegativeButton("好的", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            }).show();
                }
            }
        });

        ((RelativeLayout)findViewById(R.id.chooseresson)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(add.this, setreason.class), 2);
            }
        });
        ((RelativeLayout)findViewById(R.id.choosemem)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(add.this, multiplychoosestudent.class), 3);
            }
        });

        ((RelativeLayout)findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialogios(add.this).builder()
                        .setTitle("提示")
                        .setMsg("确认返回吗? 所做的修改将不会被保存 :(")
                        .setPositiveButton("确认", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent iB = new Intent();
                                String[] result=new String[3];
                                result[0]="NULL";
                                result[1]="NULL";
                                result[2]="NULL";
                                iB.putExtra("data", result);
                                iB.setClass(add.this, MainActivity.class);
                                add.this.setResult(2, iB);
                                add.this.finish();
                                overridePendingTransition(R.anim.slide_in_froml, R.anim.slide_out_fromr);
                            }
                        })
                        .setNegativeButton("取消", null).show();
                   }
            });
        ///////////////////////////////////////////////////////小键盘begin

        drop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isdrop) {
                    inputscore.setVisibility(View.VISIBLE);

                    isdrop = false;
                    drop.setImageResource(R.drawable.dropn);
                    RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ((TextView)findViewById(R.id.textView12)).getHeight());
                    param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                    param.addRule(RelativeLayout.ABOVE, R.id.inputscore);
                    ind.setLayoutParams(param);
                } else {
                    inputscore.setVisibility(View.GONE);

                    isdrop = true;
                    drop.setImageResource(R.drawable.dropup);
                    RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ((TextView)findViewById(R.id.textView12)).getHeight());
                    param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
                    ind.setLayoutParams(param);
                }
            }
        });

        b1.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("1");
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("2");
            }
        });
        b3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("3");
            }
        });
        b4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("4");
            }
        });
        b5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                keyboardCallback("5");
            }
        });
        b6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("6");
            }
        });
        b7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("7");
            }
        });
        b8.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("8");
            }
        });
        b9.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyboardCallback("9");
            }
        });
        b0.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (caninput){
                    if (onlyzero){
                        score.setText("0");
                        onlyzero = true;
                        caninputpoint = true;
                    }else {
                        score.setText(score.getText() + "0");
                    }

                    if (score.getText().toString().contains(".")) {
                        if (score.getText().length() - 1 - score.getText().toString().indexOf(".") > 0) {
                            caninput = false;
                        }
                    }


                }
            }
        });
        bd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (caninput&&caninputpoint){
                    score.setText(score.getText() + ".");
                    onlyzero = false;
                    caninputpoint = false;
                }

            }
        });
        backspace.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (score.getText().length() == 1) {
                    score.setText("0");
                    onlyzero = true;
                    caninputpoint = true;
                }else {
                    String s = score.getText().toString();
                    s = s.substring(0,score.getText().length() - 1);
                    score.setText(s);
                    caninput = true;
                    if (!s.contains(".")){
                        caninputpoint = true;
                    }
                    if (s.matches("0")){
                        onlyzero = true;
                    }
                }

            }
        });
        ((RelativeLayout) findViewById(R.id.back)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((TextView) findViewById(R.id.textView9)).setTextColor(Color.parseColor("#7fffffff"));
                ((ImageView) findViewById(R.id.imageView6)).setImageResource(R.drawable.backdown);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) findViewById(R.id.textView9)).setTextColor(Color.parseColor("#ffffff"));
                    ((ImageView) findViewById(R.id.imageView6)).setImageResource(R.drawable.back);
                }
                return false;
            }
        });
        ///////////////////////////////////////////////////////小键盘end

    }
    private void showToast(String msg) {
        toastCommom.ToastShow(add.this, (ViewGroup) findViewById(R.id.toast_layout_root), msg);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(resultCode){
            case 3:
                ArrayList<String> result=data.getExtras().getStringArrayList("mem"); //得到新Activity关闭后返回的数据
                ArrayList<String> _class=new ArrayList<String>();

                if (result!=null){
                    //处理返回值
                    String s="";
                    for (String s1 : result) {
                        String[] s2 = s1.split("[|]");
                        String s3 = s2[1];
                        String s4 = s2[0];
                        s += s3+", ";
                        members_giveback+=s1+"|";
                    }
                    s = s.substring(0 , s.length()-2);

                    ((TextView) findViewById(R.id.textView11)).setText(s);
                    ((ImageView) findViewById(R.id.imageView29)).setBackgroundColor(Color.parseColor("#14a2d4"));
                    ((ImageView) findViewById(R.id.imageView30)).setBackgroundColor(Color.parseColor("#14a2d4"));
                    ((ImageView) findViewById(R.id.imageView31)).setBackgroundColor(Color.parseColor("#14a2d4"));
                    ((ImageView) findViewById(R.id.imageView32)).setBackgroundColor(Color.parseColor("#14a2d4"));
                    ((TextView) findViewById(R.id.textView16)).setTextColor(Color.parseColor("#14a2d4"));
                    ((TextView) findViewById(R.id.textView11)).setGravity(Gravity.LEFT);
                    ((TextView) findViewById(R.id.textView11)).setGravity(Gravity.TOP);
                    memchoosed = true;
                }
                break;
            case 2:
                String reason = data.getExtras().getString("reason"); //得到新Activity关闭后返回的数据
                if (!reason.matches("NULL")){
                    if (reason.matches("&&nothing")){
                        ((ImageView) findViewById(R.id.imageView19)).setBackgroundColor(Color.parseColor("#e1e1e1"));
                        ((ImageView) findViewById(R.id.imageView22)).setBackgroundColor(Color.parseColor("#e1e1e1"));
                        ((ImageView) findViewById(R.id.imageView23)).setBackgroundColor(Color.parseColor("#e1e1e1"));
                        ((ImageView) findViewById(R.id.imageView24)).setBackgroundColor(Color.parseColor("#e1e1e1"));
                        ((TextView) findViewById(R.id.textView21)).setTextColor(Color.parseColor("#000000"));
                    }else {
                        reason_giveback = reason;
                        ((ImageView) findViewById(R.id.imageView19)).setBackgroundColor(Color.parseColor("#14a2d4"));
                        ((ImageView) findViewById(R.id.imageView22)).setBackgroundColor(Color.parseColor("#14a2d4"));
                        ((ImageView) findViewById(R.id.imageView23)).setBackgroundColor(Color.parseColor("#14a2d4"));
                        ((ImageView) findViewById(R.id.imageView24)).setBackgroundColor(Color.parseColor("#14a2d4"));
                        ((TextView) findViewById(R.id.textView21)).setTextColor(Color.parseColor("#14a2d4"));
                    }
                }
                break;
        }
    }

    private void keyboardCallback(String num){
        if (caninput){
            if (onlyzero){
                score.setText(num);
                onlyzero = false;
            }else {
                score.setText(score.getText() + num);
            }

        }
        if (score.getText().toString().contains(".")) {
            if (score.getText().length() - 1 - score.getText().toString().indexOf(".") > 0) {
                caninput = false;
                onlyzero = false;
            }
        }
    }
}
