package com.gupaoedu.vip.spring.formework.context;

import com.gupaoedu.vip.spring.demo.action.MyAction;
import com.gupaoedu.vip.spring.formework.annotation.GPAutowired;
import com.gupaoedu.vip.spring.formework.annotation.GPController;
import com.gupaoedu.vip.spring.formework.annotation.GPService;
import com.gupaoedu.vip.spring.formework.aop.GPAopConfig;
import com.gupaoedu.vip.spring.formework.beans.GPBeanDefinition;
import com.gupaoedu.vip.spring.formework.beans.GPBeanPostProcessor;
import com.gupaoedu.vip.spring.formework.beans.GPBeanWrapper;
import com.gupaoedu.vip.spring.formework.context.support.GPBeanDefinitionReader;
import com.gupaoedu.vip.spring.formework.core.GPBeanFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Tom on 2018/4/21.
 */
public class GPApplicationContext extends GPDefaultListableBeanFactory implements GPBeanFactory {

    private String [] configLocations;

    private GPBeanDefinitionReader reader;

    //用来保证注册式单例的容器
    private Map<String,Object> beanCacheMap = new HashMap<String, Object>();

    //用来存储所有的被代理过的对象
    private Map<String,GPBeanWrapper> beanWrapperMap = new ConcurrentHashMap<String, GPBeanWrapper>();


    public GPApplicationContext(String ... configLocations){
        this.configLocations = configLocations;
        refresh();
    }


    public void refresh(){
        //定位 拿到application.properties
        this.reader = new GPBeanDefinitionReader(configLocations);

        //加载 存起来类全路径
        List<String> beanDefinitions = reader.loadBeanDefinitions();

        //注册
        doRegisty(beanDefinitions);


        //依赖注入（lazy-init = false），要是执行依赖注入
        //在这里自动调用getBean方法
        doAutowrited();

     /*   MyAction myAction = (MyAction)this.getBean("myAction");
        myAction.query(null,null,"任性的Tom老师");*/
    }


    //开始执行自动化的依赖注入
    private void doAutowrited() {
        for(Map.Entry<String,GPBeanDefinition> beanDefinitionEntry : this.beanDefinitionMap.entrySet()){
            String beanName = beanDefinitionEntry.getKey();
            // 判断是不是懒加载 ,懒加载用的时候在加载 不是现在就加载
            if(!beanDefinitionEntry.getValue().isLazyInit()){
                Object obj = getBean(beanName);
//                System.out.println(obj.getClass());
            }

        }
        for(Map.Entry<String,GPBeanWrapper> beanWrapperEntry : this.beanWrapperMap.entrySet()){
            // 给controller注入service
            populateBean(beanWrapperEntry.getKey(),beanWrapperEntry.getValue().getOriginalInstance());

        }

//        System.out.println("===================");


    }


