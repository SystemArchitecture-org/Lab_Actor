package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.domain.enums.WeatherCondition;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;

import java.time.Duration;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {
    }

    public static final class RequestWeatherFromEnvironmentCommand implements WeatherCommand {
    }

    public static final class ReadWeatherCommand implements WeatherCommand {
        WeatherCondition weather;

        public ReadWeatherCommand(WeatherCondition weather) {
            this.weather = weather;
        }
    }

    public static Behavior<WeatherCommand> create(
            ActorRef<Environment.EnvironmentCommand> environment,
            ActorRef<Blinds.BlindsCommand> blinds,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> Behaviors.withTimers(timer -> new WeatherSensor(context, environment, blinds, groupId, deviceId, timer)));
    }

    private final String groupId;
    private final String deviceId;
    private ActorRef<Environment.EnvironmentCommand> environment;
    private ActorRef<Blinds.BlindsCommand> blinds;

    public WeatherSensor(ActorContext<WeatherCommand> context,
                         ActorRef<Environment.EnvironmentCommand> environment,
                         ActorRef<Blinds.BlindsCommand> blinds,
                         String groupId,
                         String deviceId,
                         TimerScheduler<WeatherSensor.WeatherCommand> weatherTimeScheduler
    ) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;
        this.environment = environment;
        this.blinds = blinds;

        weatherTimeScheduler.startTimerAtFixedRate(new RequestWeatherFromEnvironmentCommand(), Duration.ofSeconds(5));

        getContext().getLog().info("WeatherSensor started");
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestWeatherFromEnvironmentCommand.class, this::onRequestWeatherFromEnvironment)
                .onMessage(ReadWeatherCommand.class, this::onReadWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherSensor.WeatherCommand> onRequestWeatherFromEnvironment(RequestWeatherFromEnvironmentCommand c) {
        environment.tell(new Environment.ReceiveWeatherRequestCommand(getContext().getSelf()));
        return this;
    }

    private Behavior<WeatherSensor.WeatherCommand> onReadWeather(ReadWeatherCommand c) {
        getContext().getLog().info("WeatherSensor received {}", c.weather);
        //we call tell everytime not just when the weather actually changes because of the fire and forget implementation
        //this way it's not a big deal if one message gets lost because it will still be triggered with the next one even if nothing changes
        if (c.weather == WeatherCondition.SUNNY) {
            blinds.tell(new Blinds.CloseBlindsCommand());
        } else {
            blinds.tell(new Blinds.OpenBlindsCommand());
        }
        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
