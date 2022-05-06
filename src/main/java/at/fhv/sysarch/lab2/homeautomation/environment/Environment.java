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

    public static final class TemperatureChangerCommand implements EnvironmentCommand {
    }

    public static final class WeatherChangerCommand implements EnvironmentCommand {
    }

    public static final class SetTemperatureCommand implements EnvironmentCommand {
        Temperature temperature;

        public SetTemperatureCommand(Temperature temperature) {
            this.temperature = temperature;
        }
    }

    public static final class SetWeatherCommand implements EnvironmentCommand {
        WeatherCondition weatherCondition;

        public SetWeatherCommand(WeatherCondition weatherCondition) {
            this.weatherCondition = weatherCondition;
        }
    }

    public static final class ReceiveTemperatureRequestCommand implements EnvironmentCommand {
        ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;

        public ReceiveTemperatureRequestCommand(ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {
            this.temperatureSensor = temperatureSensor;
        }
    }

    public static final class ReceiveWeatherRequestCommand implements EnvironmentCommand {
        ActorRef<WeatherSensor.WeatherCommand> weatherSensor;

        public ReceiveWeatherRequestCommand(ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
            this.weatherSensor = weatherSensor;
        }
    }

    public static Behavior<EnvironmentCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
    }

    private Temperature temperature = new Temperature(23, "Celsius");
    private WeatherCondition weatherCondition = WeatherCondition.SUNNY;

    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

    public Environment(ActorContext<EnvironmentCommand> context, TimerScheduler<EnvironmentCommand> temperatureTimeScheduler, TimerScheduler<EnvironmentCommand> weatherTimeScheduler) {
        super(context);

        this.temperatureTimeScheduler = temperatureTimeScheduler;
        this.weatherTimeScheduler = weatherTimeScheduler;

        temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChangerCommand(), Duration.ofSeconds(5));
        weatherTimeScheduler.startTimerAtFixedRate(new WeatherChangerCommand(), Duration.ofSeconds(5));
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReceiveTemperatureRequestCommand.class, this::onReceiveTemperatureRequest)
                .onMessage(ReceiveWeatherRequestCommand.class, this::onReceiveWeatherRequest)
                .onMessage(TemperatureChangerCommand.class, this::onTemperatureChange)
                .onMessage(WeatherChangerCommand.class, this::onWeatherChange)
                .onMessage(SetTemperatureCommand.class, this::onSetTemperature)
                .onMessage(SetWeatherCommand.class, this::onSetWeather)
                .build();
    }

    private Behavior<EnvironmentCommand> onReceiveTemperatureRequest(ReceiveTemperatureRequestCommand request) {
        request.temperatureSensor.tell(new TemperatureSensor.ReadTemperatureCommand(temperature));
        return this;
    }

    private Behavior<EnvironmentCommand> onReceiveWeatherRequest(ReceiveWeatherRequestCommand request) {
        request.weatherSensor.tell(new WeatherSensor.ReadWeatherCommand(weatherCondition));
        return this;
    }

    private Behavior<EnvironmentCommand> onTemperatureChange(TemperatureChangerCommand temperatureChanger) {
        double newRanValue = this.temperature.getValue() + (4 * new Random().nextDouble() - 2);

        this.temperature.setValue(newRanValue);

        System.out.println("Temperature: " + newRanValue + " " + this.temperature.getUnit());

        return this;
    }

    private Behavior<EnvironmentCommand> onSetTemperature(SetTemperatureCommand c) {
        this.temperature.setValue(c.temperature.getValue());
        return this;
    }

    private Behavior<EnvironmentCommand> onSetWeather(SetWeatherCommand c) {
        this.weatherCondition = c.weatherCondition;
        return this;
    }


    private Behavior<EnvironmentCommand> onWeatherChange(WeatherChangerCommand weatherChanger) {
        Random rand = new Random();

        weatherCondition = WeatherCondition.values()[rand.nextInt(WeatherCondition.values().length)];

        System.out.println("WeatherCondition: " + weatherCondition);

        return this;
    }
}
