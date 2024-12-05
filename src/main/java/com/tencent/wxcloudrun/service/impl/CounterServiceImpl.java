package com.tencent.wxcloudrun.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.tencent.wxcloudrun.dao.CountersMapper;
import com.tencent.wxcloudrun.dto.ChartRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CounterServiceImpl implements CounterService {

    final CountersMapper countersMapper;

    public CounterServiceImpl(@Autowired CountersMapper countersMapper) {
        this.countersMapper = countersMapper;
    }

    @Override
    public Optional<Counter> getCounter(Integer id) {
        return Optional.ofNullable(countersMapper.getCounter(id));
    }

    @Override
    public void upsertCount(Counter counter) {
        countersMapper.upsertCount(counter);
    }

    @Override
    public void clearCount(Integer id) {
        countersMapper.clearCount(id);
    }


    @Override
    public String getChartData(ChartRequest chartRequest) {
        System.out.println("FromUserName: \n" + chartRequest.getFromUserName());
        System.out.println("question: \n" + chartRequest.getContent());
        String question = chartRequest.getContent();
        //返回答案
        String answer = "";

        //step1:创建会话
        String huihua = HttpRequest.post("https://api.coze.cn/v1/conversation/create").header("Content-Type", "application/json").header("Authorization", "Bearer pat_CjvUET74uDMwWdCnyygPY9mMGdPJRggrM6Gl6eUH5ojz58ZcEhG695YeNiiNdyeF").execute().body();
        System.out.println("huihua:\n" + huihua);
        //获取对话id
        String conversationId = JSON.parseObject(huihua).getJSONObject("data").getString("id");
        System.out.println("conversationId:\n" + conversationId);

        //step2：创建对话
        //构建参数信息
        Map<String, Object> map = new HashMap<>();
        map.put("bot_id", "7436292746321117222");//机器人id
        map.put("user_id", "112233");//用户id
        map.put("stream: false,", false); //是否采用流式对话
        map.put("auto_save_history", true);//是否保存对话记录
        map.put("auto_save_history", true);//是否保存对话记录

        //创建对话结构信息
        List<Map<String, Object>> additional_messages = new ArrayList<>();
        Map<String, Object> additional_message = new HashMap<>();
        additional_message.put("type", "question");
        additional_message.put("role", "user");
        additional_message.put("content", question);
        additional_message.put("content_type", "text");
        additional_messages.add(additional_message);
        map.put("additional_messages", additional_messages);
        //发送对话
        String duihua = HttpRequest.post("https://api.coze.cn/v3/chat?conversation_id=" + conversationId).header("Content-Type", "application/json").header("Authorization", "Bearer pat_CjvUET74uDMwWdCnyygPY9mMGdPJRggrM6Gl6eUH5ojz58ZcEhG695YeNiiNdyeF").body(JSON.toJSONString(map)).execute().body();
        System.out.println("duihua:\n" + duihua);
        //判断请求是否成功
        String code = JSON.parseObject(duihua).getString("code");
        //如果cdoee!=0，则对话失败，返回error
        if (!code.equals("0")) {
            return "error";
        } else {
            //获取对话id
            String chart_id = JSON.parseObject(duihua).getJSONObject("data").getString("id");
            //获取会话id
            String conversation_id = JSON.parseObject(duihua).getJSONObject("data").getString("conversation_id");

            // 状态信息
            String duihua_status = "created";
            //step3:轮训获取是否处理完成
            while (true) {
                //获取对话详情状态
                String duihuadetail = HttpRequest.post("https://api.coze.cn/v3/chat/retrieve?conversation_id=" + conversation_id + "&chat_id=" + chart_id).header("Content-Type", "application/json").header("Authorization", "Bearer pat_CjvUET74uDMwWdCnyygPY9mMGdPJRggrM6Gl6eUH5ojz58ZcEhG695YeNiiNdyeF").execute().body();
                //判断请求是否成功
                String codedetail = JSON.parseObject(duihuadetail).getString("code");
                //判断返回状态是否为0，如果为0则继续轮训
                if (!codedetail.equals("0")) {
                    duihua_status = "error";
                    break;
                } else {
                    //获取返回结果状态
                    String status = JSON.parseObject(duihuadetail).getJSONObject("data").getString("status");
                    //判断返回状态是否为completed 如果为completed则代表处理完成，进入下一步操作获取对话详情
                    if (status.equals("completed")) {
                        duihua_status = "completed";
                        break;
                    } else if (status.equals("failed") || status.equals("requires_action") || status.equals("canceled")) {
                        duihua_status = "error";
                        break;
                    }
                }
                // 设置延时2秒（1000毫秒）
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            //判断对对话状态 是否为completed  如果为completed则代表处理完成，进入下一步操作获取对话详情
            if (duihua_status.equals("completed")) {
                //step4:获取对话详情
                String answerStr = HttpRequest.post(" https://api.coze.cn/v3/chat/message/list?conversation_id=" + conversation_id + "&chat_id=" + chart_id).header("Content-Type", "application/json").header("Authorization", "Bearer pat_CjvUET74uDMwWdCnyygPY9mMGdPJRggrM6Gl6eUH5ojz58ZcEhG695YeNiiNdyeF").execute().body();
                System.out.println("answerStr:\n" + answerStr);
                //获取返回答案
                JSONArray jsonArray = JSON.parseObject(answerStr).getJSONArray("data");
                //转换为list集合
                List<Map> listMap = jsonArray.toJavaList(Map.class);
                // 循环处理数据
                for (Map answerMap : listMap) {
                    //判断是否为回答类型
                    String type = answerMap.get("type").toString();
                    String content_type = answerMap.get("content_type").toString();
                    //如果类型为answer 则获取回答内容、内容类型为text
                    if (type.equals("answer") && content_type.equals("text")) {
                        answer = answerMap.get("content").toString();
                        break;
                    }
                }
            }
        }

        Map<String,Object> answerMap=new HashMap<>();
        answerMap.put("touser",chartRequest.getFromUserName());
        answerMap.put("msgtype","text");
        HashMap<String,Object> answerContent=new HashMap<>();
        answerContent.put("content",answer);
        answerMap.put("text",answerContent);

        //返回答案至微信
        String send = HttpRequest.post("https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send").body(JSON.toJSONString(answerMap)).execute().body();
        System.out.println("send:\n"+send);
        return answer;
    }
}
