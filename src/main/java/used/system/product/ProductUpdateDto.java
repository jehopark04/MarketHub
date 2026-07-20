package used.system.product;

public record ProductUpdateDto(

        String title,
        String description,
        int price,
        ProductGrade grade
) {
}
