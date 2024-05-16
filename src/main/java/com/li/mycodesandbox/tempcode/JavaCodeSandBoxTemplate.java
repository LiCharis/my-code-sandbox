package com.li.mycodesandbox.tempcode;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.li.mycodesandbox.CodeSandBox;
import com.li.mycodesandbox.Utils.ProcessUtils;
import com.li.mycodesandbox.model.ExecuteCodeRequest;
import com.li.mycodesandbox.model.ExecuteCodeResponse;
import com.li.mycodesandbox.model.ExecuteMessage;
import com.li.mycodesandbox.model.JudgeInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 黎海旭
 * 模板方法
 **/
@Slf4j
public class JavaCodeSandBoxTemplate implements CodeSandBox {

    /**
     * 放置提交代码的文件夹路径
     */
    private static final String GLOBAL_CODE_DIR_PATH = "src/main/java/com/li/mycodesandbox/tempcode";

    /**
     * 文件名，这里定死为Main.java
     */
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    /**
     * 超时定义（ms）,定义程序执行时间超过10s即判定为超时(可根据要求自己定义)
     */
    private static final long TIME_OUT = 10000L;

    /**
     * 保存用户提交的代码
     * @param code
     * @return
     */
    public File saveCodeToFile(String code){

        //取到项目根目录
        String userDir = System.getProperty("user.dir");
        // todo 这里得考虑不同系统下的分隔符问题 linus windows
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_PATH;

        //如果文件路径不存在就创建目录
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //记得把代码隔离，因为不能把所有的Main.class都放在同一个目录下
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;

        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 编译代码，得到.class文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile){
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {

            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");

            if (executeMessage.getExitCode() != 0){
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 执行文件获得执行结果列表
     * @param inputList
     * @return
     */

    public List<ExecuteMessage> runCode(List<String> inputList,File userCodeFile,long timeout){
        ArrayList<ExecuteMessage> executeMessages = new ArrayList<>();
        //取到项目根目录

        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        for (String input : inputList) {
            /**
             * 指定jvm最大堆内存，避免用户传进来的代码占用过多服务器内存溢出，这里指定最大为256mb(实际上会超过一些，不会很准确)
             */
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, input);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);

                new Thread(() -> {
                    try {
                        Thread.sleep(timeout);
                        /**
                         *  如果10s后程序用户输出的程序还没有执行完，那么就中断程序，避免程序卡死占用资源
                         */
                        if (runProcess.isAlive()){
                            runProcess.destroy();
                            throw new RuntimeException("程序运行超时");
                        }

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessages.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return executeMessages;
    }

    /**
     * 获取、处理输出结果
     * @param executeMessages
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessages){
        //得到所有测试用例运行所花的最大值，有一个超时了就不符合要求
        long maxTime = 0;
        //得到所有测试用例所消耗的内存
        long maxMemery = 0;
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ArrayList<String> outputList = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessages) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                //执行中存在错误，代码运行错误
                //就将报错信息放入到返回结果信息中
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            //没有错误就将程序成功执行的结果放入到返回的列表中
            outputList.add(executeMessage.getMessage());
            Long executeTime = executeMessage.getExecuteTime();
            Long executeMemory = executeMessage.getExecuteMemory();
            if (executeTime != null) {
                maxTime = Math.max(maxTime, executeTime);
            }
            if (executeMemory != null){
                maxMemery = Math.max(maxMemery,executeMemory);
            }

        }
        //如果全部正常执行的话
        if (outputList.size() == executeMessages.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(maxMemery);
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除文件
     * @param userCodeFile
     * @return
     */
    public boolean doDelete(File userCodeFile){
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }



    /**
     * 获取错误方法
     *
     * @param e 异常值
     * @return 返回响应
     */

    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        //代表沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }






    @Override
    public ExecuteCodeResponse codeExecute(ExecuteCodeRequest executeCodeRequest) {
//        /**
//         * 设置权限管理器
//         */
//        System.setSecurityManager(new DefaultSecurityManager());
        ExecuteCodeResponse executeCodeResponse = null;
        File userCodeFile = null;
        try {
            /**
             * 1.把用户的代码保存为文件
             */
            List<String> inputList = executeCodeRequest.getInputList();
            String language = executeCodeRequest.getLanguage();
            String code = executeCodeRequest.getCode();

            userCodeFile = saveCodeToFile(code);

            /**
             *  2.编译代码，得到class文件
             */
            ExecuteMessage executeMessage = compileFile(userCodeFile);
            System.out.println(executeMessage);

            /**
             *  3.执行程序,得到代码输出值
             */
            List<ExecuteMessage> executeMessages = runCode(inputList,userCodeFile,TIME_OUT);


            /**
             *  4.收集整理输出结果
             */
            executeCodeResponse = getOutputResponse(executeMessages);


            /**
             *   5.文件清理，清除class文件，避免占用服务器空间
             */
            boolean doDelete = doDelete(userCodeFile);
            if (!doDelete){
                log.error("delete file error,userCodeFilePath = {}",userCodeFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            //删除用户提交的文件
            boolean doDelete = doDelete(userCodeFile);
            if (!doDelete){
                log.error("delete file error,userCodeFilePath = {}",userCodeFile.getAbsolutePath());
            }
            return getErrorResponse(e);
        }


        //6错误处理，提升程序健壮性，比如当用户程序编译失败未等执行

        return executeCodeResponse;
    }

}
