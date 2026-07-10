package used.system.exception;


import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice // exception핸들러의 적용을 전역으로 해주는 녀석암
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ProductNotFoundException.class)
    public String handlerProductNotFound(ProductNotFoundException e, Model model){
        model.addAttribute("message", e.getMessage());
        return "error/404";
    }
}
