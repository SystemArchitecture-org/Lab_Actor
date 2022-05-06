package at.fhv.sysarch.lab2.homeautomation.domain;

import at.fhv.sysarch.lab2.homeautomation.domain.valueobjects.Product;

import java.time.LocalDateTime;
import java.util.UUID;

public class Order {

    private final UUID uuid;
    private final LocalDateTime orderDate;

    private final Product product;

    public Order(Product product) {
        this.uuid = UUID.randomUUID();
        this.orderDate = LocalDateTime.now();

        this.product = product;
    }

    public UUID getUuid() {
        return uuid;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public String toString() {
        return "Order{" +
                "uuid=" + uuid +
                ", orderDate=" + orderDate +
                ", product=" + product +
                '}';
    }
}
