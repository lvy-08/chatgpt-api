package cn.bug.chatgpt.infrastructure.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.lang3.StringUtils;

import java.io.Writer;

/**
 * @description 微信公众号发送消息，解析工具类
 */
public class XmlUtil {

    //xstream扩展,bean转xml自动加上![CDATA[]]
    public static XStream getMyXStream() {
        return new XStream(new XppDriver() {
            @Override
            public HierarchicalStreamWriter createWriter(Writer out) {
                return new PrettyPrintWriter(out) {
                    // 对所有xml节点都增加CDATA标记
                    boolean cdata = true;

                    @Override
                    public void startNode(String name, Class clazz) {
                        super.startNode(name, clazz);
                    }

                    @Override
                    protected void writeText(QuickWriter writer, String text) {
                        if (cdata && !StringUtils.isNumeric(text)) {
                            writer.write("<![CDATA[");
                            writer.write(text);
                            writer.write("]]>");
                        } else {
                            writer.write(text);
                        }
                    }
                };
            }
        });
    }

    /**
     * bean转成微信的xml消息格式
     */
    public static String beanToXml(Object object) {
        XStream xStream = getMyXStream();
        xStream.alias("xml", object.getClass());
        xStream.processAnnotations(object.getClass());
        String xml = xStream.toXML(object);
        if (!StringUtils.isEmpty(xml)) {
            return xml;
        } else {
            return null;
        }
    }

    /**
     * xml转成bean泛型方法
     */
    public static <T> T xmlToBean(String resultXml, Class clazz) {
        // XStream对象设置默认安全防护，同时设置允许的类
        XStream stream = new XStream(new DomDriver());//指定使用DOM解析器来解析XML
        XStream.setupDefaultSecurity(stream);//默认安全设置，在1.4.7版本之后是必须的，防止恶意的XML数据导致安全问题
        stream.allowTypes(new Class[]{clazz});//指定允许反序列化的类
        stream.processAnnotations(new Class[]{clazz});//启用XStream注解，让XStream处理类clazz上定义的XStream注解，使XML元素可以映射到类的不同字段
        stream.setMode(XStream.NO_REFERENCES);//这种模式不会处理引用关系，即不会生成额外的XML元素来维护对象的引用关系。
        stream.alias("xml", clazz);//将XML根元素映射到clazz类
        return (T) stream.fromXML(resultXml);//①将XML字符串解析并转换为clazz类型的对象②返回泛型T，即传入的clazz类型
    }

}
