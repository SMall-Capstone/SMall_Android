package com.example.small.Activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.small.Adapter.TabPagerAdapter;
import com.example.small.Beacon.BeaconInfo;
import com.example.small.Beacon.BeaconList;
import com.example.small.Beacon.KalmanFilter;
import com.example.small.Dialog.PopUpDialog;
import com.example.small.Dialog.StampDialog;
import com.example.small.Info.UserInfo;
import com.example.small.R;
import com.example.small.Server.HttpClient;
import com.example.small.ViewPager.RecommendFragment;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.Math.pow;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, BeaconConsumer {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TabPagerAdapter pagerAdapter;

    private BeaconManager beaconManager;
    private BluetoothManager bluetoothManager; //블루투스 매니저는 기본적으로 있어야하기때문에 여기서는 생략합니다.
    private BluetoothAdapter bluetoothAdapter; //블루투스 어댑터에서 탐색, 연결을 담당하니 여기서는 어댑터가 주된 클래스입니다.
    private KalmanFilter mKalmanAccRSSI;
    public BeaconList beaconList;

    private UserInfo userInfo = null;
    private final String TAG="HomeActivity";
    private final int SIZEOFQUEUE = 7;
    private TextView nav_userID;

    NavigationView navigationView;
    Button loginBtn;
    Button signupBtn;
    Button logoutBtn;


    Fragment fragment = new IntroActivity();
    FrameLayout frameLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


       /*  frameLayout = (FrameLayout)findViewById(R.id.fragment2);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace( R.id.fragment2, fragment );
        fragmentTransaction.commit();*/



        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        //actionBar.setTitle("Smart Mall");


        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar);

        Recommand recommand = new Recommand();
        recommand.execute();

       // getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        tabLayout = (TabLayout)findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("추천상품"));
        tabLayout.addTab(tabLayout.newTab().setText("쇼핑뉴스"));
        tabLayout.addTab(tabLayout.newTab().setText("쿠폰"));
        tabLayout.addTab(tabLayout.newTab().setText("매장안내"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.selectedTextColor));
        tabLayout.setTabTextColors(R.color.mainTextColor,getResources().getColor(R.color.selectedTextColor));

        // Initializing ViewPager
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        // Creating TabPagerAdapter adapter
        pagerAdapter = new TabPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));


        // Set TabSelectedListener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ImageButton mapButton = (ImageButton)findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),MyLocationActivity.class);
                startActivity(intent);
            }
        });

        beaconServiceStart();//비콘관련 코드 몰아놓음

        userInfo = UserInfo.getUserInfo();
        Log.i(TAG,"userInfo->"+userInfo.getName());

        nav_userID = (TextView)navigationView.getHeaderView(0).findViewById(R.id.nav_userID);

        loginBtn = (Button)navigationView.getHeaderView(0).findViewById(R.id.loginBtn);
        signupBtn = (Button)navigationView.getHeaderView(0).findViewById(R.id.signupBtn);
        logoutBtn = (Button)navigationView.getHeaderView(0).findViewById(R.id.logoutBtn);
        if(userInfo.getName() == null){
            nav_userID.setText("Guest");
        }
        else{
            nav_userID.setText(userInfo.getName()+" 님");
            loginBtn.setVisibility(View.INVISIBLE);
            signupBtn.setVisibility(View.INVISIBLE);
            logoutBtn.setVisibility(View.VISIBLE);
        }
    }

/*
    //┌Seongwon 안드로이드 생명주기 확인용 Toast 0511 =====================================================
    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "onResume 호출 됨",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Toast.makeText(this, "onRestart 호출 됨",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "onPause 호출 됨",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Toast.makeText(this, "onStop 호출 됨",Toast.LENGTH_LONG).show();
    }

   */
