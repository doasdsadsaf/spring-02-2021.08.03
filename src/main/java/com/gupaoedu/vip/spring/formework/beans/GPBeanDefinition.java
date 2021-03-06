package com.gupaoedu.vip.spring.formework.beans;

/**
 * Created by Tom on 2018/4/21.
 */

//用来存储配置文件中的信息
//相当于保存在内存中的配置
public class GPBeanDefinition {
    // 存储类全路径
    private String beanClassName;
    private boolean lazyInit = false;
    // 存储类的对象名
    private String factoryBeanName;

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }

    public boolean isLazyInit() {
        return lazyInit;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }

}
