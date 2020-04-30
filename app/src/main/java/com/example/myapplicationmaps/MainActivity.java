package com.example.myapplicationmaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    GoogleMap mGoogleMap;
    ProgressBar pBar;
    double mLatitude = 0;
    double mLongitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinnerCari = findViewById(R.id.spnCari);
        pBar = findViewById(R.id.pBar);
        //bawaan google maps
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap = googleMap;
                mGoogleMap.getUiSettings().setZoomControlsEnabled(true); //fungsi zoom out zoom in
                initMap();
            }
        });
        //mengatur konten yang akan ditampilkan pada view
        //getResource mengambil adapter dari string yg dibuat
        ArrayAdapter<String> myAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.cari_tempat));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCari.setAdapter(myAdapter);
        spinnerCari.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

                //daftar pilihan spinner untuk lokasi yang dituju (hanya disekitar lokasi kita)
                String xType = "";
                if (position == 1)
                    xType = "school";

                if (position != 0) {

                    //place API (mendapatkan lokasi yg dituju)
                    String sb = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                            "location=" + mLatitude + "," + mLongitude +
                            "&radius=5000" +
                            "&types=" + xType +
                            "&sensor=true" +
                            "&key=" + getResources().getString(R.string.google_maps_key);
                    startProg();
                    new PlacesTask().execute(sb);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void initMap() {

        //cek permission saat pertama kali membuka maps (perijinan untuk akses lokasi saat ini)
        //mengijinkan aplikasi untuk mengakses lokasi yang tepat dari pengguna
        // mengijinkan aplikasi untuk mengakses perkiraan lokasi dari pengguna
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 115);
            return;
        }

        if (mGoogleMap != null) {
            startProg();
            mGoogleMap.setMyLocationEnabled(true); //mengaktifkan lokasi saat ini
            //akses ke sensor lokasi pada device melalui Android location service.
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            //akses lokasi terkini dari pengguna
            String provider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(provider);

            if (location != null) {
                onLocationChanged(location); // update lokasi terkini dari pengguna
            } else
                stopProg();
            locationManager.requestLocationUpdates(provider, 20000, 0, this);
        }
    }
    //fungsi untuk update lokasi saat ini
    @Override
    public void onLocationChanged(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        LatLng latLng = new LatLng(mLatitude, mLongitude);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng)); //menampilkan lokasi saat ini
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
        stopProg();
    }

    private void stopProg() {
        pBar.setVisibility(View.GONE); //meniadakan tampilan
    }

    private void startProg() {
        pBar.setVisibility(View.VISIBLE); //mengaktifkan tampilan
    }

    @SuppressLint("StaticFieldLeak")
    private class PlacesTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = null;
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                stopProg();
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            new ParserTask().execute(result);
        }
    }

    private String downloadUrl(String strUrl) {
        String data = "";
        InputStream iStream;
        HttpURLConnection urlConnection;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            br.close();
            iStream.close();
            urlConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {
        JSONObject jObject;

        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {
            List<HashMap<String, String>> places = null;
            ParserPlace parserPlace = new ParserPlace();
            try {
                jObject = new JSONObject(jsonData[0]);
                places = parserPlace.parse(jObject);
            } catch (Exception e) {
                stopProg();
                e.printStackTrace();
            }
            return places;
        }

        //untuk menampilkan jumlah lokasi terdekat
        @Override
        protected void onPostExecute(List<HashMap<String, String>> list) {
            mGoogleMap.clear();
            for (int i = 0; i < list.size(); i++) {
                MarkerOptions markerOptions = new MarkerOptions(); //penanda lokasi
                HashMap<String, String> hmPlace = list.get(i);
                BitmapDescriptor pinDrop = BitmapDescriptorFactory.fromResource(R.drawable.ic_pin); //custom icon location

                double lat = Double.parseDouble(hmPlace.get("lat")); //medapatkan lat suatu lokasi
                double lng = Double.parseDouble(hmPlace.get("lng")); //mendapatkan lang suatu lokasi
                String nama = hmPlace.get("place_name"); //nama tempat
                String namaJln = hmPlace.get("vicinity"); //deskripsi singkat suatu tempat
                LatLng latLng = new LatLng(lat, lng);

                markerOptions.icon(pinDrop); //menandai lokasi yang dipilih
                markerOptions.position(latLng); //mensetting posisi lokasi yang dipilih
                markerOptions.title(nama + " : " + namaJln);
                mGoogleMap.addMarker(markerOptions);
            }
            stopProg();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

}
