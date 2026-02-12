package com.wyb.aicodemotherme.core.parser;

import com.wyb.aicodemotherme.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提取 HTML 单文件代码
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    //```html\\s*\\n  匹配代码块开头：```html 后面允许空格 然后必须有一个换行
    //([\\s\\S]*?) 这就是捕获组 1,html的代码块。因为它被 () 包起来了,它匹配“任意字符（包含换行）”，并且 *? 是非贪婪（尽量少匹配）
    //```匹配代码块结尾
    //所以，整个匹配结构是：[开头部分]  ( [内容部分] )  [结尾部分]
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    /**
     * 解析 HTML 单文件代码
     */
    public  HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        //!htmlCode.trim().isEmpty()：去掉空白后不为空（防止只有空格/换行）
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            //把提取出来的 HTML 内容去掉首尾空白后，设置到结果对象里。
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果没有找到代码块，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 提取 HTML代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private static String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}