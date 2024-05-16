package com.li.mycodesandbox.model;

import lombok.Data;

/**
 * @author 黎海旭
 * 返回编译或者执行的结果信息
 **/
@Data
public class ExecuteMessage {
    /**
     * 程序执行状态码
     */
    private Integer exitCode;
    /**
     * 程序执行正常输出
     */
    private String message;
    /**
     * 程序执行异常输出
     */
    private String errorMessage;
    /**
     * 程序执行时间
     */
    private Long executeTime;
    /**
     * 程序消耗的内存
     */
    private Long executeMemory;
}
