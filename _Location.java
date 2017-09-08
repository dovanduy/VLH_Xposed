package com.example.yylou.vlh;

/* XPosed Framework */
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;

/* Location */
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/* Socket Connection */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.InetAddress;

/* Utility */
import android.app.AndroidAppHelper;
import android.util.Log;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Constructor;

/**
 * Created by yylou on 2017/2/24.
 */

public class _Location implements IXposedHookLoadPackage, Runnable {

    private static String TAG = "Debug";

    private double nLatitude  = 24.047477;
    private double nLongitude = 120.517046;

    private Thread runner;

    /* Elapsed Time */
    long tStart, tEnd;
    double tDelta;

    private void updateLocation() {
        try {
            this.runner = new Thread( this );
            this.runner.start();
            this.runner.join();
        } catch ( Exception e ) {
            XposedBridge.log( e );
        }
    }

    @Override
    public void run() {
        try {
            /* GET GPS Location via Access Point */
            InetAddress IP = InetAddress.getByName( "192.168.5.1" );
            int Port = 15555;
            Socket mSocket = new Socket( IP, Port );

            DataOutputStream SocketOutputStream = new DataOutputStream( mSocket.getOutputStream() );
            DataInputStream SocketInputStream = new DataInputStream( mSocket.getInputStream() );

            String sUPDATE = "UPDATE";
            byte[] bUPDATE = sUPDATE.getBytes( "UTF-8" );
            SocketOutputStream.writeInt( bUPDATE.length );
            SocketOutputStream.write( bUPDATE );
            SocketOutputStream.flush();

            String Location  = SocketInputStream.readLine();
            XposedBridge.log( AndroidAppHelper.currentPackageName() + " -> " + Location );
            String[] LocationS = Location.split( " " );

            nLatitude  = Double.parseDouble( LocationS[0] );
            nLongitude = Double.parseDouble( LocationS[1] );

            // XposedBridge.log( "\t[New Location] " + String.valueOf( nLatitude ) + ", " + String.valueOf( nLongitude ) );

        } catch ( Exception ignored ) {
            XposedBridge.log( ignored.getCause().toString() );

            nLatitude  = 24.047477;
            nLongitude = 120.517046;

            XposedBridge.log( "\t[Default Location] " + String.valueOf( nLatitude ) + ", " + String.valueOf( nLongitude ) );
        }
    }

