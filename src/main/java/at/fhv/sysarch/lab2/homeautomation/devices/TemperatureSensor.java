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

    public interface TemperatureCommand {}

    public static final class ReadTemperature implements TemperatureCommand {
        Temperature temperature;

        public ReadTemperature(Temperature temperature){
            this.temperature = temperature;
        }
    }

    public static final class RequestTemperatureFromEnvironment implements TemperatureCommand {
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

        getContext().getLog().info("TemperatureSensor started");

        temperatureTimeScheduler.startTimerAtFixedRate(new RequestTemperatureFromEnvironment(), Duration.ofSeconds(5));
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestTemperatureFromEnvironment.class, this::onRequestTemperatureFromEnvironment)
                .onMessage(ReadTemperature.class, this::onReadTemperature)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<TemperatureCommand> onRequestTemperatureFromEnvironment(RequestTemperatureFromEnvironment e) {
        environment.tell(new Environment.ReceiveTemperatureRequest(getContext().getSelf()));
        return this;
    }

    private Behavior<TemperatureCommand> onReadTemperature(ReadTemperature r) {
        getContext().getLog().info("TemperatureSensor received {}", r.temperature.getValue());
        this.airCondition.tell(new AirCondition.EnrichedTemperature(Optional.of(r.temperature.getValue()), Optional.of(r.temperature.getUnit())));
        System.out.println("TemperatureSensor received: " + r.temperature.getValue());
        return this;
    }

    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
