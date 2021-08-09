package com.gupaoedu.vip.spring.formework.context.support;

import com.gupaoedu.vip.spring.formework.beans.GPBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Tom on 2018/4/21.
 */

//用对配置文件进行查找，读取、解析
public class GPBeanDefinitionReader {

    private  Properties config = new Properties();
    // 存储扫描类全路径
    private List<String> registyBeanClasses = new ArrayList<String>();


    //在配置文件中，用来获取自动扫描的包名的key 配置文件里的名字跟这个要一致
    private final String SCAN_PACKAGE = "scanPackage";

    public GPBeanDefinitionReader(String... locations){
        //在Spring中是通过Reader去查找和定位配置文件 application.properties
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(locations[0].replace("classpath:",""));

        try {
            // config 读取流
            config.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                 if(null != is){is.close();}
            }catch (Exception e){
                e.printStackTrace();
            }

        }
            // 根据配置文件的key 拿到value
             doScanner(config.getProperty(SCAN_PACKAGE));

    }


    public List<String> loadBeanDefinitions(){ return this.registyBeanClasses;}


    //每注册一个className，就返回一个BeanDefinition，我自己包装
    //只是为了对配置信息进行一个包装
    public GPBeanDefinition registerBean(String className){
        // 判断是否有这个全路径
        if(this.registyBeanClasses.contains(className)){
            GPBeanDefinition beanDefinition = new GPBeanDefinition();
            // 有的话保存全路径
            beanDefinition.setBeanClassName(className);
            // 保存名字
            beanDefinition.setFactoryBeanName(lowerFirstCase(className.substring(className.lastIndexOf(".") + 1)));
            return beanDefinition;
        }

        return null;
    }


    //递归扫描所有的相关联的class，并且保存到一个List中
    private void doScanner(String packageName) {
        // 把. 转成/  .需要转义为\\. 拿到需要注册的全路径
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/"));
        // 根据全路径拿到下面的所有文件夹
        File classDir = new File(url.getFile());
        // 遍历所有文件夹
        for (File file : classDir.listFiles()){
            // 判断如果是文件夹,递归调用
            if(file.isDirectory()){
                doScanner(packageName + "." +file.getName());
            }else {
                // 不是文件夹 保存类的全路径 到list
                registyBeanClasses.add(packageName + "." + file.getName().replace(".class",""));
            }
        }


    }


    public Properties getConfig(){
        return this.config;
    }



    private String lowerFirstCase(String str){
        char [] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
