package com.cj;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@Component
@WebFilter(filterName = "repeatSubmitFilter",urlPatterns = "/*")
public class RepeatSubmitFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if(StringUtils.startsWithIgnoreCase(request.getContentType(),"application/json")){
            RepeatSubmitRequestWrapper requestWrapper = new RepeatSubmitRequestWrapper(request, response);
            filterChain.doFilter(requestWrapper,response);
            // 不写return  会继续往下走
            return;
        }

        filterChain.doFilter(servletRequest,servletResponse);
    }


}
