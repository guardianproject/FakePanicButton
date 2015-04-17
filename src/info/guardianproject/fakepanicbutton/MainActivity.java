
package info.guardianproject.fakepanicbutton;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
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
import java.util.List;

public class MainActivity extends ListActivity {
    private static final String TAG = "MainActivity";

    private static final int CONTACT_PICKER_RESULT = 92347;

    private EditText panicMessageEditText;
    private TextView contactTextView;

    private String displayName;
    private String phoneNumber;
    private String emailAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                // TODO use TrustedIntents here and cycle through configured list of packageNames
                startActivityForResult(intent, 0);
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
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String packageName = packageNameList.get(position);
                CheckedTextView checkedTextView = (CheckedTextView) view;
                Intent intent;
                if (checkedTextView.isChecked())
                    intent = new Intent(Panic.ACTION_CONNECT);
                else
                    intent = new Intent(Panic.ACTION_DISCONNECT);
                // TODO use TrustedIntents here
                intent.setPackage(packageName);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK)
            return;
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
    }
}
