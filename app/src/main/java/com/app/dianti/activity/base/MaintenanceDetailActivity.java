package com.app.dianti.activity.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.app.dianti.R;
import com.app.dianti.activity.ImageMatrixActivity;
import com.app.dianti.common.AppContext;
import com.app.dianti.util.DataUtil;
import com.app.dianti.util.DateUtils;
import com.app.dianti.util.Logs;
import com.app.dianti.util.StringUtils;
import com.app.dianti.vo.ResponseData;
import com.squareup.picasso.Picasso;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.callback.StringCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class MaintenanceDetailActivity extends CommonActivity {

    private static final String EXTRA_DATA = "data.extra";
    private List<Map<String, String>> data = new ArrayList<Map<String, String>>();
    private ArrayAdapter<Map<String, String>> listAdapter;
    private ImageView imageView;
    private boolean isBack;

    public static Intent getIntent(@NonNull Context context, Map<String, Object> data) {
        final Intent intent = new Intent(context, MaintenanceDetailActivity.class);

        intent.putExtra(EXTRA_DATA, JSON.toJSONString(data));

        return intent;
    }

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setContentView(R.layout.activity_maintenance_detail);

        mListView = (ListView) findViewById(R.id.list_view);
        final String item = getIntent().getStringExtra(EXTRA_DATA);
        final Map<String, Object> row = JSON.parseObject(item);
        Logs.e(row.toString());

        data = new ArrayList<>();
        data.add(DataUtil.getColMap("    注册编码", DataUtil.mapGetString(row, "code")));
        data.add(DataUtil.getColMap("    电梯名称", DataUtil.mapGetString(row, "name")));
        data.add(DataUtil.getColMap("    电梯地址", DataUtil.mapGetString(row, "address")));
        data.add(DataUtil.getColMap("    维保类型", DataUtil.mapGetString(row, "typeName")));
        data.add(DataUtil.getColMap("    计划时间", DateUtils.secondToDateStr2(DataUtil.mapGetString(row, "planTime"))));

        String status = DataUtil.mapGetString(row, "status");
        String statusName = null;
        if ("1".equals(status)) {
            statusName = "待处理";
        } else if ("2".equals(status)) {
            statusName = "已签到,待完成";
        } else if ("3".equals(status)) {
            statusName = "已完成";
        } else {
            statusName = "";
        }
        data.add(DataUtil.getColMap("    当前状态", statusName));


        String createDate = DataUtil.mapGetString(row, "createTime");
        if ("".equals(createDate) || "0".equals(createDate)) {
            data.add(DataUtil.getColMap("    创建时间", ""));
        } else {
            data.add(DataUtil.getColMap("    创建时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(createDate) * 1000))));
        }

        String endDate = DataUtil.mapGetString(row, "endTime");
        if ("".equals(endDate) || "0".equals(endDate)) {
            data.add(DataUtil.getColMap("    更新时间", ""));
        } else {
            data.add(DataUtil.getColMap("    更新时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(endDate) * 1000))));
        }

        listAdapter = new ArrayAdapter<Map<String, String>>(this, R.layout.table_row_list_row, data) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.table_row_list_row, parent, false);

                final TextView name = (TextView) convertView.findViewById(R.id.nameText);
                final TextView value = (TextView) convertView.findViewById(R.id.valueText);

                final Map<String, String> item = getItem(position);
                name.setText(item.get("name"));

                if ("img".equals(item.get("type"))) {
                    final LinearLayout valueView = (LinearLayout) convertView.findViewById(R.id.valueView);
                    loadImgs(valueView, item.get("value") + "");
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ImageMatrixActivity.getInstance().showPopupWindow(MaintenanceDetailActivity.this,item.get("value") + "");
                            isBack=true;
                        }
                    });
                } else {
                    value.setText(item.get("value"));
                }

                return convertView;
            }
        };

        mListView.setAdapter(listAdapter);

        String id = DataUtil.mapGetString(row, "id");
        loadData(id);
    }

    private void loadData(String id) {
        PostFormBuilder postFormBuilder = OkHttpUtils.post().url(AppContext.API_MAINTENANCE_VIEW).addParams("token", AppContext.userInfo.getToken())
                .addParams("maintain_id", id);
        postFormBuilder.build().execute(new StringCallback() {
            @Override
            public void onResponse(String respData, int arg1) {
                Logs.e(respData);
                ResponseData responseData = JSON.parseObject(respData, ResponseData.class);
                if (responseData.getCode().equals("200")) {
                    Map<String, Object> row = responseData.getData();
                    if (row == null) {
                        return;
                    }

                    Map<String, Object> annual_infoMap = (Map<String, Object>) row.get("annual_info");
                    if (annual_infoMap != null) {
                        data.add(DataUtil.getColMap("    问题描述", DataUtil.mapGetString(annual_infoMap, "brief")));
                        data.add(DataUtil.getColMap("    物业反馈", DataUtil.mapGetString(annual_infoMap, "confirm_brief")));
                    }


                    String type_str = DataUtil.mapGetString(row, "item_str");
                    if (StringUtils.isNotBlank(type_str)) {
                        String[] split = type_str.split("\\|");
                        String str = "";
                        for (int i = 0; i < split.length; i++) {
                            String one = split[i];
                            if (str.equals("")) {
                                str = (i + 1) + ". " + one;
                            } else {
                                str = str + "\n\n" + (i + 1) + ". " + one;
                            }
                        }
                        data.add(DataUtil.getColMap("    巡查项目", str));
                    } else {
                        data.add(DataUtil.getColMap("    巡查项目", ""));
                    }
                    List<Map<String, Object>> imgsMap = (List<Map<String, Object>>) annual_infoMap.get("imgs");
                    if (imgsMap != null && imgsMap.size() > 0) {
                        for (Map<String, Object> one :
                                imgsMap) {
                            Map<String, String> colMap = DataUtil.getColMap("      图片", one.get("url") + "");
                            colMap.put("type", "img");
                            data.add(colMap);
                        }

                    }

                    listAdapter.notifyDataSetInvalidated();

                } else {
                    Toast.makeText(getApplicationContext(), "加载数据失败: " + responseData.getMsg(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Call arg0, Exception arg1, int arg2) {
                Toast.makeText(getApplicationContext(), "加载数据失败!请返回后重试。", Toast.LENGTH_SHORT).show();
            }
        });

//        loadImgs();
    }
    public void loadImgs(final LinearLayout valueView, String url) {
        imageView = new ImageView(getApplicationContext());
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.leftMargin = 100;
                    layoutParams.bottomMargin = 100;
                    imageView.setLayoutParams(layoutParams);
                    valueView.addView(imageView);
        Picasso.with(getApplicationContext())
                .load(url)
                .placeholder(R.drawable.loading)
                .resize(200, 300)
                .centerCrop()
                .into(imageView);

    }

    @Override
    protected int getResId() {
        return R.layout.activity_maintenance_detail;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK){
            if (isBack){
                ImageMatrixActivity.getInstance().dissMissPopuoWindow();
                isBack=false;
            }else{
                finish();
            }
        }
        return false;
    }
}
