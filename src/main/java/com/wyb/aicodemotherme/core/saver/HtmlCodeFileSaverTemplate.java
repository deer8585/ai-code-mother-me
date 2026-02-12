package com.wyb.aicodemotherme.core.saver;

import cn.hutool.core.util.StrUtil;
import com.wyb.aicodemotherme.ai.model.HtmlCodeResult;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;

/**
 * Html代码文件保存器
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    /**
     * 返回生成代码类型
     * @return
     */
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    /**
     * 保存文件
     * @param result
     * @param baseDirPath
     */
    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath,"index.html",result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        //HTML代码不能为空
        if(StrUtil.isBlank(result.getHtmlCode())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"htmlCode不能为空");
        }
    }

    }