package com.chirag.RNMail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Html;
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
    String intentAction = Intent.ACTION_SENDTO;

    ArrayList<Uri> fileAttachmentUriList = getFileAttachmentUriList(options);
    if (1 <= fileAttachmentUriList.size()) {
       intentAction = Intent.ACTION_SEND_MULTIPLE;
    }

    Intent i = new Intent(intentAction);
    i.setData(Uri.parse("mailto:"));

    if (options.hasKey("subject") && !options.isNull("subject")) {
      i.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
    }

    if (options.hasKey("body") && !options.isNull("body")) {
      String body = options.getString("body");
      if (options.hasKey("isHTML") && options.getBoolean("isHTML")) {
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
      } else {
        i.putExtra(Intent.EXTRA_TEXT, body);
      }
    }

    if (options.hasKey("recipients") && !options.isNull("recipients")) {
      ReadableArray recipients = options.getArray("recipients");
      i.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
    }

    if (options.hasKey("ccRecipients") && !options.isNull("ccRecipients")) {
      ReadableArray ccRecipients = options.getArray("ccRecipients");
      i.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
    }

    if (options.hasKey("bccRecipients") && !options.isNull("bccRecipients")) {
      ReadableArray bccRecipients = options.getArray("bccRecipients");
      i.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
    }

    for(i = 0; i < fileAttachmentUriList.size(); i++){
        Uri uri = fileAttachmentUriList.get(i);
        List<ResolveInfo> resolvedIntentActivities = reactContext.getPackageManager().queryIntentActivities(i,
            PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
          String packageName = resolvedIntentInfo.activityInfo.packageName;
          reactContext.grantUriPermission(packageName, uri,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.putExtra(Intent.EXTRA_STREAM, uri);
    }

    PackageManager manager = reactContext.getPackageManager();
    List<ResolveInfo> list = manager.queryIntentActivities(i, 0);

    if (list == null || list.size() == 0) {
      callback.invoke("not_available");
      return;
    }

    if (list.size() == 1) {

      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        reactContext.startActivity(i);
      } catch (Exception ex) {
        callback.invoke("error");
      }
    } else {
      Intent chooser = Intent.createChooser(i, "Send Mail");
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
