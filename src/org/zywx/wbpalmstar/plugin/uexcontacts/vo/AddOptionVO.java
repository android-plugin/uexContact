package org.zywx.wbpalmstar.plugin.uexcontacts.vo;

import java.io.Serializable;

public class AddOptionVO implements Serializable{
    private static final long serialVersionUID = 144142963107802948L;
    private boolean isNeedAlertDialog = true;

    public boolean isNeedAlertDialog() {
        return isNeedAlertDialog;
    }

    public void setIsNeedAlertDialog(boolean isNeedAlertDialog) {
        this.isNeedAlertDialog = isNeedAlertDialog;
    }
}
