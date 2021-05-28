package com.example.mapsdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mapsdemo.databinding.ActivityMapsBinding;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.mapsdemo.HandelPermissions.LOCATION_PERMISSION_GRANTED;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private static final String TAG = "MapsActivity";

    private static final float DEFAULT_ZOOM = 20f;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    boolean mLocationPermissionGranted = false;
    private ActivityMapsBinding binding;
    private LocationManager locationManager;
    ActivityResultLauncher<Intent> mLauncher;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil
                .setContentView(this
                        , R.layout.activity_maps);
        HandelPermissions permissions = new HandelPermissions();
        permissions.checkLocationPermission(this);
        mLocationPermissionGranted = LOCATION_PERMISSION_GRANTED;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == AUTOCOMPLETE_REQUEST_CODE) {
                        Intent data = result.getData();
                        if (data != null) {
                            Place place = Autocomplete.getPlaceFromIntent(data);
                            moveCamera(place.getLatLng(), place.getName());
                            Toast.makeText(MapsActivity.this, place.getName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        if (mLocationPermissionGranted)
            initMap();
        binding.locationIv.setOnClickListener(v -> getDeviceLocation());
        initView();
    }

    private void initView() {
        Places.initialize(this, getString(R.string.google_maps_API_KEY));
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        binding.searchEt.setOnClickListener(v -> startSearchActivity(fields));
    }

    public void startSearchActivity(List<Place.Field> fields) {
        Toast.makeText(this, "clicked search bar", Toast.LENGTH_SHORT).show();
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place;
                if (data != null) {
                    place = Autocomplete.getPlaceFromIntent(data);
                    moveCamera(place.getLatLng(), place.getName());
                    Log.d(TAG, "onActivityResult: abdo " + place.getLatLng());
                }

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
                Toast.makeText(this, "search operation canceled", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void getDeviceLocation() {
        Criteria criteria;
        String bestProvider;
        if (isLocationEnabled()) {
            criteria = new Criteria();
            bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true));

            //You can still do this if you like, you might get lucky:
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Location location = locationManager.getLastKnownLocation(bestProvider);
                if (location != null) {
                    Log.e("TAG", "GPS is on");
                    moveCamera(new LatLng(location.getLatitude(), location.getLongitude()), "your location");
                }
                else{
                    //This is what you need:
                    locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);
                }
            }
        } else {
            Toast.makeText(this, "Please open your GPS to find your location", Toast.LENGTH_SHORT).show();
        }

    }

    private void moveCamera(LatLng latLng, String locationTitle) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapsActivity.DEFAULT_ZOOM));
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(locationTitle);
        mMap.addMarker(markerOptions);
        Log.d(TAG, "moveCamera: abdo " + latLng);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;
        Log.d(TAG, "onMapReady: called");
        if (mLocationPermissionGranted) {
            //getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED

                    && ActivityCompat.checkSelfPermission(this
                    , Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }
    GoogleMap mMap;

    @Override
    public void onLocationChanged(@NonNull Location location) {
        locationManager.removeUpdates(this);

        //open the map:
        moveCamera(new LatLng(location.getLatitude(), location.getLongitude()), "your location");
    }

    @Override
    protected void onPause() {
        locationManager.removeUpdates(this);
        super.onPause();
    }
}
