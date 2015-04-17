
package info.guardianproject.panic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PanicTrigger {

    public static final String PREF_RECEIVER_PACKAGE_NAMES = "panicReceiverPackageNames";

    /**
     * Checks whether the provided {@link Activity} was started with the action
     * {@link Panic.ACTION_DISCONNECT}, and if so, processes that {@link Intent}
     * , removing the sending app as the panic trigger if it is currently
     * configured to be so.
     *
     * @param activity the {@code Activity} to check for the {@code Intent}
     * @return whether an {@code ACTION_DISCONNECT Intent} was received
     */
    public static boolean checkForDisconnectIntent(Activity activity) {
        boolean result = false;
        Intent intent = activity.getIntent();
        if (TextUtils.equals(intent.getAction(), Panic.ACTION_DISCONNECT)) {
            result = true;
            String packageName = PanicUtils.getCallingPackageName(activity);
            removeReceiver(activity, packageName);
        }
        return result;
    }

    public static boolean addReceiver(Context context, String packageName) {
        HashSet<String> set = new HashSet<String>(getReceiverPackageNames(context));
        boolean result = set.add(packageName);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putStringSet(PREF_RECEIVER_PACKAGE_NAMES, set).apply();
        return result;
    }

    public static boolean removeReceiver(Context context, String packageName) {
        HashSet<String> set = new HashSet<String>(getReceiverPackageNames(context));
        boolean result = set.remove(packageName);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putStringSet(PREF_RECEIVER_PACKAGE_NAMES, set).apply();
        return result;
    }

    public static Set<String> getReceiverPackageNames(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet(PREF_RECEIVER_PACKAGE_NAMES, Collections.<String> emptySet());
    }
}
