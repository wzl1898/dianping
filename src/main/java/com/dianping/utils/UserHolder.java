package com.dianping.utils;

import com.dianping.dto.UserDTO;

public class UserHolder {
    private static final ThreadLocal<UserDTO> TL = new ThreadLocal<>();

    public static void saveUser(UserDTO user) {
        TL.set(user);
    }

    public static UserDTO getUser() {
        return TL.get();
    }

    public static Long getUserId() {
        UserDTO user = TL.get();
        return user == null ? null : user.getId();
    }

    public static void remove() {
        TL.remove();
    }
}
