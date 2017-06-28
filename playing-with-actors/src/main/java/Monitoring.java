import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Monitoring {
    private static final Logger LOGGER = LoggerFactory.getLogger(Monitoring.class);

    private static class Ares extends AbstractActor {
        private final ActorRef athena;

        public Ares(ActorRef athena) {
            this.athena = athena;
            receive(ReceiveBuilder
                    .match(Terminated.class, msg -> {
                        LOGGER.info("Ares received Terminated");
                        context().stop(self());
                    })
                    .build());

        }

        @Override
        public void preStart() throws Exception {
            context().watch(athena);
        }

        @Override
        public void postStop() throws Exception {
            LOGGER.info("Ares postStop");
        }
    }

    private static class Athena extends AbstractActor {
        public Athena() {
            receive(ReceiveBuilder
                    .match(Object.class, msg -> {
                        LOGGER.info(String.format("Athena received %s", msg));
                        context().stop(self());
                    })
                    .build());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create the 'monitoring' actor system
        ActorSystem system = ActorSystem.create("monitoring");

        ActorRef athena = system.actorOf(Props.create(Athena.class), "athena");

        ActorRef ares = system.actorOf(Props.create(Ares.class, athena), "ares");

        athena.tell("Hi", ActorRef.noSender());

        Thread.sleep(500);

        system.terminate();
    }
}
