package org.hanxingfeng.blog.Filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@WebFilter(filterName = "LoginFilter", urlPatterns = "/admin/*")   // 只拦截 admin.html
@Slf4j
public class LoginFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        log.info("拦截到请求：{}", httpServletRequest);

        String uri = httpServletRequest.getRequestURI();
        if (uri.endsWith("/admin/login")) {
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        // 获取登录状态
        if (httpServletRequest.getSession().getAttribute("userName") != null) {
            // 放行
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        httpServletResponse.sendRedirect("/qwert");
        return;
    }
}