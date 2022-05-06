package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class FridgeWeightSensor extends AbstractBehavior<FridgeWeightSensor.FridgeWeightSensorCommand> {

    public interface FridgeWeightSensorCommand {
    }

    public static final class GetAvailableWeightCommand implements FridgeWeightSensorCommand {
        ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor;

        public GetAvailableWeightCommand(ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor) {
            this.orderProcessor = orderProcessor;
        }
    }

    public static final class AddWeightCommand implements FridgeWeightSensorCommand {
        int weight;

        public AddWeightCommand(int weight) {
            this.weight = weight;
        }
    }

    public static final class RemoveWeightCommand implements FridgeWeightSensorCommand {
        int weight;

        public RemoveWeightCommand(int weight) {
            this.weight = weight;
        }
    }

    public static Behavior<FridgeWeightSensorCommand> create(
            int currentWeight,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new FridgeWeightSensor(context, currentWeight, groupId, deviceId));
    }

    private final int maxWeight = 100;
    private int currentWeight;

    private final String groupId;
    private final String deviceId;

    public FridgeWeightSensor(
            ActorContext<FridgeWeightSensorCommand> context,
            int currentWeight,
            String groupId,
            String deviceId
    ) {
        super(context);

        this.currentWeight = currentWeight;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("FridgeWeightSensor started");
    }

    @Override
    public Receive<FridgeWeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetAvailableWeightCommand.class, this::onGetAvailableWeight)
                .onMessage(AddWeightCommand.class, this::onAddWeight)
                .onMessage(RemoveWeightCommand.class, this::onRemoveWeight)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeWeightSensorCommand> onGetAvailableWeight(GetAvailableWeightCommand c) {
        c.orderProcessor.tell(new OrderProcessor.WeightReceivedCommand(maxWeight - currentWeight));
        return this;
    }

    private Behavior<FridgeWeightSensorCommand> onAddWeight(AddWeightCommand c) {
        currentWeight += c.weight;
        getContext().getLog().info("FridgeWeightSensor weight added: {} \n current total weight: {}", c.weight, currentWeight);
        return this;
    }

    private Behavior<FridgeWeightSensorCommand> onRemoveWeight(RemoveWeightCommand c) {
        currentWeight -= c.weight;
        getContext().getLog().info("FridgeWeightSensor weight removed: {} \n current total weight: {}", c.weight, currentWeight);
        return this;
    }


    private FridgeWeightSensor onPostStop() {
        getContext().getLog().info("FridgeWeightSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
