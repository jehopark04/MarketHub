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

    public Product(String sellerId, String title, String description, int price, ProductGrade grade) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.createAt = LocalDateTime.now();
        this.grade = grade;
    }

    public ProductGrade getGrade() {
        return grade;
    }

    public void setGrade(ProductGrade grade) {
        this.grade = grade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }
}
