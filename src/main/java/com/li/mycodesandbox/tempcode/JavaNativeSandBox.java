package com.li.mycodesandbox.tempcode;

import com.li.mycodesandbox.model.ExecuteCodeRequest;
import com.li.mycodesandbox.model.ExecuteCodeResponse;
import com.li.mycodesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * @author 黎海旭
 * java原生实现，直接服用模板方法(可以根据需要重写模板的方法，很灵活)
 **/
@Service
public class JavaNativeSandBox extends JavaCodeSandBoxTemplate{

    @Override
    public ExecuteCodeResponse codeExecute(ExecuteCodeRequest executeCodeRequest) {
        return super.codeExecute(executeCodeRequest);
    }
}
