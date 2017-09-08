package com.example.yylou.vlh;

/* Install Permission */
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/* Speech Intent */
import android.os.Bundle;
import java.util.Set;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/* Save Stream To File */
import java.io.IOException;

/* XPosed Framework */
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;

/* Socket Connection */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.InetAddress;

/* Utility */
import android.app.Activity;
import android.app.AndroidAppHelper;
import static android.app.Activity.RESULT_OK;
import static android.support.wearable.input.RemoteInputIntent.ACTION_REMOTE_INPUT;

/**
 * Created by yylou on 2017/3/2.
 */

public class _Speech implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private Socket mSocket;

    private List<String> grantPermissionPackageList = new ArrayList<>();

    private static final String PERM_MOD_AUDIO ="android.permission.MODIFY_AUDIO_SETTINGS";
    private static final String PERM_RECORD_AUDIO ="android.permission.RECORD_AUDIO";
    private static final String PERM_INTERNET = "android.permission.INTERNET";
    private static final String PERM_ACCESS_INTERNET = "android.permission.ACCESS_NETWORK_STATE";
    private static final String PERM_WIFI_STATE = "android.permission.ACCESS_WIFI_STATE";

    private VoiceRecorder mVoiceRecorder;
    private DataOutputStream SocketOutputStream;

    private String mSpeechResult;

    /* Elapsed Time */
    long tStart, tEnd;
    double tDelta;

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {
        @Override
        public void onVoiceStart() {
            // XposedBridge.log( "[Start recording]");
        }

        @Override
        public void onVoice( byte[] data, int size ) {
            try {
                /* Send audio to Access Point to do recognition */
                SocketOutputStream = new DataOutputStream( mSocket.getOutputStream() );
                SocketOutputStream.writeInt( data.length );
                SocketOutputStream.write( data );
            } catch ( IOException e ) {
                XposedBridge.log( e );
            }
        }

        @Override
        public void onVoiceEnd() {
            // XposedBridge.log( "[End recording]" );

            if( mVoiceRecorder != null ) {
                /* Stop the voice recorder */
                mVoiceRecorder.stop();
                mVoiceRecorder = null;

                /* Close the socket outputStream */
                try {
                    String sEND = "END";
                    byte[] bEND = sEND.getBytes( "UTF-8" );
                    SocketOutputStream.writeInt( bEND.length );
                    SocketOutputStream.write( bEND );
                    SocketOutputStream.flush();

                    /* Receive the recognition result */
                    DataInputStream SocketInputStream = new DataInputStream( mSocket.getInputStream() );
                    mSpeechResult = SocketInputStream.readLine();

                } catch ( Exception e ) {
                    XposedBridge.log( e );
                }
            }
        }
    };

    private void InitializeServer() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress IP = InetAddress.getByName( "192.168.5.1" );
                    int Port = 15566;
                    mSocket = new Socket( IP, Port );
                } catch ( Exception e ) {
                    XposedBridge.log( e );
                }
            }
        }).start();
    }

    @Override
    public void handleLoadPackage( final XC_LoadPackage.LoadPackageParam lpparam ) {
        /* Find the 3rd-party applications then GRANT PERMISSION */
        findApplication();

        try {
            Class<?> _hookClass_PackageManagerService = XposedHelpers.findClass( "com.android.server.pm.PackageManagerService", lpparam.classLoader );

            // XposedBridge.log( "PackageManagerService Class Found in : " + lpparam.packageName );

            /* HOOK PackageManagerService.grantPermissionsLPw of RECORD_AUDIO */
            XposedBridge.hookAllMethods( _hookClass_PackageManagerService, "grantPermissionsLPw", new XC_MethodHook() {
                @SuppressWarnings("unchecked")
                @Override
                protected void afterHookedMethod( MethodHookParam param ) throws Throwable {
                    final String PackageName = ( String ) XposedHelpers.getObjectField( param.args[0], "packageName" );
                    for( String pkgName : grantPermissionPackageList ) {
                        if( PackageName.contentEquals( pkgName ) ) {
                            // XposedBridge.log( pkgName + " -> grantPermissionsLPw" );

                            Object mExtras = XposedHelpers.getObjectField( param.args[0], "mExtras" );
                            Object mPermissionState = XposedHelpers.callMethod( mExtras, "getPermissionsState" );
                            List<String> mPermissionList = ( List<String> ) XposedHelpers.getObjectField( param.args[0], "requestedPermissions" );
                            Object mSettings = XposedHelpers.getObjectField( param.thisObject, "mSettings" );
                            Object mPermissions = XposedHelpers.getObjectField( mSettings, "mPermissions" );

                            if( !mPermissionList.contains( PERM_MOD_AUDIO ) ) {
                                Object pModifyAudio = XposedHelpers.callMethod( mPermissions, "get", PERM_MOD_AUDIO );
                                XposedHelpers.callMethod( mPermissionState, "grantInstallPermission", pModifyAudio );
                            }

                            if( !mPermissionList.contains( PERM_RECORD_AUDIO ) ) {
                                Object pRecordAudio = XposedHelpers.callMethod( mPermissions, "get", PERM_RECORD_AUDIO );
                                XposedHelpers.callMethod( mPermissionState, "grantInstallPermission", pRecordAudio );
                            }

                            if( !mPermissionList.contains( PERM_INTERNET ) ) {
                                Object pInternet = XposedHelpers.callMethod( mPermissions, "get", PERM_INTERNET );
                                XposedHelpers.callMethod( mPermissionState, "grantInstallPermission", pInternet );
                            }

                            if( !mPermissionList.contains( PERM_ACCESS_INTERNET ) ) {
                                Object pAccessInternet = XposedHelpers.callMethod( mPermissions, "get", PERM_ACCESS_INTERNET );
                                XposedHelpers.callMethod( mPermissionState, "grantInstallPermission", pAccessInternet );
                            }

                            if( !mPermissionList.contains( PERM_WIFI_STATE ) ) {
                                Object pWifiState = XposedHelpers.callMethod( mPermissions, "get", PERM_WIFI_STATE );
                                XposedHelpers.callMethod( mPermissionState, "grantInstallPermission", pWifiState );
                            }
                        }
                    }
                }
            });
        } catch ( XposedHelpers.ClassNotFoundError ignored ) {

        }

        try {
            final Class<?> _HOOKClass_Instrumentation = XposedHelpers.findClass( "android.app.Instrumentation", lpparam.classLoader ); /* execStartActivity */
            final Class<?> _HOOKClass_Activity        = XposedHelpers.findClass( "android.app.Activity",        lpparam.classLoader ); /* startActivityForResult */

            /* HOOK Instrumentation.execStartActivity */
            XposedBridge.hookAllMethods( _HOOKClass_Instrumentation, "execStartActivity", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod( MethodHookParam param ) throws Throwable {
                    XposedBridge.log( "[VLH SpeechRecognition START] " + String.valueOf( System.nanoTime() ) );

                    Intent InputIntent = ( Intent )param.args[4];
                    Intent SpeechRecog = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH );
                    Intent RemoteInput = new Intent( ACTION_REMOTE_INPUT );

                    /* HOOK Wearable Speech Recognition approach - RecognizeSpeech (Typical) */
                    if( InputIntent.filterEquals( SpeechRecog ) ) {
                        /* LOG the detail of API call */
                        // XposedBridge.log( "[" + AndroidAppHelper.currentPackageName() + "] -> execStartActivity" );
                        for( int count=0 ; count<param.args.length ; count++ ) {
                            // XposedBridge.log( "\t[Param] " + param.args[count] );
                            if( param.args[count] instanceof Intent ) {
                                IntentInformation( ( Intent ) param.args[count] );
                            }
                        }

                        /* START a new thread to Initialize socket server */
                        InitializeServer();

                        /* START audio recording */
                        if( mVoiceRecorder != null ) {
                            mVoiceRecorder.stop();
                        }

                        XposedBridge.log( "[VLH SpeechRecognition RECORD] " + String.valueOf( System.nanoTime() ) );

                        mVoiceRecorder = new VoiceRecorder( mVoiceCallback );
                        mVoiceRecorder.start();
                        mVoiceRecorder.mThread.join();

                        XposedBridge.log( "[VLH SpeechRecognition ENCAP] " + String.valueOf( System.nanoTime() ) );

                        // XposedBridge.log( "\t[Result] " + mSpeechResult );

                        /* According to RecognizeSpeech's original result
                           CREATE the stuff which needs in onActivityResult */
                        int requestCode = ( int ) param.args[5];
                        int answerCode  = RESULT_OK;
                        Intent ResultIntent = new Intent();
                        ArrayList<String> mResult = new ArrayList<String>();
                        mResult.add( mSpeechResult );
                        ResultIntent.putExtra( "android.speech.extra.RESULTS", mResult );
                        float mConfidenceScores [] = { 1.0f };
                        ResultIntent.putExtra( "android.speech.extra.CONFIDENCE_SCORES", mConfidenceScores );

                        XposedBridge.log( "[VLH SpeechRecognition END] " + String.valueOf( System.nanoTime() ) );

                        /* CALL Activity.onActivityResult to do the response */
                        XposedHelpers.callMethod( param.args[0], "onActivityResult", requestCode, answerCode, ResultIntent );

                        /* SET result of default activity to NULL */
                        param.setResult( null );

                    /* HOOK Wearable Speech Recognition approach - RemoteInput (Google Hangout) */
                    } else if( InputIntent.filterEquals( RemoteInput ) ) {
                        /* LOG the detail of API call */
                        // XposedBridge.log( "[" + AndroidAppHelper.currentPackageName() + "] -> execStartActivity" );
                        for( int count=0 ; count<param.args.length ; count++ ) {
                            // XposedBridge.log( "\t[Param] " + param.args[count] );
                            if( param.args[count] instanceof Intent ) {
                                IntentInformation( ( Intent ) param.args[count] );
                            }
                        }

                        /* START a new thread to Initialize socket server */
                        InitializeServer();

                        /* START audio recording */
                        if( mVoiceRecorder != null ) {
                            mVoiceRecorder.stop();
                        }

                        mVoiceRecorder = new VoiceRecorder( mVoiceCallback );
                        mVoiceRecorder.start();
                        mVoiceRecorder.mThread.join();

                        // XposedBridge.log( "\t[Result] " + mSpeechResult );

                        /* According to RemoteInput's original result
                           CREATE the stuff which needs in onActivityResult */
                        int requestCode = ( int ) param.args[5];
                        int answerCode  = RESULT_OK;
                        Intent ResultIntent = new Intent();

                        /* ClipDescription ( ClipData ) */
                        String mMIMEType [] = { "text/vnd.android.intent"  };
                        ClipDescription mClipDescription = new ClipDescription( "android.remoteinput.results",  mMIMEType );

                        /* Intent [Bundle] (ClipDataItem) */
                        Intent ClipDataIntent = new Intent();
                        Bundle mBundle = new Bundle();
                        mBundle.putString( "android.intent.extra.TEXT", mSpeechResult );
                        ClipDataIntent.putExtra( "android.remoteinput.resultsData", mBundle );

                        /* ClipDataItem [Intent] ( ClipData ) */
                        ClipData.Item mClipDataItem = new ClipData.Item( ClipDataIntent );

                        /* ClipData ( FINAL Intent ) */
                        ClipData mClipData = new ClipData( mClipDescription, mClipDataItem );

                        ResultIntent.setClipData( mClipData );

                        /* CALL Activity.onActivityResult to do the response */
                        XposedHelpers.callMethod( param.args[0], "onActivityResult", requestCode, answerCode, ResultIntent );

                        /* SET result of default activity to NULL */
                        param.setResult( null );
                    }
                }
            });

            /* HOOK Activity.startActivityForResult */
            // XposedBridge.hookAllMethods( _HOOKClass_Activity, "startActivityForResult", new XC_MethodHook() {
            //     @Override
            //     protected void afterHookedMethod( MethodHookParam param ) throws Throwable {
            //         Intent InputIntent = ( Intent )param.args[0];
            //         Intent SpeechRecog = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH );
            //         Intent RemoteInput = new Intent( ACTION_REMOTE_INPUT );

            //         /* LOG the detail of API call */
            //         if( InputIntent.filterEquals( SpeechRecog ) || InputIntent.filterEquals( RemoteInput ) ){
            //             /* LOG the detail of API call */
            //             // XposedBridge.log( "[" + AndroidAppHelper.currentPackageName() + "] -> startActivityForResult" );
            //             for( int count=0 ; count<param.args.length ; count++ ){
            //                 // XposedBridge.log( "\t[Param] " + param.args[count] );
            //                 if( param.args[count] instanceof Intent ){
            //                     IntentInformation( ( Intent ) param.args[count] );
            //                 }
            //             }
            //         }
            //     }
            // });

            /* HOOK Activity.onActivityResult */
            // XposedBridge.hookAllMethods( _HOOKClass_Activity, "onActivityResult", new XC_MethodHook() {
            //     @Override
            //     protected void afterHookedMethod( MethodHookParam param ) throws Throwable {
            //         // XposedBridge.log( "[" + AndroidAppHelper.currentPackageName() + "] -> onActivityResult" );
                    
            //         /* LOG the detail of API call */
            //         for( int count=0 ; count<param.args.length ; count++ ) {
            //             // XposedBridge.log( "\t[Param] " + param.args[count] );
            //             if( param.args[count] instanceof Intent ) {
            //                 Intent intent = ( Intent ) param.args[count];
            //                 IntentInformation( intent );

            //                 ClipData mClipData = intent.getClipData();
            //                 if( mClipData != null ) {
            //                     for( int count1=0 ; count1<mClipData.getItemCount() ; count1++ ) {
            //                         // XposedBridge.log( "\t\t[ClipData] " + mClipData.getItemAt(count1) );
            //                         // XposedBridge.log( "\t\t[Description] " + mClipData.getDescription().toString() );
            //                         ClipData.Item item = mClipData.getItemAt(count1);
            //                         IntentInformation( item.getIntent() );
            //                     }
            //                 }
            //             }
            //         }
            //     }
            // });

        } catch ( XposedHelpers.ClassNotFoundError ignored ) {
            XposedBridge.log( ignored );
        }
    }

    @Override
    public void initZygote( IXposedHookZygoteInit.StartupParam startupParam ) throws Throwable {
        /* Check System is Android 6.0 / 5.0 */
        /* if( Build.VERSION.SDK_INT < Build.VERSION_CODES.M ) {
            XposedBridge.log( "Lollipop" );
        } else {
            XposedBridge.log( "Marshallow" );
        } */
    }

    private void findApplication() {
        if( AndroidAppHelper.currentApplication() != null ) {
            PackageManager pm = AndroidAppHelper.currentApplication().getApplicationContext().getPackageManager();
            if( pm != null ) {
                List<ApplicationInfo> packages = pm.getInstalledApplications( PackageManager.GET_META_DATA );
                for( ApplicationInfo appInfo :packages ) {
                    /* System App */
                    if( ( appInfo.flags & ApplicationInfo.FLAG_SYSTEM & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP ) != 0 ) {

                    /* 3rd-party App */
                    } else {
                        if( !appInfo.processName.equals( "system" ) && !appInfo.processName.contains( "android.process" )
                                && !appInfo.processName.contains( "com.qualcomm" ) && !appInfo.processName.contains( "com.lge" ) ) {
                            grantPermissionPackageList.add( appInfo.processName );
                        }
                    }
                }
            }
        }
    }

    /* Dump the information of a Intent */
    public static void IntentInformation( Intent i ){
        Bundle bundle = i.getExtras();
        if( bundle != null ){
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();

            while( it.hasNext() ){
                String key = it.next();
                // XposedBridge.log( "\t\t[Intent Detail] " + key + " = " + bundle.get(key) );

                if( bundle.get( key ) instanceof Bundle ){
                    Bundle tmpBundle = ( Bundle ) bundle.get( key );
                    if( tmpBundle != null ) {
                        Set<String> tmpkeys = tmpBundle.keySet();
                        Iterator<String> tmpit = tmpkeys.iterator();

                        while( tmpit.hasNext() ) {
                            String tmpkey = tmpit.next();
                            // XposedBridge.log( "\t\t\t[Bundle Detail] " + tmpkey + " = " + tmpBundle.get( tmpkey ) + " ( " + tmpBundle.get( tmpkey ).getClass() + " )" );
                        }
                    }
                }
            }
        }
    }
}
