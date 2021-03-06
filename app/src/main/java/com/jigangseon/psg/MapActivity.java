package com.jigangseon.psg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.jigangseon.psg.find.MainActivity;
import com.jigangseon.psg.search.Search;
import com.jigangseon.psg.subway_line.Subway_fragment_line_1;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.CompassView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MainActivity";

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;
    private UiSettings uiSettings;

    private SlidingUpPanelLayout main_panel;

    private Context context = this;

    private static JSONObject obj;

//    MarkerList markerList = new MarkerList();
//    List<Marker> markers = markerList.getMarkerList();
    private JSONArray jsonArray;

    private Marker marker;
    private List<Marker> markers;

    //?????? ?????? ?????? ?????? String ?????? ????????? ??????
    private double subway_latitude = 0;
    private double subway_longitude = 0;

    //????????? ?????? ??????
    CameraUpdate subway_camera_update;

    //?????? ????????????
    Button add_store;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);



        // ????????? ??????
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_view);
        if(mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map_view, mapFragment).commit();
        }
        // getMapAsync??? ???????????? ???????????? onMapReady ?????? ????????? ??????
        // onMapReady?????? NaverMap ????????? ??????
        mapFragment.getMapAsync(this);

        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        main_panel = (SlidingUpPanelLayout)findViewById(R.id.main_panel);


        add_store = (Button) findViewById(R.id.add_store);
        add_store.setVisibility(View.GONE);




    }


    @SuppressLint("WrongConstant")
    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) { // ?????? ????????? ????????? ??? ?????? ??????????????? ?????????
        Log.d( TAG, "onMapReady");

        // ????????? ???????????? (????????? ??????)
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer) ;
        drawer.setDrawerLockMode(0x00000001);
        Button buttonOpen = (Button) findViewById(R.id.draw_side_menu_button) ;
        buttonOpen.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!drawer.isDrawerOpen(Gravity.LEFT)) {
                    drawer.openDrawer(Gravity.LEFT);
                    main_panel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

                }

            }
        });

        TextView marker_store_name = (TextView) findViewById(R.id.store_name);
        TextView marker_store_hours = (TextView) findViewById(R.id.store_hours);
        TextView marker_store_info = (TextView) findViewById(R.id.store_info);
        TextView marker_store_address = (TextView) findViewById(R.id.store_address);
        TextView marker_store_explanation = (TextView) findViewById(R.id.store_explanation);


        mNaverMap = naverMap; // ???????????? ??????
        mNaverMap.setLocationSource(mLocationSource); // ????????? ??????


        // ?????? UI??????
        uiSettings = mNaverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true); // ???????????? ????????? ??????
        uiSettings.setCompassEnabled(false); // ?????????
        uiSettings.setScaleBarEnabled(true);
        CompassView compassView = findViewById(R.id.compass);
        compassView.setMap(mNaverMap);


        Executor executor = Executors.newFixedThreadPool(2);
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // ??????????????? ?????????

            try {

                ObjectMapper objectMapper = new ObjectMapper();
                HashMap<String, String> map = new HashMap<>();

                String json = objectMapper.writeValueAsString(map);

                Map_HttpUtil util = new Map_HttpUtil();
                util.execute(json);

                JSONArray jsonArray = util.get();


                markers = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    obj = jsonArray.getJSONObject(i);
                    marker = new Marker();
                    marker.setTag(obj.getInt("store_code"));
                    marker.setPosition(new LatLng(Double.parseDouble(obj.getString("store_latitude")), Double.parseDouble(obj.getString("store_longitude"))));
                    marker.setWidth(50);
                    marker.setHeight(70);
                    marker.setCaptionText(obj.getString("store_category_id"));
                    marker.setOnClickListener(new Overlay.OnClickListener() {
                        @Override
                        public boolean onClick(@NonNull Overlay overlay) {
                            main_panel = (SlidingUpPanelLayout) findViewById(R.id.main_panel);
                            main_panel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                            try {
                                obj = jsonArray.getJSONObject((Integer) overlay.getTag()-1);
                                marker_store_name.setText(obj.getString("store_name"));
                                marker_store_hours.setText(obj.getString("store_hours"));
                                marker_store_info.setText(obj.getString("store_info"));
                                marker_store_address.setText(obj.getString("store_address"));
                                marker_store_explanation.setText(obj.getString("store_explanation"));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return false;
                        }
                    });
                    markers.add(marker);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }


            handler.post(() -> {
                // ?????? ?????????
                for (Marker marker : markers) {
                    marker.setMap(naverMap);

                }

            });
        });






        // ??? ????????? ???????????? ?????????
        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) { // ??? ????????? ???????????? ?????????
                Log.v("??????", "MapClick");
                if (main_panel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED){ // ??? ????????? ????????? ?????????.
                    main_panel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                }
            }
        });


        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE); // ???????????? ???????????? ??????

        main_panel = (SlidingUpPanelLayout)findViewById(R.id.main_panel);
        main_panel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN); // ????????? ?????? ???????????? ????????? ????????? ?????? ????????? ??????




    }//End onMapReady();

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // ????????? ???????????? ?????????
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // request code??? ???????????? ?????? ??????
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

                //Face
                //????????? ??????????????? ???????????? ????????? ???????????? ?????? ???????????? ??????.
                //Follow
                //????????? ??????????????? ???????????? ?????? ???????????? ??????.
                //NoFollow
                //????????? ??????????????? ????????? ???????????? ?????? ??????.
                //None
                //?????? ????????? ???????????? ?????? ??????.

            }

            //????????? ?????????
            Intent Subway_Intent = getIntent();
            //????????? ??? ?????????
            subway_latitude = Subway_Intent.getDoubleExtra("subway_latitude",1);
            subway_longitude = Subway_Intent.getDoubleExtra("subway_longitude",1);
            //?????? ?????? ?????? ?????? ?????? ?????????
            if(subway_latitude !=1 && subway_longitude !=1){
                Log.i("if","subway_latitude !=1 && subway_longitude !=1");
                Log.i("mNaverMap.getLocationTrackingMode",""+mNaverMap.getLocationTrackingMode());
                Log.i("LocationTrackingMode.Follow",""+LocationTrackingMode.Follow);
                if(mNaverMap.getLocationTrackingMode()== LocationTrackingMode.Follow){
                    Log.i("if","mNaverMap.getLocationTrackingMode()== LocationTrackingMode.Follow");
                    mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);

                    LatLng subway_click = new LatLng(subway_latitude,subway_longitude);
                    subway_camera_update = CameraUpdate.scrollAndZoomTo(subway_click,15);
                    mNaverMap.moveCamera(subway_camera_update);


                    try{
                        ObjectMapper objectMapper = new ObjectMapper();
                        HashMap<String, String> map = new HashMap<>();

                        String json = objectMapper.writeValueAsString(map);

                        Map_HttpUtil util = new Map_HttpUtil();
                        util.execute(json);

                        JSONArray check_jsonArray = util.get();
                        for(int i=0; i<check_jsonArray.length();i++){
                            JSONObject check_obj = check_jsonArray.getJSONObject(i);
                            int check = check_obj.getInt("store_code");


                            switch (check){
                                default:
                                    LatLng check_LatLng = new LatLng(check_obj.getDouble("store_latitude"),check_obj.getDouble("store_longitude"));
                                    double distance = subway_click.distanceTo(check_LatLng)/1000; // KM
                                    Log.i("distance",""+distance);
                                    if (distance >=1){

                                        add_store.setVisibility(View.VISIBLE);
                                        Toast.makeText(getApplicationContext(),"?????? ????????? ???????????????? ????????? ??????????????????",Toast.LENGTH_SHORT).show();
                                        add_store.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                add_store.setVisibility(View.GONE);
                                            }
                                        });

                                    }
                                    break;
                            }
                        }


                    }catch (ExecutionException | InterruptedException | JsonProcessingException | JSONException e){

                    }


                }

            }
        }
    }


    public double check_marker(double latitude, double longitude, double marker_latitude, double marker_longitude){



        return 0;
    }


    @Override
    protected void onDestroy() { // ????????? ???????????? ?????????
        super.onDestroy();


    }



    public void onCheckBoxClicked(View v){
        boolean checked = ((CheckBox) v).isChecked();
        String[] store_cate = new String[]{"??????","?????????/?????????","?????????","?????????","??? ??????","??????","??????"};
        switch (v.getId()){
            case R.id.side_menu_1:
                if (checked){
                    Toast.makeText(getApplicationContext(), "1 checked", Toast.LENGTH_LONG).show();
                    for (int i=0; i < markers.size(); i++) {
                            if (markers.get(i).getCaptionText().equals(store_cate[0])) {
                                markers.get(i).setVisible(true);
                            }
                    }
                }else{
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[0])) {
                            markers.get(i).setVisible(false);
                        }
                    }
                }
                break;
            case R.id.side_menu_2:
                if (checked){
                    Toast.makeText(getApplicationContext(), "2 checked", Toast.LENGTH_LONG).show();
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[1])) {
                            markers.get(i).setVisible(true);
                        }
                    }
                }else{
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[1])) {
                            markers.get(i).setVisible(false);
                        }
                    }
                }
                break;

            case R.id.side_menu_3:
                if (checked){
                    Toast.makeText(getApplicationContext(), "3 checked", Toast.LENGTH_LONG).show();
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[2])) {
                            markers.get(i).setVisible(true);
                        }
                    }
                }else{
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[2])) {
                            markers.get(i).setVisible(false);
                        }
                    }
                }
                break;

            case R.id.side_menu_4:
                if (checked){
                    Toast.makeText(getApplicationContext(), "4 checked", Toast.LENGTH_LONG).show();
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[3])) {
                            markers.get(i).setVisible(true);
                        }
                    }
                }else{
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[3])) {
                            markers.get(i).setVisible(false);
                        }
                    }
                }
                break;

            case R.id.side_menu_5:
                if (checked){
                    Toast.makeText(getApplicationContext(), "5 checked", Toast.LENGTH_LONG).show();
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[4])) {
                            markers.get(i).setVisible(true);
                        }
                    }
                }else{
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[4])) {
                            markers.get(i).setVisible(false);
                        }
                    }
                }
                break;

            case R.id.side_menu_6:
                if (checked){
                    Toast.makeText(getApplicationContext(), "6 checked", Toast.LENGTH_LONG).show();
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[5])) {
                            markers.get(i).setVisible(true);
                        }
                    }
                }else{
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[5])) {
                            markers.get(i).setVisible(false);
                        }
                    }
                }
                break;

            case R.id.side_menu_7:
                if (checked){
                    Toast.makeText(getApplicationContext(), "7 checked", Toast.LENGTH_LONG).show();
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[6])) {
                            markers.get(i).setVisible(true);
                        }
                    }
                }else{
                    for (int i=0; i < markers.size(); i++) {
                        if (markers.get(i).getCaptionText().equals(store_cate[6])) {
                            markers.get(i).setVisible(false);
                        }
                    }
                }
                break;
        }
    }


    public void clickMenu(View view) { // ????????? ??????
        int id = view.getId();
        LinearLayout linearLayout = findViewById(id);

                switch (id) {
                    //???????????? ????????????
                    case R.id.side_profile_layout:
                        Toast.makeText(context, "????????? ??????", Toast.LENGTH_SHORT).show();
                        Intent side_profile_layout = new Intent(this,LoginActivity.class);
                        startActivity(side_profile_layout);
                        break;

                    case R.id.side_together:
                        Toast.makeText(context, "?????? ?????? ?????? ??????", Toast.LENGTH_SHORT).show();
                        Intent side_menu1 = new Intent(this,MainActivity.class);
                        startActivity(side_menu1);
                        // ?????? ?????????????????? ?????????
                        break;

                }

    }

    public void searchView(View v){  // ?????????

        Intent intent = new Intent(this, Search.class);
        startActivity(intent);
    }
}