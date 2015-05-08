package org.zywx.wbpalmstar.plugin.uexcontacts;

import org.zywx.wbpalmstar.base.ResoureFinder;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class AutoAdapter extends CursorAdapter {
	private ContentResolver mContent;
	private ResoureFinder finder = null; 
	public AutoAdapter(Context context, Cursor c) {
		super(context, c);
		mContent = context.getContentResolver();
		finder = ResoureFinder.getInstance(context);
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		super.runQueryOnBackgroundThread(constraint);
		if (getFilterQueryProvider() != null) {
			return getFilterQueryProvider().runQuery(constraint);
		}
		StringBuilder buffer = null;
		String[] args = null;
		if (constraint != null) {
			buffer = new StringBuilder();
			buffer.append("UPPER(");
			buffer.append(ContactsContract.Contacts.DISPLAY_NAME);
			buffer.append(") GLOB ?");

			args = new String[] { constraint.toString().toUpperCase() + "*" // 匹配以constraint开头的所有联系人名
			};
		}

		return mContent.query(ContactsContract.Contacts.CONTENT_URI, null, buffer == null ? null : buffer.toString(), args, null);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
		View view = (View) inflater.inflate(finder.getLayoutId("plugin_contacts_item"), null);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView text_name = (TextView) view.findViewById(finder.getId("phone_name"));
		TextView text_num = (TextView) view.findViewById(finder.getId("phone_number"));
		TextView text_email = (TextView) view.findViewById(finder.getId("phone_email"));

		text_name.setText(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)));

		String contactId = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
		StringBuffer sb = new StringBuffer();

		Cursor phones = mContent.query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
		while (phones.moveToNext()) {
			String phoneNumber = phones.getString(phones.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
			sb.append(phoneNumber);
			break;
		}
		text_num.setText(sb.toString());
		sb.delete(0, sb.length());

		Cursor emails = mContent.query(android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId, null, null);
		while (emails.moveToNext()) {
			String emailAddress = emails.getString(emails.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.DATA));
			sb.append(emailAddress);
			break;
		}
		text_email.setText(sb.toString());
	}
}