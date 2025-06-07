package com.ityanyu.yanyupicturebackend.manager.email;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ityanyu.yanyupicturebackend.model.enums.UserEmailCodeTypeEnum;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

/**
* @author: dl
* @Date: 2025/4/13
* @Description: 验证获取操作安全证书（邮箱登录）
**/
public class CheckCodeUtils {
    // 常量配置，可根据实际情况修改
    private static final String SMTP_HOST = "smtp.qq.com";
    private static final String SENDER_EMAIL = "3284513242@qq.com";
    private static final String SENDER_AUTH_CODE = "lmffrizkfgzydaia";
    private static final String SENDER_NAME = "烟雨";
    private static final String EMAIL_SUBJECT = "验证码";
    private static final int SSL_SMTP_PORT = 465;
    private static final Logger LOGGER = Logger.getLogger(CheckCodeUtils.class.getName());

    /**
     * 发送验证码
     * @param targetEmail
     * @param authCode
     * @return
     */
    public static String getEmailCode(String targetEmail, String authCode,String type) {
        try {
            // 使用HtmlEmail代替SimpleEmail以支持HTML内容
            HtmlEmail mail = new HtmlEmail();
            //设置UTF-8编码
            mail.setCharset("UTF-8");
            // 设置发送邮件的服务器
            mail.setHostName(SMTP_HOST);
            // 设置邮箱认证信息
            mail.setAuthentication(SENDER_EMAIL, SENDER_AUTH_CODE);
            // 设置发件人信息
            mail.setFrom(SENDER_EMAIL, SENDER_NAME);
            // 设置发送服务端口
            mail.setSslSmtpPort(String.valueOf(SSL_SMTP_PORT));
            // 使用安全链接
            mail.setSSLOnConnect(true);
            System.setProperty("mail.smtp.ssl.enable", "true");
            System.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
            // 接收用户的邮箱
            mail.addTo(targetEmail);
            // 邮件的主题(标题)
            UserEmailCodeTypeEnum typeEnum = UserEmailCodeTypeEnum.getByValue(type);
            String subject = String.format("%s%s",typeEnum.getText(),EMAIL_SUBJECT);
            mail.setSubject(subject);

            // 创建HTML格式的邮件内容
            String htmlContent = buildHtmlEmailContent(authCode);
            // 设置HTML内容
            mail.setHtmlMsg(htmlContent);
            // 设置备用的纯文本内容（当HTML无法显示时使用）
            mail.setTextMsg("【云视图】您的验证码为:" + authCode + "(5分钟内有效)");

            // 发送
            mail.send();
            return "发送成功,请注意查收";
        } catch (EmailException e) {
            LOGGER.log(Level.SEVERE, "邮件发送失败，目标邮箱: " + targetEmail, e);
            return "邮件发送失败，请稍后重试";
        }
    }

    /**
     * 验证码格式
     *
     * @param authCode
     * @return
     */
    private static String buildHtmlEmailContent(String authCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <title>验证码邮件</title>" +
                "    <style>" +
                "        body {" +
                "            font-family: 'Arial', sans-serif;" +
                "            background-color: #f5f5f5;" +
                "            margin: 0;" +
                "            padding: 0;" +
                "        }" +
                "        .container {" +
                "            max-width: 600px;" +
                "            margin: 20px auto;" +
                "            background-color: #ffffff;" +
                "            border-radius: 8px;" +
                "            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);" +
                "            overflow: hidden;" +
                "        }" +
                "        .header {" +
                "            background-color: #1890ff;" +
                "            color: white;" +
                "            padding: 20px;" +
                "            text-align: center;" +
                "        }" +
                "        .content {" +
                "            padding: 30px;" +
                "        }" +
                "        .code {" +
                "            font-size: 28px;" +
                "            font-weight: bold;" +
                "            color: #1890ff;" +
                "            text-align: center;" +
                "            margin: 20px 0;" +
                "            letter-spacing: 5px;" +
                "        }" +
                "        .footer {" +
                "            padding: 20px;" +
                "            text-align: center;" +
                "            color: #999999;" +
                "            font-size: 12px;" +
                "            border-top: 1px solid #eeeeee;" +
                "        }" +
                "        .note {" +
                "            color: #666666;" +
                "            font-size: 14px;" +
                "            margin-top: 20px;" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <h2>云视图 - 验证码</h2>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <p>尊敬的用户，您好！</p>" +
                "            <p>您正在进行邮箱验证操作，以下是您的验证码：</p>" +
                "            <div class=\"code\">" + authCode + "</div>" +
                "            <p class=\"note\">此验证码5分钟内有效，请勿泄露给他人。</p>" +
                "            <p>如非本人操作，请忽略此邮件。</p>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>© 2025 云视图团队 版权所有</p>" +  // 使用 Unicode 转义符
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}