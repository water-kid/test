package com.cj.interceptor;

import com.cj.RepeatSubmitRequestWrapper;
import com.cj.annotation.RepeatSubmit;
import com.cj.redis.RedisCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RepeatSubmitInterceptor implements HandlerInterceptor {

    public static final String REPEAT_PARAMS = "repeat_params";
    public static final String REPEAT_TIME = "repeat_time";
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit_key";
    public static final String HEADER = "Authorization";

    @Autowired
    RedisCache redisCache;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //  handler 拦截下来的对象
        //  handlerMethod  将你定义的接口方法封装成一个对象，你这个方法是属于哪个类的，方法参数，返回值，泛型，返回值等各种信息是什么，封装成handlerMethod
        if(handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            RepeatSubmit repeatSubmit = method.getAnnotation(RepeatSubmit.class);
            if(repeatSubmit != null){
                // 注解存在
                if(isRepeatSubmit(request,repeatSubmit)){
                    // 是重复提交
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("status",500);
                    map.put("message",repeatSubmit.message());
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write(new ObjectMapper().writeValueAsString(map));
                    return false;
                }
            }
        }
        System.out.println(request.getReader().readLine());
        return true;
    }

    private boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit repeatSubmit) {
        // 请求 传入的json数据
        String nowParams = "";
        if(request instanceof RepeatSubmitRequestWrapper){
            // 请求参数是 json
            try {
                nowParams = ((RepeatSubmitRequestWrapper) request).getReader().readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // 不是 json，是key-value 格式
        if(StringUtils.isEmpty(nowParams)){
            try {
                nowParams = new ObjectMapper().writeValueAsString(request.getParameterMap());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }
        Map<String,Object> nowDataMap = new HashMap<>();
        nowDataMap.put(REPEAT_PARAMS,nowParams);
        nowDataMap.put(REPEAT_TIME,System.currentTimeMillis());
        String requestURI = request.getRequestURI();
        // header 必须传，不然为空
        String header = request.getHeader(HEADER);
        // 缓存的 key
        String cacheKey = REPEAT_SUBMIT_KEY + requestURI+header.replace("Bearer ","");

        
        // 获取redis中的值
        Object cacheObject = redisCache.getCacheObject(cacheKey);
        
        if(cacheObject != null){
            Map<String,Object> map = (Map<String,Object>) cacheObject;
            // 比较 redis中  和传入的是否相同
            if(compareParams(map,nowDataMap) && compareTime(map,nowDataMap,repeatSubmit.interval())){
                // 重复提交
                return true;
            }
        }

        // redis中为null，，，说明你是第一次请求，存入redis
        redisCache.setCacheObject(cacheKey,nowDataMap, repeatSubmit.interval(), TimeUnit.MILLISECONDS);
        
        
        return false;
    }

    private boolean compareTime(Map<String, Object> map, Map<String, Object> nowDataMap, int interval) {
        // 判断时间是否在这个 时间内
        Long time1 = (Long) map.get(REPEAT_TIME);
        Long time2 = (Long) nowDataMap.get(REPEAT_TIME);
        if(time2 - time1 < interval){
            return true;
        }
        return  false;
    }

    private boolean compareParams(Map<String, Object> map, Map<String, Object> nowDataMap) {
        // 比较内容
        String nowParams = (String) nowDataMap.get(REPEAT_PARAMS);
        String dataParams = (String) map.get(REPEAT_PARAMS);
        return nowParams.equals(dataParams);
    }
}
