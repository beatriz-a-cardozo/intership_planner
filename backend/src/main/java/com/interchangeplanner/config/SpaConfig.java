package com.interchangeplanner.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Imagem única de deploy (deploy/Dockerfile): o frontend compilado vai para {@code classpath:/static} e o próprio
 * backend o serve, no mesmo domínio da API (sem proxy e sem CORS). Só ativa quando o {@code index.html} existe; no
 * docker compose o frontend continua no nginx.
 *
 * <ul>
 *   <li>Rotas do React Router ({@code /universities/123}) devolvem o {@code index.html}.</li>
 *   <li>{@code /assets/*} têm hash no nome: cache de 1 ano, imutável. O {@code index.html} não é cacheado.</li>
 * </ul>
 */
@Configuration
@ConditionalOnResource(resources = "classpath:/static/index.html")
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());
    }

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> spaForwardFilter() {
        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>(new SpaForwardFilter());
        bean.setOrder(Ordered.LOWEST_PRECEDENCE);
        return bean;
    }

    /** GET de página (sem extensão, fora da API) → {@code index.html}. */
    static class SpaForwardFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (isPageRequest(request)) {
                request.getRequestDispatcher("/index.html").forward(request, response);
                return;
            }
            chain.doFilter(request, response);
        }

        static boolean isPageRequest(HttpServletRequest request) {
            if (!"GET".equals(request.getMethod())) {
                return false;
            }
            String path = request.getRequestURI();
            if (isBackendPath(path)) {
                return false;
            }
            String last = path.substring(path.lastIndexOf('/') + 1);
            return !last.contains(".");
        }
    }

    /** Caminhos do backend (não são páginas do SPA). */
    static boolean isBackendPath(String path) {
        return path.startsWith("/api/") || path.equals("/api") || path.startsWith("/actuator")
                || path.startsWith("/v3/") || path.startsWith("/swagger-ui") || path.equals("/error");
    }
}