/* @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "onDestroy 호출 됨",Toast.LENGTH_LONG).show();
    }*//*


    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(this, "onStart 호출 됨",Toast.LENGTH_LONG).show();
    }
    //└Seongwon 안드로이드 생명주기 확인용 Toast 0511 =====================================================

*/

    /////////////////////////로그인 회원가입 버튼///////////////////////////

    public void onButtonSignUp_home(View v){
        Intent intent = new Intent(HomeActivity.this, SignUpActivity.class);
        startActivityForResult(intent, 101);
        //startActivity(intent);
        //finish();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 101:
                if(resultCode == 102) {

                }
                break;
            case 103 :
                if(resultCode == 104) {
                    userInfo = (UserInfo)data.getSerializableExtra("userInfo");
                    Log.i("yunjae", "name = " + userInfo.getName());
                    if(userInfo.getName() == null){
                        nav_userID.setText("Guest");
                    }
                    else{
                        nav_userID.setText(userInfo.getName()+" 님");
                        loginBtn.setVisibility(View.INVISIBLE);
                        signupBtn.setVisibility(View.INVISIBLE);
                        logoutBtn.setVisibility(View.VISIBLE);
                        RecommendFragment. mWebView.loadUrl("http://" + HttpClient.ipAdress + ":8080/Android_login/recommendationService/"+userInfo.getUserid());
                    }
                }
                break;
        }
    }

    public void onButtonLogin_home(View v){
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        //startActivity(intent);
        //finish();
        startActivityForResult(intent, 103);
    }
    public void onButtonLogout_home(View v){
        //서버에 logout알리기
        java.util.Map<String,String> params = new HashMap<String,String>();
        params.put("userid",userInfo.getUserid());
        params.put("stamp",String.valueOf(userInfo.getStamp()));

        stampDB SDB = new stampDB();
        SDB.execute(params);

        userInfo.logout();//userInfo 객체 null로 초기화
        Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void beaconServiceStart(){
        //비콘 목록 불러오기(Singleton)
        beaconList = BeaconList.getBeaconListInstance();

        //SeongWon 위치권한 물어보기 //haneul 저장권한
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        }
        //위치권한 물어보기 End

        //칼만필터 초기화
        //End 칼만필터 초기화
        mKalmanAccRSSI = new KalmanFilter();

        // 실제로 비콘을 탐지하기 위한 비콘매니저 객체를 초기화
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // 여기가 중요한데, 기기에 따라서 setBeaconLayout 안의 내용을 바꿔줘야 하는듯 싶다.
        // 필자의 경우에는 아래처럼 하니 잘 동작했음.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        // 비콘 탐지를 시작한다. 실제로는 서비스를 시작하는것.
        beaconManager.bind(this);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            //블루투스를 지원하지 않거나 켜져있지 않으면 장치를끈다.
            Toast.makeText(this, "블루투스를 켜주세요", Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothAdapter.startLeScan(mLeScanCallback);//탐색시작
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    /*beaconList.clear();
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }*/
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if(id == R.id.map_button) {
            Intent intent = new Intent(getApplicationContext(),MyLocationActivity.class);
            startActivity(intent);
        }*/

        if (id == R.id.menu_myCoupon) {
            Intent intent=new Intent(HomeActivity.this, MyCouponActivity.class);
            intent.putExtra("activity","MyCupon activity");
            startActivity(intent);

            ///////////////////////////////////////////STAMP////////////////////////////////////////////////////////
        } else if (id == R.id.menu_stamp) {
            Intent intent = new Intent(getApplicationContext(),StampActivity.class);
            startActivity(intent);

        } else if (id == R.id.menu_navi) {
            Intent intent=new Intent(HomeActivity.this, NavigatorActivity.class);
            intent.putExtra("activity","Navi activity");
            startActivity(intent);

        }
        //////////////프레그먼트//////////////////////////////////
        else if (id == R.id.menu_recommend) {
            viewPager.setCurrentItem(0);

        } else if (id == R.id.menu_news) {
            viewPager.setCurrentItem(1);

        } else if (id == R.id.menu_coupon) {
            viewPager.setCurrentItem(2);

        }  else if (id == R.id.menu_infoFloor) {
            viewPager.setCurrentItem(3);
        }
        /////////////////////////////////////////////////////////

        else if (id == R.id.menu_myInfo) {
            Intent intent=new Intent(HomeActivity.this, MyInfoActivity.class);
            intent.putExtra("activity","MyInfo activity");
            startActivity   (intent);

        } else if (id == R.id.menu_setting) {
            Intent intent=new Intent(HomeActivity.this, SettingActivity.class);
            intent.putExtra("activity","Setting activity");
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);

        if (userInfo.getStamp() >= 3) {
            userInfo.setStamp(0);
        }
        //if(userInfo.getName() != null ){
        if (userInfo != null) {
            java.util.Map<String, String> params = new HashMap<String, String>();
            params.put("userid", userInfo.getUserid());
            params.put("stamp", String.valueOf(userInfo.getStamp()));

            stampDB SDB = new stampDB();
            SDB.execute(params);
        }
    }

  /*  public void replaceFragment(android.support.v4.app.Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.viewPager, fragment, "fragmentTag");
        fragmentTransaction.commit();
    }*/

    static ArrayList<BeaconInfo> beaconInfos;
    boolean firstLoginAlert=true;
    public BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            if (device.getName() != null && device.getName().contains("MiniBeacon")) {
                BeaconInfo beaconInfo = beaconList.findBeacon(device.getName());
                if(beaconInfo==null){
                    Log.i("BeaconName","beaconInfo를 얻지 못함-"+device.getName());
                    //Toast.makeText(getApplicationContext(),"beacon null / "+device.getName(),Toast.LENGTH_LONG).show();
                }
                else{
                    if (beaconInfo.getIsFirst() == true) {
                        //첫번째 측정되는 RSSI값 버리기
                        beaconInfo.setIsFirst(false);
                    } else {
                        int filteredRSSI = (int) mKalmanAccRSSI.applyFilter(rssi);//새로 필터링 된 값

                        //칼만필터+스몰필터
                        if(beaconInfo.getFilteredRssiQueue().size()==SIZEOFQUEUE){
                            beaconInfo.removeInFilteredRssiQueue();
                            beaconInfo.addFilteredRssiQueue(filteredRSSI);
                        }
                        else{
                            beaconInfo.addFilteredRssiQueue(filteredRSSI);
                        }

                        int doubleFilteredRSSI = beaconInfo.getAvgRssi(beaconInfo.getFilteredRssiQueue());
                        //새로 필터링 된 값으로 RSSI값 설정
                        beaconInfo.setFilteredRSSIvalue(doubleFilteredRSSI);

                        //거리계산해서 setting
                        double d = (double) pow(10, (beaconList.getTxPower() - doubleFilteredRSSI) / (10 * 2.0));
                        double distance = Double.parseDouble(String.format("%.2f",d));
                        beaconInfo.setDistance(distance);

                        //비콘을 rssi값 기준으로 정렬
                        beaconInfos = beaconList.findNearestBeaconsByRssi();
                        beaconList.addPointByRssiSorting(beaconInfos);

                        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        List<ActivityManager.RunningTaskInfo> info;
                        info = activityManager.getRunningTasks(7);
                        for (Iterator iterator = info.iterator(); iterator.hasNext(); ) {
                            ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) iterator.next();
                            if (runningTaskInfo.topActivity.getClassName().contains("StampActivty") || runningTaskInfo.topActivity.getClassName().contains("MyLocationActivity")) {
                                if(userInfo.getName() != null){
                                    if (beaconInfos.get(0).isStampBeacon()) {
                                        if (beaconInfos.get(0).getStampCount() == 3) {
                                            Log.i("StampEvent", beaconInfos.get(0).getMinor() + "스탬프 이벤트 발생 count=" + beaconInfos.get(0).getStampCount());
                                            //스탬프 비콘에 가장 가깝게 다가간 측정횟수가 3번일 때 스탬프 다이얼로그 발생
                                            //stampDialog(getApplicationContext());
                                            Intent intent = new Intent(getApplicationContext(), StampDialog.class);
                                            startActivity(intent);

                                            beaconInfos.get(0).setStampCount(beaconInfos.get(0).getStampCount() + 1);
                                        } else {
                                            //쿠폰 비콘에 가장 가깝게 다가간 측정횟수 +1
                                            beaconInfos.get(0).setStampCount(beaconInfos.get(0).getStampCount() + 1);
                                        }

                                    }
                                }
                            }
                        }

                        //PopUp 조정!!!
                        if(beaconInfos.get(0).isPopUpBeacon()){
                            if (beaconInfos.get(0).getPopUpCount() == 3) {
                                Intent intent = new Intent(getApplicationContext(), PopUpDialog.class);
                                startActivity(intent);

                                /*if(userInfo.getFavorite() == null){
                                    //로그인 안한 사용자 -> 기본 광고
                                    Toast.makeText(getApplicationContext(),"Default Pop Up",Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    //로그인 한 사용자 -> 관심사별 팝업광고
                                    if(userInfo.getFavorite().equals("fashion")){
                                        Toast.makeText(getApplicationContext(),"fashion Pop Up",Toast.LENGTH_SHORT).show();
                                    }
                                    else if(userInfo.getFavorite().equals("beauty")){
                                        Toast.makeText(getApplicationContext(),"beauty Pop Up",Toast.LENGTH_SHORT).show();
                                    }
                                    else if(userInfo.getFavorite().equals("general")){
                                        Toast.makeText(getApplicationContext(),"general Pop Up",Toast.LENGTH_SHORT).show();
                                    }
                                    else if(userInfo.getFavorite().equals("sports")){
                                        Toast.makeText(getApplicationContext(),"sports Pop Up",Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        //health
                                        Toast.makeText(getApplicationContext(),"health Pop Up",Toast.LENGTH_SHORT).show();
                                    }
                                }*/
                                beaconInfos.get(0).setPopUpCount(beaconInfos.get(0).getPopUpCount() + 1);
                            } else {
                                //쿠폰 비콘에 가장 가깝게 다가간 측정횟수 +1
                                beaconInfos.get(0).setPopUpCount(beaconInfos.get(0).getPopUpCount() + 1);
                            }
                        }

                    }
                }

            }

        }//onLeScan끝

    };



    class Recommand extends AsyncTask<java.util.Map<String, String>, Integer, String> {
        private UserInfo userInfo = UserInfo.getUserInfo();

        String serverURL = "";

        @Override
        protected String doInBackground(Map<String, String>...maps) {

            if(userInfo.getName() == null)
                serverURL = "http://"+HttpClient.ipAdress+":8080//main";
            else
                serverURL = "http://"+HttpClient.ipAdress+":8080/Nmain";

            HttpClient.Builder http = new HttpClient.Builder("POST",serverURL);
            //http.addAllParameters(maps[0]);

            HttpClient post = http.create();
            post.request();

            int statusCode = post.getHttpStatusCode();

            Log.i(TAG, "응답코드Recommand"+statusCode);

            String body = post.getBody();

            Log.i(TAG, "body : "+body);


            return body;
        }
    }

    class stampDB extends AsyncTask<Map<String, String>, Integer, String> {
        String serverURL = "http://"+ HttpClient.ipAdress+":8080/Android_saveStamp";
        @Override
        protected String doInBackground(java.util.Map<String, String>...maps) {

            Log.i("StampDB", "서버와 통신");
            HttpClient.Builder http = new HttpClient.Builder("POST",serverURL);
            http.addAllParameters(maps[0]);

            HttpClient post = http.create();
            post.request();

            int statusCode = post.getHttpStatusCode();

            Log.i(TAG, "응답코드stampDB"+statusCode);

            String body = post.getBody();

            Log.i(TAG, "body : "+body);

            return body;

        }
    }


}
