package com.github.hitgif.powerscore;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

public class MainActivity extends Activity implements AbsListView.OnScrollListener {
    public static MainActivity MainActivityPointer;
    public static TreeMap<String, ClassData> classes = new TreeMap<String, ClassData>();
    public static ArrayList<Group> groups = new ArrayList<Group>();
    SharedPreferences spReader;
    SharedPreferences.Editor spEditor;

    private long timeStamp = 0;
    private boolean superFlag = true;
    public boolean isSync = false;

    private String classNow = "-1";
    private String d = "不限";
    private String showYear;
    private String showMonth;
    private String showDay;
    private String filterClass = "";
    private String filterName = "";
    private Boolean isGen = true;
    private Boolean isOnAdd = false;
    private int scoreFilter = -1;
    private int countLimit = 40;
    private boolean needLoadMore;
    private int countNow;
    private int countMax;

    //布局
    private ListView lv;
    private int sbar = 0;
    private RelativeLayout leftLayout;
    private RelativeLayout rightLayout;
    private ImageView sync;

    private Animation in_per;
    private Animation round;

    private boolean pdLoading = false;

    Button gen;
    Button per;
    private boolean lastTip = false;
    private boolean showednomore = false;
    private ToastCommom toastCommom;
    private static final Object synclock = new Object();

    private void doSync() {
        if (isSync) {
            new AlertDialogios(MainActivity.this).builder()
                    .setTitle("提示")
                    .setMsg("抱歉，数据同步中，请等待同步完成 :(")
                    .setNegativeButton("好的", null).show();
            return;
        }
        isSync = true;
        sync.setAnimation(round);

        getClassInfo(false);
        countMax=classes.size();
        countNow=0;

        for (final String key : classes.keySet()) {
            final ClassData c = classes.get(key);
            //分班级同步
            String username = spReader.getString("username", "");
            String password = spReader.getString("password", "");

            new Thread(new AccessNetwork(true,
                    "http://powerscore.duapp.com/api/sync.php",
                    "username=" + username + "&password=" + password + "&cid=" + key + "&diff=" + exportHistories(c.unsyncedHistories).toString(), new Handler() {
                @Override
                public void handleMessage(Message msg) {

                    char data=msg.obj.toString().isEmpty()?'N':msg.obj.toString().substring(1).charAt(0);
                    switch(data) {
                        case 'P':
                            //无权限或已被删除的班级
                            //同时删除本地的班级数据
                            classes.remove(key);
                            deleteFile(key + ".dat");
                            synchronized (synclock) {
                                countMax--;
                            }
                            break;
                        case 'F':
                        case 'N': break;
                        default:
                            readData(String.valueOf(data),c);
                            break;

                    }
                    synchronized (synclock) {
                        countNow++;
                    }

                    if(countNow==countMax){
                        switch (data) {
                            case 'F':
                                showToast("登陆失败，用户名或密码错误");
                                break;
                            case 'N':
                                showToast("网络异常，请检查网络连接");
                                break;
                            default:
                                showToast("同步成功");
                                break;
                        }
                        isSync = false;
                        sync.clearAnimation();
                        updateList();
                    }
                }
            })).start();

            //同步结束

            c.unsyncedHistories.clear();
        }
    }

