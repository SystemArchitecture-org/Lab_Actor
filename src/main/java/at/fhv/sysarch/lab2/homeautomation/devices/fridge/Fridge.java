package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;
import at.fhv.sysarch.lab2.homeautomation.domain.Order;
import at.fhv.sysarch.lab2.homeautomation.domain.valueobjects.Product;

import java.sql.SQLOutput;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {

    public interface FridgeCommand {
    }

    public static final class ConsumeProductCommand implements FridgeCommand {
        Product product;

        public ConsumeProductCommand(Product product) {
            this.product = product;
        }
    }

    public static final class RequestOrderProductCommand implements FridgeCommand {
        Product product;

        public RequestOrderProductCommand(Product product) {
            this.product = product;
        }
    }

    public static final class StockFridgeCommand implements FridgeCommand {
        Product product;

        public StockFridgeCommand(Product product) {
            this.product = product;
        }
    }

    public static final class DisplayStockCommand implements FridgeCommand {
    }

    public static final class DisplayOrderHistoryCommand implements FridgeCommand {
    }

    public static Behavior<FridgeCommand> create(
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new Fridge(context, groupId, deviceId));
    }

    private final List<Order> orders;
    private final List<Product> products;
    private final String groupId;
    private final String deviceId;

    private final ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> weightSensor;
    private final ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> spaceSensor;

    public Fridge(
            ActorContext<FridgeCommand> context,
            String groupId,
            String deviceId
    ) {
        super(context);

        this.orders = new LinkedList<>();
        this.products = new ArrayList<>(List.of(
                Product.create("apple").get(),
                Product.create("apple").get(),
                Product.create("apple").get(),
                Product.create("watermelon").get(),
                Product.create("elden ring").get(),
                Product.create("beer").get()
        ));

        this.groupId = groupId;
        this.deviceId = deviceId;

        this.weightSensor = getContext().spawn(FridgeWeightSensor.create(getWeightOfProducts(), "1", "1"), "FridgeWeightSensor");
        this.spaceSensor = getContext().spawn(FridgeSpaceSensor.create(products.size(), "1", "1"), "FridgeSpaceSensor");

        getContext().getLog().info("Fridge started");
    }

    private int getWeightOfProducts() {
        return products.stream().mapToInt(Product::getWeight).sum();
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestOrderProductCommand.class, this::onRequestOrderProduct)
                .onMessage(ConsumeProductCommand.class, this::onConsumeProduct)
                .onMessage(StockFridgeCommand.class, this::onStockFridge)
                .onMessage(DisplayStockCommand.class, this::onDisplayStock)
                .onMessage(DisplayOrderHistoryCommand.class, this::onDisplayOrderHistory)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onRequestOrderProduct(RequestOrderProductCommand c) {

        getContext().spawn(OrderProcessor.create(getContext().getSelf(), weightSensor, spaceSensor, c.product, "1", "1"), "OrderProcessor" + UUID.randomUUID());

        return this;
    }

    private Behavior<FridgeCommand> onConsumeProduct(ConsumeProductCommand c) {
        if (products.contains(c.product)) {
            products.remove(c.product);
            getContext().getLog().info("Removed {} from fridge", c.product.getName());
            weightSensor.tell(new FridgeWeightSensor.RemoveWeightCommand(c.product.getWeight()));
            spaceSensor.tell(new FridgeSpaceSensor.RemoveSpaceCommand(1));

            //restock fridge
            if (!products.contains(c.product)) {
                getContext().getLog().info("Last {} consumed, ordering more...", c.product.getName());
                getContext().getSelf().tell(new RequestOrderProductCommand(c.product));
            }

        } else {
            getContext().getLog().info("No {} in fridge", c.product.getName());
        }


        return this;
    }

    private Behavior<FridgeCommand> onStockFridge(StockFridgeCommand c) {
        products.add(c.product);
        weightSensor.tell(new FridgeWeightSensor.AddWeightCommand(c.product.getWeight()));
        spaceSensor.tell(new FridgeSpaceSensor.AddSpaceCommand());
        getContext().getLog().info("Added {} to fridge", c.product.getName());

        Order order = new Order(c.product);
        Product product = order.getProduct();

        System.out.println("\nReceipt:\n" +
                order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                "Items \n" + product.toString() + "\n\n" +
                "Total \t€" + product.getPrice() + "\n"
        );

        orders.add(order);
        return this;
    }

    private Behavior<FridgeCommand> onDisplayStock(DisplayStockCommand c) {
        System.out.println("\nFridge content:\n");

        for (Product product : this.products) {
            System.out.println(product.toString());
        }

        System.out.println();

        return this;
    }

    private Behavior<FridgeCommand> onDisplayOrderHistory(DisplayOrderHistoryCommand c) {
        System.out.println("\nOrder history:\n");

        for (Order order : this.orders) {
            System.out.println(order.toString());
        }

        System.out.println();

        return this;
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
