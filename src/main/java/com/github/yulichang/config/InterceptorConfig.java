package com.github.yulichang.config;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import com.github.yulichang.toolkit.InterceptorList;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 兼容 page helper 插件类
 *
 * @author yulichang
 */
@Configuration
@ConditionalOnBean(SqlSessionFactory.class)
@AutoConfigureBefore(value = {PageHelper.class, PageHelperAutoConfiguration.class, PageInterceptor.class})
@SuppressWarnings("unused")
public class InterceptorConfig {


    private static final Log logger = LogFactory.getLog(InterceptorConfig.class);

    public InterceptorConfig(List<SqlSessionFactory> sqlSessionFactoryList) {
        replaceInterceptorChain(sqlSessionFactoryList);
    }

    @SuppressWarnings("unchecked")
    private void replaceInterceptorChain(List<SqlSessionFactory> sqlSessionFactoryList) {
        if (CollectionUtils.isEmpty(sqlSessionFactoryList)) {
            return;
        }
        for (SqlSessionFactory factory : sqlSessionFactoryList) {
            try {
                Field interceptorChain = org.apache.ibatis.session.Configuration.class.getDeclaredField("interceptorChain");
                interceptorChain.setAccessible(true);
                InterceptorChain chain = (InterceptorChain) interceptorChain.get(factory.getConfiguration());
                Field interceptors = InterceptorChain.class.getDeclaredField("interceptors");
                interceptors.setAccessible(true);
                List<Interceptor> list = (List<Interceptor>) interceptors.get(chain);
                if (CollectionUtils.isEmpty(list)) {
                    interceptors.set(chain, new InterceptorList<>());
                } else {
                    interceptors.set(chain, new InterceptorList<>(list));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("初始化 MPJ 拦截器失败");
                e.printStackTrace();
            }
        }
    }
}
