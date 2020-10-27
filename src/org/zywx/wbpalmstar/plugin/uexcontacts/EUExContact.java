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

import android.Manifest;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexcontacts.vo.AddOptionVO;
import org.zywx.wbpalmstar.plugin.uexcontacts.vo.DeleteOptionVO;
import org.zywx.wbpalmstar.plugin.uexcontacts.vo.ModifyOptionVO;
import org.zywx.wbpalmstar.plugin.uexcontacts.vo.SearchOptionVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import a_vcard.android.syncml.pim.PropertyNode;
import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.VCardParser;

public class EUExContact extends EUExBase {

    private static final String TAG = "EUExContact";

    public static final String KEY_CONTACT_SEARCHITEM = "uexContact.cbSearchItem";
    public static final String KEY_CONTACT_OPEN = "uexContact.cbOpen";
    public static final String KEY_CONTACT_MULTIOPEN = "uexContact.cbMultiOpen";
    public static final String KEY_CONTACT_ADD = "uexContact.cbAddItem";
    public static final String KEY_CONTACT_MODIFYITEM = "uexContact.cbModifyItem";
    public static final String KEY_CONTACT_DELETEITEM = "uexContact.cbDeleteItem";
    public static final String KEY_CONTACT_SEARCH = "uexContact.cbSearch";
    public static final String KEY_CONTACT_MODIFYWITHID = "uexContact.cbModifyWithId";
    public static final String KEY_CONTACT_DELETEWITHID = "uexContact.cbDeleteWithId";

    public static final String JK_KEY_CONTACT_ID = "contactId";
    public static final String JK_KEY_CONTACT_LIST = "contactList";

    public static final int F_ACT_REQ_CODE_UEX_CONTACT = 2;
    public static final int F_ACT_REQ_CODE_UEX_MULTI_CONTACT = 9;
    private static final int REQUESTPERSSIONSCONTACT=4;
    private static final int REQUEST_PERSSIONS_RW_CONTACT = 20000;
    private static final int REQUEST_PERMISSIONS_RW_CONTACT_ADD_VCARD = 20001;
    private ResoureFinder finder = null;
    private Object accountType, accountName;

    private HashMap<Integer, String[]> functionParamsMap = new HashMap<>();

    private String openFuncId = null;
    private String multiOpenFuncId = null;
    private String addItemFuncId = null;
    private String deleteWithIdFuncId = null;
    private String deleteItemFuncId = null;
    private String searchFuncId = null;
    private String searchItemFuncId = null;
    private String modifyWithIdFuncId = null;
    private String modifyItemFuncId = null;
    private String addItemWithVCardFuncId = null;


    public EUExContact(Context context, EBrowserView inParent) {
        super(context, inParent);
        finder = ResoureFinder.getInstance(context);
    }

    public static boolean customLinkMan = false;

    private void requestRWContactPermissions(){
        requestRWContactPermissions(-1);
    }

