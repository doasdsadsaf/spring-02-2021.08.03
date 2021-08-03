package com.gupaoedu.vip.spring.formework.beans;

/**
 * Created by Tom on 2018/4/21.
 */
//用做事件监听的
public class GPBeanPostProcessor {
    // 后处理
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    // 前处理
    public Object postProcessAfterInitialization(Object bean, String beanName){
        return bean;
    }

}
