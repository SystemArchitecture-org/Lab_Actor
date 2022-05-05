package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.environment.Environment;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;

import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private ActorRef<Environment.EnvironmentCommand> environment;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;

    public static Behavior<Void> create(
            ActorRef<Environment.EnvironmentCommand> environment,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
            ActorRef<MediaStation.MediaStationCommand> mediaStation
    ) {
        return Behaviors.setup(context -> new UI(context, environment, airCondition, tempSensor, mediaStation));
    }

    private UI(
            ActorContext<Void> context,
            ActorRef<Environment.EnvironmentCommand> environment,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
            ActorRef<MediaStation.MediaStationCommand> mediaStation
    ) {
        super(context);

        // TODO: implement actor and behavior as needed
        // TODO: move UI initialization to appropriate place

        this.environment = environment;
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.mediaStation = mediaStation;

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

    public void runCommandLine() {
        // TODO: Create Actor for UI Input-Handling
        Scanner scanner = new Scanner(System.in);
        String[] input = null;
        String reader = "";


        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            // TODO: change input handling*
            String[] command = reader.split(" ");
            if (command[0].equals("t")) {
                this.tempSensor.tell(new TemperatureSensor.ReadTemperature(new Temperature(Double.parseDouble(command[1]), "Celsius")));
            }
            if (command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
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
            // TODO: process Input
        }
        getContext().getLog().info("UI done");
    }
}
