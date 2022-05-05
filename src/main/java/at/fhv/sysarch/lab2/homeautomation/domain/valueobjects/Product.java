package at.fhv.sysarch.lab2.homeautomation.domain.valueobjects;

import java.util.Objects;
import java.util.Optional;

public class Product {

    private final String name;
    private final double price;
    private final int weight;

    private Product(String name, double price, int weight) {
        this.name = name;
        this.price = price;
        this.weight = weight;
    }

    public static Optional<Product> create(String name) {
        Optional<Product> product;

        switch (name) {
            case "apple":
                product = Optional.of(new Product(name, 1.0, 2));
                break;

            case "watermelon":
                product = Optional.of(new Product(name, 3.0, 20));
                break;

            case "elden ring":
                product = Optional.of(new Product(name, 69.0, 2));
                break;

            case "beer":
                product = Optional.of(new Product(name, 2.3, 3));
                break;

            default:
                product = Optional.empty();
        }

        return product;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "Product{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", weight=" + weight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Double.compare(product.price, price) == 0 && weight == product.weight && name.equals(product.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price, weight);
    }

}
