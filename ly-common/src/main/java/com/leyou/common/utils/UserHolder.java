package com.leyou.common.utils;


import com.leyou.common.auth.entity.UserInfo;

/**
 * @author 虎哥
 */
public class UserHolder {

    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    public static void setUser(UserInfo user) {
        tl.set(user);
    }

    public static UserInfo getUser() {
        return tl.get();
    }

    public static void remove() {
        tl.remove();
    }
}
