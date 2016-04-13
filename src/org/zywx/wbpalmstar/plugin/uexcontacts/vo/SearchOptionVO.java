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

package org.zywx.wbpalmstar.plugin.uexcontacts.vo;

import java.io.Serializable;

public class SearchOptionVO implements Serializable{
    private static final long serialVersionUID = -693582908643862781L;
    private int resultNum = 50;
    private String contactId = "";
    private String searchName = "";
    private boolean isSearchNum = true;
    private boolean isSearchEmail = true;
    private boolean isSearchAddress = true;
    private boolean isSearchCompany = true;
    private boolean isSearchTitle = true;
    private boolean isSearchNote = true;
    private boolean isSearchUrl = true;
    
    public SearchOptionVO() {
    	
	}
    
	public SearchOptionVO(int resultNum, String contactId, String searchName,
			boolean isSearchNum, boolean isSearchEmail,
			boolean isSearchAddress, boolean isSearchCompany,
			boolean isSearchTitle, boolean isSearchNote, boolean isSearchUrl) {
		super();
		this.resultNum = resultNum;
		this.contactId = contactId;
		this.searchName = searchName;
		this.isSearchNum = isSearchNum;
		this.isSearchEmail = isSearchEmail;
		this.isSearchAddress = isSearchAddress;
		this.isSearchCompany = isSearchCompany;
		this.isSearchTitle = isSearchTitle;
		this.isSearchNote = isSearchNote;
		this.isSearchUrl = isSearchUrl;
	}


	public int getResultNum() {
		return resultNum;
	}

	public void setResultNum(int resultNum) {
		this.resultNum = resultNum;
	}

	public String getContactId() {
		return contactId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}

	public boolean isSearchNum() {
		return isSearchNum;
	}

	public void setSearchNum(boolean isSearchNum) {
		this.isSearchNum = isSearchNum;
	}

	public boolean isSearchEmail() {
		return isSearchEmail;
	}

	public void setSearchEmail(boolean isSearchEmail) {
		this.isSearchEmail = isSearchEmail;
	}

	public boolean isSearchAddress() {
		return isSearchAddress;
	}

	public void setSearchAddress(boolean isSearchAddress) {
		this.isSearchAddress = isSearchAddress;
	}

	public boolean isSearchCompany() {
		return isSearchCompany;
	}

	public void setSearchCompany(boolean isSearchCompany) {
		this.isSearchCompany = isSearchCompany;
	}

	public boolean isSearchTitle() {
		return isSearchTitle;
	}

	public void setSearchTitle(boolean isSearchTitle) {
		this.isSearchTitle = isSearchTitle;
	}

	public boolean isSearchNote() {
		return isSearchNote;
	}

	public void setSearchNote(boolean isSearchNote) {
		this.isSearchNote = isSearchNote;
	}

	public boolean isSearchUrl() {
		return isSearchUrl;
	}

	public void setSearchUrl(boolean isSearchUrl) {
		this.isSearchUrl = isSearchUrl;
	}
	
}
