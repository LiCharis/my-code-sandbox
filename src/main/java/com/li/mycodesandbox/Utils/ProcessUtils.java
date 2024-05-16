package com.li.mycodesandbox.Utils;

import cn.hutool.core.date.StopWatch;
import com.li.mycodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author 黎海旭
 **/
public class ProcessUtils {
    public static ExecuteMessage runProcessAndGetMessage(Process process, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();


        try {
            //计算程序执行时间
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            //等待程序执行，获取错误码
            int exitValue = process.waitFor();
            executeMessage.setExitCode(exitValue);
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                //获取终端输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                ArrayList<String> outputList = new ArrayList<>();
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputList,"\n"));

            } else {
                System.out.println(opName + "失败,错误码为: " + exitValue);

                //获取终端输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                ArrayList<String> outputList = new ArrayList<>();
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputList,"\n"));


                //获取终端错误输出
                BufferedReader bufferedErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                ArrayList<String> outputErrorList = new ArrayList<>();
                String compileErrorOutputLine;
                while ((compileErrorOutputLine = bufferedErrorReader.readLine()) != null) {
                    outputErrorList.add(compileErrorOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(outputErrorList,"\n"));

            }
            stopWatch.stop();
            executeMessage.setExecuteTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}