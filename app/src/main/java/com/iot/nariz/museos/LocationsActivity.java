package com.iot.nariz.museos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.iot.nariz.utils.PermissionUtils;
import com.iot.nariz.utils.RESTClient;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocationsActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback
{
    private static final String TAG = LocationsActivity.class.getSimpleName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private List<String> lsMuseos = new ArrayList<>();
    private boolean mPermissionDenied = false;
    private SlidingUpPanelLayout mLayout;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map)
    {
        mMap = map;

        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();

        LatLng mex = new LatLng(19.3, -99.1);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mex));

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    private void enableMyLocation()
    {
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
        else if (mMap != null)
        {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick()
    {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();

        if( mMap!=null && mMap.getMyLocation()!=null )
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            JSONObject js = new JSONObject();
            try
            {
                Log.i(TAG, String.valueOf( mMap.getMyLocation().getLatitude() + ", " + mMap.getMyLocation().getLongitude() ) );
                String payload = "{ \"parameters\": [ { \"type\": \"decimal\", \"value\": \"" + mMap.getMyLocation().getLatitude() + "\" }, " +
                        "{ \"type\": \"decimal\", \"value\": \"" + mMap.getMyLocation().getLongitude() + "\" }, " +
                        "{ \"type\": \"decimal\", \"value\": \"" + mMap.getMyLocation().getLatitude() + "\" }, " +
                        "{ \"type\": \"int\", \"value\": \"4\" } ] }";

                js = RESTClient.request(
                        "http://138.197.19.8:8080/arm-museos-engine/museos/radarsearch",
                        payload,
                        true,
                        null,
                        "POST");
                int n = js.getJSONArray("data").length();
                for(int i=0; i<n; i++)
                {
                    JSONObject museo = js.getJSONArray("data").getJSONObject(i);
                    //Log.i(TAG, museo.toString());
                    lsMuseos.add( museo.getString("v_nombre") );
                }

                ListView lv = (ListView) findViewById(R.id.list);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                    {
                        Toast.makeText(LocationsActivity.this, "onItemClick", Toast.LENGTH_SHORT).show();
                    }
                });

                // This is the array adapter, it takes the context of the activity as a
                // first parameter, the type of list view as a second parameter and your
                // array as a third parameter.
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        lsMuseos );

                lv.setAdapter(arrayAdapter);

                mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener()
                {
                    @Override
                    public void onPanelSlide(View panel, float slideOffset)
                    {
                        //Log.i(TAG, "onPanelSlide, offset " + slideOffset);
                    }

                    @Override
                    public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState)
                    {
                        //Log.i(TAG, "onPanelStateChanged " + newState);
                    }
                });
                mLayout.setFadeOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    }
                });

                TextView t = (TextView) findViewById(R.id.name);
                t.setText(Html.fromHtml(getString(R.string.labelSliding)));
                //Show
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE)
        {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments()
    {
        super.onResumeFragments();
        if (mPermissionDenied)
        {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError()
    {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onBackPressed()
    {
        if (mLayout != null &&
                (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        else
        {
            super.onBackPressed();
        }
    }
}
