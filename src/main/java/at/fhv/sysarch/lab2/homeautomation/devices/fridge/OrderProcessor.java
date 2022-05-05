package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.valueobjects.Product;

import java.util.OptionalInt;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.OrderProcessorCommand> {

    public interface OrderProcessorCommand {
    }

    public static final class WeightReceivedCommand implements OrderProcessorCommand {
        int weight;

        public WeightReceivedCommand(int weight) {
            this.weight = weight;
        }
    }

    public static final class SpaceReceivedCommand implements OrderProcessorCommand {
        int space;

        public SpaceReceivedCommand(int space) {
            this.space = space;
        }
    }

    public static Behavior<OrderProcessorCommand> create(
            ActorRef<Fridge.FridgeCommand> fridge,
            ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> weightSensor,
            Product product,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new OrderProcessor(context, fridge, weightSensor, product, groupId, deviceId));
    }

    private OptionalInt availableSpace = OptionalInt.empty();
    private OptionalInt availableWeight = OptionalInt.empty();

    private ActorRef<Fridge.FridgeCommand> fridge;
    private ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> weightSensor;
    private ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> spaceSensor;
    private final Product product;
    private final String groupId;
    private final String deviceId;


    public OrderProcessor(
            ActorContext<OrderProcessorCommand> context,
            ActorRef<Fridge.FridgeCommand> fridge,
            ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> weightSensor,
            ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> spaceSensor,
            Product product,
            String groupId,
            String deviceId
    ) {
        super(context);
        this.product = product;
        this.fridge = fridge;
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.weightSensor = weightSensor;

        weightSensor.tell(new FridgeWeightSensor.GetAvailableWeightCommand(getContext().getSelf()));
        spaceSensor.tell(new FridgeSpaceSensor.GetAvailableSpaceCommand(getContext().getSelf()));

        getContext().getLog().info("OrderProcessor started");
    }

    @Override
    public Receive<OrderProcessorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderProcessor.WeightReceivedCommand.class, this::onWeightReceived)
                .onMessage(OrderProcessor.SpaceReceivedCommand.class, this::onSpaceReceived)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<OrderProcessorCommand> onWeightReceived(WeightReceivedCommand c) {
        this.availableWeight = OptionalInt.of(c.weight);
        completeOrContinue();
        return this;
    }

    private Behavior<OrderProcessorCommand> onSpaceReceived(SpaceReceivedCommand c) {
        this.availableSpace = OptionalInt.of(c.space);
        completeOrContinue();
        return this;
    }

    private Behavior<OrderProcessorCommand> completeOrContinue() {
        if (availableSpace.isPresent() && availableWeight.isPresent()) {
            if(availableWeight.getAsInt() > product.getWeight() && availableSpace.getAsInt() > 0){
                fridge.tell(new Fridge.StockFridgeCommand(product));
            } else {
                getContext().getLog().info("Fridge can't be stocked with " + product.getName() +"\n"+
                        "Available Space: " + availableSpace.getAsInt() + "\n" +
                        "Available Weight: " + availableWeight.getAsInt() + "\n" +
                        "Order Weight: " + product.getWeight());
            }
            return Behaviors.stopped();
        }
        return this;
    }

    private OrderProcessor onPostStop() {
        getContext().getLog().info("OrderProcessor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
