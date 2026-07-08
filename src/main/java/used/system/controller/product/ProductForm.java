package used.system.controller.product;

public class ProductForm {


    private String sellerId;
    private String title;
    private String description;
    private int price;


    public ProductForm(String sellerId, String title, String description, int price) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
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
}
