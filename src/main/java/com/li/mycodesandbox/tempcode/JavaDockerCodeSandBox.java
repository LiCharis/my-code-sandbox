package com.li.mycodesandbox.tempcode;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.li.mycodesandbox.model.ExecuteCodeRequest;
import com.li.mycodesandbox.model.ExecuteCodeResponse;
import com.li.mycodesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 黎海旭
 **/
@Service
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate{
    private static Boolean FIRST_INIT  = true;

    public static void main(String[] args) throws InterruptedException {
        JavaDockerCodeSandBox javaDockerCodeSandBox = new JavaDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","3 4"));
        String code = ResourceUtil.readStr("Main.java", StandardCharsets.UTF_8);
        System.out.println(code);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.codeExecute(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
    @Override
    public List<ExecuteMessage> runCode(List<String> inputList, File userCodeFile,long timeout) {

        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        System.out.println(userCodeParentPath);


        /**
         * 1.拉取镜像
         */
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "openjdk:8-alpine";
        if (FIRST_INIT){
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){

                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像 " + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("下载完成");
            FIRST_INIT = false;
        }

        /**
         * 2.创建容器
         */

        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        //限制内存
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath,new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withNetworkDisabled(true)  //禁用联网功能
                .withReadonlyRootfs(true)   //禁止往root目录写文件
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();
        System.out.println(containerId);

        /**
         * 3.启动容器,执行命令并获取结果(运行结果、运行时间、内存消耗、正确信息、错误信息)
         */

        dockerClient.startContainerCmd(containerId).exec();

        ArrayList<ExecuteMessage> executeMessages = new ArrayList<>();
        for (String input : inputList) {
            ExecuteMessage executeMessage = new ExecuteMessage();

            String[] inputArray = input.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java","-cp","/app","Main"},inputArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令: " + execCreateCmdResponse);
            String exeId = execCreateCmdResponse.getId();
            /**
             *判断程序是否超时
             * 设置一个标志位,默认为超时，除非在规定的超时时间内将标志位改为false,这里规定的为5s，命令执行回调限制为5s
             */
            final boolean[] isTimeout = {true};
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback(){

                /**
                 * 该方法为程序执行完命令后会调用的方法
                 */
                @Override
                public void onComplete() {
                    isTimeout[0] = false;

                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)){
                        executeMessage.setErrorMessage(new String(frame.getPayload()));
                        System.out.println("输出错误结果: " + new String(frame.getPayload()));

                    }else {
                        executeMessage.setMessage(new String(frame.getPayload()));
                        System.out.println("输出结果: " + new String(frame.getPayload()));
                    }
                    super.onNext(frame);
                }
            };

            /**
             * 获取占用的内存
             */

            final Long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用情况: " + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            statsCmd.close();


            try {
                /**
                 * 时间这里得处理一下
                 */
                //计算程序执行时间
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                /**
                 * 根据设定的超时时间，程序执行超时就停掉
                 */
                dockerClient.execStartCmd(exeId)
                        .exec(execStartResultCallback)
                        //毫秒
                        .awaitCompletion(timeout, TimeUnit.MILLISECONDS);

                stopWatch.stop();
                executeMessage.setExecuteTime(stopWatch.getLastTaskTimeMillis());
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }

            //设置内存消耗
            executeMessage.setExecuteMemory(maxMemory[0]/1000);

            executeMessages.add(executeMessage);
        }
        /**
         * 4.销毁容器
         */
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        System.out.println(executeMessages);

        return executeMessages;

    }
}
