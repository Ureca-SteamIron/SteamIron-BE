package norimaets.appapiserver.config;
//TEST 용 
import java.util.List;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.security.LoginUserIdArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginUserIdArgumentResolver loginUserIdArgumentResolver;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // 추후 실제 React 도메인으로 변경
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    // @LoginUserId 파라미터를 인식하게 등록
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserIdArgumentResolver);
    }
}
