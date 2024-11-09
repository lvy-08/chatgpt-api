package cn.bug.chatgpt.test;
import cn.bug.chatgpt.common.Constants;
import cn.bug.chatgpt.domain.chat.ChatCompletionRequest;
import cn.bug.chatgpt.domain.chat.Message;
import cn.bug.chatgpt.domain.security.service.JwtUtil;
import cn.bug.chatgpt.session.Configuration;
import cn.bug.chatgpt.session.OpenAiSession;
import cn.bug.chatgpt.session.OpenAiSessionFactory;
import cn.bug.chatgpt.session.defaults.DefaultOpenAiSessionFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.apache.http.HttpHost;


/**
 * @description 单元测试
 */
public class ApiTest {

    @Test
    public void test_jwt() {
        JwtUtil util = new JwtUtil("xfg", SignatureAlgorithm.HS256);
        // 以tom作为秘钥，以HS256加密
        Map<String, Object> map = new HashMap<>();
        map.put("username", "xfg");
        map.put("password", "123");
        map.put("age", 100);

        String jwtToken = util.encode("xfg", 30000, map);

        util.decode(jwtToken).forEach((key, value) -> System.out.println(key + ": " + value));
    }

    /**
     * 【因官网模型调整，废弃测试方法，参考下面的 test_chatGPT_3_5和test_sdk】
     * 这是一个简单的测试，后续会开发 ChatGPT API
     * 测试时候，需要先获得授权token
     * 获取方式；http://api.xfg.im:8080/authorize?username=xfg&password=123 - 此地址暂时有效，后续根据课程首页说明获取token；https://t.zsxq.com/0d3o5FKvc
     */
    @Test
    @Deprecated
    public void test_chatGPT() throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        // 用获取的 token 替换，默认有效期60分钟。地址非长期有效，只做学习验证。
        HttpPost post = new HttpPost("https://api.xfg.im/b8b6/v1/completions?token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4ZmciLCJleHAiOjE2ODM4MjE4NzIsImlhdCI6MTY4MzgxODI3MiwianRpIjoiZGUyYjY0MmYtOTUwNi00OTEzLWE1NDgtMWViNzkwOGE5YTA1IiwidXNlcm5hbWUiOiJ4ZmcifQ.O7FJ-IsQI5tUXaWnNbXqxTK2Hhu9a5-tkYHL4GAExjA");

        post.addHeader("Content-Type", "application/json");
        post.addHeader("Authorization", "Bearer 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");

        String paramJson = "{\"model\": \"text-davinci-003\", \"prompt\": \"帮我写一个java冒泡排序\", \"temperature\": 0, \"max_tokens\": 1024}";

        StringEntity stringEntity = new StringEntity(paramJson, ContentType.create("text/json", "UTF-8"));
        post.setEntity(stringEntity);

        System.out.println("请求入参：" + paramJson);
        CloseableHttpResponse response = httpClient.execute(post);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String res = EntityUtils.toString(response.getEntity());
            // api.xfg.im:443 requested authentication 表示 token 错误或者过期。
            System.out.println("测试结果：" + res);
        } else {
            System.out.println(response.getStatusLine().getStatusCode());
        }

    }

    /**
     * 因为官网模型更新，大家测试的时候使用 test_chatGPT_3_5 方法。
     */
    @Test
    public void test_chatGPT_3_5() throws Exception {
/*        CountDownLatch countDownLatch = new CountDownLatch(1);
        CloseableHttpClient httpClient = HttpClients.createDefault();*/

        CountDownLatch countDownLatch = new CountDownLatch(1);
        // 设置代理
        HttpHost proxy = new HttpHost("127.0.0.1", 7890);
        // 创建带代理的 HttpClient
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setProxy(proxy)
                .build();
        // 完成计数
        countDownLatch.countDown();
        try {
            // 如果你有自己的apihost、apikey也可以替换使用。【也可以联系小傅哥获取开发需要的 apihost、apikey】
            HttpPost httpPost = new HttpPost("https://api.openai.com/v1/chat/completions");
            String json = "{\n" +
                    "    \"model\": \"gpt-3.5-turbo-1106\",\n" +
                    "    \"max_tokens\": 1024,\n" +
                    "    \"messages\": [\n" +
                    "        {\n" +
                    "            \"role\": \"user\",\n" +
                    "            \"content\": [\n" +
                    "                {\n" +
                    "                    \"text\": \"写个java冒泡排序\",\n" +
                    "                    \"type\": \"text\"\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
            StringEntity requestEntity = new StringEntity(json, ContentType.APPLICATION_JSON);

            httpPost.setEntity(requestEntity);
            httpPost.setHeader("Authorization", "Bearer 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");
            httpPost.setHeader("Content-Type", "application/json");

            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                // 打印响应体的内容
                String result = EntityUtils.toString(responseEntity);
                System.out.println(result);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            countDownLatch.countDown();
        }

        // 等待
        countDownLatch.await();
    }

    @Test
    public void test_sdk() throws InterruptedException, JsonProcessingException, ExecutionException {
        // 1. 配置文件【可以联系小傅哥获取开发需要的 apihost、apikey】
        Configuration configuration = new Configuration();
        configuration.setApiHost("https://api.openai.com/");
        configuration.setApiKey("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");
        // 废弃属性，后续不在使用
        configuration.setAuthToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4ZmciLCJleHAiOjE2ODM0MTgwOTYsImlhdCI6MTY4MzQxNDQ5NiwianRpIjoiODIyM2FhZWQtOWJiNS00NjE0LTljNGYtNjNiMTBkYWE1YjA3IiwidXNlcm5hbWUiOiJ4ZmcifQ.5rsy5bOOJl1UG5e4IzSDU7YbUUZ4d_ZXHz2wbk1ne58");
        // 2. 会话工厂
        OpenAiSessionFactory factory = new DefaultOpenAiSessionFactory(configuration);
        // 3. 开启会话
        OpenAiSession openAiSession = factory.openSession();

        // 1. 创建参数
        ChatCompletionRequest chatCompletion = ChatCompletionRequest
                .builder()
                .messages(Collections.singletonList(Message.builder().role(Constants.Role.USER).content("1+1").build()))
                .model(ChatCompletionRequest.Model.GPT_3_5_TURBO.getCode())
                .stream(true)
                .build();
        // 2. 发起请求
        CompletableFuture<String> chatCompletions = openAiSession.chatCompletions(chatCompletion);
        // 3. 解析结果
        String message = chatCompletions.get();
        System.out.println("测试结果：" + message);

    }


}


