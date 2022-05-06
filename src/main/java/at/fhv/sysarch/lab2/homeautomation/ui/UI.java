package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.domain.enums.WeatherCondition;
import at.fhv.sysarch.lab2.homeautomation.domain.valueobjects.Product;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;

import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private final ActorRef<Environment.EnvironmentCommand> environment;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private final ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<Void> create(
            ActorRef<Environment.EnvironmentCommand> environment,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
            ActorRef<MediaStation.MediaStationCommand> mediaStation,
            ActorRef<Fridge.FridgeCommand> fridge
    ) {
        return Behaviors.setup(context -> new UI(context, environment, airCondition, tempSensor, mediaStation, fridge));
    }

    private UI(
            ActorContext<Void> context,
            ActorRef<Environment.EnvironmentCommand> environment,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
            ActorRef<MediaStation.MediaStationCommand> mediaStation,
            ActorRef<Fridge.FridgeCommand> fridge
    ) {
        super(context);

        this.environment = environment;
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.mediaStation = mediaStation;
        this.fridge = fridge;

        new Thread(this::runCommandLine).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    //TODO: UI OK?
    public void runCommandLine() {
        Scanner scanner = new Scanner(System.in);
//        String[] input = null;
        String reader = "";

        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            String[] command = reader.split(" ");

            if (command[0].equals("t")) {
                this.tempSensor.tell(new TemperatureSensor.ReadTemperatureCommand(new Temperature(Double.parseDouble(command[1]), "Celsius")));
            }

            if (command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirConditionCommand(Optional.of(Boolean.valueOf(command[1]))));
            }

            if (command[0].equals("m")) {
                if (command[1].equals("on")) {
                    this.mediaStation.tell(new MediaStation.StartMovieCommand());
                } else if (command[1].equals("off")) {
                    this.mediaStation.tell(new MediaStation.StopMovieCommand());
                } else {
                    System.out.println("wrong command");
                    System.out.println("please try 'on' or 'off' ✔");
                }
            }

            if (command[0].equals("fadd")) {
                Optional<Product> product = Product.create(command[1]);
                if (product.isPresent()) {
                    this.fridge.tell(new Fridge.RequestOrderProductCommand(product.get()));
                } else {
                    System.out.println("wrong product try: 'apple', 'watermelon' or 'beer'");
                }
            }
            if (command[0].equals("frem")) {
                Optional<Product> product = Product.create(command[1]);
                if (product.isPresent()) {
                    this.fridge.tell(new Fridge.ConsumeProductCommand(product.get()));
                } else {
                    System.out.println("wrong product try: 'apple', 'watermelon' or 'beer'");
                }
            }
            if (command[0].equals("fdis")) {
                this.fridge.tell(new Fridge.DisplayStockCommand());
            }
            if (command[0].equals("et")) {
                this.environment.tell(new Environment.SetTemperatureCommand(new Temperature(Double.parseDouble(command[1]), "Celsius")));
            }
            if (command[0].equals("ew")) {
                this.environment.tell(new Environment.SetWeatherCommand(WeatherCondition.valueOf(command[1])));
            }
            if (command[0].equals("?")) {
                System.out.println("Commands:");
                System.out.println("t [temperature]                 - set temperature");
                System.out.println("a                               - turn on/off air condition");
                System.out.println("m [on|off]                      - turn on/off movie station");
                System.out.println("fadd [apple|watermelon|beer]    - add item to fridge");
                System.out.println("frem [apple|watermelon|beer]    - remove item from fridge");
                System.out.println("fdis                            - display stock of fridge");
                System.out.println("ew [SUNNY|CLOUDY]               - set weather condition");
            }
        }

        getContext().getLog().info("UI done");
    }
}
