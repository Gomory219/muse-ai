package cn.edu.sxu.museai.service;

import cn.edu.sxu.museai.model.dto.UserAddRequest;
import cn.edu.sxu.museai.model.dto.UserQueryRequest;
import cn.edu.sxu.museai.model.vo.LoginUserVO;
import cn.edu.sxu.museai.model.vo.UserVO;
import com.mybatisflex.core.service.IService;
import cn.edu.sxu.museai.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author OneFish
 * @since 2026-02-28
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request HTTP 请求
     * @return 是否注销成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);


    String encryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return 已登录的用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user 用户实体
     * @return 脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(UserQueryRequest userQueryRequest);

    /**
     * 新增用户
     *
     * @param userAddRequest 用户新增请求
     * @return 是否新增成功
     */
    Long saveUser(UserAddRequest userAddRequest);
}
