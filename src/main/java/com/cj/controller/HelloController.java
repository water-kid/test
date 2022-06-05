package com.cj.controller;

import com.cj.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    /**
     * 你这个人+访问的这个接口+ 提交的数据  ，， 在单位时间内是一样的， 重复提交
     * @param json
     * @return
     */
    @PostMapping("/hello")
    @RepeatSubmit(interval = 10000)
    public String hello(@RequestBody String json){
        return json;
    }
}
