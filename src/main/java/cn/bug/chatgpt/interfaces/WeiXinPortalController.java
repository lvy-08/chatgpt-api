package cn.bug.chatgpt.interfaces;

import cn.bug.chatgpt.application.IWeiXinValidateService;
import cn.bug.chatgpt.common.Constants;
import cn.bug.chatgpt.domain.chat.ChatCompletionRequest;
import cn.bug.chatgpt.domain.chat.ChatCompletionResponse;
import cn.bug.chatgpt.domain.chat.Message;
import cn.bug.chatgpt.domain.receive.model.BehaviorMatter;
import cn.bug.chatgpt.domain.receive.model.MessageTextEntity;
import cn.bug.chatgpt.infrastructure.util.XmlUtil;
import cn.bug.chatgpt.session.Configuration;
import cn.bug.chatgpt.session.OpenAiSession;
import cn.bug.chatgpt.session.OpenAiSessionFactory;
import cn.bug.chatgpt.session.defaults.DefaultOpenAiSessionFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description 微信公众号，请求处理服务
 */
@RestController//在Spring Boot应用程序中，该控制器类是单例的。这意味着，只要服务一直开启，该类的实例会一直存在，chatGPTMap也会一直存在。
@RequestMapping("/wx/portal/{appid}")//Spring MVC的注解，将特定的URL映射到控制器的类或方法上。可放在类级别或方法级别。当放在类级别时，类中的每个方法都会以/wx/portal/{appid}为基础路径。
public class WeiXinPortalController {

    private Logger logger = LoggerFactory.getLogger(WeiXinPortalController.class);

    @Value("${wx.config.originalid:gh_921349cd1dd5}")
    private String originalId;

    @Resource
    private IWeiXinValidateService weiXinValidateService;

    private OpenAiSession openAiSession;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    private Map<String, String> chatGPTMap = new ConcurrentHashMap<>();//线程安全的集合，支持并发操作，即不同的请求可以同时访问和更新chatGptMap中的数据

    public WeiXinPortalController() {
        // 1. 配置文件【可以联系小傅哥获取开发需要的 apihost、apikey】
        Configuration configuration = new Configuration();
        configuration.setApiHost("https://api.openai.com/");
        configuration.setApiKey("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");
        // 废弃属性，后续不在使用
        //configuration.setAuthToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4ZmciLCJleHAiOjE2ODM0MTgwOTYsImlhdCI6MTY4MzQxNDQ5NiwianRpIjoiODIyM2FhZWQtOWJiNS00NjE0LTljNGYtNjNiMTBkYWE1YjA3IiwidXNlcm5hbWUiOiJ4ZmcifQ.5rsy5bOOJl1UG5e4IzSDU7YbUUZ4d_ZXHz2wbk1ne58");
        // 2. 会话工厂
        OpenAiSessionFactory factory = new DefaultOpenAiSessionFactory(configuration);
        // 3. 开启会话
        this.openAiSession = factory.openSession();
        logger.info("开始 openAiSession");
    }

