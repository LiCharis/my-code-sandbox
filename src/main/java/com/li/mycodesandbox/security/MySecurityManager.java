package com.li.mycodesandbox.security;

import java.security.Permission;

/**
 * @author 黎海旭
 * 默认安全管理器，检查用户是否有操作权限,保护服务器免于用户提交带有读写能力的文件的攻击
 **/
public class MySecurityManager extends SecurityManager {
    /**
     * 检查部分权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做所有的限制");
        super.checkPermission(perm);
    }

    @Override
    public void checkExec(String cmd) {
        super.checkExec(cmd);
    }

    @Override
    public void checkRead(String file, Object context) {
        super.checkRead(file, context);
    }

    @Override
    public void checkWrite(String file) {
        super.checkWrite(file);
    }

    @Override
    public void checkDelete(String file) {
        super.checkDelete(file);
    }

    @Override
    public void checkConnect(String host, int port) {
        super.checkConnect(host, port);
    }

}
