package com.nuttynehu.qrbarcodescanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    protected Context context;
    protected AlertDialog.Builder alertDialogBuilder;
    protected AlertDialog alert;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        alertDialogBuilder = new AlertDialog.Builder(context);
    }

    protected void showMessageOKDialog(String message, DialogInterface.OnClickListener okListener) {
        if(alertDialogBuilder ==null)
            alertDialogBuilder = new AlertDialog.Builder(context);
        else {
            if(alert!=null && alert.isShowing())
                alert.dismiss();
            alertDialogBuilder
                    .setMessage(message)
                    .setPositiveButton("OK", okListener);
                    alert = alertDialogBuilder.create();
                    alert.setCanceledOnTouchOutside(false);
                    alert.show();
        }
    }


    protected void showListAlertDialog(String title,String[] items, DialogInterface.OnClickListener okListener) {
        if(alertDialogBuilder ==null)
            alertDialogBuilder = new AlertDialog.Builder(context);
        else {
            if(alert!=null && alert.isShowing())
                alert.dismiss();
            alertDialogBuilder.setTitle(title);
            alertDialogBuilder.setSingleChoiceItems(items, -1, okListener);
            alert = alertDialogBuilder.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();
        }
    }


    protected void showToastMessage(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    public void copyToClipboard(Context context, String label, String text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Text Copied", Toast.LENGTH_SHORT).show();
    }
}
