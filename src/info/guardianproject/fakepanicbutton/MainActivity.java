
package info.guardianproject.fakepanicbutton;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import info.guardianproject.panic.Panic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends ListActivity {
    private static final String TAG = "MainActivity";

    private static final int CONTACT_PICKER_RESULT = 0x00;
    private static final int CONNECT_RESULT = 0x01;

    private static final String PREF_RECEIVER_PACKAGE_NAMES = "receiverPackageNames";

    private EditText panicMessageEditText;
    private TextView contactTextView;

    private SharedPreferences prefs;
    private Set<String> receiverPackageNames;
    private String requestPackageName;
    private String requestAction;

    private String displayName;
    private String phoneNumber;
    private String emailAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);
        panicMessageEditText = (EditText) findViewById(R.id.panicMessageEditText);
        contactTextView = (TextView) findViewById(R.id.contactTextView);

        Button panicButton = (Button) findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Panic.ACTION_TRIGGER);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {
                        emailAddress
                });
                intent.putExtra(Intent.EXTRA_PHONE_NUMBER, phoneNumber);
                intent.putExtra(Intent.EXTRA_SUBJECT, "panic message");
                intent.putExtra(Intent.EXTRA_TEXT,
                        panicMessageEditText.getText().toString());
                // TODO use TrustedIntents here
                for (String packageName : receiverPackageNames) {
                    intent.setPackage(packageName);
                    startActivityForResult(intent, 0);
                }
            }
        });

        Button chooseContactButton = (Button) findViewById(R.id.chooseContactButton);
        chooseContactButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(
                new Intent(Panic.ACTION_TRIGGER),
                PackageManager.GET_ACTIVITIES);
        if (resolveInfos.isEmpty())
            return;
        final List<String> appLabelList = new ArrayList<String>(resolveInfos.size());
        final List<String> packageNameList = new ArrayList<String>(resolveInfos.size());
        final List<Drawable> iconList = new ArrayList<Drawable>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null)
                continue;
            appLabelList.add(resolveInfo.activityInfo.loadLabel(pm).toString());
            packageNameList.add(resolveInfo.activityInfo.packageName);
            iconList.add(resolveInfo.activityInfo.loadIcon(pm));
        }

        List<ResolveInfo> connectInfos = pm.queryIntentActivities(
                new Intent(Panic.ACTION_CONNECT),
                PackageManager.GET_ACTIVITIES);
        final List<String> connectPackageNameList = new ArrayList<String>(connectInfos.size());
        for (ResolveInfo resolveInfo : connectInfos) {
            if (resolveInfo.activityInfo == null)
                continue;
            connectPackageNameList.add(resolveInfo.activityInfo.packageName);
        }

        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice,
                appLabelList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView checkedTextView = (CheckedTextView) super.getView(position,
                        convertView, parent);
                checkedTextView.setCompoundDrawablesWithIntrinsicBounds(iconList.get(position),
                        null, null, null);
                checkedTextView.setCompoundDrawablePadding(10);
                return checkedTextView;
            }
        });

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        receiverPackageNames = prefs.getStringSet(PREF_RECEIVER_PACKAGE_NAMES,
                Collections.<String> emptySet());
        for (int i = 0; i < packageNameList.size(); i++)
            if (receiverPackageNames.contains(packageNameList.get(i)))
                listView.setItemChecked(i, true);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                requestPackageName = packageNameList.get(position);
                boolean checked = ((CheckedTextView) view).isChecked();
                if (connectPackageNameList.contains(requestPackageName)) {
                    if (checked) {
                        requestAction = Panic.ACTION_CONNECT;
                    } else {
                        requestAction = Panic.ACTION_DISCONNECT;
                        removeReceiver(requestPackageName);
                    }
                    Intent intent = new Intent(requestAction);
                    intent.setPackage(requestPackageName);
                    // TODO add TrustedIntents here
                    startActivityForResult(intent, CONNECT_RESULT);
                } else {
                    // no config is possible with this packageName
                    if (checked)
                        addReceiver(requestPackageName);
                    else
                        removeReceiver(requestPackageName);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null)
            return;
        switch (requestCode) {
            case CONTACT_PICKER_RESULT:
                Uri uri = data.getData();
                String id = uri.getLastPathSegment();
                Log.i(TAG, uri + "");

                String[] projection = {
                        Phone.DISPLAY_NAME,
                        Phone.NUMBER
                };
                Cursor cursor = getContentResolver().query(Phone.CONTENT_URI,
                        projection,
                        Phone.CONTACT_ID + " = ? ",
                        new String[] {
                            id,
                        }, null);

                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
                    phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                    contactTextView.setText(displayName + "/" + phoneNumber);
                }
                break;
            case CONNECT_RESULT:
                /*
                 * Only ACTION_CONNECT needs the confirmation from
                 * onActivityResult(), listView.setOnItemClickListener handles
                 * all the other adding and removing of panic receivers.
                 */
                if (TextUtils.equals(requestAction, Panic.ACTION_CONNECT)) {
                    addReceiver(requestPackageName);
                }
                break;
        }
    }

    private boolean addReceiver(String packageName) {
        HashSet<String> set = new HashSet<String>(prefs.getStringSet(
                PREF_RECEIVER_PACKAGE_NAMES,
                Collections.<String> emptySet()));
        boolean result = set.add(packageName);
        prefs.edit().putStringSet(PREF_RECEIVER_PACKAGE_NAMES, set).apply();
        receiverPackageNames = set;
        return result;
    }

    private boolean removeReceiver(String packageName) {
        HashSet<String> set = new HashSet<String>(prefs.getStringSet(
                PREF_RECEIVER_PACKAGE_NAMES,
                Collections.<String> emptySet()));
        boolean result = set.remove(packageName);
        prefs.edit().putStringSet(PREF_RECEIVER_PACKAGE_NAMES, set).apply();
        receiverPackageNames = set;
        return result;
    }
}
