package org.zywx.wbpalmstar.plugin.uexcontacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import a_vcard.android.syncml.pim.PropertyNode;
import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.VCardParser;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class EUExContact extends EUExBase {
	public static final String tag = "uexContact_";
	public static final String KEY_CONTACT_SEARCHITEM = "uexContact.cbSearchItem";
	public static final String KEY_CONTACT_OPEN = "uexContact.cbOpen";
	public static final String KEY_CONTACT_MULTIOPEN = "uexContact.cbMultiOpen";
	public static final String KEY_CONTACT_ADD = "uexContact.cbAddItem";
	public static final String KEY_CONTACT_MODIFY = "uexContact.cbModifyItem";
	public static final String KEY_CONTACT_DELETE = "uexContact.cbDeleteItem";

	public static final int F_ACT_REQ_CODE_UEX_CONCACT = 2;
	public static final int F_ACT_REQ_CODE_UEX_MULTI_CONCACT = 9;
	private ResoureFinder finder = null;
	private Object accountType, accountName;

	public EUExContact(Context context, EBrowserView inParent) {
		super(context, inParent);
		finder = ResoureFinder.getInstance(context);
	}

	public static boolean customLinkMan = false;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		JSONObject jobj = new JSONObject();
		switch (requestCode) {
		case F_ACT_REQ_CODE_UEX_CONCACT:
			if (resultCode == Activity.RESULT_OK) {
				try {
					int sdkVersion = Build.VERSION.SDK_INT;
					if (EUExContact.customLinkMan && sdkVersion < 8) {
						jobj.put(EUExCallback.F_JK_NAME,
								data.getStringExtra(EUExCallback.F_JK_NAME));
						jobj.put(EUExCallback.F_JK_NUM,
								data.getStringExtra(EUExCallback.F_JK_NUM));
						jobj.put(EUExCallback.F_JK_EMAIL,
								data.getStringExtra(EUExCallback.F_JK_EMAIL));
					} else {
						Uri contactData = data.getData();
						Cursor c = ((Activity) mContext).managedQuery(
								contactData, null, null, null, null);
						c.moveToFirst();

						// 取得姓名
						String name = c
								.getString(c
										.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
						jobj.put(EUExCallback.F_JK_NAME, name);
						String contactId = c
								.getString(c
										.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
						// 取得电话
						Cursor phones = ((Activity) mContext)
								.getContentResolver()
								.query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
										null,
										android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID
												+ " = " + contactId, null, null);
						if (phones != null && phones.getCount() > 0) {
							phones.moveToFirst();
							String phoneNumber = phones
									.getString(phones
											.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
							jobj.put(EUExCallback.F_JK_NUM, phoneNumber);
							if (phones.isClosed()) {
								Log.i("tag", "phones.close()");
								phones.close();
							}
						}
						// 取得邮件
						Cursor emails = ((Activity) mContext)
								.getContentResolver()
								.query(android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
										null,
										android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID
												+ " = " + contactId, null, null);
						if (emails != null && emails.getCount() > 0) {
							emails.moveToFirst();
							String emailAddress = emails
									.getString(emails
											.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.DATA));
							jobj.put(EUExCallback.F_JK_EMAIL, emailAddress);
							if (emails.isClosed()) {
								emails.close();
							}
						}
						if (c.isClosed()) {
							c.close();
						}
					}
					jsCallback(EUExContact.KEY_CONTACT_OPEN, 0,
							EUExCallback.F_C_JSON, jobj.toString());
				} catch (Exception e) {
					Toast.makeText(mContext,
							finder.getString("plugin_contact_open_fail"),
							Toast.LENGTH_SHORT).show();
					return;
				}
			} else {
				return;
			}
			break;
		case F_ACT_REQ_CODE_UEX_MULTI_CONCACT:
			if (resultCode == Activity.RESULT_OK) {
				String result = data
						.getStringExtra(ContactActivity.F_INTENT_KEY_RETURN_SELECT_LIST);
				jsCallback(EUExContact.KEY_CONTACT_MULTIOPEN, 0,
						EUExCallback.F_C_JSON, /*
												 * jobj . toString ( )
												 */result);
			}
			break;
		}

	}

	public void open(String[] parm) {
		Intent intent = new Intent();

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			intent.setAction(Intent.ACTION_PICK);
			intent.setData(android.provider.Contacts.People.CONTENT_URI);
		} else {
			intent.setAction(Intent.ACTION_PICK);
			intent.setData(android.provider.ContactsContract.Contacts.CONTENT_URI);
		}
		startActivityForResult(intent, F_ACT_REQ_CODE_UEX_CONCACT);
	}

	public void multiOpen(String[] parm) {
		try {
			Intent intent = new Intent();
			intent.setClass(mContext, ContactActivity.class);
			startActivityForResult(intent, F_ACT_REQ_CODE_UEX_MULTI_CONCACT);
		} catch (Exception e) {
			jsCallback(KEY_CONTACT_MULTIOPEN, 0, EUExCallback.F_C_INT,
					EUExCallback.F_C_FAILED);
		}
	}

	public void addItem(String[] parm) {
		if (parm == null || parm.length != 3)
			return;
		final String inName = parm[0];
		final String inNum = parm[1];
		final String inEmail = parm[2];
		new AlertDialog.Builder(mContext)
				.setTitle(/* "提示" */finder.getStringId("prompt"))
				.setMessage(
				/* "是否添加联系人" */finder.getStringId("plugin_contact_add_prompt"))
				.setPositiveButton(/* "是" */finder.getStringId("confirm"),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (inName != null && inName.length() > 0
										&& inNum != null && inEmail != null) {
									if (PFConcactMan.add(mContext, inName,
											inNum, inEmail)) {
										jsCallback(KEY_CONTACT_ADD, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_SUCCESS);
									} else {
										jsCallback(KEY_CONTACT_ADD, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_FAILED);
									}
								} else {
									jsCallback(KEY_CONTACT_ADD, 0,
											EUExCallback.F_C_INT,
											EUExCallback.F_C_FAILED);
								}
							}

						})
				.setNegativeButton(/* "否" */finder.getStringId("cancel"), null)
				.show();

	}

	public void deleteItem(String[] parm) {
		if (parm == null || parm.length != 1)
			return;
		final String inName = parm[0];
		new AlertDialog.Builder(mContext)
				.setTitle(/* "提示" */finder.getStringId("prompt"))
				.setMessage(
						/* "是否删除联系人" */finder
								.getStringId("plugin_contact_delete_prompt"))
				.setPositiveButton(/* "是" */finder.getStringId("confirm"),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (inName != null && inName.length() > 0) {
									if (PFConcactMan.deletes(mContext, inName)) {
										jsCallback(KEY_CONTACT_DELETE, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_SUCCESS);
									} else {
										jsCallback(KEY_CONTACT_DELETE, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_FAILED);
									}
								} else {
									jsCallback(KEY_CONTACT_DELETE, 0,
											EUExCallback.F_C_INT,
											EUExCallback.F_C_FAILED);
								}
							}
						})
				.setNegativeButton(/* "否" */finder.getStringId("cancel"), null)
				.show();
	}

	public void searchItem(String[] parm) {
		if (parm == null || parm.length != 1)
			return;
		String inName = parm[0];
		if (inName != null && inName.length() >= 0) {
			JSONArray outJsonObj = PFConcactMan.search(mContext, inName);
			if (outJsonObj != null) {
				if (outJsonObj.length() < 50) {//if lenght < 50
					jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_JSON, outJsonObj.toString());
				} else {//if length >50
					int size = outJsonObj.length() / 50 + 1;
					int index = 0;
					for (int i = 0; i < size; i++) {
						JSONArray jsonArray = new JSONArray();
						if (i == size - 1) {
							for (int n = index; n < outJsonObj.length(); n++) {
								try {
									jsonArray.put(outJsonObj.get(index++));
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						} else {
							for (int j = 0; j < 50; j++) {
								try {
									jsonArray.put(outJsonObj.get(index++));
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						}
						jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_JSON, jsonArray.toString());
					}
				}
			} else {
				jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
			}
		} else {
			jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
		}
	}

	public void modifyItem(String[] parm) {
		if (parm == null || parm.length != 3)
			return;
		final String inName = parm[0];
		final String inNum = parm[1];
		final String inEmail = parm[2];
		new AlertDialog.Builder(mContext)
				.setTitle(/* "提示" */finder.getStringId("prompt"))
				.setMessage(
						/* "是否修改联系人" */finder
								.getStringId("plugin_contact_modify_prompt"))
				.setPositiveButton(/* "是" */finder.getStringId("confirm"),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (inName != null && inName.length() > 0
										&& inNum != null && inEmail != null) {
									if (PFConcactMan.modify(mContext, inName,
											inNum, inEmail)) {
										jsCallback(KEY_CONTACT_MODIFY, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_SUCCESS);
									} else {
										jsCallback(KEY_CONTACT_MODIFY, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_FAILED);
									}
								} else {
									jsCallback(KEY_CONTACT_MODIFY, 0,
											EUExCallback.F_C_INT,
											EUExCallback.F_C_FAILED);
								}
							}
						})
				.setNegativeButton(/* "否" */finder.getStringId("cancel"), null)
				.show();
	}

	public static String types[] = { "N", "TEL", "EMAIL", "ADR", "ORG",
			"TITLE", "URL", "NOTE" };

	public void addItemWithVCard(final String[] parm) {
		if(parm.length == 2 && "1".equals(parm[1])){
			VCardParser parser = new VCardParser();
			VDataBuilder builder = new VDataBuilder();
			try {
				boolean parsed = parser.parse(parm[0],
						"UTF-8", builder);
				// get all parsed contacts
				List<VNode> pimContacts = builder.vNodeList;
				Map<String, String> contactMap = new HashMap<String, String>();
				// do something for all the contacts
				for (VNode contact : pimContacts) {
					ArrayList<PropertyNode> props = contact.propList;

					// contact name - FN property
					String name = null;
					for (PropertyNode prop : props) {
						for (String type : types) {
							if (type.equals(prop.propName)) {
								contactMap.put(type,
										prop.propValue);
							}
						}

					}
				}
				if (PFConcactMan.add(mContext, contactMap,
						accountName, accountType)) {
					jsCallback(KEY_CONTACT_ADD, 0,
							EUExCallback.F_C_INT,
							EUExCallback.F_C_SUCCESS);
				} else {
					jsCallback(KEY_CONTACT_ADD, 0,
							EUExCallback.F_C_INT,
							EUExCallback.F_C_FAILED);
				}
			} catch (Exception e) {
				jsCallback(KEY_CONTACT_ADD, 0,
						EUExCallback.F_C_INT,
						EUExCallback.F_C_FAILED);
				e.printStackTrace();
			}
			return ;
		}
		new AlertDialog.Builder(mContext)
				.setTitle(/* "提示" */finder.getStringId("prompt"))
				.setMessage(
				/* "是否添加联系人" */finder.getStringId("plugin_contact_add_prompt"))
				.setPositiveButton(/* "是" */finder.getStringId("confirm"),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								VCardParser parser = new VCardParser();
								VDataBuilder builder = new VDataBuilder();
								try {
									boolean parsed = parser.parse(parm[0],
											"UTF-8", builder);
									// get all parsed contacts
									List<VNode> pimContacts = builder.vNodeList;
									Map<String, String> contactMap = new HashMap<String, String>();
									// do something for all the contacts
									for (VNode contact : pimContacts) {
										ArrayList<PropertyNode> props = contact.propList;

										// contact name - FN property
										String name = null;
										for (PropertyNode prop : props) {
											for (String type : types) {
												if (type.equals(prop.propName)) {
													contactMap.put(type,
															prop.propValue);
												}
											}

										}
									}
									if (PFConcactMan.add(mContext, contactMap,
											accountName, accountType)) {
										jsCallback(KEY_CONTACT_ADD, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_SUCCESS);
									} else {
										jsCallback(KEY_CONTACT_ADD, 0,
												EUExCallback.F_C_INT,
												EUExCallback.F_C_FAILED);
									}
								} catch (Exception e) {
									// TODO Auto-generated catch block
									jsCallback(KEY_CONTACT_ADD, 0,
											EUExCallback.F_C_INT,
											EUExCallback.F_C_FAILED);
									e.printStackTrace();
								}
							}

						})
				.setNegativeButton(/* "否" */finder.getStringId("cancel"), null)
				.show();

	}

	@Override
	public boolean clean() {

		return false;
	}
	/**
	 * Obtain the AuthenticatorDescription for a given account type.
	 * 
	 * @param type
	 *            The account type to locate.
	 * @param dictionary
	 *            An array of AuthenticatorDescriptions, as returned by
	 *            AccountManager.
	 * @return The description for the specified account type.
	 */
	private static AuthenticatorDescription getAuthenticatorDescription(
			String type, AuthenticatorDescription[] dictionary) {
		for (int i = 0; i < dictionary.length; i++) {
			if (dictionary[i].type.equals(type)) {
				return dictionary[i];
			}
		}
		// No match found
		throw new RuntimeException("Unable to find matching authenticator");
	}
}
