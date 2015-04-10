
package info.guardianproject.fakepanicbutton;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import info.guardianproject.panic.Panic;

public class MainActivity extends Activity {
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
