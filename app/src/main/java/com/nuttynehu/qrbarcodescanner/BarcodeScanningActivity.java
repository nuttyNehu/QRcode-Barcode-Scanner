package com.nuttynehu.qrbarcodescanner;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
/*import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.barcode.Barcode;*/
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.nuttynehu.qrbarcodescanner.utilities.CheckNetwork;
import com.nuttynehu.qrbarcodescanner.utilities.NetworkUtils;

import java.io.IOException;
import java.lang.reflect.Field;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

@SuppressWarnings("deprecation")
public class BarcodeScanningActivity extends BaseActivity implements View.OnClickListener, CheckNetwork.NetworkConnectivityChangeListener {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    //This class provides methods to play DTMF tones
    private ToneGenerator toneGen1;
    private ImageView imgLine;
    private Button btnClose, btnTorch;
    private RelativeLayout layoutSurfaceViewMin, layout_btns;
    private LinearLayout blankLayout;
    private Context context;
    private Camera mCamera;
    private Camera.Parameters parameters;
    private static final String TAG = "BarcodeScanningActivity";
    private boolean isDown;
    private boolean torchON;
    private ObjectAnimator translateDownAnimation, translateUpAnimation;
    private ScaleAnimation scaleDownAnimation, scaleUpAnimation;
    private TextView txtInternet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        windowInitialization();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.barcodescanning_activity);
        try {
            initView();
            startScanning();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void windowInitialization() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {

            //noinspection deprecation
            if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
                //noinspection deprecation
                setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true);
            }
            if (Build.VERSION.SDK_INT >= 19) {
                //noinspection deprecation
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
            //make fully Android Transparent Status bar
            if (Build.VERSION.SDK_INT >= 21) {
                //noinspection deprecation
                setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
        }

    }

    private void initView() {
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        surfaceView = findViewById(R.id.surface_view);
        txtInternet = findViewById(R.id.txt_internet);
        imgLine = findViewById(R.id.imgLine);
        btnClose = findViewById(R.id.btn_close);
        btnTorch = findViewById(R.id.btn_torch);
        layoutSurfaceViewMin = findViewById(R.id.layout_surface_view_min);
        blankLayout = findViewById(R.id.blank_layout);
        blankLayout.bringToFront();
        imgLine.bringToFront();
        blankLayout.invalidate();
        imgLine.invalidate();
        txtInternet.bringToFront();
        txtInternet.invalidate();
        layoutSurfaceViewMin.invalidate();
        context = BarcodeScanningActivity.this;
        btnClose.setOnClickListener(this);
        btnTorch.setOnClickListener(this);

        if(alertDialogBuilder!=null) {
            alertDialogBuilder.setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    startScanning();
                    try {
                        checkPermissions();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            });
        }
        CheckNetwork network = new CheckNetwork(this);
        network.registerNetworkCallback();
    }

    private void startScanning(){
        if(NetworkUtils.isConnected(context))
            initialiseDetectorsAndSources();
        else {
            txtInternet.setVisibility(View.VISIBLE);
            txtInternet.setBackgroundColor(getColor(R.color.red));
            txtInternet.setText(getString(R.string.no_internet_connection));
            showToastMessage(getString(R.string.internet_connection_needed));
        }
    }

    private void checkPermissions() throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            boolean permissionDeclined = false;
            for (String s : permissions)
                if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED)
                    permissionDeclined = true;

            if (permissionDeclined) {
                requestPermissions(permissions, CAMERA_PERMISSION_CODE);
            } else {
                cameraSource.start(surfaceView.getHolder());
            }
        } else
            cameraSource.start(surfaceView.getHolder());
    }

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {

        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0, length = permissions.length; i < length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        try {
                            startCameraSource();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
                                !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            String message = getString(R.string.permission_required_dialog_title);
                            showMessageOKDialog(message,
                                    (dialog, which) -> {
                                        dialog.dismiss();
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);

                                    });
                        } else {
                            showMessageOKDialog(context.getString(R.string.permission_denial_title), (dialog, which) -> {
                                dialog.dismiss();
                                try {
                                    checkPermissions();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            }

        }
    }

    private void startTranslateDownAnimation(View view) {
        translateDownAnimation = ObjectAnimator.ofFloat(view, "scaleY", 1f);
        translateDownAnimation.setDuration(2000);
        translateDownAnimation.setRepeatMode(ValueAnimator.REVERSE);
        translateDownAnimation.setRepeatCount(ValueAnimator.INFINITE);
        translateDownAnimation.start();
    }

    private void startTranslateUpAnimation(View view) {
        ObjectAnimator animation = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1f);
        animation.setDuration(2000);
        animation.start();
    }

    private void startScaleDownAnimation(View view) {
        scaleDownAnimation = new ScaleAnimation(1, 1, 0, 1);
        scaleDownAnimation.setDuration(2000);
        scaleDownAnimation.setRepeatCount(Animation.INFINITE);
        scaleDownAnimation.setRepeatMode(Animation.REVERSE);
        view.startAnimation(scaleDownAnimation);
    }

    private void startScaleUpAnimation(View view) {
        scaleUpAnimation = new ScaleAnimation(1, 1, 1, 0);
        scaleUpAnimation.setDuration(2000);
//        scaleUpAnimation.start();
        scaleUpAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isDown) {
                    startScaleDownAnimation(view);
                    //startTranslateDownAnimation(imgLine);
                }
                isDown = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(scaleUpAnimation);

    }

    private void initialiseDetectorsAndSources() {
        startScaleDownAnimation(blankLayout);
        Toast.makeText(getApplicationContext(), "QR code/ Barcode scanner started", Toast.LENGTH_SHORT).show();

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();
        if (!barcodeDetector.isOperational()) {
            Toast.makeText(this, getString(R.string.internet_connection_needed), Toast.LENGTH_SHORT).show();
            finish();
        }
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();


        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    checkPermissions();

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                //Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {

                    new Thread(() -> {
                        playSound();
                        if(barcodeDetector!=null)
                            barcodeDetector.release();

                        for (int i = 0; i < barcodes.size(); i++) {
                            Barcode barcode = barcodes.valueAt(i);
                            int type = barcode.valueFormat;
                            handler.sendMessage(Message.obtain(handler, type, barcode));
                        }
                    }).start();

                }
            }

            Handler handler = new Handler(message -> {
                clearAllAnimation();
                if(cameraSource!=null) {
                    cameraSource.stop();
                }
                Intent intent = null;
                Barcode barcode = (Barcode) message.obj;
                switch (message.what) {
                    case Barcode.PHONE:
                        try {
                            String number = barcode.phone.number;
                            Uri u = Uri.parse("tel:" + number);
                            intent = new Intent(Intent.ACTION_DIAL, u);
                            startActivity(intent);
                            //finish();
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case Barcode.EMAIL:
                        try {
                            intent = new Intent(Intent.ACTION_SEND);
                            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{barcode.email.address});
                            intent.putExtra(Intent.EXTRA_SUBJECT, barcode.email.subject);
                            intent.putExtra(Intent.EXTRA_TEXT, barcode.email.body);
                            intent.setType("message/rfc822");
                            startActivity(intent);
                            //finish();
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case Barcode.URL:
                        showListAlertDialog(context.getString(R.string.url_received),context.getResources().getStringArray(R.array.dialog_url_choices), (dialog, which) -> {
                            dialog.dismiss();
                            switch (which) {
                                case 0:
                                    try {
                                        String url = barcode.displayValue;
                                        final Intent intent2 = new Intent(Intent.ACTION_VIEW);
                                        intent2.setData(Uri.parse(url));
                                        startActivity(intent2);
                                    } catch (Exception e) {
                                        Log.d(TAG, e.toString());
                                    }
                                    break;

                                case 1:
                                    copyToClipboard(context,"URL",barcode.displayValue);
                                    startScanning();
                                    try {
                                        checkPermissions();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case 2:
                                    showResultInActivity(barcode);
                                    break;
                            }
                        });

                        break;
                    case Barcode.GEO:
                        try {
                            String geo = "geo:" + barcode.geoPoint.lat + "," + barcode.geoPoint.lng;
                            Uri gmmIntentUri = Uri.parse(geo);
                            intent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            intent.setPackage("com.google.android.apps.maps");
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                                //finish();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case Barcode.SMS:
                        try {
                            String number = barcode.sms.phoneNumber;
                            Uri uri = Uri.fromParts("sms", number, null);
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.putExtra("sms_body", barcode.sms.message);
                            startActivity(intent);
                            //finish();
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case Barcode.WIFI:
                    case Barcode.DRIVER_LICENSE:
                        try {
                            showResultInActivity(barcode);
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case Barcode.CALENDAR_EVENT:
                        try {
                            intent = new Intent(Intent.ACTION_EDIT);
                            intent.setType("vnd.android.cursor.item/event");
                            String title = barcode.calendarEvent.summary;
                            String startTime = String.valueOf(barcode.calendarEvent.start);
                            String endTime = String.valueOf(barcode.calendarEvent.end);
                            String strDescription = String.valueOf(barcode.calendarEvent.description);
                            intent.putExtra(CalendarContract.Events.TITLE, title);
                            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                    startTime);
                            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                                    endTime);
                            intent.putExtra(CalendarContract.Events.ALL_DAY, false);// periodicity
                            intent.putExtra(CalendarContract.Events.DESCRIPTION, strDescription);
                            startActivity(intent);
                            //finish();
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case Barcode.CONTACT_INFO:
                        try {
                            intent = new Intent((ContactsContract.Intents.Insert.ACTION));
                            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                            intent.putExtra(ContactsContract.Intents.Insert.NAME, barcode.contactInfo.name.formattedName);
                            if (barcode.contactInfo.phones != null && barcode.contactInfo.phones.length > 0 && barcode.contactInfo.phones[0] != null && barcode.contactInfo.phones[0].number != null)
                                intent.putExtra(ContactsContract.Intents.Insert.PHONE, barcode.contactInfo.phones[0].number);
                            if (barcode.contactInfo.emails != null && barcode.contactInfo.emails.length > 0 && barcode.contactInfo.emails[0] != null && barcode.contactInfo.emails[0].address != null)
                                intent.putExtra(ContactsContract.Intents.Insert.EMAIL, barcode.contactInfo.emails[0].address);
                            intent.putExtra(ContactsContract.Intents.Insert.COMPANY, barcode.contactInfo.organization);
                            startActivity(intent);
                            //finish();
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                        break;
                    case Barcode.PRODUCT:
                        showListAlertDialog(context.getString(R.string.product_received),context.getResources().getStringArray(R.array.dialog_text_choices), (dialog, which) -> {
                            dialog.dismiss();
                            switch (which) {
                                case 0:
                                    try {
                                        final Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
                                        i.putExtra(SearchManager.QUERY, barcode.rawValue);
                                        startActivity(i);
                                    } catch (Exception e) {
                                        Log.i(TAG, e.toString());
                                    }
                                    break;
                                case 1:
                                    showResultInActivity(barcode);
                                    break;
                            }
                            //finish();
                        });

                        break;
                    case Barcode.TEXT:
                        showListAlertDialog(getString(R.string.received_text_result),context.getResources().getStringArray(R.array.dialog_text_choices), (dialog, which) -> {
                            dialog.dismiss();
                            switch (which) {
                                case 0:
                                    try {
                                        final Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
                                        i.putExtra(SearchManager.QUERY, barcode.rawValue);
                                        startActivity(i);

                                    } catch (Exception e) {
                                        Log.i(TAG, e.toString());
                                    }
                                    break;
                                case 1:
                                    showResultInActivity(barcode);
                                    break;
                            }
                            //finish();
                        });

                        break;
                    case Barcode.ISBN:
                        showListAlertDialog(context.getString(R.string.ISBN_Recieved),context.getResources().getStringArray(R.array.dialog_text_choices), (dialog, which) -> {
                            dialog.dismiss();
                            switch (which) {
                                case 0:
                                    try {
                                        final Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
                                        i.putExtra(SearchManager.QUERY, barcode.rawValue);
                                        startActivity(i);

                                    } catch (Exception e) {
                                        Log.i(TAG, e.toString());
                                    }
                                    break;
                                case 1:
                                    showResultInActivity(barcode);
                                    break;
                            }
                            //finish();
                        });

                        break;
                    default:
                        showResultInActivity(barcode);
                        break;

                }
                return false;
            });
        });
    }

    private void showResultInActivity(Barcode barcode) {
        Intent intent = new Intent(this, QRBarCodeDetailsActivity.class);
        intent.putExtra("mBarCode", barcode);
        startActivity(intent);
//        finish();
    }

    private void playSound() {
        toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
    }


    public void startCameraSource() throws IOException {
        if (ActivityCompat.checkSelfPermission(BarcodeScanningActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraSource.start(surfaceView.getHolder());
        } else {
            ActivityCompat.requestPermissions(BarcodeScanningActivity.this, new
                    String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @SuppressWarnings("deprecation")
    public static Camera getCamera(@androidx.annotation.NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (IllegalAccessException e) {
                    Log.d(TAG, e.toString());
                }

                break;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public void turnOnFlashLight() {
        try {
            mCamera = getCamera(cameraSource);
            if (mCamera != null) {
                parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    @SuppressWarnings("deprecation")
    public void turnOffFlashLight() {
        try {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    public void slideUp(View view){
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                view.getHeight(),  // fromYDelta
                0);                // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void slideDown(View view){
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                0,                 // fromYDelta
                view.getHeight()); // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraSource!=null)
            cameraSource.release();
        clearAllAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScanning();
/*
        try {
            checkPermissions();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    private void clearAllAnimation() {
        if(blankLayout!=null && blankLayout.getAnimation()!=null) {
            blankLayout.getAnimation().cancel();
            blankLayout.clearAnimation();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_close:
                finish();
                break;
            case R.id.btn_torch:
                if (torchON) {
                    btnTorch.setBackground(getDrawable(R.drawable.ic_torch_off));
                    turnOffFlashLight();
                    torchON = false;
                } else {
                    btnTorch.setBackground(getDrawable(R.drawable.ic_torch_on));
                    turnOnFlashLight();
                    torchON = true;
                }
                break;
        }
    }

    @Override
    public void onNetworkAvailable() {
        runOnUiThread(() -> {
            if(txtInternet.getVisibility()==View.VISIBLE) {
                txtInternet.setBackgroundColor(getResources().getColor(R.color.dark_green));
                txtInternet.setText(getString(R.string.internet_available));
                startScanning();
                try {
                    checkPermissions();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new CountDownTimer(3000, 1000) {

                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        slideDown(txtInternet);
                    }
                }.start();
            }
        });

    }

    @Override
    public void onNetworkLost() {
        runOnUiThread(() -> {
            slideUp(txtInternet);
            txtInternet.setBackgroundColor(getResources().getColor(R.color.red));
            txtInternet.setText(getString(R.string.no_internet_connection));
        });

    }
}
