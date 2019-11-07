package com.chirag.RNMail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.support.v4.content.FileProvider;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

import java.util.List;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * NativeModule that allows JS to open emails sending apps chooser.
 */
public class RNMailModule extends ReactContextBaseJavaModule {
  private static final String TAG = RNMailModule.class.getSimpleName();
  ReactApplicationContext reactContext;

  public RNMailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNMail";
  }

  /**
   * Converts a ReadableArray to a String array
   *
   * @param r the ReadableArray instance to convert
   *
   * @return array of strings
   */
  private String[] readableArrayToStringArray(ReadableArray r) {
    int length = r.size();
    String[] strArray = new String[length];

    for (int keyIndex = 0; keyIndex < length; keyIndex++) {
      strArray[keyIndex] = r.getString(keyIndex);
    }

    return strArray;
  }

  @ReactMethod
  public void mail(ReadableMap options, Callback callback) {

    Log.d(TAG, "***********" );
    Log.d(TAG, "RNMail:mail" );
    Log.d(TAG, "***********" );

    String intentAction = Intent.ACTION_SENDTO;

    ArrayList<Uri> fileAttachmentUriList = getFileAttachmentUriList(options);
    Log.d(TAG, "FILE ATTACHMENT SIZE "+fileAttachmentUriList.size());

    if (1 <= fileAttachmentUriList.size()) {
        intentAction = Intent.ACTION_SEND_MULTIPLE;
    }

    Log.d(TAG, intentAction);
    Intent mailIntent = new Intent(intentAction);
    mailIntent.setData(Uri.parse("mailto:"));

    if (options.hasKey("subject") && !options.isNull("subject")) {
      mailIntent.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
    }

    if (options.hasKey("body") && !options.isNull("body")) {
      String body = options.getString("body");
      if (options.hasKey("isHTML") && options.getBoolean("isHTML")) {
        mailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
      } else {
        mailIntent.putExtra(Intent.EXTRA_TEXT, body);
      }
    }

    if (options.hasKey("recipients") && !options.isNull("recipients")) {
      ReadableArray recipients = options.getArray("recipients");
      mailIntent.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
    }

    if (options.hasKey("ccRecipients") && !options.isNull("ccRecipients")) {
      ReadableArray ccRecipients = options.getArray("ccRecipients");
      mailIntent.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
    }

    if (options.hasKey("bccRecipients") && !options.isNull("bccRecipients")) {
      ReadableArray bccRecipients = options.getArray("bccRecipients");
      mailIntent.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
    }

     if (1 <= fileAttachmentUriList.size()) {
        // If multiple attachments setType("plain/text"), else queryIntentActivities fails
        mailIntent.setType("plain/text");
        mailIntent.putExtra(Intent.EXTRA_STREAM, fileAttachmentUriList);
    }

    PackageManager manager = reactContext.getPackageManager();
    List<ResolveInfo> list = manager.queryIntentActivities(mailIntent, 0);

    Log.d(TAG, "LIST SIZE "+list.size());

    if (list == null || list.size() == 0) {
      callback.invoke("not_available");
      return;
    }

    if (list.size() == 1) {

      mailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        reactContext.startActivity(mailIntent);
      } catch (Exception ex) {
        callback.invoke("error");
      }
    } else {
      Intent chooser = Intent.createChooser(mailIntent, "Send Mail");
      chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      try {
        reactContext.startActivity(chooser);
      } catch (Exception ex) {
        callback.invoke("error");
      }
    }
    /*
     * if (uri != null) { reactContext.revokeUriPermission(uri,
     * Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
     * Intent.FLAG_GRANT_READ_URI_PERMISSION); }
     */
  }

  private ArrayList<Uri> getFileAttachmentUriList(ReadableMap options){
      ArrayList<Uri> fileAttachmentUriList = new ArrayList<Uri>();
      if(options.hasKey("attachmentList") && !options.isNull("attachmentList")){
        
        ReadableArray attachmentList = options.getArray("attachmentList");
        int length = attachmentList.size();

        for(int i = 0; i < length; ++i) {
            ReadableMap attachmentItem = attachmentList.getMap(i);
            String path = attachmentItem.getString("path");
            Uri uri = null;

            File file = new File(path);

            String provider = reactContext.getApplicationContext().getPackageName() + ".provider";

            uri = FileProvider.getUriForFile(reactContext, provider, file);

            if(uri != null){
                fileAttachmentUriList.add(uri);
            }
        }
    }
    return fileAttachmentUriList;
  }
}
