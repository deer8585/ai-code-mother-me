package com.wyb.aicodemotherme.core.saver;

import com.wyb.aicodemotherme.ai.model.HtmlCodeResult;
import com.wyb.aicodemotherme.ai.model.MultiFileCodeResult;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存执行器
 * 根据代码生成类型执行相应的保存逻辑
 */

/**
 * 调用链是：
 * saveCode(result, 1001)（父类）
 * → 2) validateInput(result)（子类覆盖：检查 htmlCode 不为空）
 * → 3) buildUniqueDir(1001)（父类：生成 tmp/code_output/html_1001）
 * → 4) saveFiles(result, "tmp/code_output/html_1001")（子类：写 index.html）
 * → 5) writeToFile(...)（父类：真正落盘写文件）
 * → 6) 返回 new File("tmp/code_output/html_1001")
 */
public class CodeFileSaverExecutor {
    public static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();
    public static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaver = new MultiFileCodeFileSaverTemplate();

    /**
     * 执行代码保存
     * @param codeResult
     * @param codeGenType
     * @param appId
     * @return
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
      return switch (codeGenType) {
          case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult,appId);
          case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult,appId);
          default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型: " + codeGenType);
      };
    }
}