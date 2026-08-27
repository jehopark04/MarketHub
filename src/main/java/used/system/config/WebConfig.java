package used.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 화면 주소 매핑과, 로그인 검사를 어느 요청에 적용할지 정한다.
 *
 * <p>전부 막고 열 곳만 뚫는다. 반대로 "막을 곳만 나열"하면 나중에 경로를 추가하며 등록을 잊었을 때 그 경로가 무방비로 열린다. 잊었을 때 과하게 막히는 쪽이 안전하다.
 *
 * <p>거절은 401이다. 302로 로그인 페이지에 보내면 클라이언트가 실패를 로그인 HTML 본문으로 받아, 요청이 실패했다는 사실 자체가 전달되지 않는다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /**
   * 확장자 없는 주소로 화면을 연다(/login → login.html).
   *
   * <p>스프링은 정적 파일을 파일 이름 그대로만 매핑한다. 이것이 없으면 /login은 404이고 주소에 .html이 드러난다. forward라 서버 안에서만 넘기므로
   * 주소창은 /login으로 남는다.
   *
   * <p>상품 목록은 여기 없다. static/index.html이 이미 /로 열린다.
   */
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/login").setViewName("forward:/login.html");
    registry.addViewController("/join").setViewName("forward:/join.html");
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {

    registry
        .addInterceptor(new ApiLoginCheckInterceptor())
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            // 상품 조회는 로그인 없이 열어둔다. 둘러보다 마음에 들면 가입하는 흐름이라
            // 목록과 상세를 막으면 가입 전 방문자가 볼 것이 없다.
            "/api/products", // 목록 조회(GET). 등록(POST)은 아래에서 따로 막는다
            "/api/products/{productId:[0-9]+}", // 상세 조회(GET). 수정·삭제도 아래에서 따로 막는다
            "/api/members", // 회원가입 제출(POST). 가입하려는 사람에게는 세션이 없다
            "/api/login", // 로그인 제출(POST). 막으면 로그인할 방법이 없다
            "/api/logout"); // 세션이 없으면 아무 일도 안 하고 204다. 막으면 로그아웃하려는 사람에게 401이 나간다

    // 위에서 통째로 열어둔 두 경로의 쓰기 요청만 메서드로 집어서 다시 막는다.
    // 인터셉터의 경로 매칭은 HTTP 메서드를 구분하지 못해, 경로 패턴만으로는 조회와 쓰기를 가를 수 없다.
    registry.addInterceptor(new ApiLoginCheckInterceptor("POST")).addPathPatterns("/api/products");
    registry
        .addInterceptor(new ApiLoginCheckInterceptor("PUT", "DELETE"))
        .addPathPatterns("/api/products/{productId:[0-9]+}");
  }
}
