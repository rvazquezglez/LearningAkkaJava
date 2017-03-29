import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorCreation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActorCreation.class);

    private static class MusicController extends AbstractActor {

        // Music Controller Messages
        private interface ControllerMsg {
        }

        private static class Play implements ControllerMsg {
        }

        private static class Stop implements ControllerMsg {
        }

        private static Props props() {
            return Props.create(MusicController.class);
        }

        public MusicController() {
            receive(ReceiveBuilder
                .match(
                    Play.class,
                    playMsg -> LOGGER.info("Music Started .....")
                )
                .match(
                    Stop.class,
                    playMsg -> LOGGER.info("Music Stopped .....")
                )
                .build());
        }
    }

    private static class MusicPlayer extends AbstractActor {

        // Music Player Messages
        private interface PlayMsg {
        }

        private static class StopMusic implements PlayMsg {
        }

        private static class StartMusic implements PlayMsg {
        }

        public MusicPlayer() {
            receive(ReceiveBuilder
                .match(
                    StopMusic.class,
                    stopMusicMsg -> LOGGER.info("I don't want to stop music "))
                .match(
                    StartMusic.class,
                    startMusicMsg -> {
                        ActorRef controller = context().actorOf(MusicController.props(), "controller");
                        controller.tell(new MusicController.Play(), self());
                    })
                .build());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create the 'creation' actor system
        ActorSystem system = ActorSystem.create("creation");

        // Create the 'MusicPlayer' actor
        ActorRef player = system.actorOf(Props.create(MusicPlayer.class), "player");

        //send StartMusic Message to actor
        player.tell(new MusicPlayer.StartMusic(), ActorRef.noSender());

        // Send StopMusic Message to actor
        player.tell(new MusicPlayer.StopMusic(), ActorRef.noSender());

        // Two previous sentences are async, the main process might end before they are completed
        Thread.sleep(100);

        //shutdown system
        system.terminate();
    }
}
