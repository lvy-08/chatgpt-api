package cn.bug.chatgpt.application;

 /*
 *@description 微信公众号验签服务
  */

public interface IWeiXinValidateService {
    boolean checkSign(String signature, String timestamp, String nonce);
}
