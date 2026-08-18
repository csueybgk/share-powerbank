package com.ai.controller;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class ChatDeepSeekController {

    @Autowired
    private OpenAiChatModel chatModel;

    @GetMapping("/ai/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "hello")
                           String message) {
        String data = "需求：" + message + "\n" +
                "表结构：\n" +
                "`order_info`表字段：`order_no`(订单号),`total_amount`(订单金额),`create_time`(创建时间,格式YYYY-MM-DD HH:MM:SS)\n" +
                "要求：\n" +
                "1. 生成的sql语句不要中文注释\n" +
                "2. 只返回纯SQL，用```sql和```包裹\n" +
                "3. 日期月份列别名必须用 order_date\n" +
                "4. 订单数量列别名必须用 order_count\n" +
                "5. 订单金额列别名必须用 total_amount\n" +
                "6. 如果涉及分组统计，必须使用 GROUP BY 和 COUNT/SUM 聚合函数";
        String response = this.chatModel.call(data);
        return parseSql(response);
    }

    /**
     * 从AI返回内容中提取SQL语句，支持多种格式
     */
    private String parseSql(String response) {
        // 1. 优先提取 ```sql ... ``` 代码块
        Pattern p1 = Pattern.compile("```sql\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(response);
        if (m1.find()) {
            return m1.group(1).trim()
                    .replaceAll("\\n", " ")
                    .replaceAll("\\s+", " ");
        }

        // 2. 提取 ``` ... ``` 任意代码块
        Pattern p2 = Pattern.compile("```\\s*([\\s\\S]*?)```");
        Matcher m2 = p2.matcher(response);
        if (m2.find()) {
            return m2.group(1).trim()
                    .replaceAll("\\n", " ")
                    .replaceAll("\\s+", " ");
        }

        // 3. 提取 SELECT ... ; 语句
        Pattern p3 = Pattern.compile("(SELECT[\\s\\S]*?;)", Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(response);
        if (m3.find()) {
            return m3.group(1).trim()
                    .replaceAll("\\n", " ")
                    .replaceAll("\\s+", " ");
        }

        // 4. 最后兜底：返回原始响应
        return response;
    }
}
