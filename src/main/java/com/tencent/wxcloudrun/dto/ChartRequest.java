package com.tencent.wxcloudrun.dto;

import lombok.Data;

@Data
public class ChartRequest {

    /**
     * 小程序的原始ID
     */
    private String toUserName;

    /**
     * 发送者的openid
     */
    private String fromUserName;

    /**
     * 消息创建时间(整型）
     */
    private String createTime;

    /**
     * text
     */
    private String msgType;

    /**
     * 文本消息内容
     */
    private String content;

    /**
     * 消息id，64位整型
     */
    private String msgId;

}
