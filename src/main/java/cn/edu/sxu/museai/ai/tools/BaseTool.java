package cn.edu.sxu.museai.ai.tools;

import cn.edu.sxu.museai.constant.AppConstant;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.json.JSONObject;

public abstract class BaseTool {

    public abstract String getToolName();

    public abstract String getToolDescription();

    public abstract String getResultDescription(JSONObject json);

    protected String projectRootPath(Long appId) {
        return AppConstant.CODE_BATH_PATH + "/"
                + CodeGenTypeEnum.VUE.getValue() + "/"
                + appId;
    }
}
