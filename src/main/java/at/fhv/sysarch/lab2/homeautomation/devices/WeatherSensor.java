package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.domain.WeatherCondition;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;

import java.time.Duration;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {
    }

    public static final class RequestWeatherFromEnvironment implements WeatherCommand {
    }

    public static final class ReadWeather implements WeatherCommand {
        WeatherCondition weather;

        public ReadWeather(WeatherCondition weather) {
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

        getContext().getLog().info("WeatherSensor started");

        weatherTimeScheduler.startTimerAtFixedRate(new RequestWeatherFromEnvironment(), Duration.ofSeconds(5));
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherSensor.RequestWeatherFromEnvironment.class, this::onRequestWeatherFromEnvironment)
                .onMessage(WeatherSensor.ReadWeather.class, this::onReadWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherSensor.WeatherCommand> onRequestWeatherFromEnvironment(RequestWeatherFromEnvironment w) {
        environment.tell(new Environment.ReceiveWeatherRequest(getContext().getSelf()));
        return this;
    }

    private Behavior<WeatherSensor.WeatherCommand> onReadWeather(WeatherSensor.ReadWeather r) {
        getContext().getLog().info("WeatherSensor received {}", r.weather);
        if (r.weather == WeatherCondition.SUNNY) {
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
