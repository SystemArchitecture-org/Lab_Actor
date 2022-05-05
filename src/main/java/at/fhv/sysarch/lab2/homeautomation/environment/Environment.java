package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.domain.enums.WeatherCondition;

import java.time.Duration;
import java.util.Random;

public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public interface EnvironmentCommand {
    }

    public static final class TemperatureChanger implements EnvironmentCommand {
    }

    public static final class WeatherChanger implements EnvironmentCommand {
    }

    public static final class ReceiveTemperatureRequest implements EnvironmentCommand {
        ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;

        public ReceiveTemperatureRequest(ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {
            this.temperatureSensor = temperatureSensor;
        }
    }

    public static final class ReceiveWeatherRequest implements EnvironmentCommand {
        ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
        public ReceiveWeatherRequest(ActorRef<WeatherSensor.WeatherCommand> weatherSensor){
            this.weatherSensor = weatherSensor;
        }
    }

    public static Behavior<EnvironmentCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
    }

    private Temperature temperature = new Temperature(23, "Celsius");
    private WeatherCondition weatherCondition = WeatherCondition.SUNNY;
    private boolean setHighTemp = false;
    private boolean setLowTemp = true;

    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

    public Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> temperatureTimeScheduler, TimerScheduler<EnvironmentCommand> weatherTimeScheduler) {
        super(context);

        this.temperatureTimeScheduler = temperatureTimeScheduler;
        this.weatherTimeScheduler = weatherTimeScheduler;

        temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(5));
        weatherTimeScheduler.startTimerAtFixedRate(new WeatherChanger(), Duration.ofSeconds(5));
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReceiveTemperatureRequest.class, this::onReceiveTemperatureRequest)
                .onMessage(ReceiveWeatherRequest.class, this::onReceiveWeatherRequest)
                .onMessage(TemperatureChanger.class, this::onTemperatureChange)
                .onMessage(WeatherChanger.class, this::onWeatherChange)
                .build();
    }

    private Behavior<EnvironmentCommand> onReceiveTemperatureRequest(ReceiveTemperatureRequest request){
        request.temperatureSensor.tell(new TemperatureSensor.ReadTemperature(temperature));
        return this;
    }

    private Behavior<EnvironmentCommand> onReceiveWeatherRequest(ReceiveWeatherRequest request){
        request.weatherSensor.tell(new WeatherSensor.ReadWeather(weatherCondition));
        return this;
    }

    private Behavior<EnvironmentCommand> onTemperatureChange(TemperatureChanger temperatureChanger) {
        Random rand = new Random();

        temperature.setValue(temperature.getValue() + 4 * rand.nextDouble() - 2);

        if (temperature.getValue() >= 25) {
            setHighTemp = true;
            setLowTemp = false;
        } else {
            setLowTemp = true;
            setHighTemp = false;
        }

        System.out.println("Temperature: " + temperature.getValue() + " " + temperature.getUnit());

        return this;
    }

    private Behavior<EnvironmentCommand> onWeatherChange(WeatherChanger weatherChanger) {
        Random rand = new Random();

        weatherCondition = WeatherCondition.values()[rand.nextInt(WeatherCondition.values().length)];

        System.out.println("WeatherCondition: " + weatherCondition);

        return this;
    }
}
