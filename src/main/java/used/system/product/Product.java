package used.system.product;

import java.time.LocalDateTime;

public class Product {

  private Long id;
  private String sellerId;
  private String title;
  private String description;
  private int price;
  private ProductGrade grade;
  private LocalDateTime createAt;
  private LocalDateTime updatedAt;

  public Product(String sellerId, String title, String description, int price, ProductGrade grade) {
    this.sellerId = sellerId;
    this.title = title;
    this.description = description;
    this.price = price;
    this.createAt = LocalDateTime.now();
    this.updatedAt = this.createAt;
    this.grade = grade;
  }

  public void update(String title, String description, int price, ProductGrade grade) {
    this.title = title;
    this.description = description;
    this.price = price;
    this.grade = grade;
    this.updatedAt = LocalDateTime.now();
  }

  public ProductGrade getGrade() {
    return grade;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) { // 저장 시점에 리포지토리가 채번하여 부여 (그 외 용도 금지)
    this.id = id;
  }

  public String getSellerId() {
    return sellerId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public int getPrice() {
    return price;
  }

  public LocalDateTime getCreateAt() {
    return createAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
