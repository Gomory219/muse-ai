package cn.edu.sxu.museai.model.dto;

import cn.edu.sxu.museai.model.enums.UserRoleEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private UserRoleEnum userRole;


    private static final long serialVersionUID = 1L;
}