    private void updateList() {
        ((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
    }

    public void jumpToStudent(String classID, String studentName) {
        filterClass = classID;
        filterName = studentName;
        ((TextView) findViewById(R.id._name)).setText(filterName);
        per.setTextColor(Color.parseColor("#ffffff"));
        gen.setTextColor(Color.parseColor("#7fffffff"));
        ((TextView) findViewById(R.id.year)).setText("");
        ((TextView) findViewById(R.id.day)).setText("");
        ((TextView) findViewById(R.id.textView5)).setText("");
        ((TextView) findViewById(R.id.textView7)).setText("");
        findViewById(R.id.bar).startAnimation(in_per);
        findViewById(R.id.pcr).setVisibility(View.GONE);
        findViewById(R.id.pnm).setVisibility(View.VISIBLE);
        isGen = false;
        ((DrawerLayout) findViewById(R.id.drawerlayout)).closeDrawer(leftLayout);

        updateList();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivityPointer = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            sbar = getResources().getDimensionPixelSize(Integer.parseInt(c.getField("status_bar_height")
                    .get(c.newInstance()).toString()));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        Util.setTranslucent(this);
        RelativeLayout genLayout;
        ImageView add;
        //初始化
        spReader = getSharedPreferences("data", Activity.MODE_PRIVATE);
        spEditor = spReader.edit();
        try {
            new UpdateManager(this).check();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //布局初始化
        setContentView(R.layout.activity_main);
        boolean splash = getSharedPreferences("data", 0).getBoolean("splash", true);
        if (splash) {
            findViewById(R.id.onspl).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.onspl).setVisibility(View.GONE);
        }
        ((TextView) findViewById(R.id.realname)).setText(spReader.getString("username", "未登录"));
        gen = (Button) findViewById(R.id.gen);
        per = (Button) findViewById(R.id.per);
        genLayout = (RelativeLayout) findViewById(R.id.genlayout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.LayoutParams lp = findViewById(R.id.ds).getLayoutParams();
            lp.width = 1;
            lp.height = sbar;
            findViewById(R.id.ds).setLayoutParams(lp);
            findViewById(R.id.ds2).setLayoutParams(lp);
        }

        add = (ImageView) findViewById(R.id.add);
        sync = (ImageView) findViewById(R.id.sync);
        final Animation operatingAnim = AnimationUtils.loadAnimation(this, R.anim.tip);
        final Animation out_gen = AnimationUtils.loadAnimation(this, R.anim.personal_out);
        final Animation add_rotate_out = AnimationUtils.loadAnimation(this, R.anim.add_rotate_out);
        final Animation add_rotate_in = AnimationUtils.loadAnimation(this, R.anim.add_rotate_in);
        final Animation inputbtn_out = AnimationUtils.loadAnimation(this, R.anim.inputbtn_out);
        final Animation inputtex_out = AnimationUtils.loadAnimation(this, R.anim.inputtex_out);
        final Animation inputbtn_in = AnimationUtils.loadAnimation(this, R.anim.inputbtn_in);
        final Animation inputtex_in = AnimationUtils.loadAnimation(this, R.anim.inputtex_in);
        final Animation prelayout_out = AnimationUtils.loadAnimation(this, R.anim.perlayout_out);
        final Animation prelayout_in = AnimationUtils.loadAnimation(this, R.anim.perlayout_in);
        in_per = AnimationUtils.loadAnimation(this, R.anim.personal_in);
        round = AnimationUtils.loadAnimation(this, R.anim.tip);
        per.setTextColor(Color.parseColor("#7fffffff"));
        toastCommom = ToastCommom.createToastConfig();
        in_per.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                findViewById(R.id.lpd).setVisibility(View.GONE);
                findViewById(R.id.lpd2).setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        genLayout.setVisibility(View.VISIBLE);
        leftLayout = (RelativeLayout) findViewById(R.id.left);
        rightLayout = (RelativeLayout) findViewById(R.id.right);
        ((DrawerLayout) findViewById(R.id.drawerlayout)).setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.END);
        ((DrawerLayout) findViewById(R.id.drawerlayout)).setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                ((DrawerLayout) findViewById(R.id.drawerlayout)).setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.END);
                findViewById(R.id.button3).setVisibility(View.VISIBLE);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                ((DrawerLayout) findViewById(R.id.drawerlayout)).setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.END);
                findViewById(R.id.button3).setVisibility(View.GONE);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        findViewById(R.id.home).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse("http://powerscore.duapp.com/login.php"));
                it.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
                startActivity(it);
            }
        });

        sync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSync) {
                    //开始转
                    sync.startAnimation(operatingAnim);
                    doSync();
                }
            }
        });
        gen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!isGen) {
                    findViewById(R.id.bar).startAnimation(out_gen);
                }
                gen.setTextColor(Color.parseColor("#ffffff"));
                per.setTextColor(Color.parseColor("#7fffffff"));
                findViewById(R.id.lpd).setVisibility(View.VISIBLE);
                findViewById(R.id.lpd2).setVisibility(View.GONE);
                findViewById(R.id.pnm).setVisibility(View.GONE);
                findViewById(R.id.pcr).setVisibility(View.VISIBLE);
                isGen = true;
                countLimit = 40;
                updateList();
            }
        });

        per.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                per.setTextColor(Color.parseColor("#ffffff"));
                gen.setTextColor(Color.parseColor("#7fffffff"));
                ((TextView) findViewById(R.id.year)).setText("");
                ((TextView) findViewById(R.id.day)).setText("");
                ((TextView) findViewById(R.id.textView5)).setText("");
                ((TextView) findViewById(R.id.textView7)).setText("");
                findViewById(R.id.bar).startAnimation(in_per);
                findViewById(R.id.pcr).setVisibility(View.GONE);
                findViewById(R.id.pnm).setVisibility(View.VISIBLE);
                updateList();
                isGen = false;
            }
        });

        findViewById(R.id.personal).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((DrawerLayout) findViewById(R.id.drawerlayout)).openDrawer(leftLayout);
            }
        });
        findViewById(R.id.flit).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DrawerLayout) findViewById(R.id.drawerlayout)).openDrawer(rightLayout);
            }
        });

        findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialogios(MainActivity.this).builder()
                        .setTitle("退出登录")
                        .setMsg("确定要退出登录吗?\n(未同步数据将会丢失)")
                        .setPositiveButton("退出登录", new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                spEditor.putString("username", "");
                                spEditor.putString("password", "");
                                spEditor.putString("classes", "");
                                spEditor.apply();
                                //清除所有数据
                                for (final String key : classes.keySet()) {
                                    deleteFile(key + ".dat");
                                }

                                classes.clear();
                                startActivity(new Intent(getApplication(), login.class));
                                overridePendingTransition(R.anim.slide_in_froml, R.anim.slide_out_fromr);
                                MainActivity.this.finish();
                            }
                        })
                        .setNegativeButton("取消", null).show();
            }
        });
        findViewById(R.id.buttong).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplication(), about.class));
                overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);
            }
        });

        findViewById(R.id.reason).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, reason_setting.class));
                overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);
            }

        });

        findViewById(R.id.setspl).setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                SharedPreferences.Editor sharedata2 = getSharedPreferences("data", 0).edit();
                Boolean Oncp;
                if (findViewById(R.id.onspl).getVisibility() == View.VISIBLE) {
                    Oncp = false;
                    findViewById(R.id.onspl).setVisibility(View.GONE);
                } else {
                    Oncp = true;
                    findViewById(R.id.onspl).setVisibility(View.VISIBLE);
                }
                sharedata2.putBoolean("splash", Oncp);
                sharedata2.apply();

            }

        });

        findViewById(R.id.linearLayout7).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, group_setting.class));
                overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);
            }
        });

        findViewById(R.id.overView).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OverView.class));
                overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);
            }
        });
        lv = (ListView) findViewById(R.id.listView3);

        MyAdapter mAdapter = new MyAdapter(this);//得到一个MyAdapter对象
        lv.setAdapter(mAdapter);
        lv.setOnScrollListener(this);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,final int position, long id) {
                final ClassData c = classes.get(classNow);
                final ArrayList<History> histories = c.histories;
                final ArrayList<History> usHistories = c.unsyncedHistories;

                History h = histories.get(getPos(position));
                final String reason = (h.reason);
                new ActionSheetDialog(MainActivity.this).builder()
                    .setTitle(reason)
                    .setCancelable(false)
                    .setCanceledOnTouchOutside(true)
                    .addSheetItem("删除", ActionSheetDialog.SheetItemColor.Red,
                            new ActionSheetDialog.OnSheetItemClickListener() {
                                @Override
                                public void onClick(int which) {
                                    if(isSync){
                                        new AlertDialogios(MainActivity.this).builder()
                                                .setTitle("提示")
                                                .setMsg("抱歉，数据同步中，删除记录暂不可用，请等待同步完成 :(")
                                                .setNegativeButton("好的", null).show();
                                        return;
                                    }
                                    new AlertDialogios(MainActivity.this).builder()
                                            .setTitle("删除记录")
                                            .setMsg("确认删除记录“" + reason + "”吗?\n该条记录所修改的分数将被撤销")
                                            .setPositiveButton("删除", new OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    History h = histories.get(getPos(position));
                                                    final int change = h.score;
                                                    final String names = h.names;
                                                    for (int i = 0; i != c.members.length; i++) {
                                                        if (names.contains(c.members[i])) {
                                                            c.scores[i] -= change;
                                                        }
                                                    }
                                                    histories.remove(h);
                                                    usHistories.add(new History(h.date));
                                                    updateList();
                                                }
                                            })
                                            .setNegativeButton("取消", null).show();
                                }

                            })
                    .addSheetItem("查看详细", ActionSheetDialog.SheetItemColor.Blue,
                            new ActionSheetDialog.OnSheetItemClickListener() {
                                @Override
                                public void onClick(int which) {
                                    History h = histories.get(getPos(position));
                                    String info_of_record = h.reason + "|" + c.name + "|" + h.names + "|" + h.getScore() + "|" + h.getDate(true) + "|" + h.oper;
                                    Intent i = new Intent();
                                    i.putExtra("record", info_of_record);
                                    i.setClass(MainActivity.this, moreinfo.class);
                                    startActivity(i);
                                    overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);
                                }
                            })
                            //可添加多个SheetItem
                    .show();
                }
            }

        );


        findViewById(R.id.pick).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((TextView) findViewById(R.id.year)).setTextColor(Color.parseColor("#7fffffff"));
                ((TextView) findViewById(R.id.month)).setTextColor(Color.parseColor("#7fffffff"));
                ((TextView) findViewById(R.id.day)).setTextColor(Color.parseColor("#7fffffff"));
                ((TextView) findViewById(R.id.textView5)).setTextColor(Color.parseColor("#7fffffff"));
                ((TextView) findViewById(R.id.textView7)).setTextColor(Color.parseColor("#7fffffff"));
                ((ImageView) findViewById(R.id.droppp)).setImageResource(R.drawable.dropdown);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) findViewById(R.id.year)).setTextColor(Color.parseColor("#ffffff"));
                    ((TextView) findViewById(R.id.month)).setTextColor(Color.parseColor("#ffffff"));
                    ((TextView) findViewById(R.id.day)).setTextColor(Color.parseColor("#ffffff"));
                    ((TextView) findViewById(R.id.textView5)).setTextColor(Color.parseColor("#ffffff"));
                    ((TextView) findViewById(R.id.textView7)).setTextColor(Color.parseColor("#ffffff"));
                    ((ImageView) findViewById(R.id.droppp)).setImageResource(R.drawable.drop);
                }
                return false;
            }
        });

        findViewById(R.id.pickclass).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((TextView) findViewById(R.id.classnow)).setTextColor(Color.parseColor("#7fffffff"));
                ((ImageView) findViewById(R.id.dropclass)).setImageResource(R.drawable.dropdown);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) findViewById(R.id.classnow)).setTextColor(Color.parseColor("#ffffff"));
                    ((ImageView) findViewById(R.id.dropclass)).setImageResource(R.drawable.drop);
                }
                return false;
            }
        });

        findViewById(R.id.pnmb).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((TextView) findViewById(R.id._name)).setTextColor(Color.parseColor("#7fffffff"));
                ((ImageView) findViewById(R.id.nmdr)).setImageResource(R.drawable.dropdown);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) findViewById(R.id._name)).setTextColor(Color.parseColor("#ffffff"));
                    ((ImageView) findViewById(R.id.nmdr)).setImageResource(R.drawable.drop);
                    startActivityForResult(new Intent(MainActivity.this, choosestudent.class), 1);
                    overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);
                }
                return false;
            }
        });

        findViewById(R.id.pickpm).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((TextView) findViewById(R.id.pm)).setTextColor(Color.parseColor("#7fffffff"));
                ((ImageView) findViewById(R.id.drop2)).setImageResource(R.drawable.dropdown);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) findViewById(R.id.pm)).setTextColor(Color.parseColor("#ffffff"));
                    ((ImageView) findViewById(R.id.drop2)).setImageResource(R.drawable.drop);
                }
                return false;
            }
        });

        //筛选加减分
        findViewById(R.id.pickpm).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new ActionSheetDialog(MainActivity.this).builder()
                        .setTitle("筛选加/减分")
                        .setCancelable(false)
                        .setCanceledOnTouchOutside(true)
                        .addSheetItem("加分", ActionSheetDialog.SheetItemColor.Blue,
                                new ActionSheetDialog.OnSheetItemClickListener() {
                                    @Override
                                    public void onClick(int which) {
                                        ((TextView) findViewById(R.id.pm)).setText("  + ");
                                        // ((TextView) findViewById(R.id.pm)).setTextSize(30);
                                        scoreFilter = 1;
                                        updateList();
                                    }
                                })
                        .addSheetItem("减分", ActionSheetDialog.SheetItemColor.Blue,
                                new ActionSheetDialog.OnSheetItemClickListener() {
                                    @Override
                                    public void onClick(int which) {
                                        ((TextView) findViewById(R.id.pm)).setText(" — ");
                                        //  ((TextView) findViewById(R.id.pm)).setTextSize(30);
                                        scoreFilter = 0;
                                        updateList();
                                    }
                                })
                        .addSheetItem("不限", ActionSheetDialog.SheetItemColor.Blue,
                                new ActionSheetDialog.OnSheetItemClickListener() {
                                    @Override
                                    public void onClick(int which) {
                                        ((TextView) findViewById(R.id.pm)).setText("不限");
                                        //  ((TextView) findViewById(R.id.pm)).setTextSize(20);
                                        scoreFilter = -1;
                                        updateList();
                                    }
                                })
                                //可添加多个SheetItem
                        .show();
            }
        });

        findViewById(R.id.pickclass).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ActionSheetDialog ASD = new ActionSheetDialog(MainActivity.this).builder()
                        .setTitle("选择班级")
                        .setCancelable(false)
                        .setCanceledOnTouchOutside(true);
                for (final String key : classes.keySet()) {
                    final String name = classes.get(key).name;
                    ASD.addSheetItem(name, ActionSheetDialog.SheetItemColor.Blue,
                            new ActionSheetDialog.OnSheetItemClickListener() {
                                @Override
                                public void onClick(int which) {
                                    ((TextView) findViewById(R.id.classnow)).setText(name);
                                    classNow = key;
                                    updateList();
                                }
                            });
                }
                ASD.show();
            }
        });

        findViewById(R.id.perlayout).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findViewById(R.id.add).startAnimation(add_rotate_in);

                findViewById(R.id.hand_ic).startAnimation(inputbtn_in);
                findViewById(R.id.hand_tex).startAnimation(inputtex_in);

                findViewById(R.id.speak_ic).startAnimation(inputbtn_in);
                findViewById(R.id.speak_tex).startAnimation(inputtex_in);

                findViewById(R.id.speak_ic).startAnimation(inputbtn_in);
                findViewById(R.id.speak_tex).startAnimation(inputtex_in);

                findViewById(R.id.perlayout).startAnimation(prelayout_in);

                findViewById(R.id.hand_ic).setVisibility(View.GONE);
                findViewById(R.id.hand_tex).setVisibility(View.GONE);

                findViewById(R.id.speak_ic).setVisibility(View.GONE);
                findViewById(R.id.speak_tex).setVisibility(View.GONE);

                findViewById(R.id.perlayout).setVisibility(View.GONE);
                isOnAdd = false;

            }
        });

        findViewById(R.id.hand_ic).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, add.class);
                startActivityForResult(intent, 2);
                overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);

            }
        });

        findViewById(R.id.speak_ic).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                showDevelopingListen();

                /*
                //打开语音识别
                Intent intent = new Intent(MainActivity.this, Listener.class);
                //打开新窗口。参数：主窗口，被调用窗口
                Bundle bundle = new Bundle();//通过Bundle实现数据的传递:
                String userwords = "{\"userword\":[{\"name\":\"学生\",\"words\":[";
                for (final String i : classes.keySet()) {
                    for (int j = 0; j < classes.get(i).members.length; j++) {
                        final String name = classes.get(i).members[j];
                        userwords += "\"" + name + "\",";
                    }
                }
                userwords = userwords.substring(0,userwords.length()-1);
                userwords += "]}]}";
                bundle.putString("key",userwords);
                intent.putExtras(bundle);
                startActivityForResult(intent, 2);
                overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);
                */

            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isSync) {
                    new AlertDialogios(MainActivity.this).builder()
                            .setTitle("提示")
                            .setMsg("抱歉，数据同步中，添加记录暂不可用，请等待同步完成 :(")
                            .setNegativeButton("好的", null).show();
                    return;
                }
                if (isOnAdd) {
                    findViewById(R.id.add).startAnimation(add_rotate_in);

                    findViewById(R.id.hand_ic).startAnimation(inputbtn_in);
                    findViewById(R.id.hand_tex).startAnimation(inputtex_in);

                    findViewById(R.id.speak_ic).startAnimation(inputbtn_in);
                    findViewById(R.id.speak_tex).startAnimation(inputtex_in);

                    findViewById(R.id.speak_ic).startAnimation(inputbtn_in);
                    findViewById(R.id.speak_tex).startAnimation(inputtex_in);

                    findViewById(R.id.perlayout).startAnimation(prelayout_in);

                    findViewById(R.id.hand_ic).setVisibility(View.GONE);
                    findViewById(R.id.hand_tex).setVisibility(View.GONE);

                    findViewById(R.id.speak_ic).setVisibility(View.GONE);
                    findViewById(R.id.speak_tex).setVisibility(View.GONE);

                    findViewById(R.id.perlayout).setVisibility(View.GONE);
                    isOnAdd = false;
                } else {
                    findViewById(R.id.add).startAnimation(add_rotate_out);

                    findViewById(R.id.perlayout).setVisibility(View.VISIBLE);

                    findViewById(R.id.hand_ic).setVisibility(View.VISIBLE);
                    findViewById(R.id.hand_tex).setVisibility(View.VISIBLE);

                    findViewById(R.id.speak_ic).setVisibility(View.VISIBLE);
                    findViewById(R.id.speak_tex).setVisibility(View.VISIBLE);

                    findViewById(R.id.perlayout).startAnimation(prelayout_out);

                    findViewById(R.id.hand_ic).startAnimation(inputbtn_out);
                    findViewById(R.id.hand_tex).startAnimation(inputtex_out);

                    findViewById(R.id.speak_ic).startAnimation(inputbtn_out);
                    findViewById(R.id.speak_tex).startAnimation(inputtex_out);
                    isOnAdd = true;
                }

                 /*
                 //打开语音识别
                 Intent intent = new Intent(MainActivity.this, Listener.class);
                 //打开新窗口。参数：主窗口，被调用窗口
                 Bundle bundle = new Bundle();//通过Bundle实现数据的传递:
                 String userwords = "{\"userword\":[{\"name\":\"学生\",\"words\":[";
                 for (final String i : classes.keySet()) {
                     for (int j = 0; j < classes.get(i).members.length; j++) {
                         final String name = classes.get(i).members[j];
                         userwords += "\"" + name + "\",";
                     }
                 }
                 userwords = userwords.substring(0,userwords.length()-1);
                 userwords += "]}]}";
                 bundle.putString("key",userwords);
                 intent.putExtras(bundle);
                 startActivityForResult(intent, 2);
                 overridePendingTransition(R.anim.slide_in_fromr, R.anim.slide_out_froml);*/
            }
        });

        //筛选日期
        findViewById(R.id.pick).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                superFlag = true;
                DatePickerDialog dpd = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        if (superFlag) {
                            showYear = String.valueOf(year) + "年";
                            showMonth = ((month + 1) < 10) ?
                                    "0" + (month + 1) :
                                    String.valueOf(month + 1);
                            showDay = (day < 10) ? "0" + day : String.valueOf(day);
                            ((TextView) findViewById(R.id.year)).setText(showYear);
                            ((TextView) findViewById(R.id.month)).setText(showMonth);
                            ((TextView) findViewById(R.id.day)).setText(showDay);
                            ((TextView) findViewById(R.id.textView5)).setText("月");
                            ((TextView) findViewById(R.id.textView7)).setText("日");
                            //  ((TextView) findViewById(R.id.month)).setTextSize(30);
                            d = String.valueOf(year) + '-' + (month + 1) + '-' + day;
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                            Date date = new Date();
                            try {
                                date = simpleDateFormat.parse(d);
                            } catch (ParseException e) {
                                //e.printStackTrace();
                            }
                            timeStamp = date.getTime() / 1000;
                            updateList();
                        }

                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dpd.setButton(DialogInterface.BUTTON_NEGATIVE, "不限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        d = "不限";
                        ((TextView) findViewById(R.id.month)).setText(d);
                        ((TextView) findViewById(R.id.year)).setText("");
                        ((TextView) findViewById(R.id.day)).setText("");
                        ((TextView) findViewById(R.id.textView5)).setText("");
                        ((TextView) findViewById(R.id.textView7)).setText("");
                        superFlag = false;
                        timeStamp = 0;
                        updateList();
                    }
                });
                dpd.show();
            }
        });

        //读取个人信息
        //读取组列表
        String content = spReader.getString("groups", "");
        String[] result = content.split(",");
        groups.clear();
        for (int i = 0; i < result.length - 1; i += 2) {
            groups.add(new Group(result[i], result[i + 1]));
        }

        //读取数据
        String rawClasses = spReader.getString("classes", "");


        if(rawClasses.isEmpty()) getClassInfo(true);

        else {
            String[] classesInfo = rawClasses.split(",");
            for (int i = 0; i < classesInfo.length; i += 2) {
                ClassData readNow = new ClassData(classesInfo[i + 1]);
                //读取数据
                try {
                    FileInputStream inputStream = this.openFileInput(classesInfo[i] + ".dat");
                    byte[] bytes = new byte[inputStream.available()];
                    ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                    while (inputStream.read(bytes) != -1) {
                        arrayOutputStream.write(bytes, 0, bytes.length);
                    }
                    inputStream.close();
                    arrayOutputStream.close();
                    readData(new String(arrayOutputStream.toByteArray()), readNow);

                } catch (Exception ignored) {
                }
                classes.put(classesInfo[i], readNow);
            }
        }

        //默认选择一个班
        if (classes.size() != 0) {
            ClassData c = classes.get(classes.firstKey());
            ((TextView) findViewById(R.id.classnow)).setText(c.name);
            classNow = classes.firstKey();
            updateList();
        }


    }

    public JSONArray exportHistories(ArrayList<History> histories){
        JSONArray ret = new JSONArray();
        try {
            for (int i = 0; i != histories.size(); i++) {
                JSONObject record= new JSONObject();
                record.put("score",histories.get(i).score);
                record.put("reason",histories.get(i).reason);
                record.put("operator",histories.get(i).oper);
                record.put("objects",histories.get(i).names);
                record.put("time",histories.get(i).date.getTime()/1000);
                ret.put(record);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void readData(String data, ClassData readNow) {
        try {

            String[] jsons = data.split("\n");

            readNow.loadMembers(new JSONArray(jsons[0]));

            if (jsons.length < 2) return;
            readNow.loadHistories(new JSONArray(jsons[1]));

            if (jsons.length < 3) return;
            readNow.loadUnsyncedHistories(new JSONArray(jsons[2]));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateList();
    }

    public void onStop() {
        super.onStop();
        //保存数据
        String classesData = "";
        for (final String key : classes.keySet()) {
            ClassData c = classes.get(key);
            classesData += key + "," + c.name + ",";

            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(key + ".dat", Activity.MODE_PRIVATE);
                outputStream.write(c.exportMembers().toString().getBytes());
                outputStream.write(("\n"+exportHistories(c.histories).toString()).getBytes());
                outputStream.write(("\n"+exportHistories(c.unsyncedHistories).toString()).getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        spEditor.putString("classes", classesData);
        String groupsStr = "";
        for (Group group : groups) {
            groupsStr += group.groupName + "," + group.groupMembers + ",";
        }
        spEditor.putString("groups", groupsStr);
        spEditor.apply();
    }

    @TargetApi(19)
    private void getClassInfo(final boolean getData) {

        final String username = spReader.getString("username", "");
        final String password = spReader.getString("password", "");
        new Thread(new AccessNetwork(true,
                "http://powerscore.duapp.com/api/get_classes.php",
                "username=" + username + "&password=" + password, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String data=msg.obj.toString().isEmpty()?"":msg.obj.toString().substring(1);
                switch (data) {
                    case "F":
                        showToast("无法获取班级数据：用户名或密码错误");
                        break;
                    case "P":
                        showToast("无法获取班级数据：无法连接到网络");
                        break;

                    default:
                        try {
                            JSONArray json= new JSONArray(data);
                            for (int i = 0; i < json.length() ; ++i) {
                                String name = json.getJSONObject(i).getString("name");
                                String id = String.valueOf(json.getJSONObject(i).getInt("id"));
                                if(classes.containsKey(id)) continue;
                                final ClassData classReading=new ClassData(name);
                                classes.put(id, classReading);
                                if(getData){
                                    new Thread(new AccessNetwork(true,
                                            "http://powerscore.duapp.com/api/sync.php",
                                            "username=" + username + "&password=" + password + "&cid=" + id + "&diff=", new Handler() {
                                        @Override
                                        public void handleMessage(Message msg) {
                                            countNow++;
                                            String data=msg.obj.toString().isEmpty()?"":msg.obj.toString().substring(1);
                                            switch (data) {
                                                case "F":
                                                    if(countNow==countMax) showToast("无法获取班级数据：请稍候重试");
                                                    break;
                                                case "P":
                                                    if(countNow==countMax) showToast("无法获取班级数据：网络连接不稳定");
                                                    break;
                                                default:
                                                    readData(String.valueOf(data), classReading);
                                                    break;
                                            }
                                            if(countNow==countMax) {
                                                isSync = false;
                                                sync.clearAnimation();
                                            }
                                        }
                                    })).start();
                                }
                                if(classNow.equals("-1")) {
                                    ((TextView) findViewById(R.id.classnow)).setText(name);
                                    classNow = id;
                                    updateList();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        })).start();
    }

    public void onResume() {
        super.onResume();
        MyAdapter mAdapter = new MyAdapter(this);//得到一个MyAdapter对象
        lv.setAdapter(mAdapter);
        FlowerCollector.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FlowerCollector.onPause(this);
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {

        if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount > 0) {
            if (countLimit != classes.get(classNow).histories.size()) {
                if (!pdLoading) {
                    /*
                    showCustomProgrssDialog("加载中...");
                    pdLoading = true;

                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(200);
                                if(pdLoading) {
                                    hideCustomProgressDialog();
                                    pdLoading = false;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }

                    }.start();*/
                }
                needLoadMore = true;
            } else {
                if (!lastTip && !showednomore) {
                    showToast("没有更多了");
                    lastTip = true;
                    showednomore = true;
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(2000);
                                showednomore = false;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        } else {
            lastTip = false;
        }


    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //下拉到空闲是，且最后一个item的数等于数据的总数时，进行更新
        if (needLoadMore && scrollState == SCROLL_STATE_IDLE) {
            loadMore();
            updateList();
            needLoadMore = false;
        }
    }

    private void loadMore() {
        countLimit = lv.getAdapter().getCount() + 40;
        if (countLimit > classes.get(classNow).histories.size())
            countLimit = classes.get(classNow).histories.size();
    }

    private ArrayList<HashMap<String, Object>> getData() {
        ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
        if (classNow.equals("-1") && isGen) return listItem;
        if (filterClass.isEmpty() && !isGen) return listItem;
        if (filterName.isEmpty() && !isGen) return listItem;
        final ArrayList<History> histories;
        if (isGen)
            histories = classes.get(classNow).histories;
        else
            histories = classes.get(filterClass).histories;
        int vaildItem = 0;
        for (int i = histories.size() - 1; i >= 0; i--) {
            if (vaildItem++ > countLimit) break;
            History h = histories.get(i);
            if (d.compareTo("不限") != 0 && (h.date.getTime() / 1000 - 86400 > timeStamp || h.date.getTime() / 1000 <= timeStamp))
                continue;
            if (scoreFilter == 0 && h.score > 0) continue; //筛选扣分但是是加分记录，忽略
            if (scoreFilter == 1 && h.score < 0) continue; //筛选加分但是是扣分记录，忽略
            if (!h.names.contains(filterName) && !isGen) continue;
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("ItemTitle", h.shortReason);
            map.put("ItemText", h.shortNames);
            map.put("strmark", h.scoreWithSign);
            map.put("mark", h.score);
            map.put("date", h.getDate(false));
            map.put("id", vaildItem);
            listItem.add(map);
        }
        return listItem;
    }

    private int getPos(int position) {
        if (classNow.equals("-1") && isGen) return -1;
        if (filterClass.isEmpty() && !isGen) return -1;
        if (filterName.isEmpty() && !isGen) return -1;
        int count = 0;
        final ArrayList<History> histories;
        if (isGen)
            histories = classes.get(classNow).histories;
        else
            histories = classes.get(filterClass).histories;
        for (int i = histories.size() - 1; i >= 0; i--) {
            if (histories.size() - i > countLimit) break;
            if (d.compareTo("不限") != 0 && (histories.get(i).date.getTime() / 1000 - 86400 > timeStamp || histories.get(i).date.getTime() / 1000 <= timeStamp))
                continue;
            if (scoreFilter == 0 && histories.get(i).score > 0) continue; //筛选扣分但是是加分记录，忽略
            if (scoreFilter == 1 && histories.get(i).score < 0) continue; //筛选加分但是是扣分记录，忽略
            if (!histories.get(i).names.contains(filterName) && !isGen) continue;
            if (count < position) {
                count++;
                continue;
            }
            return i;
        }
        return -1;
    }

    class MyAdapter extends BaseAdapter {

        private LayoutInflater mInflater;//得到一个LayoutInfalter对象用来导入布局

        /*构造函数*/
        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return getData().size();
        }

        @Override
        public Object getItem(int position) {
            return getData().get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        /*书中详细解释该方法*/
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            MainActivity.MyAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.itemmoreinfo, null);
                holder = new ViewHolder();
                    /*得到各个控件的对象*/
                holder.title = (TextView) convertView.findViewById(R.id.ItemTitle);
                holder.text = (TextView) convertView.findViewById(R.id.ItemText);
                holder.mark = (TextView) convertView.findViewById(R.id.mark);
                holder.date = (TextView) convertView.findViewById(R.id.date);
                holder.positive = (ImageView) convertView.findViewById(R.id.positive);
                convertView.setTag(holder);//绑定ViewHolder对象
            } else {
                holder = (ViewHolder) convertView.getTag();//取出ViewHolder对象
            }
            /*设置TextView显示的内容，即我们存放在动态数组中的数据*/
            HashMap<String, Object> hm = getData().get(position);
            holder.title.setText((String) (hm.get("ItemTitle")));
            String s = (String) (hm.get("ItemText"));
            holder.text.setText(s);
            holder.mark.setText((String) (hm.get("strmark")));
            holder.date.setText((String) (hm.get("date")));
            if ((Integer) (hm.get("mark")) > 0) {
                holder.positive.setImageResource(R.drawable.green);
            } else {
                holder.positive.setImageResource(R.drawable.red);
            }
            if (pdLoading) {
                hideCustomProgressDialog();
                pdLoading = false;
            }

            return convertView;
        }


        final class ViewHolder {
            public TextView title;
            public TextView text;
            public TextView mark;
            public TextView date;
            public ImageView positive;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case 1:
                String result = data.getExtras().getString("result"); //得到新Activity关闭后返回的数据
                assert result != null;
                if (!result.matches("NULL")) {
                    String[] strArray = result.split("[|]");
                    filterClass = strArray[0];
                    filterName = strArray[1];
                    ((TextView) findViewById(R.id._name)).setText(filterName);
                    updateList();
                }
                break;
            case 2:
                String[] results = data.getExtras().getStringArray("data");
                //接收add
                assert results != null;
                if (!results[0].equals("NULL") && !results[2].isEmpty()) {
                    //更新记录
                    String[] namesByClasses = new String[classes.size()]; //分别储存每个班的人
                    String[] allNames = results[1].split("\\|");
                    for (int i = 0; i != namesByClasses.length; i++) {
                        namesByClasses[i] = "";
                    }
                    for (int i = 0; i < allNames.length; i += 2) {
                        String[] members = classes.get(allNames[i]).members;
                        for (int j = 0; j != members.length; j++) {
                            if (allNames[i + 1].equals(members[j])) {
                                int cid = 0;
                                for (final String key : classes.keySet()) { //查找每个班
                                    if (key.equals(allNames[i])) break;
                                    cid++;
                                }
                                namesByClasses[cid] += allNames[i + 1] + ",";
                            }
                        }
                    }
                    int cid = 0;
                    int score = (int) (Float.parseFloat(results[2]) * 10);
                    for (final String key : classes.keySet()) { //查找每个班
                        if (!namesByClasses[cid].isEmpty()) {
                            String names = namesByClasses[cid].substring(0, namesByClasses[cid].length() - 1);
                            String oper = spReader.getString("username", "未登录用户");
                            Date d = new Date();
                            classes.get(key).histories.add(new History(score, names, results[0], d, oper));
                            classes.get(key).unsyncedHistories.add(new History(score, names, results[0], d, oper));
                            for (String name : names.split(",")) {
                                for (int i = 0; i < classes.get(key).members.length; i++) {
                                    if (classes.get(key).members[i].equals(name))
                                        classes.get(key).scores[i] += score;
                                }
                            }
                        }
                        cid++;
                    }
                    updateList();
                }
                break;
        }
    }

    private void showToast(String msg) {
        toastCommom.ToastShow(MainActivity.this, (ViewGroup) findViewById(R.id.toast_layout_root), msg);
    }



    private SFProgrssDialog m_customProgrssDialog;


    final void hideCustomProgressDialog() {
        if (null != m_customProgrssDialog) {
            m_customProgrssDialog.dismiss();
            m_customProgrssDialog = null;
        }
    }

    private void showDevelopingListen() {
        new AlertDialogios(MainActivity.this).builder()
                .setTitle("提示")
                .setMsg("语音记录功能尚在开发中，敬请期待:)")
                .setNegativeButton("好的", null)
                .show();
    }

}
