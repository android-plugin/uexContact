package org.zywx.wbpalmstar.plugin.uexcontacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.text.TextUtils;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;
import android.graphics.drawable.Drawable;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class ContactActivity extends Activity implements OnClickListener,
		OnFocusChangeListener {
	public static String F_INTENT_KEY_RETURN_SELECT_LIST = "returnSelectList";

	private final int handle_select_all = 300;
	private final int handle_cancel_all = 400;
	private final int handle_select_enter = 500;
	private final int handle_auto_select = 600;

	public static String IS_SELECT = "select";
	private JSONObject m_content;
	private final static String CONTACT_PHOTO = "photo";
	private final static String SORT_KEY = "sort";
	private List<Map<String, Object>> list = null;
	private List<Map<String, Object>> contactsList = new ArrayList<Map<String, Object>>();
	private Button m_return;// 返回按钮
	private Button m_prompt;// 显示选择Group组件
	private ListView listView = null;
	private LinearLayout m_select_layout;
	private Button m_select_all;// 全选按钮
	private Button m_select_cancel;// 取消全选按钮
	private Button m_select_enter;// 确定按钮
	private SimpleAdapter adapter = null;
	private boolean isShowSelectMode = false;
	private ProgressDialog progress = null;
	private ResoureFinder finder = null;
	private Drawable cancelDrawable = null;
	private Drawable multiSelectDrawable = null;
	private AutoCompleteTextView autoText = null;
	private AutoAdapter aAdapter = null;// 自定义的adapter提供给AutoCompleteTextView使用

	public JSONObject getContent() {
		return m_content;
	}

	public void setContent(JSONObject content) {
		m_content = content;
	}

	public Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 100:
				adapter = new SimpleAdapter(
						ContactActivity.this,
						list,
						finder.getLayoutId("plugin_contacts_item"),
						new String[] { CONTACT_PHOTO, EUExCallback.F_JK_NAME,
								EUExCallback.F_JK_NUM, EUExCallback.F_JK_EMAIL },
						new int[] { finder.getId("img"),
								finder.getId("phone_name"),
								finder.getId("phone_number"),
								finder.getId("phone_email") }) {
					public int getViewTypeCount() {
						return 2;
					};

					public int getItemViewType(int position) {
						String sort = (String) list.get(position).get(SORT_KEY);
						return sort == null ? 0 : 1;
					};

					public boolean isEnabled(int position) {
						String sort = (String) list.get(position).get(SORT_KEY);
						return sort == null ? true : false;
					};

					@Override
					public View getView(int position, View convertView,
							android.view.ViewGroup parent) {
						ViewHolder holder = null;
						if (convertView == null) {
							holder = new ViewHolder();
							LayoutInflater flater = LayoutInflater
									.from(ContactActivity.this);
							if (getItemViewType(position) == 0) {
								convertView = flater.inflate(finder
										.getLayoutId("plugin_contacts_item"),
										null);
								holder.name = (TextView) convertView
										.findViewById(finder
												.getId("phone_name"));
								holder.num = (TextView) convertView
										.findViewById(finder
												.getId("phone_number"));
								holder.email = (TextView) convertView
										.findViewById(finder
												.getId("phone_email"));
								holder.check = (CheckBox) convertView
										.findViewById(finder.getId("check_box"));
							} else {
								convertView = flater
										.inflate(
												finder.getLayoutId("plugin_contacts_item_group"),
												null);
								holder.name = (TextView) convertView
										.findViewById(finder
												.getId("sort_group"));
							}

							convertView.setTag(holder);
						} else {
							holder = (ViewHolder) convertView.getTag();
						}
						if (getItemViewType(position) == 0) {
							holder.name.setText((String) list.get(position)
									.get(EUExCallback.F_JK_NAME));
							if (list.get(position).containsKey(
									EUExCallback.F_JK_NUM)) {
								holder.num.setText((String) list.get(position)
										.get(EUExCallback.F_JK_NUM));
							} else {
//								holder.num.setText("");
							}
							if (list.get(position).containsKey(
									EUExCallback.F_JK_EMAIL)) {
								holder.email.setText((String) list
										.get(position).get(
												EUExCallback.F_JK_EMAIL));
							} else {
//								holder.email.setText("");
							}
							if (isShowSelectMode) {
								holder.check.setVisibility(View.VISIBLE);
								holder.check.setChecked((Boolean) list.get(
										position).get(IS_SELECT));
							} else {
								holder.check.setVisibility(View.INVISIBLE);
							}
						} else {
							holder.name.setText((String) list.get(position)
									.get(SORT_KEY));
						}
						return convertView;
					};
				};

				listView.setAdapter(adapter);
				aAdapter = new AutoAdapter(ContactActivity.this, null);
				autoText.setAdapter(aAdapter);
				listView.setOnItemClickListener(new myOnItemClickListener());

				hideLoading();
				break;
			case handle_select_all:
				for (Map<String, Object> i : list) {
					i.put(IS_SELECT, true);
				}
				adapter.notifyDataSetChanged();
				hideLoading();
				break;
			case handle_cancel_all:
				for (Map<String, Object> i : list) {
					i.put(IS_SELECT, false);
				}
				adapter.notifyDataSetChanged();
				hideLoading();
				break;
			case handle_select_enter:
				JSONArray jsonArray = new JSONArray();
				try {
					for (int j=0,size = list.size(); j < size;j++) {
						Map<String, Object> i = list.get(j);
						Map<String, Object> map = null;
						if(j < contactsList.size()){
							map = contactsList.get(j);
						}
						boolean bool = false;
						if (i.containsKey(IS_SELECT)) {
							bool = (Boolean) i.get(IS_SELECT);
						}
						
						if (bool) {
							if(map != null){
								JSONObject jsonPeople = new JSONObject();
//								if (i.containsKey(EUExCallback.F_JK_NAME)) {
									String name = (String) map
											.get(EUExCallback.F_JK_NAME);
									jsonPeople.put(EUExCallback.F_JK_NAME, name);
//								}

//								if (i.containsKey(EUExCallback.F_JK_NUM)) {
									String num = (String) map
											.get(EUExCallback.F_JK_NUM);
									jsonPeople.put(EUExCallback.F_JK_NUM, num);
//								} else {
////									jsonPeople.put(EUExCallback.F_JK_NUM, "");
//								}

//								if (i.containsKey(EUExCallback.F_JK_EMAIL)) {
									String email = (String) map
											.get(EUExCallback.F_JK_EMAIL);
									jsonPeople.put(EUExCallback.F_JK_EMAIL, email);
//								} else {
////									jsonPeople.put(EUExCallback.F_JK_EMAIL, "");
//								}
								PFConcactMan.getValueWithName(ContactActivity.this,
										(String) map.get("contactId"), jsonPeople);
								jsonArray.put(jsonPeople);
							}
						}
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}
				hideLoading();
				Intent intent = new Intent(getIntent().getAction());
				intent.putExtra(F_INTENT_KEY_RETURN_SELECT_LIST,
						jsonArray.toString());
				setResult(Activity.RESULT_OK, intent);
				finish();
				break;
			case handle_auto_select:
				int index = 0;

				String autoName = autoText.getText().toString().trim();
				for (Map<String, Object> m : list) {
					String name = (String) m.get(EUExCallback.F_JK_NAME);
					if (name != null && name.trim().equals(autoName)) {
						listView.requestFocusFromTouch();
						listView.setSelection(index);
						break;
					} else {
						index++;
					}
				}
				hideLoading();
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		finder = ResoureFinder.getInstance(this);
		setContentView(finder
				.getLayoutId("plugin_contacts_layout"));
		multiSelectDrawable = finder
				.getDrawable("plugin_contacts_btn_multiselect_bg_selector");
		cancelDrawable = finder
				.getDrawable("plugin_contacts_btn_cancel_bg_selector");

		showLoading("Waiting...");

		m_return = (Button) findViewById(finder
				.getId("title_return"));
		m_prompt = (Button) findViewById(finder
				.getId("title_prompt"));

		autoText = (AutoCompleteTextView) findViewById(finder
				.getId("auto_edit"));
		autoText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (aAdapter != null)
					aAdapter.runQueryOnBackgroundThread(s);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
		/*
		 * 点击AutoCompleteTextView下拉选项
		 */
		autoText.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TextView tv = (TextView) view
						.findViewById(finder
								.getId("phone_name"));
				autoText.setText(tv.getText().toString() + " ");
				autoText.setSelection((autoText.getText().toString()).length());
				hideKeyBoard();
				showLoading("Waiting...");
				handler.sendEmptyMessage(handle_auto_select);
			}
		});
		autoText.setOnFocusChangeListener(this);
		m_select_layout = (LinearLayout) findViewById(finder
				.getId("select_group"));
		m_select_all = (Button) findViewById(finder
				.getId("select_all"));
		m_select_cancel = (Button) findViewById(finder
				.getId("select_cancel"));
		m_select_enter = (Button) findViewById(finder
				.getId("select_enter"));

		m_return.setOnClickListener(this);
		m_prompt.setOnClickListener(this);

		m_select_all.setOnClickListener(this);
		m_select_cancel.setOnClickListener(this);
		m_select_enter.setOnClickListener(this);

		listView = (ListView) findViewById(finder
				.getId("plugin_contact_listview"));

		new Thread(new Runnable() {
			@Override
			public void run() {
				list = getData();
				handler.sendEmptyMessage(100);
			}
		}).start();

	}

	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		ContentResolver cr = getContentResolver();
		/* 2.0 */
		// 索爱特例使用
		String tmpSort = "";// 缓存得到的sort_key
		Cursor cursor = null;
		try {// 如果数据库没有此列，则会报错
			cursor = cr.query(
					android.provider.ContactsContract.Contacts.CONTENT_URI,
					null, null, null, "sort_key COLLATE LOCALIZED asc");
		} catch (Exception e) {// 启用普通联系人号码
			cursor = cr.query(
					android.provider.ContactsContract.Contacts.CONTENT_URI,
					null, null, null, null);
		}

		while (cursor.moveToNext()) {
			map = new HashMap<String, Object>();
			String contactId = cursor
					.getString(cursor
							.getColumnIndex(android.provider.ContactsContract.Contacts._ID));
			String name = cursor
					.getString(cursor
							.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
			String sort = null;
			map.put("contactId", contactId);
			try {// 如果数据库没有此列，则会报错
				sort = cursor.getString(cursor
						.getColumnIndexOrThrow("sort_key"));
			} catch (Exception e) {

			}
			if (sort != null)
				sort = sort.substring(0, 1);
			if (sort != null && !sort.equals(tmpSort)) {
				Map<String, Object> m = new HashMap<String, Object>();
				m.put(SORT_KEY, sort);
				list.add(m);
			}
			tmpSort = sort;

			map.put(EUExCallback.F_JK_NAME, name);
			String hasPhone = cursor
					.getString(cursor
							.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER));
			if (hasPhone.equalsIgnoreCase("1"))
				hasPhone = "true";
			else
				hasPhone = "false";
			if (Boolean.parseBoolean(hasPhone)) {
				Cursor phones = getContentResolver()
						.query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
								null,
								android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID
										+ " = " + contactId, null, null);
				while (phones.moveToNext()) {
					String phoneNumber = phones
							.getString(phones
									.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
					map.put(EUExCallback.F_JK_NUM, phoneNumber);
					break;
				}
				phones.close();
			}
			Cursor emails = getContentResolver()
					.query(android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
							null,
							android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID
									+ " = " + contactId, null, null);
			while (emails.moveToNext()) {
				String emailAddress = emails
						.getString(emails
								.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.DATA));
				map.put(EUExCallback.F_JK_EMAIL, emailAddress);
				break;
			}
			emails.close();
			map.put(IS_SELECT, false);
			list.add(map);
			contactsList.add(map);
		}
		cursor.close();
		for(int i=0; i < list.size(); i++){
			String name = (String) list.get(i).get(EUExCallback.F_JK_NAME);
			if(TextUtils.isEmpty(name)){
				list.remove(i);
			}
		}
		return list;
	}

	class ViewHolder {
		public TextView name;
		public TextView num;
		public TextView email;
		public CheckBox check;
	}

	/*
	 * listView的item选项监听
	 */
	class myOnItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			if (isShowSelectMode) {
				CheckBox check = (CheckBox) arg1
						.findViewById(finder
								.getId("check_box"));
				check.toggle();
				Map<String,Object> obj = list.get(arg2);
				obj.put(IS_SELECT, check.isChecked());
				hideKeyBoard();
			} else {
				try {
					Map<String, Object> mm = list.get(arg2);
					JSONArray jsonArray = new JSONArray();
					JSONObject jsonPerson = new JSONObject();
					jsonPerson.put(EUExCallback.F_JK_NAME,
							(String) mm.get(EUExCallback.F_JK_NAME));
					jsonPerson.put(EUExCallback.F_JK_NUM,
							(String) mm.get(EUExCallback.F_JK_NUM));
					jsonPerson.put(EUExCallback.F_JK_EMAIL,
							(String) mm.get(EUExCallback.F_JK_EMAIL));
					try {
						PFConcactMan.getValueWithName(ContactActivity.this,
								(String) mm.get("contactId"), jsonPerson);
					} catch (Exception e) {
						e.printStackTrace();
					}

					jsonArray.put(jsonPerson);
					Intent intent = new Intent(getIntent().getAction());
					intent.putExtra(F_INTENT_KEY_RETURN_SELECT_LIST,
							jsonArray.toString());
					setResult(Activity.RESULT_OK, intent);
					hideKeyBoard();
					finish();
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
		}
	}

	/*
	 * (non-Javadoc)所有的button监听
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == finder.getId("title_return")) {// 返回按鈕
			hideKeyBoard();
			finish();
		} else if (id == finder.getId("title_prompt")) {// 隱藏顯示多選操作按鈕
			if (m_select_layout.getVisibility() != View.VISIBLE) {// 如果多選欄沒有顯示
				m_select_layout.setVisibility(View.VISIBLE);
				isShowSelectMode = true;
				m_prompt.setBackgroundDrawable(cancelDrawable);
				m_prompt.setText(finder.getString("cancel"));
				m_select_layout.requestFocus();
			} else {
				m_prompt.setBackgroundDrawable(multiSelectDrawable);
				m_prompt.setText("");
				isShowSelectMode = false;
				m_select_layout.setVisibility(View.GONE);
				m_select_layout.requestFocus();
			}
			hideKeyBoard();
			adapter.notifyDataSetChanged();
		} else if (id == finder.getId("select_all")) {// 全選按鈕
			hideKeyBoard();
			showLoading("Waiting...");
			handler.sendEmptyMessage(handle_select_all);
		} else if (id == finder.getId("select_cancel")) {// 取消全選按鈕
			hideKeyBoard();
			showLoading("Waiting...");
			handler.sendEmptyMessage(handle_cancel_all);
		} else if (id == finder.getId("select_enter")) {// 確定多選按鈕
			hideKeyBoard();
			showLoading("Waiting...");
			handler.sendEmptyMessage(handle_select_enter);
		}
	}

	/*
	 * (non-Javadoc)监听autoCompleteTextView的焦点
	 * 
	 * @see
	 * android.view.View.OnFocusChangeListener#onFocusChange(android.view.View,
	 * boolean)
	 */
	@Override
	public void onFocusChange(View v, boolean hasFocus) {

	}

	/*
	 * 显示dialog的loading滚动
	 */
	private void showLoading(String text) {
		if (progress == null)
			progress = ProgressDialog.show(this, null, text);
	}

	/*
	 * 隐藏dialog的loading滚动
	 */
	private void hideLoading() {
		if (progress != null) {
			progress.dismiss();
			progress = null;
		}
	}

	/*
	 * 隐藏键盘
	 */
	private final void hideKeyBoard() {
		try {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(ContactActivity.this.getCurrentFocus()
					.getWindowToken(), 0);
		} catch (Exception e) {

		}
	}
}