    /**
     * 注入
     * @param beanName
     * @param instance
     */
    public void populateBean(String beanName,Object instance){

        Class clazz = instance.getClass();

        //判断如果没有Controller Service注解的不管
        if(!(clazz.isAnnotationPresent(GPController.class) ||
                clazz.isAnnotationPresent(GPService.class))){
            return;
        }
        // 拿到class中所有的字段
        Field [] fields = clazz.getDeclaredFields();
        // 循环这些字段
        for (Field field : fields) {
            // 如果字段没有Autowired 直接返回
            if (!field.isAnnotationPresent(GPAutowired.class)){ continue; }
            // 拿到注解的实例
            GPAutowired autowired = field.getAnnotation(GPAutowired.class);
            // 拿到注解里的value
            String autowiredBeanName = autowired.value().trim();
            // 注解先取里面的value 也就是名字,如果为空的话,取类型
            if("".equals(autowiredBeanName)){
                autowiredBeanName = field.getType().getName();
            }
            // 设置这个字段可以通过反射进行访问
            field.setAccessible(true);

            try {

                System.out.println("=======================" +instance +"," + autowiredBeanName + "," + this.beanWrapperMap.get(autowiredBeanName));
                // 把拿到的service给controller注入进去
                field.set(instance,this.beanWrapperMap.get(autowiredBeanName).getWrappedInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }



    }



    //真正的将BeanDefinitions注册到beanDefinitionMap中

    private void doRegisty(List<String> beanDefinitions) {


        //beanName有三种情况:
        //1、默认是类名首字母小写
        //2、自定义名字
        //3、接口注入
        try {
            // 循环全路径
            for (String className : beanDefinitions) {
                // 反射获取实体
                Class<?> beanClass = Class.forName(className);

                //如果是一个接口，是不能实例化的
                //用它实现类来实例化
                if(beanClass.isInterface()){ continue; }
                // 把类信息包装起来
                GPBeanDefinition beanDefinition = reader.registerBean(className);
                // 如果不为空
                if(beanDefinition != null){
                    // 保存 类名字 和 GPBeanDefinition对象
                    this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(),beanDefinition);
                }
                // 判断如果这个类有实现接口,那么把他的接口名字的实力set为这个类
                Class<?>[] interfaces = beanClass.getInterfaces();
                for (Class<?> i: interfaces) {
                    //如果是多个实现类，只能覆盖
                    //为什么？因为Spring没那么智能，就是这么傻
                    //这个时候，可以自定义名字
                    this.beanDefinitionMap.put(i.getName(),beanDefinition);
                }


                //到这里为止，容器初始化完毕

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    //依赖注入，从这里开始，通过读取BeanDefinition中的信息
    //然后，通过反射机制创建一个实例并返回
    //Spring做法是，不会把最原始的对象放出去，会用一个BeanWrapper来进行一次包装
    //装饰器模式：
    //1、保留原来的OOP关系
    //2、我需要对它进行扩展，增强（为了以后AOP打基础）
    @Override
    public Object getBean(String beanName) {
        // 根据key 获取存储的配置信息
        GPBeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);
        try{

            //生成通知事件
            GPBeanPostProcessor beanPostProcessor = new GPBeanPostProcessor();
            // 拿到实例
            Object instance = instantionBean(beanDefinition);
            if(null == instance){ return  null;}

            //在实例初始化以前调用一次
            beanPostProcessor.postProcessBeforeInitialization(instance,beanName);
            // 动态代理实例
            GPBeanWrapper beanWrapper = new GPBeanWrapper(instance);
            // set代理后返回的实例
            beanWrapper.setAopConfig(instantionAopConfig(beanDefinition));
            // set通知实例
            beanWrapper.setPostProcessor(beanPostProcessor);
            // 把实例名和代理后的对象存起来
            this.beanWrapperMap.put(beanName,beanWrapper);

            //在实例初始化以后调用一次
            beanPostProcessor.postProcessAfterInitialization(instance,beanName);

//            populateBean(beanName,instance);

            //通过这样一调用，相当于给我们自己留有了可操作的空间 返回增强后的类
            return this.beanWrapperMap.get(beanName).getWrappedInstance();
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 设置代理切面
     * @param beanDefinition
     * @return
     * @throws Exception
     */
    private GPAopConfig instantionAopConfig(GPBeanDefinition beanDefinition) throws  Exception{
        // 获取代理类实例
        GPAopConfig config = new GPAopConfig();
        // 获取配置文件里的代理信息 拿到pointCut aspectBefore aspectAfter 的value
        String expression = reader.getConfig().getProperty("pointCut");
        String[] before = reader.getConfig().getProperty("aspectBefore").split("\\s");
        String[] after = reader.getConfig().getProperty("aspectAfter").split("\\s");

        String className = beanDefinition.getBeanClassName();
        // 拿到被代理类的实例
        Class<?> clazz = Class.forName(className);
        // 拿到expression 里的配置信息
        Pattern pattern = Pattern.compile(expression);
        // 拿到before 里的配置信息的代理类
        Class aspectClass = Class.forName(before[0]);
        //在这里得到的方法都是原生的方法 循环被代理类的方法
        for (Method m : clazz.getMethods()){

            //public .* com\.gupaoedu\.vip\.spring\.demo\.service\..*Service\..*\(.*\)
            //public java.lang.String com.gupaoedu.vip.spring.demo.service.impl.ModifyService.add(java.lang.String,java.lang.String)
            //把正则表达式和循环的方法做匹配
            Matcher matcher = pattern.matcher(m.toString());
            // 如果完全匹配的上
            if(matcher.matches()){
                //能满足切面规则的类，添加的AOP配置中
                // 把要切的方法,代理类的实例,切进去的方法put到里面
                config.put(m,aspectClass.newInstance(),new Method[]{aspectClass.getMethod(before[1]),aspectClass.getMethod(after[1])});
            }
        }
        return  config;
    }




    //传一个BeanDefinition，就返回一个实例Bean
    private Object instantionBean(GPBeanDefinition beanDefinition){
        Object instance = null;
        // 获取全路径
        String className = beanDefinition.getBeanClassName();
        try{
            // 判断是否初始化过这个类的实例
            if(this.beanCacheMap.containsKey(className)){
                // 如果初始化过 就拿出来用
                instance = this.beanCacheMap.get(className);
            }else{
                // 没有初始化就反射获取
                Class<?> clazz = Class.forName(className);
                instance = clazz.newInstance();
                // 存进来
                this.beanCacheMap.put(className,instance);
            }

            return instance;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }




    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[this.beanDefinitionMap.size()]);
    }


    public int getBeanDefinitionCount() {
        return  this.beanDefinitionMap.size();
    }


    public Properties getConfig(){
        return this.reader.getConfig();
    }

}
