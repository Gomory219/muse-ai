package cn.edu.sxu.museai.controller;

import cn.edu.sxu.museai.constant.UserConstant;
import cn.edu.sxu.museai.model.dto.*;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.model.enums.UserRoleEnum;
import cn.edu.sxu.museai.model.vo.LoginUserVO;
import cn.edu.sxu.museai.model.vo.UserVO;
import cn.edu.sxu.museai.service.UserService;
import cn.edu.sxu.museai.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 单元测试
 * <p>
 * 使用 standaloneSetup 方式构建 MockMvc，不依赖 Spring 容器。
 * AOP 权限控制（@AuthCheck）需要单独测试。
 * <p>
 * Session 认证通过 MockHttpSession 模拟，设置 USER_INFO 属性即可。
 *
 * @author OneFish
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 单元测试")
class UserControllerTest {

    private MockMvc mockMvc;

    private MockHttpSession session;

    @Mock
    private UserService userService;

    private UserController userController;

    private ObjectMapper objectMapper;

    private User adminUser;

    private User normalUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        session = new MockHttpSession();

        // 创建控制器实例
        userController = new UserController(userService);

        // 使用 standaloneSetup 构建 MockMvc，配置 Jackson 消息转换器和全局异常处理器
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // 创建管理员用户
        adminUser = User.builder()
                .id(1L)
                .userAccount("admin")
                .userPassword("encryptedPassword")
                .userName("管理员")
                .userRole(UserRoleEnum.ADMIN)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();

