package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class FridgeSpaceSensor extends AbstractBehavior<FridgeSpaceSensor.FridgeSpaceSensorCommand> {

    public interface FridgeSpaceSensorCommand {
    }

    public static final class GetAvailableSpaceCommand implements FridgeSpaceSensorCommand {
        ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor;

        public GetAvailableSpaceCommand(ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor) {
            this.orderProcessor = orderProcessor;
        }
    }

    public static final class AddSpaceCommand implements FridgeSpaceSensor.FridgeSpaceSensorCommand {
    }

    public static final class RemoveSpaceCommand implements FridgeSpaceSensor.FridgeSpaceSensorCommand {
        int space;

        public RemoveSpaceCommand(int space) {
            this.space = space;
        }
    }

    public static Behavior<FridgeSpaceSensor.FridgeSpaceSensorCommand> create(
            int usedSpace,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new FridgeSpaceSensor(context, usedSpace, groupId, deviceId));
    }

    private final int maxSpace = 10;
    private int usedSpace;

    private final String groupId;
    private final String deviceId;

    public FridgeSpaceSensor(
            ActorContext<FridgeSpaceSensor.FridgeSpaceSensorCommand> context,
            int usedSpace,
            String groupId,
            String deviceId
    ) {
        super(context);

        this.usedSpace = usedSpace;
        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("FridgeSpaceSensor started");
    }

    @Override
    public Receive<FridgeSpaceSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetAvailableSpaceCommand.class, this::onGetAvailableSpace)
                .onMessage(AddSpaceCommand.class, this::onAddSpace)
                .onMessage(RemoveSpaceCommand.class, this::onRemoveSpace)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeSpaceSensorCommand> onGetAvailableSpace(GetAvailableSpaceCommand c) {
        c.orderProcessor.tell(new OrderProcessor.SpaceReceivedCommand(maxSpace - usedSpace));
        return this;
    }

    private Behavior<FridgeSpaceSensorCommand> onAddSpace(AddSpaceCommand c) {
        usedSpace += 1;
        getContext().getLog().info("Item added to Fridge \n used space: {}/{}", usedSpace, maxSpace);
        return this;
    }

    private Behavior<FridgeSpaceSensorCommand> onRemoveSpace(RemoveSpaceCommand c) {
        usedSpace -= 1;
        getContext().getLog().info("Item removed from Fridge \n used space: {}/{}", usedSpace, maxSpace);
        return this;
    }

    private FridgeSpaceSensor onPostStop() {
        getContext().getLog().info("FridgeSpaceSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
