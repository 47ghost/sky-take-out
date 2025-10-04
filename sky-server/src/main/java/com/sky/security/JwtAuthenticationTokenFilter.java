package com.sky.security;


import com.sky.config.security.SecurityProperties;
import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.exception.AuthenticationFailedException;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;


import java.io.IOException;
import java.util.List;


@Slf4j
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAuthenticationTokenFilter(JwtProperties jwtProperties,SecurityProperties securityProperties, HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtProperties = jwtProperties;
        this.securityProperties=securityProperties;
        this.handlerExceptionResolver=handlerExceptionResolver;
    }

/*
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        StringBuffer url = request.getRequestURL();
        String method = request.getMethod();

        String token = request.getHeader(jwtProperties.getAdminTokenName());
        if (token == null || token.isBlank()) {
            log.info("jwt过滤器放行[{}] {}", method, url);
            filterChain.doFilter(request, response);
            return;
        }

        // 仅解析 JWT 的阶段进行捕获
        Claims claims;
        try {
            claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.info("[{}]{} - jwt校验失败: {}", method, url, e.getMessage());
            AuthenticationFailedException authEx = new AuthenticationFailedException("令牌验证失败，" + e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, authEx);
            return;
        }

        Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
        String username = String.valueOf(claims.get(JwtClaimsConstant.EMP_NAME));
        log.info("[{}]{} - jwt校验通过，员工id: {}", method, url, empId);

        BaseContext.setCurrentId(empId);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(username, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);


            filterChain.doFilter(request, response);

    }
*/

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI    = request.getRequestURI();
        String method = request.getMethod();
        String requestUrl= String.valueOf(request.getRequestURL());
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestUrl += "?" + queryString;
        }

        // 根据请求路径选择不同的令牌和密钥
        int tag=1;
        String token;
        String secretKey;
        String claimId;

        if (requestURI.startsWith("/admin")) {
            token = request.getHeader(jwtProperties.getAdminTokenName());
            secretKey = jwtProperties.getAdminSecretKey();
            claimId = JwtClaimsConstant.EMP_ID;
        } else if (requestURI.startsWith("/user")) {
            token = request.getHeader(jwtProperties.getUserTokenName());
            secretKey = jwtProperties.getUserSecretKey();
            claimId = JwtClaimsConstant.USER_ID; // 假设用户ID的声明为 "userId"
            tag=2;
        } else {
            // 对于非 /admin/ 和 /user/ 的路径，直接放行
            log.info("非admin和user的路径，直接放行({})",requestUrl);
            tag=2;
            filterChain.doFilter(request, response);
            return;
        }

        // 如果没有令牌，直接放行（适用于登录等白名单请求）
        if (token == null || token.isBlank()) {
            log.info("jwt过滤器放行(无令牌) [{}] {}", method, requestUrl);
            filterChain.doFilter(request, response);
            return;
        }

        // 解析JWT令牌
        Claims claims;
        try {
            claims = JwtUtil.parseJWT(secretKey, token);
        } catch (JwtException | IllegalArgumentException e) {
            log.info("[{}]{} - jwt校验失败: {}", method, requestUrl, e.getMessage());
            AuthenticationFailedException authEx = new AuthenticationFailedException("令牌验证失败，" + e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, authEx);
            return;
        }

        String username;
        Long currentId = Long.valueOf(claims.get(claimId).toString());

        if(tag==1) {

            username = String.valueOf(claims.get("name")); // 假设声明中包含 'name'
            log.info("[{}]{} - jwt校验通过，员工id: {}", method, requestUrl, currentId);

            // 存储当前员工ID到线程上下文中
            BaseContext.setCurrentId(currentId);

        }else {
            username="userfromWechat";
            log.info("[{}]{} - 微信用户(id:{})jwt通过",method,requestUrl,currentId);

            BaseContext.setCurrentId(currentId);
        }
        // 创建Authentication对象并存入SecurityContext
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(username, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
