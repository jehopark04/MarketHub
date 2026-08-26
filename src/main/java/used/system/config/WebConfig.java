package used.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로그인 검사를 어느 요청에 적용할지 정한다.
 *
 * <p>전부 막고 열 곳만 뚫는다. 반대로 "막을 곳만 나열"하면 나중에 페이지를 추가하며 등록을 잊었을 때 그 페이지가 무방비로 열린다. 잊었을 때 과하게 막히는 쪽이
 * 안전하다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(InterceptorRegistry registry) {

    registry
        .addInterceptor(new LoginCheckInterceptor())
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/", // 홈. 비로그인도 본다
            "/login", // 로그인 폼(GET)과 제출(POST). 막으면 로그인하러 갈 수가 없다
            "/logout", // 세션이 없으면 아무 일도 안 하고 끝난다. 막으면 로그아웃하려는 사람을 /login으로 보낸다
            "/members/new", // 회원가입 폼
            "/members", // 회원가입 제출(POST)
            "/products", // 상품 목록(GET). 등록(POST)은 아래에서 따로 막는다
            "/products/{productId:[0-9]+}", // 상품 상세(GET). 숫자로 제한해야 /products/new가 여기 걸리지 않는다
            // API는 아래 전용 인터셉터가 맡는다. 여기 걸리면 API 요청에 302 + 로그인 HTML이 나간다
            "/api/**",
            "/css/**",
            "/*.ico",
            "/error");

    // /products는 목록 조회(GET)와 상품 등록(POST)이 같은 경로다. 경로 패턴만으로는 둘을 가를 수 없어
    // 위에서 통째로 열어뒀으니, 등록 쪽만 메서드로 집어서 다시 막는다.
    registry.addInterceptor(new LoginCheckInterceptor("POST")).addPathPatterns("/products");

    // 판단은 위와 같고 거절하는 방법만 다르다 - 리다이렉트가 아니라 401.
    // 상품 조회만 연다. 화면 쪽 /products가 이미 비로그인에게 열려 있어, API만 막으면
    // JS로 옮겼을 때 비로그인 방문자가 상품을 못 보게 된다 - 화면과 정책이 어긋난다.
    registry
        .addInterceptor(new ApiLoginCheckInterceptor())
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/products", // 목록 조회(GET). 등록(POST)은 아래에서 따로 막는다
            "/api/products/{productId:[0-9]+}", // 상세 조회(GET). 숫자 제약은 화면 쪽과 같은 이유다
            "/api/members", // 회원가입 제출(POST). 가입하려는 사람에게는 세션이 없다
            "/api/login", // 로그인 제출(POST). 막으면 로그인할 방법이 없다
            "/api/logout"); // 세션이 없으면 아무 일도 안 하고 204다. 막으면 로그아웃하려는 사람에게 401이 나간다

    // 위에서 통째로 열어둔 두 경로의 쓰기 요청만 메서드로 집어서 다시 막는다.
    // /products를 GET은 열고 POST는 막은 것과 같은 구조다 - 경로 패턴만으로는 둘을 가를 수 없다.
    registry.addInterceptor(new ApiLoginCheckInterceptor("POST")).addPathPatterns("/api/products");
    registry
        .addInterceptor(new ApiLoginCheckInterceptor("PUT", "DELETE"))
        .addPathPatterns("/api/products/{productId:[0-9]+}");
  }
}
