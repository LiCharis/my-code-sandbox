package com.li.mycodesandbox.security;

import java.security.Permission;

/**
 * @author 黎海旭
 * 默认安全管理器，检查用户是否有操作权限,保护服务器免于用户提交带有读写能力的文件的攻击
 **/
public class DenySecurityManager extends SecurityManager {
    /**
     * 拒绝所有的权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
       throw new SecurityException("权限不足 " + perm.toString());
    }
}
