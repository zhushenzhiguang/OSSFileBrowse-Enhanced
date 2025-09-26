package net.jdr2021.utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * @version 1.0
 * @Author jdr
 * @Date 2024-5-24 21:32
 * @注释
 */

public class XMLParser {

    public static boolean isXMLData(String data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            InputSource is = new InputSource(new StringReader(data));
            is.setEncoding("UTF-8"); // 可选
            Document doc = builder.parse(is);

            // 检查根节点是否为空
            if (doc.getDocumentElement() != null) {
                // 解析成功，是 XML 数据
                return true;
            } else {
                // 解析成功，但根节点为空
                return false;
            }
        } catch (Exception e) {
            // 解析失败，不是 XML 数据
            return false;
        }
    }

    public static boolean containsListBucketResult(String data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(data));
            is.setEncoding("UTF-8"); // 可选
            Document doc = builder.parse(is);

            // 查找 ListBucketResult 的节点
            NodeList nodeList = doc.getElementsByTagName("ListBucketResult");
            return nodeList.getLength() > 0;
        } catch (Exception e) {
            // 解析失败或不包含 ListBucketResult
            return false;
        }
    }

    public static boolean containsAccessDenied(String data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(data));
            is.setEncoding("UTF-8"); // 可选
            Document doc = builder.parse(is);

            // 查找所有Code节点，并检查其值是否为 AccessDenied
            NodeList nodeList = doc.getElementsByTagName("Code");
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getTextContent().equals("AccessDenied")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // 解析失败或不包含 AccessDenied
            return false;
        }
    }
    //获取所有的key，也就是文件路径

    public static class File {
        String Path;
        Long Size;
        String LastModified;

        public File(String path, Long size, String lastModified) {
            Path = path;
            Size = size;
            LastModified = lastModified;
        }

        public String getPath() {
            return Path;
        }

        public Long getSize() {
            return Size;
        }

        public String getLastModified() {
            return LastModified;
        }
    }

    public static File[] extractKeys(String xml) {
        List<File> values = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            is.setEncoding("UTF-8"); // 可选
            Document doc = builder.parse(is);

            NodeList keys = doc.getDocumentElement().getElementsByTagName("Key");

            for (int i = 0; i < keys.getLength(); i++) {
                Element keyElement = (Element) keys.item(i);
                String key = keyElement.getTextContent();

                if (!key.endsWith("/")) { // 过滤掉结尾为 '/' 的键值

                    if (!key.startsWith("/")) {
                        key = "/" + key;
                    }


                    Long size = (long) -1;
                    String lastModified = "";
                    Node nn = keyElement.getNextSibling();
                    while (nn != null) {
                        if (nn.getNodeName().equals("LastModified")) {
                            lastModified = nn.getTextContent();
                            if (lastModified.indexOf("T") > -1) {
//                                把时间截取部分
                                lastModified = lastModified.substring(0, lastModified.indexOf("T"));
                            }
                        }
                        if (nn.getNodeName().equals("Size")) {
                            size = Long.parseLong(nn.getTextContent());
                        }

                        if (size != -1 && lastModified.equals("") == false) {
                            break;
                        }
                        nn = nn.getNextSibling();
                    }

                    values.add(new File(key, size, lastModified));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

//        排序
        values.sort(Comparator.comparingLong(File::getSize).reversed().thenComparing(File::getLastModified));  //级联排序


        return values.toArray(new File[0]);
    }
}
