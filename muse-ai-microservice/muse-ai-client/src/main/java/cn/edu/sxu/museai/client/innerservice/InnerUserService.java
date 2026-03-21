package cn.edu.sxu.museai.client.innerservice;

import cn.edu.sxu.museai.constant.UserConstant;
import cn.edu.sxu.museai.exception.BusinessException;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface InnerUserService {

    List<User> listByIds(Collection<? extends Serializable> ids);

    User getById(Serializable id);

    UserVO getUserVO(User user);

    // 静态方法，避免跨服务调用
    static User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_INFO);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
}
