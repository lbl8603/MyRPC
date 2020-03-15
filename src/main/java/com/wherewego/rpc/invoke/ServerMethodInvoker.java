package com.wherewego.rpc.invoke;

import com.wherewego.rpc.config.annotation.MyRPCService;
import com.wherewego.rpc.context.SpringBeanFactory;
import com.wherewego.rpc.transport.Request;
import com.wherewego.rpc.transport.Response;
import org.springframework.beans.BeansException;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 服务器处理请求
 * @Author:lbl
 * @Date:Created in 23:16 2020/3/6
 * @Modified By:
 */
public class ServerMethodInvoker implements Invoker<Request>{

    @Override
    public Object invoke(Request request) {
        Response response = new Response();
        int id = request.getId();
        response.setId(id);
        try {
            check(request);
        }catch (Exception e){
            response.setResult(e);
            return response;
        }
        String interfaceName = request.getInterfaceName();
        String beanName = request.getBeanName();
        String methodName = request.getMethodName();
        Class<?>[] paramTypes = request.getParamTypes();
        Object[] params = request.getParams();
        try {
            Object bean;
            if(StringUtils.isEmpty(beanName)){
                bean = SpringBeanFactory.beanFactory.getBean(Class.forName(interfaceName));
            }else{
                bean = SpringBeanFactory.beanFactory.getBean(beanName);
            }
            if(!bean.getClass().isAnnotationPresent(MyRPCService.class)){//有这个注解的才调用
                response.setResult(new RuntimeException(beanName+"未使用MyRPCService注解"));
                return response;
            }
            if(bean==null){
                response.setResult(new RuntimeException("未找到名称为"+beanName+"的bean"));
                return response;
            }
            Method method = bean.getClass().getMethod(methodName,paramTypes);
            response.setResult(method.invoke(bean,params));
        }catch (ClassNotFoundException e){
            response.setResult(new RuntimeException("未找到接口"+interfaceName+"请填写注解value或在服务端实现接口"));
        }catch (BeansException e){
            e.printStackTrace();
            response.setResult(new RuntimeException("未找到名称为"+beanName+"的bean"));
        }catch (NoSuchMethodException e){
            e.printStackTrace();
            response.setResult(new RuntimeException("bean中没有"+methodName+"方法"));
        }catch (SecurityException e){
            response.setResult(new RuntimeException(methodName+"方法不能访问"));
        }catch (InvocationTargetException e){//方法执行异常,需要取出具体的异常返回给调用方
            response.setResult(e.getTargetException()==null?e:e.getTargetException());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            response.setResult(e);
        }catch (Exception e){
            response.setResult(e);
        }
        return response;
    }
    private boolean check(Request request){
        if(request==null|| StringUtils.isEmpty(request.getMethodName())){
            new RuntimeException("call参数不完整");
        }
        if(StringUtils.isEmpty(request.getBeanName())&&StringUtils.isEmpty(request.getInterfaceName())){
            new RuntimeException("接口和bean名称不能同时为空");
        }
        return true;
    }
}
