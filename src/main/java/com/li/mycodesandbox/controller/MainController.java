package com.li.mycodesandbox.controller;

import com.li.mycodesandbox.model.ExecuteCodeRequest;
import com.li.mycodesandbox.model.ExecuteCodeResponse;
import com.li.mycodesandbox.tempcode.JavaDockerCodeSandBox;
import com.li.mycodesandbox.tempcode.JavaNativeSandBox;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author 黎海旭
 **/
@RestController("/")
public class MainController {
    /**
     * 定义鉴权请求头和密钥,先保证接口不会被外来请求调用(服务内部调用阶段)
     */
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeSandBox javaNativeSandBox;

    @Resource
    private JavaDockerCodeSandBox javaDockerCodeSandBox;

    @RequestMapping("/health")
    public String testHealth(){
        return "ok";
    }


    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                           HttpServletRequest httpServletRequest,
                                           HttpServletResponse httpServletResponse){

        //进行基本的验证
        String authHeader = httpServletRequest.getHeader(AUTH_REQUEST_HEADER);
        if (!authHeader.equals(AUTH_REQUEST_HEADER)){
            httpServletResponse.setStatus(403);
            return null;
        }

        if (executeCodeRequest == null){
            throw new RuntimeException("请求参数为空");
        }
        return javaDockerCodeSandBox.codeExecute(executeCodeRequest);
    }
}
