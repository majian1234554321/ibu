package com.zcy.ibu.login_Register;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.google.gson.Gson;
import com.soarsky.ibu.R;
import com.soarsky.ibu.activity.BaseActivity;
import com.soarsky.ibu.common.app.BaseApplication;
import com.soarsky.ibu.common.data.Config;
import com.soarsky.ibu.common.util.CryptoUtil;
import com.soarsky.ibu.common.util.FileUtil;
import com.soarsky.ibu.common.util.NetUtil;
import com.soarsky.ibu.common.util.PhoneInfoUtil;
import com.soarsky.ibu.common.util.UserUtil;
import com.zcy.ibu.model.LoginResponseModel;
import com.zcy.ibu.model.RegistModel;
import com.zcy.ibu.model.Regist_V3_2_Request_Model;
import com.zcy.ibu.model.Regist_V3_5_Response_Model;
import com.zcy.ibu.utils.ActivityStartUtils;
import com.zcy.ibu.utils.PhoneInfoUtils;
import com.zcy.ibu.utils.ProgessUtils;
import com.zcy.ibu.utils.ToastUtils;
import com.zcy.ibu.view.TitleBarView;

import java.util.ArrayList;
import java.util.List;

import cn.jpush.android.api.JPushInterface;

/**
 * Created by Apple on 15/5/29.
 * 注册功能的第二个界面
 * <p/>
 * <p/>
 * 业务逻辑：注册成功后---登录————登陆成功---打考勤和写日志。
 */
public class Register_V3_2_Activity extends BaseActivity implements View.OnClickListener {


    public int i;
    private LatLng locPoint;
    private LocationClient locClient;
    private double wd;
    private double jd;
    private EditText et_code;
    private Button tv_tips;
    private EditText et_password;
    private Button bt_obtain;
    private MyThread mt;
    private String regisRes;
    private String mobilenumber;
    private String tips;
    private boolean isClick = true;
    private String password;
    private String loginRes;
    private Gson gson;
    private ArrayList<String> users = new ArrayList<String>();
    private ArrayList<String> usersPwd = new ArrayList<String>();
    private String addrres;
    private LocationClient mLocationClient;
    private String mLocationAddress;
    private String time;
    private Handler mhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100://计时器
                    Log.i("TAGS", i + "");
                    if (i < 2) {
                        tv_tips.setText("重新获取");
                        isClick = true;
                        tv_tips.setOnClickListener(Register_V3_2_Activity.this);

                    } else {
                        tv_tips.setText(i + " S");
                        isClick = false;
                    }
                    i--;
                    break;
                case 200://注册按钮点击联网请求返回


                    ToastUtils.show(Register_V3_2_Activity.this,regisRes,Toast.LENGTH_SHORT);

                     if ("100".equals(gson.fromJson(regisRes, Regist_V3_5_Response_Model.class).code)) {
                   //if (true) {

                        String maps = "";
                        Config.USERNAME = mobilenumber;
                        String encodeUn = CryptoUtil.AESencode(Config.USERNAME, Config.KEY);
                        String encodePw = CryptoUtil.AESencode(password, Config.KEY);
                        maps += "authcode=" + "";//hw
                        maps = maps + "&" + "password=" + encodePw;
                        maps = maps + "&" + "username=" + encodeUn;

                        final String loginRequest = maps;


                        //此处登录设置username和seesionID
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    //注册成功后登录 获取相应的sessionId
                                    loginRes = NetUtil.HttpPost(Config.LOGIN_URL, loginRequest, 1002);
                                    LoginResponseModel loginResponseModel = gson.fromJson(loginRes, LoginResponseModel.class);
                                    Config.SESSIONID = loginResponseModel.sessionId;
                                    Config.NEWSESSIONID = loginResponseModel.sessionId;
                                    Config.ISENTERPRISE = loginResponseModel.siUsrFlag;

                                    if (Config.ISENTERPRISE.equals("1")) {
                                        UserUtil.setIsIsEnterPrice(Register_V3_2_Activity.this, false);
                                    } else {
                                        UserUtil.setIsIsEnterPrice(Register_V3_2_Activity.this, true);
                                    }

                                    BaseApplication.getInstance().setRolenames(null);
                                    String roles = loginResponseModel.rolenames;
                                    List<String> rolenames = new ArrayList<String>();
                                    rolenames.add(roles);
                                    BaseApplication.getInstance().setRolenames(
                                            rolenames);

                                    BaseApplication.getInstance();
                                    //hw
                                    BaseApplication.setIslogin("1");


                                    // 开启线程绑定

                                    JPushInterface.setAliasAndTags(getApplicationContext(), PhoneInfoUtil.getIMEI(Register_V3_2_Activity.this), null);

                                    remeberUserName(mobilenumber, password);


                                    //打考勤
                                    OtherDoSomethings.addAttendance(Register_V3_2_Activity.this, mobilenumber, jd, wd, mLocationAddress);


                                    //写日志
                                    OtherDoSomethings.addLog(Register_V3_2_Activity.this, mobilenumber, jd, wd, time, mLocationAddress);

                                    if (cacheMenu(loginRes)) {
                                        UserUtil.setOpenfireLoginState(Register_V3_2_Activity.this, 0);
                                        // 读取SharedPreferences中需要的数据
                                        SharedPreferences preferences = getSharedPreferences("count", Activity.MODE_PRIVATE);
                                        int count = preferences.getInt("count", 0);
                                        if (count == 0) {

                                            overridePendingTransition(android.R.anim.fade_in,
                                                    android.R.anim.fade_out);
                                            SharedPreferences.Editor editor = preferences.edit();
                                            // 存入数据
                                            editor.putInt("count", ++count);
                                            // 提交修改
                                            editor.commit();
                                            // finish();
                                        } else {
                                            // checkVersion();

                                            overridePendingTransition(android.R.anim.fade_in,
                                                    android.R.anim.fade_out);
                                            // finish();
                                        }
                                    } else {

                                        Toast.makeText(Register_V3_2_Activity.this, "获取登录数据失败，请重试",
                                                Toast.LENGTH_SHORT).show();
                                    }


                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }.start();