    @Override
    public void handleLoadPackage( final XC_LoadPackage.LoadPackageParam lpparam ) {
        try {
            final Class <?> _hookClass_LocaionManager     = XposedHelpers.findClass( "android.location.LocationManager", lpparam.classLoader );
            final Class <?> _hookClass_LocationProvider   = XposedHelpers.findClass( "android.location.LocationProvider", lpparam.classLoader );
            final Class <?> _hookClass_ProviderProperties = XposedHelpers.findClass( "com.android.internal.location.ProviderProperties", lpparam.classLoader );

            // XposedBridge.log( "LocationManager Class Found in : " + lpparam.packageName );

            /* HOOK LocationManager.checkProvider */
            XposedBridge.hookAllMethods( _hookClass_LocaionManager, "checkProvider", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod( MethodHookParam param ) throws Throwable {
                    XposedBridge.log( "[VLH CheckProvider START] " + String.valueOf( System.nanoTime() ) );

                    ConnectivityManager connManager = (ConnectivityManager) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            XposedBridge.log( AndroidAppHelper.currentPackageName() + " -> check [" + param.args[0] + "] Provider = " + param.getResult() );

                            if( param.args[0] == "gps" ) {
                                param.setResult( true );
                            }
                        }
                    }

                    XposedBridge.log( "[VLH CheckProvider END] " + String.valueOf( System.nanoTime() ) );
                }
            });

            /* HOOK LocationManager.isProviderEnabled */
            XposedBridge.hookAllMethods( _hookClass_LocaionManager, "isProviderEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod( MethodHookParam param ) throws Throwable {
                    ConnectivityManager connManager = (ConnectivityManager) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            XposedBridge.log( AndroidAppHelper.currentPackageName() + " -> is [" + param.args[0] + "] ProviderEnabled = " + param.getResult() );

                            if (param.args[0] == "gps") {
                                param.setResult(true);
                            }
                        }
                    }
                }
            });

            /* HOOK LocationManager.getLastKnownLocation */
            XposedBridge.hookAllMethods( _hookClass_LocaionManager, "getLastKnownLocation", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod( MethodHookParam param ) throws Throwable {
                    ConnectivityManager connManager = (ConnectivityManager) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            // XposedBridge.log(AndroidAppHelper.currentPackageName() + " -> { getLastKnownLocation } (" + param.args[0].toString() + ")");

                            /* CREATE a Location of current GPS */
                            Location location = new Location( "gps" );
                            location.setTime( System.currentTimeMillis() );
                            location.setLatitude( nLatitude );
                            location.setLongitude( nLongitude );
                            location.setAccuracy( ( float ) 20.0 );

                            /* SET result of default activity to NEW location */
                            param.setResult( location );

                            // XposedBridge.log( "\t[Current Location] " + String.valueOf(nLatitude) + ", " + String.valueOf(nLongitude) );
                        }
                    }
                }
            });

            /* HOOK LocationManager.requestLocationUpdates */
            XposedBridge.hookAllMethods( _hookClass_LocaionManager, "requestLocationUpdates", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod( MethodHookParam param ) throws Throwable {
                    ConnectivityManager connManager = (ConnectivityManager) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            XposedBridge.log(AndroidAppHelper.currentPackageName() + " -> [GPS] requestLocationUpdates");

                            for( int count=0 ; count<param.args.length ; count++ ) {
                                // XposedBridge.log("\t[GPS] Class of Param : " + param.args[count].getClass().getName());

                                if( param.args[count] instanceof LocationListener ) {
                                    // XposedBridge.log("\t  [O] GPS_FindLocationListener");

                                    /* GET location from the Fog-Node */
                                    updateLocation();

                                    /* CREATE a Location of current GPS */
                                    Location location = new Location( "gps" );
                                    location.setTime( System.currentTimeMillis() );
                                    location.setLatitude( nLatitude );
                                    location.setLongitude( nLongitude );
                                    location.setAccuracy( ( float ) 21.0 );

                                    /* CALL LocationListener.onLocationChanged to renew the location */
                                    // XposedBridge.log( AndroidAppHelper.currentPackageName() + " -> [GPS] onLocationChanged" );
                                    LocationListener locationListener = ( LocationListener ) param.args[count];
                                    XposedHelpers.callMethod( locationListener, "onLocationChanged", location );

                                    /* SET result of default activity to NULL */
                                    param.setResult( null );
                                }
                            }
                        }
                    }
                }
            });

            /* HOOK LocationManager.getProvider */
            XposedBridge.hookAllMethods( _hookClass_LocaionManager, "getProvider", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod( MethodHookParam param) throws Throwable {
                    XposedBridge.log( "[VLH GetProvider START] " + String.valueOf( System.nanoTime() ) );

                    ConnectivityManager connManager = (ConnectivityManager) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            XposedBridge.log(AndroidAppHelper.currentPackageName() + " -> { getProvider } = " + param.getResult());

                            for( int count=0 ; count<param.args.length ; count++ ) {
                                // XposedBridge.log("\t[GPS] Class of Param : " + param.args[count].getClass().getName() + ", " + param.args[count]);
                            }

                            if ( param.args[0] == "gps" ) {
                                /* GET ProviderProperties's constructor & New a instance */
                                Constructor<?> mProviderPropertiesConstructor =
                                        XposedHelpers.findConstructorBestMatch(
                                                _hookClass_ProviderProperties,
                                                Boolean.class, Boolean.class, Boolean.class, Boolean.class, Boolean.class,
                                                Boolean.class, Boolean.class, int.class, int.class);
                                Object mProviderProperties = mProviderPropertiesConstructor.newInstance( true, false, false, false, false, false, false, 0, 21 );

                                /* GET LocationProvider's constructor & New a instance */
                                Constructor<?> mLocationProviderConsctructor = XposedHelpers.findConstructorBestMatch(
                                        _hookClass_LocationProvider,
                                        String.class, _hookClass_ProviderProperties );
                                Object mLocationProvider = mLocationProviderConsctructor.newInstance( "gps", mProviderProperties );

                                /* SET the result of default to MY LocationProvider */
                                param.setResult( mLocationProvider );
                            }
                        }
                    }

                    XposedBridge.log( "[VLH GetProvider END] " + String.valueOf( System.nanoTime() ) );
                }
            });

            /* HOOK LocationManager.getProviders */
            XposedBridge.hookAllMethods( _hookClass_LocaionManager, "getProviders", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod( MethodHookParam param) throws Throwable {
                    XposedBridge.log( "[VLH GetProviders START] " + String.valueOf( System.nanoTime() ) );

                    ConnectivityManager connManager = ( ConnectivityManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.CONNECTIVITY_SERVICE );
                    NetworkInfo mWifi = connManager.getNetworkInfo( ConnectivityManager.TYPE_WIFI );

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            XposedBridge.log(AndroidAppHelper.currentPackageName() + " -> { getProviders } = " + param.getResult());

                            List<String> result = Arrays.asList("gps");
                            param.setResult(result);
                        }
                    }

                    XposedBridge.log( "[VLH GetProviders END] " + String.valueOf( System.nanoTime() ) );
                }
            });

            /* =================================================================================================================== */

            Class<?> _hookClass_LocationServices = XposedHelpers.findClass( "com.google.android.gms.location.LocationServices", lpparam.classLoader );
            final Class<?> _hookClass_LocationListener = XposedHelpers.findClass( "com.google.android.gms.location.LocationListener", lpparam.classLoader );

            // XposedBridge.log( "LocationServices Class Found in : " + lpparam.processName );

            /* Get FusedLocationApi Object for further function calls */
            Object obj = XposedHelpers.getStaticObjectField( _hookClass_LocationServices, "FusedLocationApi" );

            /* HOOK LocationServices.FusedLocationApi.getLastLocation */
            XposedBridge.hookAllMethods( obj.getClass(), "getLastLocation", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod( MethodHookParam param ) throws Throwable {
                    ConnectivityManager connManager = (ConnectivityManager) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            // XposedBridge.log( AndroidAppHelper.currentPackageName() + " -> getLastLocation" );

                            /* CREATE a Location of current GPS */
                            Location location = new Location( "gps" );
                            location.setTime( System.currentTimeMillis() );
                            location.setLatitude( nLatitude );
                            location.setLongitude( nLongitude );
                            location.setAccuracy( ( float ) 22.0 );

                            /* SET result of default activity to NEW location */
                            param.setResult( location );

                            // XposedBridge.log( "\t[Current Location] " + String.valueOf(nLatitude) + ", " + String.valueOf(nLongitude) );
                        }
                    }
                }
            });

            /* HOOK LocationServices.FusedLocationApi.requestLocationUpdates */
            XposedBridge.hookAllMethods( obj.getClass(), "requestLocationUpdates", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod( MethodHookParam param ) throws Throwable {
                    XposedBridge.log( "[VLH GMS LocationUpdate START] " + String.valueOf( System.nanoTime() ) );

                    ConnectivityManager connManager = (ConnectivityManager) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if( mWifi.isConnected() ) {
                        WifiManager mWifiManager = ( WifiManager ) AndroidAppHelper.currentApplication().getApplicationContext().getSystemService( Context.WIFI_SERVICE );
                        WifiInfo info = mWifiManager.getConnectionInfo();

                        if( info.getSSID().contains( "FogNode_1" ) ) {
                            for( Object arg : param.args ) {
                                String className = arg.getClass().getName();

                                if ( !className.startsWith( "com.google.android.gms.internal" ) &&
                                        !className.equals( "com.google.android.gms.location.LocationRequest" ) &&
                                        !className.equals( "android.os.looper" ) ) {

                                    XposedBridge.log( AndroidAppHelper.currentPackageName() + " -> [GMS] requestLocationUpdates" );

                                    XposedBridge.log( "[VLH GMS LocationUpdate GET] " + String.valueOf( System.nanoTime() ) );

                                    /* GET location from the Fog-Node */
                                    updateLocation();

                                    XposedBridge.log( "[VLH GMS LocationUpdate ENCAP] " + String.valueOf( System.nanoTime() ) );

                                    /* CREATE a Location of current GPS */
                                    Location location = new Location( "gps" );
                                    location.setTime( System.currentTimeMillis() );
                                    location.setLatitude( nLatitude );
                                    location.setLongitude( nLongitude );
                                    location.setAccuracy( ( float ) 21.0 );

                                    XposedBridge.log( "[VLH GMS LocationUpdate END] " + String.valueOf( System.nanoTime() ) );

                                    /* CALL LocationListener.onLocationChanged to renew the location */
                                    // XposedBridge.log( AndroidAppHelper.currentPackageName() + " -> [GMS] onLocationChanged" );
                                    XposedHelpers.callMethod( param.args[2], "onLocationChanged", location );

                                    /* SET result of default activity to NULL */
                                    param.setResult( null );
                                }
                            }
                        }
                    }
                }
            });

        } catch ( XposedHelpers.ClassNotFoundError ignored ) {

        }
    }
}
