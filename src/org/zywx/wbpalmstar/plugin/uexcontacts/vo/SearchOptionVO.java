package org.zywx.wbpalmstar.plugin.uexcontacts.vo;

import java.io.Serializable;

public class SearchOptionVO implements Serializable{
    private static final long serialVersionUID = -693582908643862781L;
    private int resultNum = 50;

    public int getResultNum() {
        return resultNum;
    }

    public void setResultNum(int resultNum) {
        this.resultNum = resultNum;
    }
}
