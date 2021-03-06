package com.example.myfirstapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.example.myfirstapplication.gps.GPSManager;

import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;

import android.view.MenuItem;

import com.example.myfirstapplication.gps.GPSManagerCallerInterface;
import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, GPSManagerCallerInterface {

    GPSManager gpsManager;
    private MapView osm;
    private MapController mc;
    private LocationManager ubicacion;
    ArrayList<User> users = new ArrayList<User>();
    private User selectedUser;
    Button bFIni, bFFin, bHIni, bHFin, bLocsHist;
    private int iDia, iMes, iAno, iHora, iMinutos, fDia, fMes, fAno, fHora, fMinutos;
    private double lat, longi;
    private long time;
    Date iDate = new Date();
    Date fDate = new Date();
    private boolean sfi, shi, sff, shf; // Swtiches para revisar que se selecciono las fechas y horas iniciales y finales para la busqueda en el historial de posiciones
    Location loc;
    boolean online = false, gpsOn=false;;
    TextView InternetState, GPSState;
    private MyLocationNewOverlay mLocationOverlay;
    private GPSManagerCallerInterface caller;
    ArrayList<String> listOfMessages = new ArrayList<>();
    private static final String TAG = "MainActivity";
    private static ConnectivityManager manager;
    private String cambio;
    private Marker myMarker;
    User yo;
    private ArrayList<ArrayList> locHistArray;
    GPSStatus gpsStatus;
    MyAppDatabase db;
    long fechaI,fechaF;
    int i=0;

    // Funciona sin problemas


    public void GPSCheckstartThread() {
        GPStionCheckThread e = new GPStionCheckThread();
        e.start();
    }
    class GPStionCheckThread extends Thread {
        @Override
        public void run() {
            InternetState = (TextView) findViewById(R.id.internetState);
            while (true) {
                gpsStatus = new GPSStatus();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // NO MOVER ESTO
        MapConfig();
        confBotones();
        // NO MOVER ESTO
        db= Room.databaseBuilder(getApplicationContext(),MyAppDatabase.class, "Historial de Posciones").allowMainThreadQueries().build();
        Log.i("Confirmacion", "Se creo el RoomDatabase");
        ConectionCheckstartThread();
        Log.i("Confirmaci??n", "Botones configurados");
        gpsStatus = new GPSStatus(); // No estoy seguro de que pasa si lo quito, asi que mejor lo dejo
        crearUsuariosDePrueba();// BORRAR
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder =new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                if (!online){
                    builder.setTitle("No hay conexi??n a internet");
                    builder.setMessage("Por favor conectese a un una red con acceso a internet");
                    builder.show();
                }/*Notifico que no hay internet*/
                Log.i("Confirmaci??n", "Revision inicial de conexion="+online);
                if (GpsOn()) {
                    users.clear();
                    yo = new User("Yo",myPos(),true);
                    addMarker(yo,true,false);
                    users.add(yo);
                    crearUsuariosDePrueba();// BORRAR
                    mc.animateTo(new GeoPoint(lat,longi));
                    Log.i("Usuario Creado", "4");
                    GPSManager();
                    GPSStatus s= new GPSStatus();
                    if (!online){
                    db.myDao().add(new UserLocHist(yo.getTime(), yo.getLatitude(), yo.getLongitude()));
                    }/*Guardo su pos en el Room DB*/else {
                        //CONSUMO EL WS
                        // Crear usuario con nombre, y estado
                        //Crear location con time, lat y longi y a??adir al usuario creado
                        //A??adir al usuario a la lista de usuarios

                        for (int i = 1; i < users.size() ; i++) {
                            Log.d("Confirmaci??n","USUARIO "+i);
                            addMarker(users.get(i),false,false);
                        }
                    }//A??ado a los usuarios que consigo atravez del ws
                }/*Creo el user y lo pongo en el mapa, en caso de estar offline guardo su pos en el Room DB*/else{
                    GPSState.setTextColor(Color.RED);
                    builder.setTitle("GPS Desactivado");
                    builder.setMessage("Por favor active el GPS para ver su posicion y la de los demas usuarios");
                    builder.show();
                }//Notifico que el GPS no esta activado
                Log.i("Confirmaci??n", "Revision inicial de gps="+GpsOn());
            }
        },1000); //Funciona
        Log.d("ConfBotnoes OK", "");
    }
    private boolean GpsOn() {
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        System.out.println("Provider contains=> " + provider);
        if (provider.contains("gps") || provider.contains("network")){
            GPSState.setTextColor(Color.GREEN);
            if (!gpsOn) {

                //regLoc();
            }
            Log.i("Confirmacion","GPS activado");
            gpsOn=true;
            return true;
        }
        return false;
    }
    private void GPSManager() {
        ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
              return;
        }
        ubicacion.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, gpsStatus);
    }

    double y=0.9;
    double x=0.7;
    private class GPSStatus implements LocationListener{
        @Override
        public void onLocationChanged(Location location) {
            if (gpsOn) {
                if (yo != null) {
                    yo.setLoc(location);
                    yo.getLoc().setLatitude(location.getLatitude() + y); // Es solo para probar, QUITAR AL FINAL
                    yo.getLoc().setLongitude(location.getLongitude() + x); // Es solo para probar, QUITAR AL FINAL
                    osm.getOverlays().remove(myMarker);
                    addMarker(users.get(0), true,false);
                    Log.i("Coordenadas ", i + ": " + String.valueOf(loc.getLatitude()) + ", " + String.valueOf(loc.getLongitude()) + ", At: " + String.valueOf(loc.getTime()));
                    i++;
                    y = x+0.1;
                    x = y+0.2;
                    if (!online) {
                        Log.d("Guardar posicion", "antes");
                        db.myDao().add(new UserLocHist(yo.getTime(), yo.getLatitude(), yo.getLongitude()));
                        Log.i("Guardar posicion", "pos a??adida, tama??o de la db:" + String.valueOf(db.myDao().getAll().size()));
                    } else {
                        Log.i("Guardar posicion", "entro al else");
                        //CONSUMIR WEB SERVICE
                    }
                    Log.d("Onlineeee", String.valueOf(online));
                }
            }
        }
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.i("Confirmacion","GPS status cambio");

        }
        @Override
        public void onProviderEnabled(String s) {
            Log.i("Confirmacion","GPS actiado");

            Log.d("GPS Activado","GPS Activado00000");
                    GPSState.setTextColor(Color.GREEN);
                    MapConfig();
                    localizaciones();
                    GPSManager();
                    yo.setLoc(myPos());

        }
        @Override
        public void onProviderDisabled(String s) {
            Log.i("Confirmacion","GPS desactivado");

            AlertDialog.Builder builder =new AlertDialog.Builder(MainActivity.this);
            builder.setCancelable(true);
            GPSState.setTextColor(Color.RED);
            builder.setTitle("GPS Desactivado");
            builder.setMessage("Por favor active el gps");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });

            builder.show();
        }
}
    private void confBotones() {

        GPSState = (TextView) findViewById(R.id.gpsState);
        bFIni=(Button) findViewById(R.id.bFechaIni);
        bFFin=(Button) findViewById(R.id.bFechaFin);
        bHIni=(Button) findViewById(R.id.bHoraIni);
        bHFin=(Button) findViewById(R.id.bHoraFin);
        bLocsHist=(Button) findViewById(R.id.bLocsHist);
        bFIni.setOnClickListener(this);
        bFFin.setOnClickListener(this);
        bHIni.setOnClickListener(this);
        bHFin.setOnClickListener(this);
        bLocsHist.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(final View view) {
        final Calendar iCalendar =Calendar.getInstance();
        final Calendar fCalendar =Calendar.getInstance();
        if(view==bFIni || view==bFFin){
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                    Log.d("Confirmaci??n","Entro al calendario");
                    if (view==bFIni) {
                        iAno=i;
                        iMes=i1;
                        iDia=i2;
                        bFIni.setText("Fecha inicial: "+i + "/" + (i1 + 1) + "/" + i2);
                        Log.d("Confirmaci??n","Seleccionada la fecha inicial");
                        sfi=true;
                    }else{
                        if (view==bFFin){
                            fAno=i;
                            fMes=i1;
                            fDia=i2;
                            bFFin.setText("Fecha final: "+i + "/" + (i1 + 1) + "/" + i2);
                            Log.d("Confirmaci??n","Seleccionada la fecha final");
                            sff=true;
                        }
                    }
                }
            }
            ,iDia,iMes,iAno);
            datePickerDialog.show();
        }
        if(view==bHIni||view==bHFin){
            TimePickerDialog timePickerDialog=new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int i, int i1) {
                    if (view == bHIni) {
                        iHora=i;
                        iMinutos=i1;
                        bHIni.setText("Hora inicial: " + i + ":" + i1);
                        Log.d("Confirmaci??n", "Seleccionada la hora inicial");
                        shi=true;
                    } else {
                        if (view == bHFin) {
                            fHora=i;
                            fMinutos=i1;
                            bHFin.setText("Hora Final: " + i + ":" + i1);
                            Log.d("Confirmaci??n", "Seleccionada la hora final");
                            shf=true;
                        }
                    }
                }
                },iHora,iMinutos,true);
            timePickerDialog.show();


        }
        if(view==bLocsHist){
            AlertDialog.Builder builder =new AlertDialog.Builder(MainActivity.this);
            builder.setCancelable(true);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            Log.d("Online== ", String.valueOf(online));
            if (selectedUser == null) {
                builder.setTitle("Faltan datos");
                builder.setMessage("Seleccione un usuario");
                builder.show();
            }else{
                if (!online && selectedUser.getName().equals(yo.getName())){
                  builder.setTitle("Estas desconectado");
                  builder.setMessage("Por favor conectese a una red con acceso a internet para ver el historial de otros usuarios");
                  builder.show();
                }else {
                    if (!sfi) {
                        builder.setTitle("Faltan datos");
                        builder.setMessage("Seleccione la fecha inicial");
                        builder.show();
                    } else {
                        if (!sff) {
                            builder.setMessage("Seleccione la fecha final");
                            builder.show();
                        } else {
                            if (!shi) {
                                builder.setMessage("Seleccione la hora inicial");
                                builder.show();
                            } else {
                                if (!shf) {
                                    builder.setMessage("Seleccione la hora final");
                                    builder.show();
                                } else {
                                    iCalendar.set(iAno, iMes, iDia, iHora, iMinutos);
                                    fCalendar.set(fAno, fMes, fDia, fHora, fMinutos);
                                    fechaI=iCalendar.getTimeInMillis();
                                    fechaF=fCalendar.getTimeInMillis();
                                    startThreadMarcarHist();
                                    Date iDate = new Date(iCalendar.getTimeInMillis());
                                    Log.d("", "Date Inicial"+iDate.toString());
                                    Date fDate = new Date(fCalendar.getTimeInMillis());
                                    Log.d("Date Final:", fDate.toString());

                                }
                            }
                        }
                    }

                }
            }


            }
      //  InfoPopUp.instantiate(this,"");

    }

    public void marcarLocsHist(final User user, long fechaI, long fechaF){
        Double latitud,longitud;
        long time;
        ArrayList<User> userLocHist=new ArrayList<>();
        Log.d("Tama??o osm.getoverlays", "El tama??o es: "+String.valueOf(osm.getOverlays().size()));

        if (users.size()<osm.getOverlays().size()) {
            int inicio = osm.getOverlays().size() - 1;
            for (int i = inicio; i >= users.size(); i--) {
                osm.getOverlays().remove(i);
            }
        }
            if (selectedUser.getId()==yo.getId()&& !online){
                Log.d("online: ", "Tama??o del dao"+String.valueOf(db.myDao().getAll().size()));
                for (int i = 0; i < db.myDao().getAll().size(); i++) {
                   latitud=Double.parseDouble(db.myDao().getAll().get(i).getLatitude());
                   longitud=Double.parseDouble((db.myDao().getAll().get(i).getLongitude()));
                   time=Long.parseLong((db.myDao().getAll().get(i).getTime()));
                   Location loc = new Location("");
                   loc.setTime(time);
                   loc.setLongitude(longitud);
                   loc.setLatitude(latitud);
                   User s=new User(selectedUser.getName(),loc,false);
                   userLocHist.add(s);
                }
                Log.i("Tama??o de userLocHist", "el tama??o es: "+userLocHist.size());
            }else {
                dx=0;
                dy=0;
                for (int i = 0; i < 10; i++) {
                    userLocHist.add((crearLocHist(selectedUser)));
                } //QUITAR AL CONSUMIR EL WERB SERVICE

                //CONSUMIR EL WEBSERVICES MANDNADO LAS VARIABLES DE TIEMPO Y RECIBIR  EL JSON CON EL ARRAY DE ARRAYS DE TIME, LAT, LONG
                // CREAR EL USUARIO CON EL NOMBRE DEL SELECTED USER Y LOS DATOS QUE RETORNA EL JSON
            }
            for (int i = 0; i < userLocHist.size(); i++) {
                Log.d("valor de i","el valor de i = "+String.valueOf(i));
                addMarker(userLocHist.get(i),false,true);
                //addMarkerHist(l/*aqui va la lisa de posiciones que devuelve el web service*/.get(i),user.getName());
            }

       }


    double dx,dy;
    public User crearLocHist(User user){
        User s=new User(user.getName(),user.getLoc(), false);
        double lat=Double.parseDouble(user.getLatitude()),longi=Double.parseDouble(user.getLongitude());
        long time=loc.getTime();
        Log.d("Locs Hist","");
        Location loc2=new Location("");
        loc2.setLongitude(longi-dx);
        loc2.setLatitude(lat-dy);
        loc2.setTime(time);
        s.setLoc(loc2);
        dx=dx+0.1;
        dy=dy+0.2;
        return s;
    }
    public void addMarkerHist(String time, String latitud, String longitud, String name) {
        // Log.d("Long3 Hist",String.valueOf(loc.getLongitude()));
        Marker marker = new Marker(osm);
        marker.setPosition(new GeoPoint(loc));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.pos_azul));
        marker.setTitle(name);
        Date date=new Date(loc.getTime());
        marker.setSnippet(loc.getLatitude()+", "+loc.getLongitude()+"\n"+"At: "+date.toString());
        osm.getOverlays().add(marker);

        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                marker.showInfoWindow();
                Log.d("Confirmaci??n: ", "Usuario Seleccionado");
                Log.d("Confirmaci??n: ", String.valueOf(selectedUser.getId()));
                return false;
            }

        });
    }

    public void addMarker(final User user, boolean soyYo, boolean hist){
        try {
            Log.i("Confirmacion: oms= ", "osm=" + osm.toString());
            Marker marker = new Marker(osm);

            marker.setPosition(new GeoPoint(user.getLoc()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            osm.invalidate(); //Esto es para que se actualice mi marker en el mapa
            if (hist){
                marker.setIcon(getResources().getDrawable(R.drawable.pos_azul));
            }else {
                if (soyYo) {
                    marker.setIcon(getResources().getDrawable(R.drawable.pos_amarilla));
                    myMarker = marker;
                } else {
                    if (user.conectado) {
                        marker.setIcon(getResources().getDrawable(R.drawable.pos_verde));
                    } else {
                        marker.setIcon(getResources().getDrawable(R.drawable.pos_roja));
                    }
                }
                marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker, MapView mapView) {
                        marker.showInfoWindow();
                        Log.i("Confirmaci??n: ", "InfoWindow abierta");
                        selectedUser = user;
                        Log.i("Confirmaci??n: ", "Usuario Seleccionado");
                        Log.i("Confirmaci??n: ", "nombre del usuairo = "+selectedUser.getName());
                        return false;
                    }

                });
            }
            Log.d("Confirmaci??n: ", "A??adido OnMarkerCickListener");
            Log.d("Confirmaci??n: ", "Conectividad revisada");
            marker.setTitle(user.getName());
            Date date = new Date(user.getLoc().getTime());
            marker.setSnippet(user.getLoc().getLatitude() + ", " + user.getLoc().getLongitude() + "\n" + "At: " + date.toString());
            //"At: "+date.toString()

            Log.d("Confirmaci??n: ", "A??adida info de usario al InfoWindow");
            if (soyYo) {
                osm.getOverlays().add(0, marker);
            } else {
                osm.getOverlays().add(marker);

            }


        }catch (Exception e){
            Log.e("Error en addMarker",e.toString());
        }
    }

    private class operacionSoap extends AsyncTask<String,String,String>{
        static final String NAMESPACE="http://paqueteService/";
        static final String METHODNAME="addPosition";
        static final String URL="http://192.168.0.102:8080/WebServiceMovil/databaseConn?WSDL";
        static final String SOAP_ACTION=NAMESPACE+METHODNAME;
        @Override
        protected String doInBackground(String... params) {
            SoapObject request = new SoapObject(NAMESPACE, METHODNAME);
            request.addProperty("id", params[0]);
            request.addProperty("dateTime", params[1]);
            request.addProperty("latitud", params[2]);
            request.addProperty("longitud", params[3]);

            SoapSerializationEnvelope envelope= new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet=false;
            envelope.setOutputSoapObject(request);
            HttpTransportSE transporte = new HttpTransportSE(URL);
            try{
                transporte.call(SOAP_ACTION, envelope);
                SoapPrimitive respone = (SoapPrimitive) envelope.getResponse();
                Log.d("Respuesta",respone.toString());
                Log.d("Respuesta",params[0]+", "+params[2]+", "+params[3]);
            }catch(Exception e) {
               Log.d("eXXX", e.getMessage());
            }
            return null;
        }
    }

    public void localizaciones() {

        // Recibir usuarios

        Log.i("Confirmaci??n", "11");

        Log.i("Confirmaci??n", "12");




        Log.d("ONLINE?",String.valueOf(online));
        new operacionSoap().execute(String.valueOf(yo.getId()), yo.getTime(), yo.getLatitude(), yo.getLongitude());
        Log.d("Usuario Creado","");

        //RECIBIR LOS USUARIOS CON EL WEB SERVICE



        //osm.getOverlays().clear();

        //Poner iconos en las posiciones
        for (int i = 1; i < users.size() ; i++) {
            Log.d("Confirmaci??n","USUARIO "+i);
          //  addMarker(users.get(i),false);
        }
    }

    public void crearUsuariosDePrueba(){
        Location loc2=new Location("");
        loc2.setLongitude(longi+0.3);
        loc2.setLatitude(lat+0.3);
        loc2.setTime(time);
        Location loc3=new Location("");

        loc3.setLongitude(longi-0.2);
        loc3.setLatitude(lat+0.2);
        loc3.setTime(time);

        User us0 = new User("2",loc2,false);
        User us1 = new User("3",loc3,true);
        users.add(us0);
        users.add(us1);
    } //BORRAR


    public  void MapConfig(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            }

        osm = (MapView) findViewById(R.id.mapView);
        osm.setTileSource(TileSourceFactory.MAPNIK);
        osm.setBuiltInZoomControls(true);
        osm.setMultiTouchControls(true);
        mc = (MapController) osm.getController();
        mc.setZoom(12);
    }

    public Location myPos(){
        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                   Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1000);
        }
        while (loc==null) {
            Log.i("Confirmaci??n", "13");
            ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            loc = ubicacion.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) {
                loc = ubicacion.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            loc = ubicacion.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //ENVIAR MI LOC A LA BASE DE DATOS
        }
        longi = loc.getLongitude();
        lat = loc.getLatitude();
        time = loc.getTime();
        return loc;
    }

    public void conectado() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();


        if (isConnected) {
            try {
                HttpURLConnection urlc = (HttpURLConnection)
                        (new URL("http://clients3.google.com/generate_204")
                                .openConnection());
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                if (urlc.getResponseCode() == 204 &&
                        urlc.getContentLength() == 0) {
                    Log.d("TAG", "Conetado");
                    // METODO PARA PREGUNTAR SI CAMBIO ALGUNA POSICION, RECIBIR DICHA POSICION Y MARCARLA
                    if (true/*SI CAMBIO ALGUNA POS*/) {
                        osm.getOverlays().clear();
                        for (int j = 0; j < users.size(); j++) {
                            boolean soyYo = true;
                            if (j > 0) {
                                soyYo = false;
                            }
                            addMarker(users.get(j), soyYo, false);
                        }
                    }
                    // Crear funcion que reciba el id del usuario que cambio de pos, eliminar el marker en la osm.getoverlays().getpos(msima pos del usuario en el ector de users)
                    // preguntar si hay algun usuario nuevo
                    if(!online) {
                        InternetState.setTextColor(Color.GREEN);
                        List<UserLocHist> locs = db.myDao().getAll();
                        Log.d("NOTAAA", String.valueOf(db.myDao().getAll()));
                        Log.d("Lista: ", "");
                        if (locs != null) {
                            for (int j = 0; j < locs.size(); j++) {
                                Log.d("Elemento: ", j + ": " + locs.get(j));
                                // ENVIAR CON WEB SERVICE EL ID DE YO, LOC.TIM, LOC.LONGI Y LOC.LATI
                            }
                        }

                        db.myDao().deleteTable();
                    }
                    Log.i("Confirmaci??n", "Wifi Activado");
                    online = true;
                    //     InternetState.setText("Conectado");
                } else {
                    Log.d("TAG", "Error checking internet connection");
                    InternetState.setTextColor(Color.RED);
                    online = false;
                    Log.i("Confirmaci??n", "Wifi Desactivado");

                }
            } catch (IOException e) {
                InternetState.setTextColor(Color.RED);
                online = false;
                Log.e("TAG", "Error checking internet connection", e);
                Log.i("Confirmaci??n", "Wifi Desactivado");


                //   InternetState.setText("Desconectado");


            }
        } else {
            Log.d("TAG", "No network available!");
            InternetState.setTextColor(Color.RED);
            online = false;
            // InternetState.setText("Desconectado");
        }
    }

    public void ConectionCheckstartThread() {
        ConectionCheckThread e = new ConectionCheckThread();
        e.start();
    }
    class ConectionCheckThread extends Thread {
        @Override
        public void run() {
            InternetState = (TextView) findViewById(R.id.internetState);
            while (true) {
                conectado();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startThreadMarcarHist() {
        MarcarHiststartThread e = new MarcarHiststartThread();
        e.start();
    }
    class MarcarHiststartThread extends Thread {
        @Override
        public void run() {
            InternetState = (TextView) findViewById(R.id.internetState);
            marcarLocsHist(selectedUser,  fechaI,fechaF);
            Thread.interrupted();

        }
    } //USAR ESTE HILO PARA MARCAR LAS POS HISTORICAS EN EL MAPA SIN QUE SE CONGELE EL APP

    public class User {
        int id;
        String name;
        Location loc;
        boolean conectado;
        ArrayList<Location> locHist= new ArrayList<>();

        public User(String name,Location loc, boolean conectado) {
            this.name=name;
            this.id=id;
            this.loc = (loc);
            this.conectado=conectado;
            this.locHist=locHist;

        }
        public String getTime(){
            return String.valueOf(this.loc.getTime());
        }
        public String getLatitude(){
            return String.valueOf(this.loc.getLatitude());
        }
        public String getLongitude(){
            return String.valueOf(this.loc.getLongitude());
        }
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Location getLoc() {
            return loc;
        }

        public void setLoc(Location loc) {
            this.loc = loc;
        }

        public boolean isConectado() {
            return conectado;
        }

        public void setConectado(boolean conectado) {
            this.conectado = conectado;
        }

        public ArrayList<Location> getLocHist() {
            return locHist;
        }

        public void setLocHist(ArrayList<Location> locHist) {
            this.locHist = locHist;
        }

    }
































    //COSAS QUE NO USO PERO TENIA EL PROFESOR
    public Location initializeOSM(){
        try{
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    !=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.
                                        WRITE_EXTERNAL_STORAGE},1002);

            }
            Context ctx = getApplicationContext();
            Configuration.getInstance().load(ctx,
                    PreferenceManager.
                            getDefaultSharedPreferences(ctx));
            osm = (MapView) findViewById(R.id.mapView);
            osm.setTileSource(TileSourceFactory.MAPNIK);
            this.mLocationOverlay =
                    new MyLocationNewOverlay(
                            new GpsMyLocationProvider(
                                    this),osm);
            this.mLocationOverlay.enableMyLocation();
            osm.getOverlays().add(this.mLocationOverlay);

            Location loc=new Location(String.valueOf(mLocationOverlay.getMyLocation()));
            Log.d("Valor:mLocationOverlay:",String.valueOf(loc.getLongitude()));
          //  loc.setLatitude(this.mLocationOverlay.getMyLocation().getLatitude());
            //loc.setLongitude(this.mLocationOverlay.getMyLocation().getLongitude());

            return loc;
        }catch (Exception error){
            Toast.makeText(this,error.getMessage(),Toast.LENGTH_SHORT).show();
return null;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {
        } else if (id == R.id.nav_slideshow) {
        } else if (id == R.id.nav_tools) {
        } else if (id == R.id.nav_share) {
        } else if (id == R.id.nav_send) {
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    }

    @Override
    public void needPermissions() {
        this.requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                1001);
    }


    @Override
    public void locationHasBeenReceived(final Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {



            }
        });
    }
    @Override
    public void gpsErrorHasBeenThrown(final Exception error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                androidx.appcompat.app.AlertDialog.Builder builder=
                        new androidx.appcompat.app.AlertDialog.
                                Builder(getApplicationContext());
                builder.setTitle("GPS Error")
                        .setMessage(error.getMessage())
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //TODO
                            }
                        });
                builder.show();
            }
        });

    }




}
