package com.ruoyi.web.controller.system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.framework.aspectj.DataScopeAspect;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.impl.SysUserServiceImpl;

class SysUserControllerTest
{
    @AfterEach
    void cleanUp()
    {
        PageHelper.clearPage();
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void listReturnsTheScopedServiceResult()
    {
        SysUser expectedUser = new SysUser(9100L);
        expectedUser.setUserName("test-admin");
        ISysUserService scopedUserService = (ISysUserService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { ISysUserService.class }, (proxy, method, args) -> {
                    if ("selectAdminUserList".equals(method.getName()))
                    {
                        return Collections.singletonList(expectedUser);
                    }
                    throw new AssertionError("Unexpected service operation: " + method.getName());
                });
        SysUserController controller = new SysUserController();
        ReflectionTestUtils.setField(controller, "userService", scopedUserService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "1");
        request.setParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TableDataInfo result = assertDoesNotThrow(() -> controller.list(new SysUser()));

        assertEquals(Collections.singletonList(expectedUser), result.getRows());
    }

    @Test
    void adminListDataScopeUsesTheUserAndDepartmentAliases()
    {
        assertDataScope("5", " AND (u.user_id = 77 )");
        assertDataScope("3", " AND (d.dept_id = 10 )");
    }

    private void assertDataScope(String scope, String expectedScope)
    {
        SysUser filter = new SysUser();
        ISysUserService service = proxiedUserService();
        signIn(scope);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertDoesNotThrow(() -> service.selectAdminUserList(filter));

        assertEquals(expectedScope, filter.getParams().get(DataScopeAspect.DATA_SCOPE));
    }

    private ISysUserService proxiedUserService()
    {
        SysUserServiceImpl target = new SysUserServiceImpl();
        ReflectionTestUtils.setField(target, "userMapper", Mockito.mock(SysUserMapper.class));
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new DataScopeAspect());
        return proxyFactory.getProxy();
    }

    private void signIn(String dataScope)
    {
        SysRole role = new SysRole();
        role.setRoleId(3L);
        role.setDataScope(dataScope);
        role.setStatus("0");
        SysUser user = new SysUser(77L);
        user.setDeptId(10L);
        user.setRoles(Arrays.asList(role));
        LoginUser loginUser = new LoginUser(77L, 10L, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }
}
