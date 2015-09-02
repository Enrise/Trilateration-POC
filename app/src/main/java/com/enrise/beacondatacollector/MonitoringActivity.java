package com.enrise.beacondatacollector;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

/**
 * @author dyoung
 * @author Matt Tyler
 */
public class MonitoringActivity extends Activity implements BeaconConsumer {
    protected static final String TAG = "MonitoringActivity";
    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in Milliseconds
    protected LocationManager locationManager;
    protected EditText editText;
    protected Button retrieveLocationButton;
    protected Button clearButton;
    protected Button exportButton;
    Collection<Beacon> beaconList;
    private BeaconManager beaconManager;
    WriteToFile writeToFile;

    JSONObject dataObj = new JSONObject();
    JSONArray dataArr = new JSONArray();
    JSONObject beaconObj = new JSONObject();
    JSONArray beaconArr = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);

        writeToFile = new WriteToFile();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        retrieveLocationButton = (Button) findViewById(R.id.retrieve_location_button);
        clearButton = (Button) findViewById(R.id.clear_textfield);
        exportButton = (Button) findViewById(R.id.export_data);
        editText = (EditText) findViewById(R.id.response_text);


        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText("");
                dataArr = new JSONArray();
            }
        });

        retrieveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBeaconList() == null) {
                    return;
                }
                try {
                    dataObj = new JSONObject();
                    beaconArr = new JSONArray();

                    dataObj.put("timestamp", getGPSTimeStamp());
                    dataObj.put("latitude", getCurrentLocation(0));
                    dataObj.put("longitude", getCurrentLocation(1));
                    for (Beacon beacon : getBeaconList()) {
                        beaconObj = new JSONObject();
                        beaconObj.put("major", beacon.getId2());
                        beaconObj.put("minor", beacon.getId3());
                        beaconObj.put("rssi", beacon.getRssi());
                        beaconArr.put(beaconObj);
                    }
                    dataObj.put("beacons", beaconArr);
                    dataArr.put(dataObj);
                    System.out.println(dataArr.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                editText.setText(dataArr.toString());
                editText.append("\n\n\n");

            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeToFile.writeToFile(getApplicationContext(), String.valueOf(editText.getText()));
                Toast.makeText(getApplicationContext(), "Exported, see README.md how to get it.", Toast.LENGTH_SHORT).show();
            }
        });


        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, new MyLocationListener());
        beaconManager.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
                Toast.makeText(getApplicationContext(), "Now you can get beacons", Toast.LENGTH_SHORT).show();
                Log.i(TAG, region.toString());

            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
                Log.i(TAG, region.toString());
            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                if (beacons.size() > 0) {
                    setBeaconList(beacons);
                    Log.i(TAG, "The first beacon I see is about " + beacons.iterator().next().getDistance() + " meters away.");
                    Log.i(TAG, beacons.toString());
                }
            }
        });

        try {
            Identifier uuid = Identifier.parse("44C0FFEE-988A-49DC-0BAD-A55C0DE2D1E4");
            Identifier major = Identifier.fromInt(701);
            Identifier minor = Identifier.fromInt(479);
            //                                                          uuid, major, minor
            beaconManager.startRangingBeaconsInRegion(new Region("1", uuid, null, null));
        } catch (RemoteException e) {
        }

    }

    protected double getCurrentLocation(int type) {
        double message = 0;
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {

            if (type == 0) {
                message = location.getLatitude();
            } else {
                message = location.getLongitude();
            }
        }

        return message;
    }

    protected String getGPSTimeStamp() {
        String message = "null";
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            message = String.valueOf(location.getTime());
        }
        return message;
    }

    public Collection<Beacon> getBeaconList() {
        return this.beaconList;
    }

    public void setBeaconList(Collection<Beacon> beaconList) {
        this.beaconList = beaconList;
    }

    private class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            String message = String.format(
                    "New Location \n Longitude: %1$s \n Latitude: %2$s",
                    location.getLongitude(), location.getLatitude()
            );
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
        }

        public void onStatusChanged(String s, int i, Bundle b) {
            Toast.makeText(getBaseContext(), "Provider status changed",
                    Toast.LENGTH_LONG).show();
        }

        public void onProviderDisabled(String s) {
            Toast.makeText(getBaseContext(),
                    "Provider disabled by the user. GPS turned off",
                    Toast.LENGTH_LONG).show();
        }

        public void onProviderEnabled(String s) {
            Toast.makeText(getBaseContext(),
                    "Provider enabled by the user. GPS turned on",
                    Toast.LENGTH_LONG).show();
        }

    }


}
