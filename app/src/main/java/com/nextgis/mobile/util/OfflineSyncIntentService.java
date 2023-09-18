package com.nextgis.mobile.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SyncResult;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.INGWLayer;

import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.mobile.datasource.SyncAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class OfflineSyncIntentService extends IntentService {

    private static final String ACTION_OFFSYNC = "com.nextgis.mobile.util.action.OFFSYNC";

    public OfflineSyncIntentService() {
        super("OfflineSyncIntentService");
    }

    public static void startActionFoo(Context context) {
        Intent intent = new Intent(context, OfflineSyncIntentService.class);
        intent.setAction(ACTION_OFFSYNC);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_OFFSYNC.equals(action)) {
                handleActionFoo();
            }
        }
    }

    private void handleActionFoo() {

        List<Account>         mAccounts = new ArrayList<>();
        final AccountManager accountManager = AccountManager.get(getApplicationContext());
        final IGISApplication application = (IGISApplication) getApplication();
        List<INGWLayer> layers = new ArrayList<>();

        for (Account account : accountManager.getAccountsByType(application.getAccountsType())) {
            layers.clear();
            MapContentProviderHelper.getLayersByAccount(application.getMap(), account.name, layers);

            if (layers.size() > 0 )
                mAccounts.add(account);
        }

        SyncResult syncResult = new SyncResult();

        SyncAdapter syncAdapter = new SyncAdapter(getApplicationContext(), true);


        for (Account account : mAccounts){
            syncAdapter.onPerformSync(account, null, com.nextgis.mobile.util.AppSettingsConstants.AUTHORITY,
                    null, syncResult);
        }
    }

}