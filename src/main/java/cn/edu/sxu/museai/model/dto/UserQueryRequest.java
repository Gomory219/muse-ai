package cn.edu.sxu.museai.model.dto;

import cn.edu.sxu.museai.common.PageRequest;
import cn.edu.sxu.museai.model.enums.UserRoleEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private UserRoleEnum userRole;


    private static final long serialVersionUID = 1L;
}