    /**
     * 处理微信服务器发来的get请求，进行签名的验证【付费的natapp对接稳定，3块钱域名，9块钱渠道费】
     * http://xfg.nat300.top/wx/portal/wx470537fb2f5bf897
     * <p>
     * appid     微信端AppID
     * signature 微信端发来的签名
     * timestamp 微信端发来的时间戳
     * nonce     微信端发来的随机字符串
     * echostr   微信端发来的验证字符串
     */
    @GetMapping(produces = "text/plain;charset=utf-8")// /wx/portal/{appid}会映射到到validate方法（get请求）
    public String validate(@PathVariable String appid,
                           @RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
                           @RequestParam(value = "nonce", required = false) String nonce,
                           @RequestParam(value = "echostr", required = false) String echostr) {
        try {
            logger.info("微信公众号验签信息{}开始 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {//检查多个字符串是否为空白
                throw new IllegalArgumentException("请求参数非法，请核实!");
            }
            boolean check = weiXinValidateService.checkSign(signature, timestamp, nonce);
            logger.info("微信公众号验签信息{}完成 check：{}", appid, check);
            if (!check) {
                return null;
            }
            return echostr;
        } catch (Exception e) {
            logger.error("微信公众号验签信息{}失败 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr, e);
            return null;
        }
    }

    /**
     * 此处是处理微信服务器的消息转发的
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")//注解表示该方法接收 HTTP POST 请求，并且返回 XML 格式的响应，字符编码为 UTF-8
    public String post(@PathVariable String appid,
                       @RequestBody String requestBody,//可以使用字符串获取请求体
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,//required = false,表示参数可选，如果请求没有，spring不会报错
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        try {
            logger.info("接收微信公众号信息请求{}开始 {}", openid, requestBody);
            MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
            // 异步任务。
            if (chatGPTMap.get(message.getContent().trim()) == null || "NULL".equals(chatGPTMap.get(message.getContent().trim()))) {//.trim()去除字符串开头和结尾的空白字符，是一个新的字符串，不会改变旧字符串
                // 反馈信息[文本]
                MessageTextEntity res = new MessageTextEntity();
                res.setToUserName(openid);
                res.setFromUserName(originalId);
                res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
                res.setMsgType("text");
                res.setContent("消息处理中，请再回复我一句【" + message.getContent().trim() + "】");
                if (chatGPTMap.get(message.getContent().trim()) == null) {
                    doChatGPTTask02(message.getContent().trim());
                }
                logger.info("Response MessageTextEntity: {}", res);
                return XmlUtil.beanToXml(res);
            }

            // 反馈信息[文本]
            MessageTextEntity res = new MessageTextEntity();
            res.setToUserName(openid);
            res.setFromUserName(originalId);
            res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
            res.setMsgType("text");
            res.setContent(chatGPTMap.get(message.getContent().trim()));
            String result = XmlUtil.beanToXml(res);
            logger.info("接收微信公众号信息请求{}完成 {}", openid, result);
            chatGPTMap.remove(message.getContent().trim());
            return result;
        } catch (Exception e) {
            logger.error("接收微信公众号信息请求{}失败 {}", openid, requestBody, e);
            return "";
        }
    }

    /**
     * 模型更换，调整为 doChatGPTTask02 调用。对象的 pom.xml 中的 chatgpt-sdk-java 版本调整为 1.0 【此版本可以直接从maven仓库拉取，也可以从代码库自己 install 构建 https://gitcode.net/KnowledgePlanet/chatgpt/chatgpt-sdk-java】
     * @param content
     */
    @Deprecated
    public void doChatGPTTask(String content) {
        chatGPTMap.put(content, "NULL");
        taskExecutor.execute(() -> {//在另一个线程中执行请求，即异步处理，方法立即返回，不等待 ChatGPT 的响应结果
            // OpenAI 请求
            // 1. 创建参数
            ChatCompletionRequest chatCompletion = ChatCompletionRequest
                    .builder()
                    .messages(Collections.singletonList(Message.builder().role(Constants.Role.USER).content(content).build()))
                    .model(ChatCompletionRequest.Model.GPT_3_5_TURBO.getCode())
                    .build();
            // 2. 发起请求
            ChatCompletionResponse chatCompletionResponse = openAiSession.completions(chatCompletion);
            // 3. 解析结果
            StringBuilder messages = new StringBuilder();
            chatCompletionResponse.getChoices().forEach(e -> {
                messages.append(e.getMessage().getContent());
            });

            chatGPTMap.put(content, messages.toString());//有可能拿不到chatgpt返回的数据
        });
    }

    public void doChatGPTTask02(String content) throws Exception {
        chatGPTMap.put(content, "NULL");
        ChatCompletionRequest chatCompletion = ChatCompletionRequest
                .builder()
                .messages(Collections.singletonList(Message.builder().role(Constants.Role.USER).content(content).build()))
                .model(ChatCompletionRequest.Model.GPT_3_5_TURBO.getCode())
                .stream(true)
                .build();
        // 2. 发起请求
        CompletableFuture<String> chatCompletions = openAiSession.chatCompletions(chatCompletion);
        // 3. 解析结果
        String message = chatCompletions.get();
        chatGPTMap.put(content, message);
    }

}

