package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.domain.WeatherCondition;

import java.time.Duration;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {
    }

    public static final class RequestWeatherFromEnvironment implements WeatherCommand {
    }
    public static final class ReadWeather implements WeatherCommand{
        WeatherCondition weather;

        public ReadWeather(WeatherCondition weather) {
            this.weather = weather;
        }
    }

    public static Behavior<WeatherCommand> create(
            ActorRef<Environment.EnvironmentCommand> environment,
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> Behaviors.withTimers(timer -> new WeatherSensor(context, environment, groupId, deviceId, timer)));
    }

    private final String groupId;
    private final String deviceId;
    private ActorRef<Environment.EnvironmentCommand> environment;

    public WeatherSensor(ActorContext<WeatherCommand> context,
                         ActorRef<Environment.EnvironmentCommand> environment,
                         String groupId,
                         String deviceId,
                         TimerScheduler<WeatherSensor.WeatherCommand> weatherTimeScheduler
    ) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;
        this.environment = environment;

        getContext().getLog().info("WeatherSensor started");

        weatherTimeScheduler.startTimerAtFixedRate(new RequestWeatherFromEnvironment(), Duration.ofSeconds(35));
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
        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
