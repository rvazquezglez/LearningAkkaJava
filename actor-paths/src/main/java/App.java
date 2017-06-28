import akka.actor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException {
        /*
        ActorSystem system = ActorSystem.create("Actor-Paths");

        ActorRef counter1 = system.actorOf(Props.create(Counter.class), "counter");

        LOGGER.info("Actor reference for counter 1 : {}", counter1);

        ActorSelection counterSelection1 = system.actorSelection("counter");

        LOGGER.info("Actor selection for counter 1 : {}", counterSelection1);

        counter1.tell(PoisonPill.getInstance(), ActorRef.noSender());

        Thread.sleep(100);

        ActorRef counter2 = system.actorOf(Props.create(Counter.class), "counter");

        LOGGER.info("Actor reference for counter 2 : {}", counter1);

        ActorSelection counterSelection2 = system.actorSelection("counter");

        LOGGER.info("Actor selection for counter 2 : {}", counterSelection1);
        */

//        ---

        ActorSystem system = ActorSystem.create("Watsh-actor-selection");

        ActorRef counter = system.actorOf(Props.create(Counter.class), "counter");

        ActorRef watcher = system.actorOf(Props.create(Watcher.class), "watcher");

        Thread.sleep(1000);

        system.terminate();
    }
}
