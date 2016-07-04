package org.zywx.wbpalmstar.plugin.uexcontacts.vo;

import java.io.Serializable;

public class ModifyOptionVO implements Serializable {

    private static final long serialVersionUID = 8633063031728174340L;

    private String contactId = "";
    private String name = "";
    private String num = "";
    private String email = "";

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
