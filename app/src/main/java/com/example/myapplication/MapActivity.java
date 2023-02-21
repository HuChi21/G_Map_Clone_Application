package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.icu.text.Transliterator;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.Manifest;
import android.se.omapi.SEService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.OnConnectionFailedListener {
    //vars
    private static final String TAG = "MapActivity";
    private static final float DEFAULT_ZOOM = 10f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40,-168), new LatLng(71,136));
    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlaceAutocompleteAdapter autocompleteAdapter;
    private GoogleApiClient googleApiClient;
    private static int changeView = 0;
    private Marker mMarker;
    private Address address;


    //widget
    private AutoCompleteTextView edtInputSearch;
    private ImageView imgGps,imgView,imgSearch, imgInfo, imgDirect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        edtInputSearch = (AutoCompleteTextView) findViewById(R.id.edtInputSearch);
        //anhs xaj
        imgGps = (ImageView) findViewById(R.id.imgGps);
        imgView = (ImageView) findViewById(R.id.imgView);
        imgSearch = (ImageView) findViewById(R.id.imgSearch);
        imgInfo =(ImageView) findViewById(R.id.imgInfo);
        imgDirect =(ImageView) findViewById(R.id.imgDirect);
        //khởi tại Map
        initMap();
        //khởi tạo search

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "G-Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        gMap = googleMap;
        //Lấy vị trí của thiết bị
        getDeviceLocation();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        gMap.setMyLocationEnabled(true);
        gMap.getUiSettings().setMyLocationButtonEnabled(false);
        gMap.getUiSettings().setZoomControlsEnabled(true);
        init();
    }
    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }


    private void init(){

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        autocompleteAdapter = new PlaceAutocompleteAdapter(this, googleApiClient,
                LAT_LNG_BOUNDS, null);

        edtInputSearch.setAdapter(autocompleteAdapter);

        imgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                geoLocate();
            }
        });
//        edtInputSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if(actionId== EditorInfo.IME_ACTION_SEARCH||actionId==EditorInfo.IME_ACTION_DONE
//                        ||event.getAction()==event.ACTION_DOWN||event.getAction()==event.KEYCODE_ENTER){
//                    //Thực hiện phương thức tìm kiếm
//                    geoLocate();
//                }
//                return false;
//            }
//        });
        imgGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
            }
        });
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(changeView == 0){
                    gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    changeView++;
                }
                else if (changeView == 1){
                    gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    changeView = 0;
                }

            }
        });
        imgInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                geoLocate();
                try{
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    }else{
                        Log.d(TAG, "onClick: place info: " + address.toString());
                        mMarker.showInfoWindow();
                    }
                }catch (NullPointerException e){
                    Log.e(TAG, "onClick: NullPointerException: " + e.getMessage() );
                }
            }
        });
        imgDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
                try
                {
                    gMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                        @Override
                        public void onMapLongClick(@NonNull LatLng latLng) {
                            address.getLatitude();
                        }
                    });
                }
                catch(Exception e)
                {
                    Log.e(TAG, "onClick: NullPointerException: " + e.getMessage() );
                }
            }
        });
    }

    private void geoLocate() {
        String searchString = edtInputSearch.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
            try{
                list = geocoder.getFromLocationName(searchString,1);
            }catch(IOException e){
                Toast.makeText(MapActivity.this,"Error "+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
            if(list.size() > 0) {
                address = list.get(0);
                moveCam(new LatLng(address.getLatitude(), address.getLongitude()), 15f,searchString.toUpperCase() ,address.getAddressLine(0));
            }

    }

    private void getDeviceLocation(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            final Task location = fusedLocationProviderClient.getLastLocation();

            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){

                        LatLng Hcm = new LatLng(10.762622, 106.660172);
                        Location currentLocation = (Location) task.getResult();
                        //Di chuyển camera
                        moveCam(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),15f,"My Location","My Address");
                    }else{
                        Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }catch (SecurityException e){
           Toast.makeText(MapActivity.this,"Error" + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void moveCam(LatLng latLng, float zoom,String title,String snippet){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions().position(latLng).title(title).snippet(snippet);
            mMarker = gMap.addMarker(options);
        }
        //ẩn softkeyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
//    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
//        @Override
//        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//
//            final AutocompletePrediction item = autocompleteAdapter.getItem(i);
//            final String placeId = item.getPlaceId();
//
//            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
//                    .getPlaceById(googleApiClient, placeId);
//            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
//        }
//    };

//    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
//        @Override
//        public void onResult(@NonNull PlaceBuffer places) {
//            if(!places.getStatus().isSuccess()){
//                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
//                places.release();
//                return;
//            }
//            final Place place = places.get(0);
//
//            try{
//                mPlace = new PlaceInfo();
//                mPlace.setName(place.getName().toString());
//                Log.d(TAG, "onResult: name: " + place.getName());
//                mPlace.setAddress(place.getAddress().toString());
//                Log.d(TAG, "onResult: address: " + place.getAddress());
////                mPlace.setAttributions(place.getAttributions().toString());
////                Log.d(TAG, "onResult: attributions: " + place.getAttributions());
//                mPlace.setId(place.getId());
//                Log.d(TAG, "onResult: id:" + place.getId());
//                mPlace.setLatlng(place.getLatLng());
//                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
//                mPlace.setRating(place.getRating());
//                Log.d(TAG, "onResult: rating: " + place.getRating());
//                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
//                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
//                mPlace.setWebsiteUri(place.getWebsiteUri());
//                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());
//
//                Log.d(TAG, "onResult: place: " + mPlace.toString());
//            }catch (NullPointerException e){
//                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage() );
//            }
//
//            moveCam(new LatLng(place.getViewport().getCenter().latitude,
//                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace.getName());
//
//            places.release();
//        }
//    };
}
