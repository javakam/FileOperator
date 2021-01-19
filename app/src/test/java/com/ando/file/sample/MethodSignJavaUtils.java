package com.ando.file.sample;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 获取Java类的所有方法签名
 * <p>
 * https://blog.csdn.net/weixin_38106322/article/details/108218774
 */
public class MethodSignJavaUtils {

    public static void main(String[] args) {
        printMethodSign(new String("666"));
    }

    private static <T> void printMethodSign(T t) {
        //获取对象类信息
        Class<?> aClass = t.getClass();
        //获取类中方法
        Method[] methods = aClass.getDeclaredMethods();
        //遍历方法
        System.out.println("-------------------------------------------------------------");
        for (Method method : methods) {
            //获取方法修饰符
            String mod = Modifier.toString(method.getModifiers());
            //先拼接修饰符+方法名+'('
            System.out.print(mod + " " + method.getName() + "(");
            //获取方法参数类型
            Class<?>[] parameterTypes = method.getParameterTypes();
            //如果没有参数，那就直接拼接上+')'
            if (parameterTypes.length == 0) {
                System.out.print(")");
            } else {
                //有参数则遍历
                for (int i = 0; i < parameterTypes.length; i++) {
                    //没到最后一位参数，都用','，否则使用')'最收尾
                    char end = i == parameterTypes.length - 1 ? ')' : ',';
                    //输出参数
                    System.out.print(parameterTypes[i].getSimpleName() + end);
                }
            }
            //为每个方法换行
            System.out.println();
        }
        System.out.println("-------------------------------------------------------------");
    }
}