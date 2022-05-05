package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {

    public interface MediaStationCommand {
    }

    public static final class StartMovieCommand implements MediaStationCommand {
    }

    public static final class StopMovieCommand implements MediaStationCommand {
    }

    public static Behavior<MediaStationCommand> create(ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        return Behaviors.setup(context -> new MediaStation(context, blinds, groupId, deviceId));
    }

    private ActorRef<Blinds.BlindsCommand> blinds;
    private Boolean isMoviePlaying = false;
    private final String groupId;
    private final String deviceId;

    public MediaStation(
            ActorContext<MediaStationCommand> context,
            ActorRef<Blinds.BlindsCommand> blinds,
            String groupId,
            String deviceId
    ) {
        super(context);

        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;
    }


    @Override
    public Receive<MediaStation.MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(MediaStation.StartMovieCommand.class, this::onStartMovie)
                .onMessage(MediaStation.StopMovieCommand.class, this::onStopMovie)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStationCommand> onStartMovie(StartMovieCommand c) {
        if (!isMoviePlaying) {
            isMoviePlaying = true;
            getContext().getLog().info("Movie starts playing...");
            this.blinds.tell(new Blinds.MovieStateChangedCommand(isMoviePlaying));
        } else {
            getContext().getLog().info("Movie is already playing...");
        }
        return this;
    }

    private Behavior<MediaStationCommand> onStopMovie(StopMovieCommand c) {
        if (isMoviePlaying) {
            isMoviePlaying = false;
            getContext().getLog().info("Movie stops playing...");
            this.blinds.tell(new Blinds.MovieStateChangedCommand(isMoviePlaying));
        } else {
            getContext().getLog().info("Can't stop movie, because no movie is playing....");
        }
        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
