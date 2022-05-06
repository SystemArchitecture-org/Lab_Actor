package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;

import java.time.Duration;
import java.util.Optional;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> {

    public interface TemperatureCommand {
    }

    public static final class ReadTemperatureCommand implements TemperatureCommand {
        Temperature temperature;

        public ReadTemperatureCommand(Temperature temperature) {
            this.temperature = temperature;
        }
    }

    public static final class RequestTemperatureFromEnvironmentCommand implements TemperatureCommand {
    }

    public static Behavior<TemperatureCommand> create(
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Environment.EnvironmentCommand> environment,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> Behaviors.withTimers(timer -> new TemperatureSensor(context, airCondition, environment, groupId, deviceId, timer)));
    }

    private final String groupId;
    private final String deviceId;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<Environment.EnvironmentCommand> environment;

    public TemperatureSensor(
            ActorContext<TemperatureCommand> context,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Environment.EnvironmentCommand> environment,
            String groupId,
            String deviceId,
            TimerScheduler<TemperatureSensor.TemperatureCommand> temperatureTimeScheduler
    ) {
        super(context);
        this.airCondition = airCondition;
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.environment = environment;

        temperatureTimeScheduler.startTimerAtFixedRate(new RequestTemperatureFromEnvironmentCommand(), Duration.ofSeconds(5));

        getContext().getLog().info("TemperatureSensor started");
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestTemperatureFromEnvironmentCommand.class, this::onRequestTemperatureFromEnvironment)
                .onMessage(ReadTemperatureCommand.class, this::onReadTemperature)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<TemperatureCommand> onRequestTemperatureFromEnvironment(RequestTemperatureFromEnvironmentCommand c) {
        environment.tell(new Environment.ReceiveTemperatureRequestCommand(getContext().getSelf()));
        return this;
    }

    private Behavior<TemperatureCommand> onReadTemperature(ReadTemperatureCommand c) {
        getContext().getLog().info("TemperatureSensor received {}", c.temperature.getValue());
        this.airCondition.tell(new AirCondition.EnrichedTemperatureCommand(Optional.of(c.temperature.getValue()), Optional.of(c.temperature.getUnit())));
        System.out.println("TemperatureSensor received: " + c.temperature.getValue());
        return this;
    }

    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