        // 创建普通用户
        normalUser = User.builder()
                .id(2L)
                .userAccount("testUser")
                .userPassword("encryptedPassword")
                .userName("测试用户")
                .userRole(UserRoleEnum.USER)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();
    }

    @Nested
    @DisplayName("用户注册接口测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册用户")
        void testRegisterSuccess() throws Exception {
            // Given
            when(userService.userRegister("newUser", "password123", "password123"))
                    .thenReturn(100L);

            UserRegisterRequest request = new UserRegisterRequest();
            request.setUserAccount("newUser");
            request.setUserPassword("password123");
            request.setConfirmPassword("password123");

            // When & Then
            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("100"));

            verify(userService).userRegister("newUser", "password123", "password123");
        }

        @Test
        @DisplayName("注册 - 密码不匹配")
        void testRegisterPasswordMismatch() throws Exception {
            // Given
            when(userService.userRegister("newUser", "password123", "password456"))
                    .thenThrow(new RuntimeException("密码不匹配"));

            UserRegisterRequest request = new UserRegisterRequest();
            request.setUserAccount("newUser");
            request.setUserPassword("password123");
            request.setConfirmPassword("password456");

            // When & Then - GlobalExceptionHandler 捕获异常返回 HTTP 200 + 错误码
            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(50000)); // SYSTEM_ERROR

            verify(userService).userRegister("newUser", "password123", "password456");
        }
    }

    @Nested
    @DisplayName("用户登录接口测试")
    class LoginTests {

        @Test
        @DisplayName("正常登录")
        void testLoginSuccess() throws Exception {
            // Given
            LoginUserVO loginUserVO = new LoginUserVO();
            loginUserVO.setId(1L);
            loginUserVO.setUserAccount("admin");
            loginUserVO.setUserName("管理员");
            loginUserVO.setUserRole(UserRoleEnum.ADMIN);
            loginUserVO.setCreateTime(LocalDateTime.now());

            when(userService.userLogin(eq("admin"), eq("password123"), any()))
                    .thenReturn(loginUserVO);

            UserLoginRequest request = new UserLoginRequest();
            request.setUserAccount("admin");
            request.setUserPassword("password123");

            // When & Then
            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.userAccount").value("admin"))
                    .andExpect(jsonPath("$.data.userName").value("管理员"));

            verify(userService).userLogin(eq("admin"), eq("password123"), any());
        }

        @Test
        @DisplayName("登录 - 请求体为空")
        void testLoginWithNullRequest() throws Exception {
            // When & Then
            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(null)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists());

            verify(userService, never()).userLogin(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("获取当前登录用户接口测试")
    class GetLoginUserTests {

        @Test
        @DisplayName("正常获取当前登录用户")
        void testGetLoginUserSuccess() throws Exception {
            // Given
            LoginUserVO loginUserVO = new LoginUserVO();
            loginUserVO.setId(2L);
            loginUserVO.setUserAccount("testUser");
            loginUserVO.setUserName("测试用户");

            when(userService.getLoginUser(any())).thenReturn(normalUser);
            when(userService.getLoginUserVO(normalUser)).thenReturn(loginUserVO);

            // When & Then
            mockMvc.perform(get("/user/get/login")
                            .session(session))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(2))
                    .andExpect(jsonPath("$.data.userAccount").value("testUser"));

            verify(userService).getLoginUser(any());
            verify(userService).getLoginUserVO(normalUser);
        }
    }

    @Nested
    @DisplayName("用户登出接口测试")
    class LogoutTests {

        @Test
        @DisplayName("正常登出")
        void testLogoutSuccess() throws Exception {
            // Given
            when(userService.userLogout(any())).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/user/logout")
                            .session(session))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));

            verify(userService).userLogout(any());
        }
    }

    @Nested
    @DisplayName("新增用户接口测试（需要ADMIN权限）")
    class SaveUserTests {

        @Test
        @DisplayName("管理员新增用户成功")
        void testSaveUserSuccess() throws Exception {
            // Given - 设置管理员session（注：standaloneSetup 不支持 AOP，权限检查需单独测试）
            when(userService.saveUser(any(UserAddRequest.class))).thenReturn(100L);

            UserAddRequest request = new UserAddRequest();
            request.setUserAccount("newUser");
            request.setUserName("新用户");
            request.setUserRole(UserRoleEnum.USER);

            // When & Then
            mockMvc.perform(post("/user/save")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("userAccount", "newUser")
                            .param("userName", "新用户"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(100));

            verify(userService).saveUser(any(UserAddRequest.class));
        }
    }

    @Nested
    @DisplayName("获取用户详情接口测试（需要ADMIN权限）")
    class GetUserTests {

        @Test
        @DisplayName("管理员获取用户成功")
        void testGetUserSuccess() throws Exception {
            // Given
            when(userService.getById(any())).thenReturn(adminUser);

            // When & Then
            mockMvc.perform(get("/user/get")
                            .param("id", "1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.userAccount").value("admin"));

            verify(userService).getById(any());
        }

        @Test
        @DisplayName("获取用户 - id为空")
        void testGetUserWithNullId() throws Exception {
            // When & Then
            mockMvc.perform(get("/user/get"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists()); // 参数错误

            verify(userService, never()).getById(any());
        }
    }

    @Nested
    @DisplayName("获取用户VO接口测试（无需权限）")
    class GetUserVOTests {

        @Test
        @DisplayName("正常获取用户VO")
        void testGetUserVoSuccess() throws Exception {
            // Given
            UserVO userVO = new UserVO();
            userVO.setId(2L);
            userVO.setUserAccount("testUser");
            userVO.setUserName("测试用户");
            userVO.setUserRole(UserRoleEnum.USER);
            userVO.setCreateTime(LocalDateTime.now());

            when(userService.getById(any())).thenReturn(normalUser);
            when(userService.getUserVO(any())).thenReturn(userVO);

            // When & Then
            mockMvc.perform(get("/user/get/vo")
                            .param("id", "2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(2))
                    .andExpect(jsonPath("$.data.userAccount").value("testUser"));

            verify(userService).getById(any());
            verify(userService).getUserVO(normalUser);
        }

        @Test
        @DisplayName("获取用户VO - id为空")
        void testGetUserVoWithNullId() throws Exception {
            // When & Then
            mockMvc.perform(get("/user/get/vo"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists());

            verify(userService, never()).getById(any());
        }
    }

    @Nested
    @DisplayName("用户列表接口测试（需要ADMIN权限）")
    class ListUserTests {

        @Test
        @DisplayName("管理员获取用户列表")
        void testListUserSuccess() throws Exception {
            // Given
            UserVO userVO1 = new UserVO();
            userVO1.setId(1L);
            userVO1.setUserAccount("admin");
            userVO1.setUserRole(UserRoleEnum.ADMIN);

            UserVO userVO2 = new UserVO();
            userVO2.setId(2L);
            userVO2.setUserAccount("testUser");
            userVO2.setUserRole(UserRoleEnum.USER);

            List<UserVO> userList = Arrays.asList(userVO1, userVO2);

            when(userService.getUserVOList(any(UserQueryRequest.class))).thenReturn(userList);

            // When & Then
            mockMvc.perform(get("/user/list"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].userAccount").value("admin"))
                    .andExpect(jsonPath("$.data[1].userAccount").value("testUser"));

            verify(userService).getUserVOList(any(UserQueryRequest.class));
        }
    }

    @Nested
    @DisplayName("更新用户接口测试（需要ADMIN权限）")
    class UpdateUserTests {

        @Test
        @DisplayName("管理员更新用户成功")
        void testUpdateUserSuccess() throws Exception {
            // Given
            when(userService.updateById(any(User.class))).thenReturn(true);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setId(2L);
            request.setUserName("更新后的用户名");

            // When & Then
            mockMvc.perform(post("/user/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));

            verify(userService).updateById(any(User.class));
        }

        @Test
        @DisplayName("更新用户 - id为空")
        void testUpdateUserWithNullId() throws Exception {
            // Given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setUserName("更新后的用户名");

            // When & Then
            mockMvc.perform(post("/user/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists());

            verify(userService, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("删除用户接口测试（需要ADMIN权限）")
    class DeleteUserTests {

        @Test
        @DisplayName("管理员删除用户成功")
        void testDeleteUserSuccess() throws Exception {
            // Given
            when(userService.removeById("2")).thenReturn(true);

            DeleteRequest request = new DeleteRequest();
            request.setId("2");

            // When & Then
            mockMvc.perform(post("/user/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));

            verify(userService).removeById("2");
        }

        @Test
        @DisplayName("删除用户 - id为空")
        void testDeleteUserWithNullId() throws Exception {
            // Given
            DeleteRequest request = new DeleteRequest();

            // When & Then
            mockMvc.perform(post("/user/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists());

            verify(userService, never()).removeById(any());
        }
    }
}
