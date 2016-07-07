package org.zywx.wbpalmstar.plugin.uexcontacts.vo;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ylt on 16/7/5.
 */

public class ContactVO implements Serializable {

    private static final Pattern CONTACT_LETTER = Pattern.compile("[^a-zA-Z]*([a-zA-Z]).*");


    private static final long serialVersionUID = -9039895412857775382L;

    public static ContactVO fromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
        String[] names = displayName != null ? displayName.split("\\s+") : new String[]{"---", "---"};
        String firstName = names.length >= 1 ? names[0] : displayName;
        String lastName = names.length >= 2 ? names[1] : "";
        String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
        Uri uri = photoUri != null ? Uri.parse(photoUri) : null;
        return new ContactVO(id, lookupKey, displayName, firstName, lastName, uri);
    }
    final private long mId;
    private String mDisplayName;

    final private String mLookupKey;
    private String mFirstName = "";
    private String mLastName = "";
    private List<String> mEmail = new ArrayList<String>();
    private List<String> mPhone = new ArrayList<String>();
    transient private Uri mPhotoUri;
    private Set<Long> mGroupIds = new HashSet<Long>();

    public boolean isSelect() {
        return mIsSelect;
    }

    public void setSelect(boolean select) {
        mIsSelect = select;
    }

    public String getSort() {
        return mSort;
    }

    public void setSort(String sort) {
        mSort = sort;
    }

    private  boolean mIsSelect = false;
    private  String mSort;
    private char mContactLetterBadge;
    private char mContactLetterScroll;

    protected ContactVO(long id, String lookupKey, String displayName, String firstName, String lastName, Uri photoUri) {
        mId = id;
        mDisplayName = TextUtils.isEmpty(displayName) ? "---" : displayName;

        mLookupKey = lookupKey;
        mFirstName =TextUtils.isEmpty(firstName) ? "---" : firstName;
        mLastName = TextUtils.isEmpty(lastName) ? "---" : lastName;
        mPhotoUri = photoUri;
    }
    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public List<String> getEmail() {
        return mEmail;
    }

    public List<String> getPhone() {
        return mPhone;
    }

    public char getContactLetter() {
        if (mContactLetterBadge == 0) {
            Matcher m = CONTACT_LETTER.matcher(getDisplayName());
            String letter = m.matches() ? m.group(1).toUpperCase(Locale.US) : "?";
            mContactLetterBadge = TextUtils.isEmpty(letter) ? '?' : letter.charAt(0);
        }

        return mContactLetterBadge;
    }

    public long getId() {
        return mId;
    }


    public String getDisplayName() {
        return mDisplayName != null ? mDisplayName : "";
    }


    /**
     * Matches:
     * https://developer.android.com/reference/android/provider/ContactsContract.ContactsColumns.html#LOOKUP_KEY
     *
     * Used as unique key to cache contact pictures for a specific contact and also to create the
     * contact Uri: ContactsContract.Contacts.CONTENT_LOOKUP_URI + "/" + LOOKUP_KEY
     */
    public String getLookupKey() {
        return mLookupKey;
    }

    public Uri getPhotoUri() {
        return mPhotoUri;
    }

    public Set<Long> getGroupIds() {
        return mGroupIds;
    }

    public void setFirstName(String value) {
        mFirstName = value;
    }

    public void setLastName(String value) {
        mLastName = value;
    }

    public void setEmail(int type, String value) {
        if (mEmail==null){
            mEmail=new ArrayList<String>();
        }
        mEmail.add(value);
    }

    public void setPhone(int type, String value) {
        if (mPhone==null){
            mPhone=new ArrayList<String>();
        }
        mPhone.add(value);
    }

    protected void setPhotoUri(Uri photoUri) {
        mPhotoUri = photoUri;
    }

    public void addGroupId(long value) {
        mGroupIds.add(value);
    }

    @Override
    public String toString() {
        return super.toString() + ", " + mFirstName + " " + mLastName + ", " + mEmail;
    }





}