    private void requestRWContactPermissions(int requestCode){
        if (requestCode == -1){
            requestCode = REQUEST_PERSSIONS_RW_CONTACT;
        }
        requsetPerssionsMore(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, "本次操作需要读写联系人的权限", requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        JSONObject jobj = new JSONObject();
        switch (requestCode) {
            case F_ACT_REQ_CODE_UEX_CONTACT:
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
                                JSONArray jsonArray = new JSONArray();
                                while (phones.moveToNext()) {
                                    String phoneNumber = phones
                                            .getString(phones
                                                    .getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
                                    boolean exist=false;
                                    for (int i=0;i<jsonArray.length();i++){
                                        if (jsonArray.get(i).equals(phoneNumber)){
                                            exist=true;
                                        }
                                    }
                                    if (!exist) {
                                        jsonArray.put(phoneNumber);
                                    }
                                }
                                jobj.put(EUExCallback.F_JK_NUM, jsonArray);
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
                        if (null != openFuncId) {
                            callbackToJs(Integer.parseInt(openFuncId), false,0,jobj);
                        } else {
                            jsCallback(EUExContact.KEY_CONTACT_OPEN, 0,
                                    EUExCallback.F_C_JSON, jobj.toString());
                        }
                    } catch (Exception e) {
                        Toast.makeText(mContext,
                                finder.getString("plugin_contact_open_fail"),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    if (null != openFuncId) {
                        callbackToJs(Integer.parseInt(openFuncId), false,-1);
                    } else {
                        jsCallback(EUExContact.KEY_CONTACT_OPEN, 1,
                                EUExCallback.F_C_JSON,null);
                    }
                    return;
                }
                break;
            case F_ACT_REQ_CODE_UEX_MULTI_CONTACT:
                if (resultCode == Activity.RESULT_OK) {
                    String result = data
                            .getStringExtra(ContactActivity.F_INTENT_KEY_RETURN_SELECT_LIST);
                    if (multiOpenFuncId != null) {
                        try {
                            JSONArray array = new JSONArray(result);
                            callbackToJs(Integer.parseInt(multiOpenFuncId), false,0, array);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        jsCallback(EUExContact.KEY_CONTACT_MULTIOPEN, 0,
                                EUExCallback.F_C_JSON, result);
                    }
                }else{
                    if (multiOpenFuncId != null) {
                        callbackToJs(Integer.parseInt(multiOpenFuncId), false,-1);

                    }
                }
                break;
        }

    }

    public void open(String[] parm) {
        if (parm != null && parm.length == 1) {
            openFuncId = parm[0];
        }

        requsetPerssions(Manifest.permission.READ_CONTACTS,"读取联系人权限!",REQUESTPERSSIONSCONTACT);
//        readContact();
    }

    @Override
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        String[] params = functionParamsMap.get(requestCode);
        if(requestCode==REQUESTPERSSIONSCONTACT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted 授予权限
                readContact();
            } else {
                // Permission Denied 权限被拒绝
                Toast.makeText(mContext, "为了不影响读取联系人，请开启相关权限!",
                        Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode==REQUEST_PERSSIONS_RW_CONTACT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted 授予权限
                // 权限申请成功，但是暂时不需要什么操作。
                BDebug.i(TAG, "onRequestPermissionResult REQUEST_PERSSIONS_RW_CONTACT");
            } else {
                // Permission Denied 权限被拒绝
                Toast.makeText(mContext, "为了不影响读写联系人，请开启相关权限!",
                        Toast.LENGTH_SHORT).show();
            }
        }else if (requestCode == REQUEST_PERMISSIONS_RW_CONTACT_ADD_VCARD){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted 授予权限
                BDebug.i(TAG, "onRequestPermissionResult PERMISSION_GRANTED REQUEST_PERMISSIONS_RW_CONTACT_ADD_VCARD");
                addItemWithVCard_Internal(params);
            } else {
                // Permission Denied 权限被拒绝
                BDebug.e(TAG, "onRequestPermissionResult PERMISSION_DENYED REQUEST_PERMISSIONS_RW_CONTACT_ADD_VCARD");
                Toast.makeText(mContext, "为了不影响读写联系人，请开启相关权限!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void readContact() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            intent.setAction(Intent.ACTION_PICK);
            intent.setData(android.provider.Contacts.People.CONTENT_URI);
        } else {
            intent.setAction(Intent.ACTION_PICK);
            intent.setData(android.provider.ContactsContract.Contacts.CONTENT_URI);
        }
        startActivityForResult(intent, F_ACT_REQ_CODE_UEX_CONTACT);
    }

    public void multiOpen(String[] parm) {
        if (parm != null && parm.length == 1) {
            multiOpenFuncId = parm[0];
        }
        try {
            Intent intent = new Intent();
            intent.setClass(mContext, ContactActivity.class);
            startActivityForResult(intent, F_ACT_REQ_CODE_UEX_MULTI_CONTACT);
        } catch (Exception e) {
            if (multiOpenFuncId != null) {
                callbackToJs(Integer.parseInt(multiOpenFuncId), false,1);
            } else {
                jsCallback(KEY_CONTACT_MULTIOPEN, 0, EUExCallback.F_C_INT,
                        EUExCallback.F_C_FAILED);
            }
        }
    }

    public void addItem(final String[] parm) {
        requestRWContactPermissions();
        if (parm == null || parm.length < 3)
            return;
        final String inName = parm[0];
        final String inNum = parm[1];
        final String inEmail = parm[2];
        boolean isNeedAlertDialog = true;
        if (parm.length == 4) {
            try {
                int temp = Integer.parseInt(parm[3]);
                addItemFuncId = parm[3];
            } catch (NumberFormatException e) { //如果报错了，那么第4个参数一定是业务参数
                String optionJson = parm[3];
                AddOptionVO dataVO = DataHelper.gson.fromJson(optionJson, AddOptionVO.class);
                if (dataVO != null) {
                    isNeedAlertDialog = dataVO.isNeedAlertDialog();
                }
            }
        } else if (parm.length == 5) {
            String optionJson = parm[3];
            AddOptionVO dataVO = DataHelper.gson.fromJson(optionJson, AddOptionVO.class);
            if (dataVO != null) {
                isNeedAlertDialog = dataVO.isNeedAlertDialog();
            }
            addItemFuncId = parm[4];
        }
        if (isNeedAlertDialog) {
            new AlertDialog.Builder(mContext)
                    .setTitle(finder.getStringId("prompt"))
                    .setMessage(finder.getStringId("plugin_contact_add_prompt"))
                    .setPositiveButton(finder.getStringId("confirm"),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            addContact(inName, inNum, inEmail);
                                        }
                                    }).start();
                                }
                            })
                    .setNegativeButton(finder.getStringId("cancel"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (addItemFuncId != null) {
                                callbackToJs(Integer.parseInt(addItemFuncId), false, -1);
                            }
                        }
                    })
                    .show();
        } else {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    addContact(inName, inNum, inEmail);
                }
            }).start();
        }
    }

    private void addContact(String inName, String inNum, String inEmail) {
        requestRWContactPermissions();
        if (inName != null && inName.length() > 0
                && inNum != null && inEmail != null) {
            if (mContext != null && PFConcactMan.add(mContext, inName,
                    inNum, inEmail)) {
                if (addItemFuncId != null) {
                    callbackToJs(Integer.parseInt(addItemFuncId), false, 0);
                } else {
                    jsCallback(KEY_CONTACT_ADD, 0,
                            EUExCallback.F_C_INT,
                            EUExCallback.F_C_SUCCESS);
                }
            } else {
                if (addItemFuncId != null) {
                    callbackToJs(Integer.parseInt(addItemFuncId), false, 1);
                } else {
                    jsCallback(KEY_CONTACT_ADD, 0,
                            EUExCallback.F_C_INT,
                            EUExCallback.F_C_FAILED);
                }
            }
        } else {
            if (addItemFuncId != null) {
                callbackToJs(Integer.parseInt(addItemFuncId), false, 1);
            } else {
                jsCallback(KEY_CONTACT_ADD, 0,
                        EUExCallback.F_C_INT,
                        EUExCallback.F_C_FAILED);
            }
        }
    }

    public void deleteWithId(final String[] parm) {
        requestRWContactPermissions();
        if (parm == null || parm.length < 1)
            return;
        if (parm.length == 2) {
            deleteWithIdFuncId = parm[1];
        }
        new AlertDialog.Builder(mContext)
                .setTitle(finder.getStringId("prompt"))
                .setMessage(finder.getStringId("plugin_contact_delete_prompt"))
                .setPositiveButton(finder.getStringId("confirm"),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        DeleteOptionVO deleteOptionVO = null;
                                        try {
                                            deleteOptionVO = DataHelper.gson.fromJson(parm[0],
                                                    DeleteOptionVO.class);
                                        } catch (Exception e) {
                                            if (BDebug.DEBUG){
                                                e.printStackTrace();
                                            }
                                        }
                                        if (deleteOptionVO != null) {
                                            if (mContext != null && PFConcactMan.deletesWithContactId(mContext, deleteOptionVO.getContactId())) {
                                                if (deleteWithIdFuncId != null) {
                                                    callbackToJs(Integer.parseInt(deleteWithIdFuncId), false, 0);
                                                } else {
                                                    jsCallback(KEY_CONTACT_DELETEWITHID, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_SUCCESS);
                                                }
                                            } else {
                                                if (deleteWithIdFuncId != null) {
                                                    callbackToJs(Integer.parseInt(deleteWithIdFuncId), false, 1);
                                                } else {
                                                    jsCallback(KEY_CONTACT_DELETEWITHID, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_FAILED);
                                                }
                                            }
                                        } else {
                                            if (deleteWithIdFuncId != null) {
                                                callbackToJs(Integer.parseInt(deleteWithIdFuncId), false, 1);
                                            } else {
                                                jsCallback(KEY_CONTACT_DELETEWITHID, 0,
                                                        EUExCallback.F_C_INT,
                                                        EUExCallback.F_C_FAILED);
                                            }
                                        }
                                    }
                                }).start();
                            }
                        })
                .setNegativeButton(finder.getStringId("cancel"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (deleteWithIdFuncId != null) {
                            callbackToJs(Integer.parseInt(deleteWithIdFuncId), false, -1);
                        }
                    }
                })
                .show();
    }

    public void deleteItem(String[] parm) {
        requestRWContactPermissions();
        if (parm == null || parm.length < 1)
            return;
        final String inName = parm[0];
        if (parm.length == 2) {
            deleteItemFuncId = parm[1];
        }
        new AlertDialog.Builder(mContext)
                .setTitle(finder.getStringId("prompt"))
                .setMessage(finder.getStringId("plugin_contact_delete_prompt"))
                .setPositiveButton(finder.getStringId("confirm"),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (inName != null && inName.length() > 0) {
                                            if (mContext != null && PFConcactMan.deletes(mContext, inName)) {
                                                if (deleteItemFuncId != null) {
                                                    callbackToJs(Integer.parseInt(deleteItemFuncId), false, 0);
                                                } else {
                                                    jsCallback(KEY_CONTACT_DELETEITEM, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_SUCCESS);
                                                }
                                            } else {
                                                if (deleteItemFuncId != null) {
                                                    callbackToJs(Integer.parseInt(deleteItemFuncId), false, 1);
                                                } else {
                                                    jsCallback(KEY_CONTACT_DELETEITEM, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_FAILED);
                                                }
                                            }
                                        } else {
                                            if (deleteItemFuncId != null) {
                                                callbackToJs(Integer.parseInt(deleteItemFuncId), false, 1);
                                            } else {
                                                jsCallback(KEY_CONTACT_DELETEITEM, 0,
                                                        EUExCallback.F_C_INT,
                                                        EUExCallback.F_C_FAILED);
                                            }
                                        }
                                    }
                                }).start();
                            }
                        })
                .setNegativeButton(finder.getStringId("cancel"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (deleteItemFuncId != null) {
                            callbackToJs(Integer.parseInt(deleteItemFuncId), false, -1);
                        }
                    }
                })
                .show();
    }

    public void search(final String[] parm) {
        if (parm == null || parm.length < 1)
            return;
        if (parm.length == 2) {
            searchFuncId = parm[1];
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                SearchOptionVO searchOptionVO = null;
                try {
                    String optionJson = parm[0];
                    searchOptionVO = DataHelper.gson.fromJson(optionJson,
                            SearchOptionVO.class);
                } catch (Exception e) {
                    if (BDebug.DEBUG){
                        e.printStackTrace();
                    }
                }
                if (searchOptionVO == null || mContext == null) {
                    searchCallback(false, null);
                    return;
                }
                int resultNum = searchOptionVO.getResultNum();
                JSONArray outJsonObj = PFConcactMan.search(mContext,
                        searchOptionVO);
                if (outJsonObj != null) {
                    if (resultNum == -1 || outJsonObj.length() < resultNum) {
                        searchCallback(true, outJsonObj);
                    } else {
                        int size = outJsonObj.length() / resultNum + 1;
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
                                for (int j = 0; j < resultNum; j++) {
                                    try {
                                        jsonArray.put(outJsonObj.get(index++));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            searchCallback(true, outJsonObj);
                        }
                    }
                } else {
                    searchCallback(false, null);
                }
            }
        }).start();
    }

    public void searchItem(final String[] parm) {
        if (parm == null || parm.length < 1)
            return;
        new Thread(new Runnable() {

            @Override
            public void run() {
                String inName = parm[0];
                int resultNum = 50;
                SearchOptionVO searchOptionVO = new SearchOptionVO();

                if (parm.length == 2) {
                    try {
                        int temp = Integer.parseInt(parm[1]); //如果是fuction
                        searchItemFuncId = parm[1];
                    } catch (NumberFormatException e) {
                        String optionJson = parm[1];
                        searchOptionVO = DataHelper.gson.fromJson(optionJson,
                                SearchOptionVO.class);
                        if (searchOptionVO != null) {
                            resultNum = searchOptionVO.getResultNum();
                        }
                    }
                }
                if (parm.length == 3) {
                    String optionJson = parm[1];
                    searchOptionVO = DataHelper.gson.fromJson(optionJson,
                            SearchOptionVO.class);
                    if (searchOptionVO != null) {
                        resultNum = searchOptionVO.getResultNum();
                    }
                    searchItemFuncId = parm[2];
                }

                searchOptionVO.setSearchName(inName);
                if (mContext != null && inName != null && inName.length() >= 0) {
                    JSONArray outJsonObj = PFConcactMan.search(mContext, searchOptionVO);
                    if (outJsonObj != null) {
                        if (resultNum == -1) {
                            if (searchItemFuncId != null) {
                                callbackToJs(Integer.parseInt(searchItemFuncId), false,0, outJsonObj);
                            } else {
                                jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_JSON, outJsonObj.toString());
                            }
                        } else {
                            if (outJsonObj.length() < resultNum) {//if lenght < resultNum
                                if (searchItemFuncId != null) {
                                    callbackToJs(Integer.parseInt(searchItemFuncId), false,0, outJsonObj);
                                } else {
                                    jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_JSON, outJsonObj.toString());
                                }
                            } else {//if length >resultNum
                                int size = outJsonObj.length() / resultNum + 1;
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
                                        for (int j = 0; j < resultNum; j++) {
                                            try {
                                                jsonArray.put(outJsonObj.get(index++));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    if (searchItemFuncId != null) {
                                        callbackToJs(Integer.parseInt(searchItemFuncId), false,0, jsonArray);
                                    } else {
                                        jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_JSON, jsonArray.toString());
                                    }
                                }
                            }
                        }
                    } else {
                        if (searchItemFuncId != null) {
                            callbackToJs(Integer.parseInt(searchItemFuncId), false, 1);
                        } else {
                            jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                        }
                    }
                } else {
                    if (searchItemFuncId != null) {
                        callbackToJs(Integer.parseInt(searchItemFuncId), false, 1);
                    } else {
                        jsCallback(KEY_CONTACT_SEARCHITEM, 0, EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
                    }
                }
            }
        }).start();
    }

    public void modifyWithId(final String[] parm) {
        requestRWContactPermissions();
        if (parm == null || parm.length < 1)
            return;
        if (parm.length == 2) {
            modifyWithIdFuncId = parm[1];
        }
        new AlertDialog.Builder(mContext)
                .setTitle(finder.getStringId("prompt"))
                .setMessage(finder.getStringId("plugin_contact_modify_prompt"))
                .setPositiveButton(/* "是" */finder.getStringId("confirm"),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        ModifyOptionVO modifyOptionVO = null;
                                        try {
                                            modifyOptionVO = DataHelper.gson.fromJson(parm[0],
                                                    ModifyOptionVO.class);
                                        } catch (Exception e) {
                                            if (BDebug.DEBUG){
                                                e.printStackTrace();
                                            }
                                        }
                                        if (modifyOptionVO != null) {
                                            if (mContext != null && PFConcactMan.modify(mContext, modifyOptionVO)) {
                                                if (modifyWithIdFuncId != null) {
                                                    callbackToJs(Integer.parseInt(modifyWithIdFuncId), false, 0);
                                                } else {
                                                    jsCallback(KEY_CONTACT_MODIFYWITHID, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_SUCCESS);
                                                }
                                            } else {
                                                if (modifyWithIdFuncId != null) {
                                                    callbackToJs(Integer.parseInt(modifyWithIdFuncId), false, 1);
                                                } else {
                                                    jsCallback(KEY_CONTACT_MODIFYWITHID, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_FAILED);
                                                }
                                            }
                                        } else {
                                            if (modifyWithIdFuncId != null) {
                                                callbackToJs(Integer.parseInt(modifyWithIdFuncId), false, 1);
                                            } else {
                                                jsCallback(KEY_CONTACT_MODIFYWITHID, 0,
                                                        EUExCallback.F_C_INT,
                                                        EUExCallback.F_C_FAILED);
                                            }
                                        }
                                    }
                                }).start();
                            }
                        })
                .setNegativeButton(finder.getStringId("cancel"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (modifyWithIdFuncId != null) {
                            callbackToJs(Integer.parseInt(modifyWithIdFuncId), false, -1);
                        }
                    }
                })
                .show();
    }


    public void modifyItem(String[] parm) {
        requestRWContactPermissions();
        if (parm == null || parm.length < 3)
            return;
        final String inName = parm[0];
        final String inNum = parm[1];
        final String inEmail = parm[2];

        if (parm.length == 4) {
            modifyItemFuncId = parm[3];
        }
        new AlertDialog.Builder(mContext)
                .setTitle(finder.getStringId("prompt"))
                .setMessage(finder.getStringId("plugin_contact_modify_prompt"))
                .setPositiveButton(finder.getStringId("confirm"),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (inName != null && inName.length() > 0
                                                && inNum != null && inEmail != null) {
                                            if (mContext != null && PFConcactMan.modify(mContext, inName,
                                                    inNum, inEmail)) {
                                                if (modifyItemFuncId != null) {
                                                    callbackToJs(Integer.parseInt(modifyItemFuncId), false, 0);
                                                } else {
                                                    jsCallback(KEY_CONTACT_MODIFYITEM, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_SUCCESS);
                                                }
                                            } else {
                                                if (modifyItemFuncId != null) {
                                                    callbackToJs(Integer.parseInt(modifyItemFuncId), false, 1);
                                                } else {
                                                    jsCallback(KEY_CONTACT_MODIFYITEM, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_FAILED);
                                                }
                                            }
                                        } else {
                                            if (modifyItemFuncId != null) {
                                                callbackToJs(Integer.parseInt(modifyItemFuncId), false, 1);
                                            } else {
                                                jsCallback(KEY_CONTACT_MODIFYITEM, 0,
                                                        EUExCallback.F_C_INT,
                                                        EUExCallback.F_C_FAILED);
                                            }
                                        }
                                    }
                                }).start();
                            }
                        })
                .setNegativeButton(finder.getStringId("cancel"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (modifyItemFuncId != null) {
                            callbackToJs(Integer.parseInt(modifyItemFuncId), false, -1);
                        }
                    }
                })
                .show();
    }

    public static String types[] = {"N", "TEL", "EMAIL", "ADR", "ORG",
            "TITLE", "URL", "NOTE"};

    public void addItemWithVCard(final String[] parm){
        functionParamsMap.put(REQUEST_PERMISSIONS_RW_CONTACT_ADD_VCARD, parm);
        requestRWContactPermissions(REQUEST_PERMISSIONS_RW_CONTACT_ADD_VCARD);
    }

    public void addItemWithVCard_Internal(final String[] parm) {
        if (parm == null || parm.length < 2)
            return;
        if (parm.length == 3) {
            addItemWithVCardFuncId = parm[2];
        }
        if ("1".equals(parm[1])) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    VCardParser parser = new VCardParser();
                    VDataBuilder builder = new VDataBuilder();
                    try {
                        boolean parsed = parser.parse(parm[0],
                                "UTF-8", builder);
                        // get all parsed contacts
                        List<VNode> pimContacts = builder.vNodeList;
                        Map<String, Object> contactMap = new HashMap<>();
                        // do something for all the contacts
                        for (VNode contact : pimContacts) {
                            ArrayList<PropertyNode> props = contact.propList;

                            // contact name - FN property
                            String name = null;
                            for (PropertyNode prop : props) {
                                for (String type : types) {
                                    if (type.equals(prop.propName)) {
                                        if (prop.paramMap_TYPE != null
                                                && prop.paramMap_TYPE.size() > 0
                                                && (type.equals("TEL") || type.equals("EMAIL"))){
                                            // 细分了类型(note: 目前仅支持TEL和EMAIL的细分类型的处理，如果要增加类型，则需要同时修改PFContactMan类里的逻辑，不要忘记)
                                            HashMap<Set<String>, String> paramTypeMap  = null;
                                            Object paramTypeObject  = contactMap.get(type);
                                            // 如果已经有多个相同type的数据，则取出上一个数据
                                            if (!(paramTypeObject instanceof HashMap)){
                                                paramTypeMap = new HashMap<>();
                                            }else{
                                                paramTypeMap = (HashMap)paramTypeObject;
                                            }
                                            paramTypeMap.put(prop.paramMap_TYPE, prop.propValue);
                                            contactMap.put(type, paramTypeMap);
                                        }else{
                                            // 没有细分类型
                                            contactMap.put(type,
                                                    prop.propValue);
                                        }
                                    }
                                }

                            }
                        }
                        if (PFConcactMan.add(mContext, contactMap,
                                accountName, accountType)) {
                            if (null != addItemWithVCardFuncId) {
                                callbackToJs(Integer.parseInt(addItemWithVCardFuncId), false, 0);
                            } else {
                                jsCallback(KEY_CONTACT_ADD, 0,
                                        EUExCallback.F_C_INT,
                                        EUExCallback.F_C_SUCCESS);
                            }
                        } else {
                            if (null != addItemWithVCardFuncId) {
                                callbackToJs(Integer.parseInt(addItemWithVCardFuncId), false, 1);
                            } else {
                                jsCallback(KEY_CONTACT_ADD, 0,
                                        EUExCallback.F_C_INT,
                                        EUExCallback.F_C_FAILED);
                            }
                        }
                    } catch (Exception e) {
                        if (null != addItemWithVCardFuncId) {
                            callbackToJs(Integer.parseInt(addItemWithVCardFuncId), false, 1);
                        } else {
                            jsCallback(KEY_CONTACT_ADD, 0,
                                    EUExCallback.F_C_INT,
                                    EUExCallback.F_C_FAILED);
                        }
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            new AlertDialog.Builder(mContext)
                    .setTitle(finder.getStringId("prompt"))
                    .setMessage(finder.getStringId("plugin_contact_add_prompt"))
                    .setPositiveButton(finder.getStringId("confirm"),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    new Thread(new Runnable() {

                                        @Override
                                        public void run() {
                                            VCardParser parser = new VCardParser();
                                            VDataBuilder builder = new VDataBuilder();
                                            try {
                                                boolean parsed = parser.parse(parm[0],
                                                        "UTF-8", builder);
                                                // get all parsed contacts
                                                List<VNode> pimContacts = builder.vNodeList;
                                                Map<String, Object> contactMap = new HashMap<>();
                                                // do something for all the contacts
                                                for (VNode contact : pimContacts) {
                                                    ArrayList<PropertyNode> props = contact.propList;

                                                    // contact name - FN property
                                                    String name = null;
                                                    for (PropertyNode prop : props) {
                                                        for (String type : types) {
                                                            if (type.equals(prop.propName)) {
                                                                if (prop.paramMap_TYPE != null
                                                                        && prop.paramMap_TYPE.size() > 0
                                                                        && (type.equals("TEL") || type.equals("EMAIL"))){
                                                                    // 细分了类型(note: 目前仅支持TEL和EMAIL的细分类型的处理，如果要增加类型，则需要同时修改PFContactMan类里的逻辑，不要忘记)
                                                                    HashMap<Set<String>, String> paramTypeMap  = null;
                                                                    Object paramTypeObject  = contactMap.get(type);
                                                                    // 如果已经有多个相同type的数据，则取出上一个数据
                                                                    if (!(paramTypeObject instanceof HashMap)){
                                                                        paramTypeMap = new HashMap<>();
                                                                    }else{
                                                                        paramTypeMap = (HashMap)paramTypeObject;
                                                                    }
                                                                    paramTypeMap.put(prop.paramMap_TYPE, prop.propValue);
                                                                    contactMap.put(type, paramTypeMap);
                                                                }else{
                                                                    // 没有细分类型
                                                                    contactMap.put(type,
                                                                            prop.propValue);
                                                                }
                                                            }
                                                        }

                                                    }
                                                }
                                                if (PFConcactMan.add(mContext, contactMap,
                                                        accountName, accountType)) {
                                                    if (null != addItemWithVCardFuncId) {
                                                        callbackToJs(Integer.parseInt(addItemWithVCardFuncId), false, 0);
                                                    } else {
                                                        jsCallback(KEY_CONTACT_ADD, 0,
                                                                EUExCallback.F_C_INT,
                                                                EUExCallback.F_C_SUCCESS);
                                                    }
                                                } else {
                                                    if (null != addItemWithVCardFuncId) {
                                                        callbackToJs(Integer.parseInt(addItemWithVCardFuncId), false, 1);
                                                    } else {
                                                        jsCallback(KEY_CONTACT_ADD, 0,
                                                                EUExCallback.F_C_INT,
                                                                EUExCallback.F_C_FAILED);
                                                    }
                                                }
                                            } catch (Exception e) {
                                                if (null != addItemWithVCardFuncId) {
                                                    callbackToJs(Integer.parseInt(addItemWithVCardFuncId), false, 1);
                                                } else {
                                                    jsCallback(KEY_CONTACT_ADD, 0,
                                                            EUExCallback.F_C_INT,
                                                            EUExCallback.F_C_FAILED);
                                                }
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();
                                }

                            })
                    .setNegativeButton(/* "否" */finder.getStringId("cancel"), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (null != addItemWithVCardFuncId) {
                                callbackToJs(Integer.parseInt(addItemWithVCardFuncId), false, -1);
                            }
                        }
                    })
                    .show();
        }
    }

    @Override
    public boolean clean() {
        return false;
    }

    /**
     * Obtain the AuthenticatorDescription for a given account type.
     *
     * @param type       The account type to locate.
     * @param dictionary An array of AuthenticatorDescriptions, as returned by
     *                   AccountManager.
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

    private void searchCallback(boolean isSuccess, JSONArray inData) {
        if (searchFuncId != null) {
            callbackToJs(Integer.parseInt(searchFuncId), false,isSuccess?0:1,inData);
        } else {
            JSONObject jsonCallback = new JSONObject();
            try {
                jsonCallback.put(EUExCallback.F_JK_RESULT,
                        isSuccess ? EUExCallback.F_C_SUCCESS : EUExCallback.F_C_FAILED);
                jsonCallback.put(JK_KEY_CONTACT_LIST, inData == null ? new JSONArray() : inData);
            } catch (Exception e) {
            }
            jsJsonCallback(KEY_CONTACT_SEARCH, jsonCallback.toString());
        }
    }

    public void jsJsonCallback(String inCallbackName, String inData) {
        String js = SCRIPT_HEADER + "if(" + inCallbackName + "){"
                + inCallbackName + "(" + inData + SCRIPT_TAIL;
        onCallback(js);
    }
}