                        //跳转至图片扫描的界面
                        ProgessUtils.closeProgressDialog();

//                        ToastUtils.show(Register_V3_2_Activity.this, gson.fromJson(regisRes, Regist_V3_5_Response_Model.class).tip, 1);
//                        ActivityStartUtils.jump(Register_V3_2_Activity.this, Register_V3_6_Activity.class);
                        String tipsss = gson.fromJson(regisRes, Regist_V3_5_Response_Model.class).tip;
                        if ("header参数错误".equals(tipsss
                        )) {
                            tipsss = "感谢您对我不忧的支持";
                        }


                        TipsDialogFragment tdf = new TipsDialogFragment(tipsss);
                        // tdf.setTitle(gson.fromJson(regisRes, Regist_V3_5_Response_Model.class).tip);
                        tdf.show(Register_V3_2_Activity.this.getFragmentManager(), "tdf");


                        i = -1;
                    } else {
                        ToastUtils.show(Register_V3_2_Activity.this, gson.fromJson(regisRes, Regist_V3_5_Response_Model.class).tip, 0);
                        ProgessUtils.closeProgressDialog();
                    }
                    break;

                case 300://再次获取验证码
                    Log.i("TAGS", i + "");

                    break;

                default:
            }
        }
    };
    private SharedPreferences jump;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mt = new MyThread();//执行倒计时的线程
        mt.start();

        setContentView(R.layout.activity_register_v3_2);

        jump = getSharedPreferences("jump", 0);


        gson = new Gson();
        mobilenumber = getIntent().getStringExtra("mobilenumber");

        iniit();
        intView();

        intAction();


    }

    private void iniit() {
        locClient = new LocationClient(this);
        TaskLocationListenner taskLocationListenner = new TaskLocationListenner();
        locClient.registerLocationListener(taskLocationListenner);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        locClient.setLocOption(option);
        locClient.start();


    }

    private void intAction() {

        tips = tv_tips.getText().toString().trim();


        bt_obtain.setOnClickListener(this);
    }

    private void intView() {


        TitleBarView tbv_title = (TitleBarView) findViewById(R.id.tbv_title);
        tbv_title.setTitle("注册");
        et_code = (EditText) findViewById(R.id.et_code);
        tv_tips = (Button) findViewById(R.id.tv_tips);

        et_password = (EditText) findViewById(R.id.et_password);
        bt_obtain = (Button) findViewById(R.id.bt_Obtain);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_Obtain:
                String code = et_code.getText().toString().trim();
                password = et_password.getText().toString();


                if (TextUtils.isEmpty(code)) {
                    ToastUtils.show(this, "验证码不能为空", 0);
                    return;
                }


                if (TextUtils.isEmpty(password)) {
                    ToastUtils.show(this, "密码不能为空", 0);
                    return;
                } else if (password.length() < 6 || password.length() >= 16) {
                    ToastUtils.show(this, "密码长度需在6到16之间", 0);
                    return;
                }

                if ("重新获取".equalsIgnoreCase(tips)) {
                    ToastUtils.show(this, "验证码已经超时", 0);
                    // tv_tips.setOnClickListener(this);
                    return;
                }
                //tv_tips.setText("点击获取");

                Regist_V3_2_Request_Model rrm = new Regist_V3_2_Request_Model();
                rrm.ip = "127.0.0.1";
                rrm.telephone = mobilenumber;
                rrm.username = mobilenumber;
                rrm.userpwd = password;
                rrm.loginNickName = Config.loginNickName;
                rrm.loginSign = Config.loginSign;
                rrm.loginType = Config.loginType;
                rrm.randcode = code;


                rrm.v = "1";


                final String map = "data=" + gson.toJson(rrm);

                ProgessUtils.showProgressDialog(Register_V3_2_Activity.this, "温馨提示", "正在获取数据");

                new Thread() {//点击确定联网上传数据
                    @Override
                    public void run() {

                        try {
                            //上传注册的相应信息
                            regisRes = NetUtil.HttpPost(Config.REGISTER_URL, map, 1002);
                            Log.i("Log",regisRes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        Message msg = Message.obtain();
                        msg.what = 200;
                        mhandler.sendMessage(msg);
                    }
                }.start();

                break;

            case R.id.tv_tips://再次获取验证码

                if (isClick) {
                    MyThread mt2 = new MyThread();
                    mt2.start();
                }
                final Gson gsons = new Gson();
                final RegistModel rm = new RegistModel();
                rm.account = mobilenumber;
                gsons.toJson(rm);

                new Thread() {


                    @Override
                    public void run() {
                        try {
                            mobilenumber = NetUtil.NewHttpPost(Config.IBU_PHONE_VALIDCODE, gsons.toJson(rm), "", 10086);

                            Message msg = Message.obtain();
                            msg.what = 300;
                            mhandler.sendMessage(msg);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

                break;

            default:
        }

    }

    /**
     * 将登录用户的菜单加入缓存
     *
     * @param obj
     * @return 缓存状态
     */
    public boolean cacheMenu(Object obj) {

        SharedPreferences share = getSharedPreferences(Config.preferences_key,
                0);
        SharedPreferences.Editor edit = share.edit();
        edit.remove(Config.MENU_STR);
        edit.putString(Config.MENU_STR, (String) obj);
        Log.d("post", "菜单数据:" + (String) obj);
        return edit.commit();

        // Log.e(Common.MENU_STR+"-->", share.getString(Common.MENU_STR, ""));

    }

    /**
     * 保存用户名
     */
    public void remeberUserName(String username, String pwd) {
        boolean hasSaved = false;// 是否已经记录此帐号
        if (users.size() > 0) {
            for (int i = 0; i < users.size(); i++) {
                if (username.equals(users.get(i))) {
                    users.remove(i);
                    usersPwd.remove(i);
                    hasSaved = false;
                    break;
                } else {
                    hasSaved = false;
                }
            }
        }


        if (!hasSaved) {
            users.add(users.size(), username);
            usersPwd.add(usersPwd.size(), pwd);
        }

        saveXml();
        savePwdXml();// hw
    }

    /**
     * 将用户列表保存成文件
     */
    public void saveXml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<ibu>");
        for (int i = 0; i < users.size(); i++) {
            sb.append("<user>" + users.get(i) + "</user>");
        }
        sb.append("</ibu>");
        FileUtil.saveXml("UserAccount.xml", sb.toString());
    }

    /**
     * 将用户密码列表保存成文件 hw
     */
    public void savePwdXml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<ibu>");
        for (int i = 0; i < usersPwd.size(); i++) {
            sb.append("<user>" + usersPwd.get(i) + "</user>");
        }
        sb.append("</ibu>");
        FileUtil.saveXml("UserPwd.xml", sb.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        i = -1;
        locClient.stop();
    }

    public String getdata(String addrStr) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("\"locationInfo\":\"" + addrStr + 12 + "\",");
        sb.append("\"longitude\":\"" + "" + "\",");
        sb.append("\"dimensions\":\"" + "" + "\"");
        sb.append("}");
        return sb.toString();
    }

    public class MyThread extends Thread {


        @Override
        public void run() {
            super.run();
            i = 60;
            while (i > 0) {
                SystemClock.sleep(1000);

                Message msg = Message.obtain();
                msg.what = 100;

                mhandler.sendMessage(msg);
            }

        }
    }

    private class TaskLocationListenner implements BDLocationListener {


        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null)
                return;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                            // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            //  baiduMap.setMyLocationData(locData);
            locPoint = new LatLng(location.getLatitude(), location.getLongitude());
            mLocationAddress = location.getAddrStr();
            wd = location.getLatitude();
            jd = location.getLongitude();
            time = location.getTime();

            double longitude = location.getLongitude();
            double dimensions = location.getLatitude();

            LatLng currentLatLng = new LatLng(location.getLatitude(),
                    location.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory
                    .newLatLng(currentLatLng);
            // baiduMap.animateMapStatus(u);

            ReverseGeoCodeOption rgcOption = new ReverseGeoCodeOption();
            rgcOption.location(new LatLng(dimensions, longitude));

        }
    }

    @SuppressLint("ValidFragment")
    public class TipsDialogFragment extends DialogFragment {

        private String title;
        private TextView tv_content;

        public TipsDialogFragment(String title) {

            this.title = title;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
            final View inflate = View.inflate(inflater.getContext(), R.layout.item_activityregist_2, null);

            tv_content = (TextView) inflate.findViewById(R.id.tv_content);

            tv_content.setText(title);
            inflate.findViewById(R.id.tv_edit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {







                    if (!jump.getString("jump", "12121").equals(PhoneInfoUtils.getVersionCode(inflater.getContext()))) {

                        jump.edit().putString("jump", PhoneInfoUtils.getVersionCode(inflater.getContext())).commit();

                         ActivityStartUtils.jump(inflater.getContext(), Tips_Activity.class);

                    } else {
                        ActivityStartUtils.jump(Register_V3_2_Activity.this, Register_V3_7_Activity.class);

                    }














                    TipsDialogFragment.this.dismiss();
                    finish();
                }
            });


            return inflate;
        }


        public void setTitle(String content) {
            tv_content.setText(content);
        }


    }


}
