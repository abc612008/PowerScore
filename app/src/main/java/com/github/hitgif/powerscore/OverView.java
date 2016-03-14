package com.github.hitgif.powerscore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class OverView extends Activity {
    private  List<String> groupArray;
    private  List<List<String>> childArray;
    private String filter="";
    ExpandableListView lv;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.overview);

        lv=(ExpandableListView)findViewById(R.id.expandableListView);

        groupArray = new ArrayList<String>();
        childArray = new ArrayList<List<String>>();
        findViewById(R.id.backc).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((TextView) findViewById(R.id.textView15c)).setTextColor(Color.parseColor("#9b9b9b"));
                ((ImageView) findViewById(R.id.backimc)).setImageResource(R.drawable.backdown);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ((TextView) findViewById(R.id.textView15c)).setTextColor(Color.parseColor("#ffffff"));
                    ((ImageView) findViewById(R.id.backimc)).setImageResource(R.drawable.back);
                }
                return false;
            }
        });

        findViewById(R.id.backc).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                OverView.this.finish();
            }
        });
        for (final String key : MainActivity.classes.keySet()) {
            Classes c = MainActivity.classes.get(key);
            groupArray.add(c.name);
            List<String> tempArray = new ArrayList<String>();
            for (int j = 0; j < c.members.length; j++) {
                tempArray.add(c.members[j]+"|"+c.scores[j]);
            }
            childArray.add(tempArray);
        }

        lv.setAdapter(new ExpandableAdapter(this));

        lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        final int childPosition, long id) {
                Classes c = null;
                String Key="";
                int cid = 0;
                for (final String key : MainActivity.classes.keySet()) { //查找每个班
                    if (cid == groupPosition) {
                        c = MainActivity.classes.get(key);
                        Key=key;
                        break;
                    }
                    cid++;
                }
                final String fKey=Key;
                if(c==null) return false;
                new ActionSheetDialog(OverView.this).builder()
                        .setTitle(String.valueOf(c.members[childPosition]))
                        .setCancelable(false)
                        .setCanceledOnTouchOutside(true)
                        .addSheetItem("修改分数", ActionSheetDialog.SheetItemColor.Blue,
                                new ActionSheetDialog.OnSheetItemClickListener() {
                                    @Override
                                    public void onClick(int which) {
                                        final EditText text = new EditText(OverView.this);
                                        text.setText(String.valueOf(MainActivity.classes.get(fKey).scores[childPosition]/10.0));
                                        new AlertDialog.Builder(OverView.this)
                                                .setTitle("请设置分数")
                                                .setIcon(android.R.drawable.ic_dialog_info)
                                                .setView(text)
                                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Classes c = MainActivity.classes.get(fKey);
                                                        try {
                                                            c.scores[childPosition]= (int) (Double.parseDouble(text.getText().toString()) * 10);
                                                            showToast("设置分数成功");
                                                        }catch (Exception e){
                                                            showToast("设置分数失败:必须输入数字");
                                                            e.printStackTrace();
                                                        }
                                                        ((BaseAdapter)lv.getAdapter()).notifyDataSetChanged();
                                                    }})
                                                .setNegativeButton("取消", null)
                                                .show();
                                    }
                                })
                        .addSheetItem("查看记录", ActionSheetDialog.SheetItemColor.Blue,
                                new ActionSheetDialog.OnSheetItemClickListener() {
                                    @Override
                                    public void onClick(int which) {

                                    }
                                })
                        .show();
                return true;
            }
        });

        ((EditText) (findViewById(R.id.editText))).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                filter = ((EditText) (findViewById(R.id.editText))).getText().toString();

                groupArray = new ArrayList<String>();
                childArray = new ArrayList<List<String>>();

                ArrayList<Integer> needExpand = new ArrayList<Integer>();

                for (final String key : MainActivity.classes.keySet()) {
                    Classes c = MainActivity.classes.get(key);
                    List<String> tempArray = new ArrayList<String>();
                    for (int j = 0; j < c.members.length; j++) {
                        if (!filter.isEmpty() && !c.members[j].contains(filter)) continue;
                        tempArray.add(c.members[j] + "|" + c.scores[j]);
                    }
                    if (!tempArray.isEmpty()) {
                        groupArray.add(c.name);
                        childArray.add(tempArray);
                        needExpand.add(groupArray.size() - 1);
                    }
                }

                lv.setAdapter(new ExpandableAdapter(OverView.this));

                for (int id : needExpand) {
                    lv.expandGroup(id);
                }

                return false;
            }
        });
    }
    public  class  ExpandableAdapter extends BaseExpandableListAdapter
    {
        Activity activity;

        public ExpandableAdapter(Activity a)
        {
            activity = a;
        }
        public Object getChild(int  groupPosition, int  childPosition)
        {
            return  childArray.get(groupPosition).get(childPosition);
        }
        public long getChildId(int  groupPosition, int  childPosition)
        {
            return  childPosition;
        }
        public int getChildrenCount(int  groupPosition)
        {
            return  childArray.get(groupPosition).size();
        }
        public View getChildView(int  groupPosition, int  childPosition,
                                  boolean  isLastChild, View convertView, ViewGroup parent)
        {
            String[] strings = childArray.get(groupPosition).get(childPosition).split("\\|");

            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.itemoverview, null);
            }

            TextView tv = (TextView) convertView.findViewById(R.id.itemName);
            tv.setText(strings[0]);

            TextView tv2 = (TextView) convertView.findViewById(R.id.Totalmark);
            tv2.setText(strings[1]);

            return convertView;
        }
        // group method stub
        public  Object getGroup(int  groupPosition)
        {
            return  groupArray.get(groupPosition);
        }
        public  int  getGroupCount()
        {
            return  groupArray.size();
        }
        public  long  getGroupId(int  groupPosition)
        {
            return  groupPosition;
        }
        public  View getGroupView(int  groupPosition, boolean  isExpanded,
                                  View convertView, ViewGroup parent)
        {
            String string = groupArray.get(groupPosition);
            AbsListView.LayoutParams layoutParams = new  AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, 84 );
            TextView text = new  TextView(activity);
            text.setLayoutParams(layoutParams);
            text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            text.setPadding(65 , 0 , 0 , 0 );
            text.setText(string);
            text.setTextSize(17);
            return  text;
        }
        public  boolean  hasStableIds()
        {
            return  false ;
        }
        public  boolean  isChildSelectable(int  groupPosition, int  childPosition)
        {
            return  true ;
        }
    }

    private void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
