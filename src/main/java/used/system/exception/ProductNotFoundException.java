package used.system.exception;

public class ProductNotFoundException extends RuntimeException { // 예외를 직접 만든 상황

  public ProductNotFoundException(String message) {
    super(message);
  }
}
