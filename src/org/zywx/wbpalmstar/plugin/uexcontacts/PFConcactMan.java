/*
 * 添加联系人values.put(Contacts.Phones.TYPE, Contacts.Phones.TYPE_HOME);通过类型每次只能添加一个
 * 修改联系人Cursor通过得到时进行条件筛选来修改具体某条记录中的具体电话或邮件类型
 */
package org.zywx.wbpalmstar.plugin.uexcontacts;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class PFConcactMan {
	private static ResoureFinder finder = ResoureFinder.getInstance();

	/*
	 * 添加联系人name,num,email内容
	 */
	public static boolean add(Context context, String name, String num, String email) {
		try {
			int sdkVersion = Build.VERSION.SDK_INT;
			if (sdkVersion < 8) {
				ContentValues values = new ContentValues();
				values.put(android.provider.Contacts.People.NAME, name);

				Uri uri = context.getContentResolver().insert(android.provider.Contacts.People.CONTENT_URI, values);

				// 电话
				Uri numberUri = Uri.withAppendedPath(uri, android.provider.Contacts.People.Phones.CONTENT_DIRECTORY);
				values.clear();
				values.put(android.provider.Contacts.Phones.NUMBER, num);
				values.put(android.provider.Contacts.Phones.TYPE, android.provider.Contacts.Phones.TYPE_HOME);
				numberUri = context.getContentResolver().insert(numberUri, values);

				// 邮件
				Uri emailUri = Uri.withAppendedPath(uri, android.provider.Contacts.People.ContactMethods.CONTENT_DIRECTORY);
				values.clear();
				values.put(android.provider.Contacts.ContactMethods.KIND, android.provider.Contacts.KIND_EMAIL);
				values.put(android.provider.Contacts.ContactMethods.DATA, email);
				values.put(android.provider.Contacts.ContactMethods.TYPE, android.provider.Contacts.ContactMethods.TYPE_HOME);
				context.getContentResolver().insert(emailUri, values);
				Toast.makeText(context, finder.getString(context, "plugin_contact_add_succeed"), Toast.LENGTH_SHORT).show();
			} else {
				ContentValues values = new ContentValues();
				Uri rawContactUri = context.getContentResolver().insert(android.provider.ContactsContract.RawContacts.CONTENT_URI, values);
				long rawContactId = android.content.ContentUris.parseId(rawContactUri);
				// 向data表插入姓名数据
				if (name != "") {
					values.clear();
					values.put(android.provider.ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
					values.put(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
					values.put(android.provider.ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name);
					context.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
				}
				// 向data表插入电话数据
				if (num != "") {
					values.clear();
					values.put(android.provider.ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
					values.put(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
					values.put(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER, num);
					values.put(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE, android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
					context.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
				}
				// 向data表插入Email数据
				if (email != "") {
					values.clear();
					values.put(android.provider.ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
					values.put(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
					values.put(android.provider.ContactsContract.CommonDataKinds.Email.DATA, email);
					values.put(android.provider.ContactsContract.CommonDataKinds.Email.TYPE, android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK);
					context.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
				}
			}
		} catch (Exception e) {
			Toast.makeText(context, finder.getString(context, "plugin_contact_add_fail"), Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	/*
	 * 根据inName删除联系人列表中的信息
	 */
	public static boolean deletes(Context context, String inName) {
		ContentResolver cr = context.getContentResolver();
		try {
			ContentResolver contentResolver = context.getContentResolver();
			if (inName != null) {
				JSONArray jsonsArray = search(context, inName);
				if (jsonsArray!=null && jsonsArray.length()>0) {
					int sdkVersion = Build.VERSION.SDK_INT;
					if (sdkVersion <= 8) {// 小于2.2版本
						Cursor cur = cr.query(android.provider.Contacts.People.CONTENT_URI, null, null, null, null);
						cur.moveToFirst();
						while (cur.moveToNext()) {
							String name = cur.getString(cur.getColumnIndex(android.provider.Contacts.People.DISPLAY_NAME));
							String contactName = name;// 系统名字
							if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
								contactName = contactName.replaceAll(" ", "");
							}
							if (name != null && contactName.equals(inName)) {
								contentResolver.delete(android.provider.Contacts.People.CONTENT_URI, android.provider.Contacts.People.NAME + "=?", new String[] { name });
							}
						}
						cur.close();
					} else if (sdkVersion < 14) {// 小于4.0版本
						Cursor cur = cr.query(android.provider.ContactsContract.RawContacts.CONTENT_URI, null, null, null, null);
						cur.moveToFirst();
						String[] idName = cur.getColumnNames();
						while (cur.moveToNext()) {
							String name = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
							String contactId = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
							String contactName = name;// 系统名字
							if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
								contactName = contactName.replaceAll(" ", "");
							}
							if (name != null && contactName.equals(inName)) {
								contentResolver.delete(android.content.ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId)), null, null);
							}
						}
						cur.close();
					} else {
						Cursor cur = cr.query(android.provider.ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
						while (cur.moveToNext()) {
							String name = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
							String id = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
							String contactName = name;// 系统名字
							if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
								contactName = contactName.replaceAll(" ", "");
							}
							if (name != null && contactName.equals(inName)) {
								contentResolver.delete(android.content.ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id)), null, null);
								break;
							}
						}
						cur.close();
					}
					Toast.makeText(context, finder.getString(context, "plugin_contact_delete_succeed"), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(context, finder.getString(context, "plugin_contact_delete_fail"), Toast.LENGTH_SHORT).show();
					return false;
				}
			}
		} catch (Exception e) {
			Toast.makeText(context, finder.getString(context, "plugin_contact_delete_fail"), Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	/*
	 * 查找联系人根据inName人名参数
	 */
	public static /*JSONObject*/JSONArray search(Context context, String inName) {
		ContentResolver cr = context.getContentResolver();
		/* 1.6 */
		JSONArray jsonArray = new JSONArray();
		int sdkVersion = Build.VERSION.SDK_INT;
		if (sdkVersion < 8) {
			Cursor cur = cr.query(android.provider.Contacts.People.CONTENT_URI, null, null, null, null);
			StringBuffer sb = new StringBuffer();
			try {
				while (cur.moveToNext()) {
					JSONObject jsonObject = new JSONObject();
					sb.append(cur.getString(cur.getColumnIndex(android.provider.Contacts.People.DISPLAY_NAME)));
					String id = cur.getString(cur.getColumnIndex(android.provider.Contacts.People._ID));
					String contactName = sb.toString();// 系统名字

					if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
						sb.delete(0, sb.length());
						contactName = contactName.replaceAll(" ", "");
						sb.append(contactName);
					}

					if ((sb.length() > 0 && sb.toString().indexOf(inName)>-1) || inName.length()==0) {
						// 名字
						jsonObject.put(EUExCallback.F_JK_NAME, sb.toString());
						sb.delete(0, sb.length());
						// 电话
						Cursor pCur = cr.query(android.provider.Contacts.Phones.CONTENT_URI, null, android.provider.Contacts.Phones.PERSON_ID + " = ?", new String[] { id }, null);
						if (pCur.moveToNext())
							sb.append(pCur.getString(pCur.getColumnIndex(android.provider.Contacts.Phones.NUMBER)));
						pCur.close();
						jsonObject.put(EUExCallback.F_JK_NUM, sb.toString());
						sb.delete(0, sb.length());
						// 邮件
						Cursor emailCur = cr.query(android.provider.Contacts.ContactMethods.CONTENT_EMAIL_URI, null, android.provider.Contacts.ContactMethods.PERSON_ID + " = ?", new String[] { id }, null);
						if (emailCur.moveToNext())
							sb.append(emailCur.getString(emailCur.getColumnIndex(android.provider.Contacts.ContactMethodsColumns.DATA)));
						emailCur.close();
						jsonObject.put(EUExCallback.F_JK_EMAIL, sb.toString());
						sb.delete(0, sb.length());

						sb.delete(0, sb.length());
						getValueWithName(context, id, jsonObject);
						Toast.makeText(context, finder.getString(context, "plugin_contact_find_succeed"), Toast.LENGTH_SHORT).show();
					}
					sb.delete(0, sb.length());
					jsonArray.put(jsonObject);
				}
				sb.delete(0, sb.length());
			} catch (Exception e) {
				Toast.makeText(context, finder.getString(context, "plugin_contact_find_fail"), Toast.LENGTH_SHORT).show();
				return null;
			} finally {
				if (cur != null)
					cur.close();
			}
		} else {
			Cursor cursor = cr.query(android.provider.ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
			try {
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					do {
						String name = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
						String contactId = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
						String contactName = name;// 系统名字
						if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
							contactName = contactName.replaceAll(" ", "");
							name = contactName;
						}
						if (name!=null&&(name.indexOf(inName)>-1/*equals(inName)*/ || inName.length()==0)) {// 如果和输入的名字相同就取得内容
							JSONObject jsonObject = new JSONObject();
							jsonObject.put(EUExCallback.F_JK_NAME, name);
							// 取得电话
							Cursor phones = ((Activity) context).getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
							if (phones != null && phones.getCount() > 0) {
								phones.moveToFirst();
								String phoneNumber = phones.getString(phones.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
								jsonObject.put(EUExCallback.F_JK_NUM, phoneNumber);
								phones.close();
							}
							// 取得邮件
							Cursor emails = ((Activity) context).getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId, null, null);
							if (emails != null && emails.getCount() > 0) {
								emails.moveToFirst();
								String emailAddress = emails.getString(emails.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.DATA));
								jsonObject.put(EUExCallback.F_JK_EMAIL, emailAddress);
								emails.close();
							}
							getValueWithName(context, contactId, jsonObject);
							jsonArray.put(jsonObject);
						}

					} while (cursor.moveToNext());

				}
			} catch (Exception e) {
				Toast.makeText(context, finder.getString(context, "plugin_contact_find_fail"), Toast.LENGTH_SHORT).show();
				return null;
			} finally {
				if (cursor != null)
					cursor.close();
			}
		}
		return /*jsonObject*/jsonArray;
	}

	/*
	 * 修改手机联系人信息 根据inName提供的人名,修改inNum电话号码和inEmail邮件
	 */
	public static boolean modify(Context context, String inName, String inNum, String inEmail) {
		int sdkVersion = Build.VERSION.SDK_INT;
		ContentResolver contentResolver = context.getContentResolver();
		if (sdkVersion < 8) {
			try {
				Cursor cusor = null;
				String[] projection = new String[] { android.provider.Contacts.People._ID, android.provider.Contacts.People.NAME, android.provider.Contacts.People.NUMBER };
				cusor = contentResolver.query(android.provider.Contacts.People.CONTENT_URI, projection, null, null, null);
				cusor.moveToFirst();

				while (cusor.moveToNext()) {
					String name = cusor.getString(cusor.getColumnIndex(android.provider.Contacts.People.NAME));
					String contactName = name;// 系统名字

					if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
						contactName = contactName.replaceAll(" ", "");
						name = contactName;
					}

					if (name != null && name.equals(inName)) {
						String id = cusor.getString(cusor.getColumnIndex(android.provider.Contacts.People._ID));
						ContentValues values = new ContentValues();

						// 电话（根据type类型不同，可以修改不同类型电话）
						values.clear();
						values.put(android.provider.Contacts.Phones.TYPE, android.provider.Contacts.Phones.TYPE_MOBILE);
						values.put(android.provider.Contacts.Phones.NUMBER, inNum);
						String numWhere = android.provider.Contacts.Phones.PERSON_ID + "=? ";
						String[] numWhereParams = { id };
						contentResolver.update(android.provider.Contacts.Phones.CONTENT_URI, values, numWhere, numWhereParams);

						// 邮件（根据type类型不同，可以修改不同类型邮件）
						values.clear();
						values.put(android.provider.Contacts.ContactMethods.KIND, android.provider.Contacts.KIND_EMAIL);
						values.put(android.provider.Contacts.ContactMethods.DATA, inEmail);
						values.put(android.provider.Contacts.ContactMethods.TYPE, android.provider.Contacts.ContactMethods.TYPE_HOME);
						String emailWhere = android.provider.Contacts.ContactMethods.PERSON_ID + "=? ";
						String[] emailWhereParams = { id };
						contentResolver.update(android.provider.Contacts.ContactMethods.CONTENT_URI, values, emailWhere, emailWhereParams);
					}
				}
				if (cusor != null)
					cusor.close();
				Toast.makeText(context, finder.getString(context, "plugin_contact_modify_succeed"), Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(context, finder.getString(context, "plugin_contact_modify_fail"), Toast.LENGTH_SHORT).show();
				return false;
			}
		} else if (sdkVersion < 14) {
			try {
				Cursor cur = contentResolver.query(android.provider.ContactsContract.RawContacts.CONTENT_URI, null, null, null, null);
				cur.moveToFirst();
				while (cur.moveToNext()) {
					String name = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
					String id = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts._ID));

					String contactName = name;// 系统名字
					if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
						contactName = contactName.replaceAll(" ", "");
						name = contactName;
					}

					if (name.equals(inName)) {
						ContentValues values = new ContentValues();
						values.clear();
						values.put(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER, inNum);
						values.put(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE, android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
						String nameWhere = android.provider.ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + android.provider.ContactsContract.Data.MIMETYPE + " = ?";
						String[] nameSelection = new String[] { id, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, };
						context.getContentResolver().update(android.provider.ContactsContract.Data.CONTENT_URI, values, nameWhere, nameSelection);

						values.clear();
						values.put(android.provider.ContactsContract.Data.RAW_CONTACT_ID, id);
						values.put(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
						values.put(android.provider.ContactsContract.CommonDataKinds.Email.DATA, inEmail);
						values.put(android.provider.ContactsContract.CommonDataKinds.Email.TYPE, android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK);

						String emailWhere = android.provider.ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + android.provider.ContactsContract.Data.MIMETYPE + " = ?";
						String[] emailSelection = new String[] { id, android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, };
						context.getContentResolver().update(android.provider.ContactsContract.Data.CONTENT_URI, values, emailWhere, emailSelection);
					}
				}
				if (cur != null)
					cur.close();
				Toast.makeText(context, finder.getString(context, "plugin_contact_modify_succeed"), Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(context, finder.getString(context, "plugin_contact_modify_fail"), Toast.LENGTH_SHORT).show();
			}
		} else {
			try {
				Cursor cur = contentResolver.query(android.provider.ContactsContract.RawContacts.CONTENT_URI, null, null, null, null);
				while (cur.moveToNext()) {
					String name = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
					String id = cur.getString(cur.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
					String contactName = name;// 系统名字
					if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
						contactName = contactName.replaceAll(" ", "");
						name = contactName;
					}

					if (name.equals(inName)) {
						ContentValues values = new ContentValues();
						values.clear();
						values.put(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER, inNum);
						values.put(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE, android.provider.ContactsContract.PhoneLookup.NUMBER);
						String nameWhere = android.provider.ContactsContract.RawContacts.CONTACT_ID + " = ? AND " + android.provider.ContactsContract.Data.MIMETYPE + " = ?";
						String[] nameSelection = new String[] { id, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, };
						context.getContentResolver().update(android.provider.ContactsContract.Data.CONTENT_URI, values, nameWhere, nameSelection);

						values.clear();
						values.put(android.provider.ContactsContract.Data.RAW_CONTACT_ID, id);
						values.put(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
						values.put(android.provider.ContactsContract.CommonDataKinds.Email.DATA, inEmail);
						values.put(android.provider.ContactsContract.CommonDataKinds.Email.TYPE, android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK);

						String emailWhere = android.provider.ContactsContract.RawContacts.CONTACT_ID + " = ? AND " + android.provider.ContactsContract.Data.MIMETYPE + " = ?";
						String[] emailSelection = new String[] { id, android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, };
						context.getContentResolver().update(android.provider.ContactsContract.Data.CONTENT_URI, values, emailWhere, emailSelection);

					}
				}
				if (cur != null)
					cur.close();
				Cursor cursor = context.getContentResolver().query(android.provider.ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
				while (cursor.moveToNext()) {
					String name = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
					String id = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
					String contactName = name;// 系统名字
					if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
						contactName = contactName.replaceAll(" ", "");
						name = contactName;
					}

					if (name.equals(inName)) {
						ContentValues values = new ContentValues();
						values.clear();
						values.put(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER, inNum);
						values.put(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE, android.provider.ContactsContract.PhoneLookup.NUMBER);
						String nameWhere = android.provider.ContactsContract.RawContacts.CONTACT_ID + " = ? AND " + android.provider.ContactsContract.Data.MIMETYPE + " = ?";
						String[] nameSelection = new String[] { id, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, };
						context.getContentResolver().update(android.provider.ContactsContract.Data.CONTENT_URI, values, nameWhere, nameSelection);

						values.clear();
						values.put(android.provider.ContactsContract.Data.RAW_CONTACT_ID, id);
						values.put(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
						values.put(android.provider.ContactsContract.CommonDataKinds.Email.DATA, inEmail);
						values.put(android.provider.ContactsContract.CommonDataKinds.Email.TYPE, android.provider.ContactsContract.CommonDataKinds.Email.TYPE_WORK);

						String emailWhere = android.provider.ContactsContract.RawContacts.CONTACT_ID + " = ? AND " + android.provider.ContactsContract.Data.MIMETYPE + " = ?";
						String[] emailSelection = new String[] { id, android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, };
						context.getContentResolver().update(android.provider.ContactsContract.Data.CONTENT_URI, values, emailWhere, emailSelection);

					}
				}
				if (cursor != null)
					cursor.close();
				
				try {
					Cursor cusor = null;
					String[] projection = new String[] { android.provider.Contacts.People._ID, android.provider.Contacts.People.NAME, android.provider.Contacts.People.NUMBER };
					cusor = contentResolver.query(android.provider.Contacts.People.CONTENT_URI, projection, null, null, null);
					cusor.moveToFirst();

					while (cusor.moveToNext()) {
						String name = cusor.getString(cusor.getColumnIndex(android.provider.Contacts.People.NAME));
						String contactName = name;// 系统名字

						if (contactName != null && contactName.indexOf(" ") != -1) {// 如果有空格去掉空格（无论在中间还是在两边）
							contactName = contactName.replaceAll(" ", "");
							name = contactName;
						}

						if (name != null && name.equals(inName)) {
							String id = cusor.getString(cusor.getColumnIndex(android.provider.Contacts.People._ID));
							ContentValues values = new ContentValues();

							// 电话（根据type类型不同，可以修改不同类型电话）
							values.clear();
							values.put(android.provider.Contacts.Phones.TYPE, android.provider.Contacts.Phones.TYPE_MOBILE);
							values.put(android.provider.Contacts.Phones.NUMBER, inNum);
							String numWhere = android.provider.Contacts.Phones.PERSON_ID + "=? ";
							String[] numWhereParams = { id };
							contentResolver.update(android.provider.Contacts.Phones.CONTENT_URI, values, numWhere, numWhereParams);

							// 邮件（根据type类型不同，可以修改不同类型邮件）
							values.clear();
							values.put(android.provider.Contacts.ContactMethods.KIND, android.provider.Contacts.KIND_EMAIL);
							values.put(android.provider.Contacts.ContactMethods.DATA, inEmail);
							values.put(android.provider.Contacts.ContactMethods.TYPE, android.provider.Contacts.ContactMethods.TYPE_HOME);
							String emailWhere = android.provider.Contacts.ContactMethods.PERSON_ID + "=? ";
							String[] emailWhereParams = { id };
							contentResolver.update(android.provider.Contacts.ContactMethods.CONTENT_URI, values, emailWhere, emailWhereParams);
						}
					}
					if (cusor != null)
						cusor.close();
					Toast.makeText(context, finder.getString(context, "plugin_contact_modify_succeed"), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(context, finder.getString(context, "plugin_contact_modify_fail"), Toast.LENGTH_SHORT).show();
					return false;
				}
				Toast.makeText(context, finder.getString(context, "plugin_contact_modify_succeed"), Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(context, finder.getString(context, "plugin_contact_modify_fail"), Toast.LENGTH_SHORT).show();
			}
		}

		return true;
	}

	public static boolean add(Context context, Map content, Object accountType, Object accountName) {
		if (content == null || content.size() == 0) {
			return false;
		}
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
				.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).withValue(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED).build());

		for (int i = 0; i < EUExContact.types.length; i++) {

			String valuse = (String) content.get(EUExContact.types[i]);
			if (TextUtils.isEmpty(valuse)) {
				continue;
			}

			if (i == 0) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, valuse.replaceAll(";", "")).build());
			}
			if (i == 1) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, valuse).withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME).build());
			}
			if (i == 2) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Email.DATA, valuse).withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME).build());
			}
			if (i == 3) {
				String[] structured = valuse.split(";");
				if (structured.length == 6) {
					String[] newstructured = new String[7];
					for (int j = 0; j < structured.length; j++) {
						newstructured[j] = structured[j];
					}
					newstructured[6] = "";
					structured = newstructured;
				}
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, structured[6]).withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, structured[5]).withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, structured[4])
						.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, structured[3]).withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, structured[0] + structured[1] + structured[2])
						.withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK).build());
			}
			if (i == 4) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, valuse).withValue(ContactsContract.CommonDataKinds.Organization.TITLE, (String) content.get(EUExContact.types[i + 1]))
						.withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK).build());

			}
			if (i == 6) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Website.URL, valuse).withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_WORK).build());
			}
			if (i == 7) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Note.NOTE, valuse).build());
			}
		}

		try {
			context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public static void getValueWithName(Context context, String id, JSONObject jsonobject) {
		// 获取该联系人地址
		Cursor address = context.getContentResolver().query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
		if (address.moveToFirst()) {
			do {
				// 遍历所有的地址
				String street = address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
				String city = address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
				String region = address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
				String postCode = address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
				String formatAddress = address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));

				try {
					JSONObject addressjson = new JSONObject();
					addressjson.put(EUExCallback.F_JK_STREET, street);
					addressjson.put(EUExCallback.F_JK_ZIP, postCode);
					addressjson.put(EUExCallback.F_JK_STATE, region);
					addressjson.put(EUExCallback.F_JK_STATE, region);
					jsonobject.put(EUExCallback.F_JK_ADDRESS, addressjson);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			} while (address.moveToNext());
		} else {
			try {
				JSONObject addressjson = new JSONObject();
				addressjson.put(EUExCallback.F_JK_STREET, "");
				addressjson.put(EUExCallback.F_JK_ZIP, "");
				addressjson.put(EUExCallback.F_JK_STATE, "");
				addressjson.put(EUExCallback.F_JK_STATE, "");
				jsonobject.put(EUExCallback.F_JK_ADDRESS, addressjson);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		address.close();
		address = null;
		// 获取该联系人组织
		Cursor organizations = context.getContentResolver().query(Data.CONTENT_URI, new String[] { Data._ID, Organization.COMPANY, Organization.TITLE }, Data.CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + Organization.CONTENT_ITEM_TYPE + "'", new String[] { id }, null);
		if (organizations.moveToFirst()) {
			do {
				String company = organizations.getString(organizations.getColumnIndex(Organization.COMPANY));
				String title = organizations.getString(organizations.getColumnIndex(Organization.TITLE));
				try {
					jsonobject.put(EUExCallback.F_JK_COMPANY, company);
					jsonobject.put(EUExCallback.F_JK_TITLE, title);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} while (organizations.moveToNext());
		} else {
			try {
				jsonobject.put(EUExCallback.F_JK_COMPANY, "");
				jsonobject.put(EUExCallback.F_JK_TITLE, "");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		organizations.close();
		organizations = null;
		// 获取备注信息
		Cursor notes = context.getContentResolver().query(Data.CONTENT_URI, new String[] { Data._ID, Note.NOTE }, Data.CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + Note.CONTENT_ITEM_TYPE + "'", new String[] { id }, null);
		if (notes.moveToFirst()) {
			do {
				String noteinfo = notes.getString(notes.getColumnIndex(Note.NOTE));
				try {
					jsonobject.put(EUExCallback.F_JK_NOTE, noteinfo);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} while (notes.moveToNext());
		} else {
			try {
				jsonobject.put(EUExCallback.F_JK_NOTE, "");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		notes.close();
		notes = null;
		// url
		Cursor url = context.getContentResolver().query(Data.CONTENT_URI, new String[] { Data._ID, Website.URL }, Data.CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + Website.CONTENT_ITEM_TYPE + "'", new String[] { id }, null);
		if (url.moveToFirst()) {
			do {
				String urlString = url.getString(url.getColumnIndex(Website.URL));
				try {
					jsonobject.put(EUExCallback.F_JK_URL, urlString);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} while (url.moveToNext());
		} else {
			try {
				jsonobject.put(EUExCallback.F_JK_URL, "");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		url.close();
		url = null;
	}
}
