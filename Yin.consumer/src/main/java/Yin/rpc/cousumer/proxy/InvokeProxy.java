package Yin.rpc.cousumer.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.HashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import Yin.rpc.cousumer.annotation.RemoteInvoke;
import Yin.rpc.cousumer.core.NettyClient;
import Yin.rpc.cousumer.param.ClientRequest;
import Yin.rpc.cousumer.param.Response;

@Component
public class InvokeProxy implements BeanPostProcessor {
	//创建Enhancer代理类，既可以代理类，又可以拦截方法
	public static Enhancer enhancer = new Enhancer();

	public Object postProcessAfterInitialization(Object bean, String arg1) throws BeansException {
		return bean;
	}

	//对属性的所有方法和属性类型放入到HashMap中
	private void putMethodClass(HashMap<Method, Class> methodmap, Field field) {
		Method[] methods = field.getType().getDeclaredMethods();
		for(Method method : methods){
			methodmap.put(method, field.getType());
		}
		
	}

	public Object postProcessBeforeInitialization(Object bean, String arg1) throws BeansException {
//		System.out.println(bean.getClass().getName());
		Field[] fields = bean.getClass().getDeclaredFields();
		for(Field field : fields){
			if(field.isAnnotationPresent(RemoteInvoke.class)){
				field.setAccessible(true);
				//对属性的所有方法和属性类型放入到HashMap中
				final HashMap<Method, Class> methodmap = new HashMap<Method, Class>();
				putMethodClass(methodmap,field);
//				Enhancer enhancer = new Enhancer();
				//开头创建静态变量
				enhancer.setInterfaces(new Class[]{field.getType()});
				enhancer.setCallback(new MethodInterceptor() {
					
					public Object intercept(Object instance, Method method, Object[] args, MethodProxy proxy) throws Throwable {
						//需要采用netty客户端去调用服务器
						ClientRequest clientRequest = new ClientRequest();
						clientRequest.setContent(args[0]);
						//command=从map中获取需要调用属性的接口名+方法名
						String command= methodmap.get(method).getName()+"."+method.getName();
//						String command = method.getName();//修改
//						System.out.println("InvokeProxy中的Command是:"+command);
						clientRequest.setCommand(command);
						//调用客户端请求发送
						Response response = NettyClient.send(clientRequest);
						return response;
					}
				});
				try {
					field.set(bean, enhancer.create());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return bean;
	}

}
