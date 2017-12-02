package s.travelio;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import s.travelio.POJO.Example;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    private GoogleMap mMap1;
    LatLng origin;
    LatLng dest;
    ArrayList<LatLng> MarkerPoints;
    DatabaseReference databaseLocation;
    TextView ShowDistanceDuration;
    Polyline line;
    DatabaseReference screenChange;
    private FirebaseAuth mAuth;
    String lattitude,longitude;
    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        screenChange = FirebaseDatabase.getInstance().getReference("Conductor");
        databaseLocation = FirebaseDatabase.getInstance().getReference("UserRegistration");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        final Dialog settingsDialog = new Dialog(this);
        settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        settingsDialog.setContentView(getLayoutInflater().inflate(R.layout.image_layout, null));
        ShowDistanceDuration = (TextView) findViewById(R.id.show_distance_time);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // Initializing
        MarkerPoints = new ArrayList<>();

        //show error dialog if Google Play Services not available
        if (!isGooglePlayServicesAvailable()) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.");
            finish();
        }
        else {
            Log.d("onCreate", "Google Play Services available. Continuing.");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap1 = googleMap;

        // Add a marker in Sydney and move the camera
        /*LatLng Model_Town = new LatLng(28.7158727, 77.1910738);
        mMap.addMarker(new MarkerOptions().position(Model_Town).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Model_Town));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        */
        // Setting onclick event listener for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                // clearing map and generating new marker points if user clicks on map more than two times
                if (MarkerPoints.size() > 1) {
                    mMap.clear();
                    MarkerPoints.clear();
                    MarkerPoints = new ArrayList<>();
                    ShowDistanceDuration.setText("");
                }

                // Adding new item to the ArrayList
                MarkerPoints.add(point);

                // Creating MarkerOptions
                final MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(point);

                /**
                 * For the start location, the color of marker is GREEN and
                 * for the end location, the color of marker is RED.
                 */
                if (MarkerPoints.size() == 1) {
                    //options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    return;
                } else if (MarkerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                }

                screenChange = FirebaseDatabase.getInstance().getReference("Conductor");

                screenChange.orderByChild("Conductor").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot databaseSnapshot :dataSnapshot.getChildren()){

                            screenChange sc = databaseSnapshot.getValue(screenChange.class);

                            //Toast.makeText(MapsActivity.this,"current location is \n" + sc.getCurrentLattitude() + "\n" + sc.getCurrentLongitude(), Toast.LENGTH_SHORT).show();
                            LatLng current = new LatLng(Double.valueOf(sc.getCurrentLattitude().toString()),Double.valueOf(sc.getCurrentLongitude().toString()));
                            mMap.addMarker(new MarkerOptions().position(current).title("Journey Started from Here").visible(true));
                            mMap.addMarker(options);
                            origin = MarkerPoints.set(0,current);

                            // Checks, whether start and end locations are captured
                            if (MarkerPoints.size() >= 2) {
                                origin = MarkerPoints.set(0,current);
                                dest = MarkerPoints.get(1);

                                build_retrofit_and_get_response("driving");
                            }
                        }
                        return;
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
        });

        return;

    }

    private void build_retrofit_and_get_response(String type) {

        String url = "https://maps.googleapis.com/maps/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitMaps service = retrofit.create(RetrofitMaps.class);

        Call<Example> call = service.getDistanceDuration("metric", origin.latitude + "," + origin.longitude,dest.latitude + "," + dest.longitude, type);

        call.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Response<Example> response, Retrofit retrofit) {

                try {
                    //Remove previous line from map
                    if (line != null) {
                        line.remove();
                    }
                    // This loop will go through all the results and add marker on each location.
                    for (int i = 0; i < response.body().getRoutes().size(); i++) {
                        String distance = response.body().getRoutes().get(i).getLegs().get(i).getDistance().getText();
                        String time = response.body().getRoutes().get(i).getLegs().get(i).getDuration().getText();
                        //String encodedString = response.body().getRoutes().get(0).getOverviewPolyline().getPoints();

                        String fares = farer(Double.parseDouble(distance.substring(0, 2)));

                        //bundle passing
                        Intent farr = new Intent(MapsActivity.this, scanoutActivity.class);
                        farr.putExtra("message", fares);
                        startActivity(farr);
                        return;
                    }
                } catch (Exception e) {
                    Log.d("onResponse", "There is an error");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("onFailure", t.toString());
            }
        });

    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    // Checking if Google Play Services Available or not
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }
    boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finish();
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        screenChange = FirebaseDatabase.getInstance().getReference("Conductor");

        screenChange.orderByChild("Conductor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot databaseSnapshot :dataSnapshot.getChildren()){

                    screenChange sc = databaseSnapshot.getValue(screenChange.class);
                    LatLng current = new LatLng(Double.valueOf(sc.getCurrentLattitude().toString()),Double.valueOf(sc.getCurrentLongitude().toString()));
                    mMap1.addMarker(new MarkerOptions().position(current).title("Journey Started from Here").visible(true));
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
                }
                return;
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public String farer(double dist)
    {int fare1;
        if(dist<=2)
        {fare1 = 4;}
        else if(dist>2 && dist<=4)
        {fare1 = 5;}
        else if(dist>4 && dist<=6)
        {fare1 = 7;}
        else if(dist>6 && dist<=8)
        {fare1 = 8;}
        else if(dist>8 && dist<=10)
        {fare1 = 11;}
        else if(dist>10 && dist<=12)
        {fare1 = 14;}
        else if(dist>12 && dist<=14)
        {fare1 = 15;}
        else if(dist>14 && dist<=17)
        {fare1 = 19;}
        else if(dist>17 && dist<=20)
        {fare1 = 21;}
        else if(dist>20 && dist<=25)
        {fare1 = 23;}
        else
        {fare1 = 25;}
        return String.valueOf(fare1);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                double latti = location.getLatitude();
                double longi = location.getLongitude();
                lattitude = String.valueOf(latti);
                longitude = String.valueOf(longi);
                LatLng last = new LatLng(Double.valueOf(lattitude),Double.valueOf(longitude));
                mMap.addMarker(new MarkerOptions().position(last).title("Journey Ends Here").visible(true));
            }else{
                Toast.makeText(this,"Unable to Trace your location", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    protected void buildAlertMessageNoGps() {

        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("Please Turn ON your GPS Connection")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final android.app.AlertDialog alert = builder.create();
        alert.show();
    }
}
