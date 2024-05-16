package com.li.mycodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * @author 黎海旭
 * java操作docker的demo
 **/
public class DockerDemo {
    public static void main(String[] args) {
        //获取默认的DockerClient
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        PingCmd pingCmd = dockerClient.pingCmd();
        pingCmd.exec();

    }
}
