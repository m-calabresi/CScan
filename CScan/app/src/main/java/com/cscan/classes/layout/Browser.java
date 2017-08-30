package com.cscan.classes.layout;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.v4.content.res.ResourcesCompat;

import com.cscan.R;
import com.cscan.classes.CustomTabsBroadcastReceiver;
import com.cscan.classes.Info;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Browser {

    private static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";

    //private static final int BROWSER_TYPE_IN_APP = 0;
    private static final int BROWSER_TYPE_DEFAULT = 1;

    private static final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
    private static final String HTTP_HEADER = "http://";
    private static final String HTTPS_HEADER = "http://";
    private static final String FTP_HEADER = "ftp://";

    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private CustomTabsClient mCustomTabsClient;
    private CustomTabsIntent customTabsIntent;

    private Context context;

    public Browser(Context context){
        this.context = context;
    }

    public void bindCustomTabsService() {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        Intent intent = new Intent(context, CustomTabsBroadcastReceiver.class); //copy link action
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setToolbarColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPrimary, null));
        builder.setShowTitle(true);
        //back arrow icon - NOT WORKING
        /*builder.setCloseButtonIcon(BitmapFactory.decodeResource(
                getResources(), R.drawable.ic_arrow_back));*/
        builder.addMenuItem(context.getString(R.string.action_copy_link), pendingIntent);

        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName,
                                                     CustomTabsClient customTabsClient) {
                mCustomTabsClient = customTabsClient;
                mCustomTabsClient.warmup(0);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mCustomTabsClient = null;
            }
        };

        if (!CustomTabsClient.bindCustomTabsService(
                context, CUSTOM_TAB_PACKAGE_NAME, mCustomTabsServiceConnection))
            mCustomTabsServiceConnection = null;
        customTabsIntent = builder.build();
    }

    public void unbindCustomTabsService() {
        if (mCustomTabsServiceConnection == null) return;
        context.unbindService(mCustomTabsServiceConnection);
        mCustomTabsClient = null;
    }

    public void openLink(Info info, int browserType) {
        String link = info.getText();
        Uri url;
        //check for syntax URI error
        link = toLink(link);
        //open link
        url = Uri.parse(link);

        if(browserType == BROWSER_TYPE_DEFAULT){
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
            context.startActivity(browserIntent);
        } else{
            customTabsIntent.launchUrl(context, url);
        }
    }

    public boolean isURI(String str) {
        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(str);

        return m.find();
    }

    private String toLink(String str) {
        if (!str.startsWith(HTTP_HEADER) &&
                !str.startsWith(HTTPS_HEADER) &&
                !str.startsWith(FTP_HEADER))
            str = HTTP_HEADER + str;

        return str;
    }
}
