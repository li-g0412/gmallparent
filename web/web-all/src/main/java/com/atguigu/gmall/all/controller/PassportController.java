package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PassportController {

    //  http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        //  在这个控制器中需要保存：originUrl
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);

        //  返回视图名称
        return "login";
    }
}
