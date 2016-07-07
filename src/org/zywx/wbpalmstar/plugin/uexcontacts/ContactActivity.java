/*
 *  Copyright (C) 2014 The AppCan Open Source Project.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.zywx.wbpalmstar.plugin.uexcontacts;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexcontacts.vo.ContactVO;
import org.zywx.wbpalmstar.plugin.uexcontacts.vo.SearchOptionVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.provider.ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS;

public class ContactActivity extends Activity implements OnClickListener,
        OnFocusChangeListener {
    public static String F_INTENT_KEY_RETURN_SELECT_LIST = "returnSelectList";
    private static final String CONTACTS_SORT = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " COLLATE LOCALIZED ASC";

    private final int handle_select_all = 300;
    private final int handle_cancel_all = 400;
    private final int handle_select_enter = 500;
    private final int handle_auto_select = 600;

    public static String IS_SELECT = "select";
    private JSONObject m_content;
    private final static String CONTACT_PHOTO = "photo";
    private final static String SORT_KEY = "sort";
    private TextView m_return;// 返回按钮
    private TextView m_prompt;// 显示选择Group组件
    private ListView listView = null;
    private RelativeLayout m_select_layout;
    private TextView m_select_all;// 全选按钮
    private TextView m_select_enter;// 确定按钮
    private ContactAdapter adapter = null;
    private boolean isShowSelectMode = false;
    private ProgressDialog progress = null;
    private ResoureFinder finder = null;
    private AutoCompleteTextView autoText = null;
    private AutoAdapter aAdapter = null;// 自定义的adapter提供给AutoCompleteTextView使用

    private boolean mIsSelectAll=false;
    public JSONObject getContent() {
        return m_content;
    }

    public void setContent(JSONObject content) {
        m_content = content;
    }

    List<ContactVO> mAllContactList = new ArrayList<ContactVO>();

    private static final String[] CONTACT_DETAILS_PROJECTION = {
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.MIMETYPE,
            FORMATTED_ADDRESS,
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.TYPE,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
    };

    /*
 * Map of all contacts by lookup key (ContactsContract.Contacts.LOOKUP_KEY).
 * We use this to find the contacts when the contact details are loaded.
 */
    private Map<String, ContactVO> mContactsByLookupKey = new HashMap<String, ContactVO>();


    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    adapter = new ContactAdapter();

                    listView.setAdapter(adapter);
                    aAdapter = new AutoAdapter(ContactActivity.this, null);
                    autoText.setAdapter(aAdapter);
                    listView.setOnItemClickListener(new myOnItemClickListener());

                    hideLoading();
                    break;
                case handle_select_all:
                    for (ContactVO contactVO : mAllContactList) {
                        contactVO.setSelect(true);
                    }
                    adapter.notifyDataSetChanged();
                    hideLoading();
                    break;
                case handle_cancel_all:
                    for (ContactVO contactVO : mAllContactList) {
                        contactVO.setSelect(false);
                    }
                    adapter.notifyDataSetChanged();
                    hideLoading();
                    break;
                case handle_select_enter:
                    JSONArray jsonArray = new JSONArray();
                    try {
                        for (int j = 0, size = mAllContactList.size(); j < size; j++) {
                            ContactVO contactVO = mAllContactList.get(j);
                            boolean bool= contactVO.isSelect();
                            if (bool) {
                                    JSONObject jsonPeople = new JSONObject();
                                    String name = contactVO.getDisplayName();
                                    jsonPeople.put(EUExCallback.F_JK_NAME, name);
                                    jsonPeople.put(EUExCallback.F_JK_NUM, contactVO.getPhone());
                                    jsonPeople.put(EUExCallback.F_JK_EMAIL, contactVO.getEmail());
                                    PFConcactMan.getValueWithName(ContactActivity.this,
                                            String.valueOf(contactVO.getId()), jsonPeople, new SearchOptionVO());
                                    jsonArray.put(jsonPeople);
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
                    for (ContactVO contactVO : mAllContactList) {
                        String name =contactVO.getDisplayName();
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

        showLoading("Waiting...");

        m_return = (TextView) findViewById(finder
                .getId("title_return"));
        m_prompt = (TextView) findViewById(finder
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
        m_select_layout = (RelativeLayout) findViewById(finder
                .getId("select_group"));
        m_select_all = (TextView) findViewById(finder
                .getId("select_all"));
        m_select_enter = (TextView) findViewById(finder
                .getId("select_enter"));

        m_return.setOnClickListener(this);
        m_prompt.setOnClickListener(this);

        m_select_all.setOnClickListener(this);
        m_select_enter.setOnClickListener(this);

        listView = (ListView) findViewById(finder
                .getId("plugin_contact_listview"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                getData();
                handler.sendEmptyMessage(100);
            }
        }).start();

    }

    private void getData() {

        long startTime = System.currentTimeMillis();
        ContentResolver cr = getContentResolver();
        /* 2.0 */
        // 索爱特例使用
        String tmpSort = "";// 缓存得到的sort_key
        Cursor cursor = null;
        try {// 如果数据库没有此列，则会报错
            cursor = cr.query(
                    android.provider.ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, CONTACTS_SORT);
        } catch (Exception e) {// 启用普通联系人号码
            BDebug.e(e.getMessage());
            cursor = cr.query(
                    android.provider.ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);
        }

        while (cursor.moveToNext()) {
            ContactVO contactVO = ContactVO.fromCursor(cursor);
            mAllContactList.add(contactVO);
            // LOOKUP_KEY is the one we use to retrieve the contact when the contact details are loaded
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            mContactsByLookupKey.put(lookupKey, contactVO);
        }
        cursor.close();

        Cursor detailCursor = getContentResolver()
                .query(ContactsContract.Data.CONTENT_URI,
                        CONTACT_DETAILS_PROJECTION,
                        null, null, null);
        readContactDetails(detailCursor);
        detailCursor.close();

        BDebug.e("time--------------------", System.currentTimeMillis() - startTime);
    }

    public class ContactAdapter extends BaseAdapter{

        public int getViewTypeCount() {
            return 2;
        }

        ;

        public int getItemViewType(int position) {
            String sort = (String) mAllContactList.get(position).getSort();
            return sort == null ? 0 : 1;
        }

        ;

        public boolean isEnabled(int position) {
            String sort = (String) mAllContactList.get(position).getSort();
            return sort == null;
        }

        @Override
        public int getCount() {
            return mAllContactList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAllContactList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

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
            ContactVO contactVO = mAllContactList.get(position);
            if (getItemViewType(position) == 0) {
                holder.name.setText(contactVO.getDisplayName());
                if (contactVO.getPhone() != null && contactVO.getPhone().size() > 0) {
                    holder.num.setText(contactVO.getPhone().get(0));
                } else {
                    holder.num.setText("");
                }

                if (contactVO.getEmail() != null && contactVO.getEmail().size() > 0) {
                    holder.email.setText(contactVO.getEmail().get(0));
                } else {
                    holder.email.setText("");
                }
                if (isShowSelectMode) {
                    holder.check.setVisibility(View.VISIBLE);
                    holder.check.setChecked(contactVO.isSelect());
                } else {
                    holder.check.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.name.setText(contactVO.getDisplayName());
            }
            return convertView;
        }
    }


    private void readContactDetails(Cursor detailCursor) {
        if (detailCursor != null && detailCursor.moveToFirst()) {
            detailCursor.moveToPrevious();
            while (detailCursor.moveToNext()) {
                String lookupKey = detailCursor.getString(detailCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY));
                ContactVO contact = mContactsByLookupKey.get(lookupKey);

                if (contact != null) {
                    readContactDetails(detailCursor, contact);
                }
            }
        }
    }


    private void readContactDetails(Cursor cursor, ContactVO contact) {
        String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
        if (mime.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
            String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            int type = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
            if (email != null) {
                contact.setEmail(type, email);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
            String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            int type = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            if (phone != null) {
                contact.setPhone(type, phone);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
            String firstName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            String lastName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            if (firstName != null) contact.setFirstName(firstName);
            if (lastName != null) contact.setLastName(lastName);
        } else if (mime.equals(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)) {
            int groupId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID));
            contact.addGroupId(groupId);
        }
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
                ContactVO contactVO = mAllContactList.get(arg2);
                contactVO.setSelect(check.isChecked());
                hideKeyBoard();
            } else {
                try {
                    ContactVO contactVO= mAllContactList.get(arg2);
                    JSONArray jsonArray = new JSONArray();
                    JSONObject jsonPerson = new JSONObject();
                    jsonPerson.put(EUExCallback.F_JK_NAME,
                            contactVO.getDisplayName());
                    jsonPerson.put(EUExCallback.F_JK_NUM,
                            contactVO.getPhone());
                    jsonPerson.put(EUExCallback.F_JK_EMAIL,
                            contactVO.getEmail());
                    try {
                        PFConcactMan.getValueWithName(ContactActivity.this,
                                String.valueOf( contactVO.getId()), jsonPerson, new SearchOptionVO());
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
                m_prompt.setText(finder.getString("cancel"));
                m_select_layout.requestFocus();
            } else {
                m_prompt.setText(finder.getString("plugin_contact_multi_select"));
                isShowSelectMode = false;
                m_select_layout.setVisibility(View.GONE);
                m_select_layout.requestFocus();
            }
            hideKeyBoard();
            adapter.notifyDataSetChanged();
        } else if (id == finder.getId("select_all")) {// 全選按鈕
            if (mIsSelectAll){
                mIsSelectAll=false;
                m_select_all.setText("全选");
                hideKeyBoard();
                showLoading("Waiting...");
                handler.sendEmptyMessage(handle_cancel_all);
            }else{
                mIsSelectAll=true;
                m_select_all.setText("取消全选");
                hideKeyBoard();
                showLoading("Waiting...");
                handler.sendEmptyMessage(handle_select_all);
            }
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
