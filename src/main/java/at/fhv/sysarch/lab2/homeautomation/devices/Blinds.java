package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.enums.BlindsState;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {

    public interface BlindsCommand {
    }

    public static final class CloseBlindsCommand implements BlindsCommand {
    }

    public static final class OpenBlindsCommand implements BlindsCommand {
    }

    public static final class MovieStateChangedCommand implements BlindsCommand {
        boolean isMoviePlaying;

        public MovieStateChangedCommand(boolean isMoviePlaying) {
            this.isMoviePlaying = isMoviePlaying;
        }
    }

    public static Behavior<BlindsCommand> create(
            String groupId,
            String deviceId
    ) {
        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    private boolean isMoviePlaying = false;
    private final String groupId;
    private final String deviceId;
    private BlindsState blindsState = BlindsState.OPEN;

    public Blinds(
            ActorContext<Blinds.BlindsCommand> context,
            String groupId,
            String deviceId
    ) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;
    }

    @Override
    public Receive<Blinds.BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Blinds.OpenBlindsCommand.class, this::onOpenBlindsCommand)
                .onMessage(Blinds.CloseBlindsCommand.class, this::onCloseBlindsCommand)
                .onMessage(Blinds.MovieStateChangedCommand.class, this::onMovieStateChangedCommand)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Blinds.BlindsCommand> onOpenBlindsCommand(OpenBlindsCommand c) {
        //only open blinds if they aren't already open and no movie is playing
        if (blindsState != BlindsState.OPEN && !isMoviePlaying) {
            blindsState = BlindsState.OPEN;
            getContext().getLog().info("Blinds: {}", blindsState);
        }
        return this;
    }

    private Behavior<Blinds.BlindsCommand> onCloseBlindsCommand(CloseBlindsCommand c) {
        //only close blinds if they aren't already closed
        if (blindsState != BlindsState.CLOSED) {
            blindsState = BlindsState.CLOSED;
            getContext().getLog().info("Blinds: {}", blindsState);
        }
        return this;
    }

    private Behavior<Blinds.BlindsCommand> onMovieStateChangedCommand(MovieStateChangedCommand c) {
        this.isMoviePlaying = c.isMoviePlaying;
        return this;
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor {}-{} stopped", groupId, deviceId);
        return this;
    }

}
