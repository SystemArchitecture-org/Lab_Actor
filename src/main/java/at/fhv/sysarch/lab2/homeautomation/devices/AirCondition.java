package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.util.Optional;

/**
 * This class shows ONE way to switch behaviors in object-oriented style. Another approach is the use of static
 * methods for each behavior.
 *
 * The switching of behaviors is not strictly necessary for this example, but is rather used for demonstration
 * purpose only.
 *
 * For an example with functional-style please refer to: {@link https://doc.akka.io/docs/akka/current/typed/style-guide.html#functional-versus-object-oriented-style}
 *
 */

@SuppressWarnings("JavadocReference")
public class AirCondition extends AbstractBehavior<AirCondition.AirConditionCommand> {

    public interface AirConditionCommand {
    }

    public static final class PowerAirConditionCommand implements AirConditionCommand {
        final Optional<Boolean> value;

        public PowerAirConditionCommand(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class EnrichedTemperatureCommand implements AirConditionCommand {
        Optional<Double> value;
        Optional<String> unit;

        public EnrichedTemperatureCommand(Optional<Double> value, Optional<String> unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public static Behavior<AirConditionCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new AirCondition(context, groupId, deviceId));
    }

    private final String groupId;
    private final String deviceId;
    private boolean active = false;
    private boolean poweredOn = true;

    public AirCondition(ActorContext<AirConditionCommand> context, String groupId, String deviceId) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;

        getContext().getLog().info("AirCondition started");
    }

    @Override
    public Receive<AirConditionCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(EnrichedTemperatureCommand.class, this::onReadTemperature)
                .onMessage(PowerAirConditionCommand.class, this::onPowerAirConditionOff)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<AirConditionCommand> onReadTemperature(EnrichedTemperatureCommand c) {
        getContext().getLog().info("AirCondition reading {}", c.value.get());

        if (c.value.get() >= 20) {
            getContext().getLog().info("AirCondition activated");
            this.active = true;
        } else {
            getContext().getLog().info("AirCondition deactivated");
            this.active = false;
        }

        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOff(PowerAirConditionCommand c) {
        getContext().getLog().info("Turning AirCondition to {}", c.value);

        if (!c.value.get()) {
            return this.powerOff();
        }

        return this;
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOn(PowerAirConditionCommand c) {
        getContext().getLog().info("Turning AirCondition to {}", c.value);

        if (c.value.get()) {
            return Behaviors.receive(AirConditionCommand.class)
                    .onMessage(EnrichedTemperatureCommand.class, this::onReadTemperature)
                    .onMessage(PowerAirConditionCommand.class, this::onPowerAirConditionOff)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }

        return this;
    }

    private Behavior<AirConditionCommand> powerOff() {
        this.poweredOn = false;
        return Behaviors.receive(AirConditionCommand.class)
                .onMessage(PowerAirConditionCommand.class, this::onPowerAirConditionOn)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private AirCondition onPostStop() {
        getContext().getLog().info("AirCondition actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
