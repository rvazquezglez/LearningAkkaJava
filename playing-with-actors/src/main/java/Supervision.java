import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.concurrent.duration.Duration;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;

import java.util.concurrent.TimeUnit;

public class Supervision {
    private static final Logger LOGGER = LoggerFactory.getLogger(Supervision.class);

    private static class Aphrodite extends AbstractActor {
        static class ResumeException extends Exception {
        }

        static class StopException extends Exception {
        }

        static class RestartException extends Exception {
        }

        public Aphrodite() {
            receive(ReceiveBuilder
                    .match(String.class, msg -> {
                        switch (msg) {
                            case "Resume":
                                throw new ResumeException();
                            case "Stop":
                                throw new StopException();
                            case "Restart":
                                throw new RestartException();
                            default:
                                throw new Exception();
                        }
                    })
                    .build());
        }

        @Override
        public void preStart() throws Exception {
            LOGGER.info("Aphrodite preStart hook....");
            super.preStart();
        }

        @Override
        public void preRestart(Throwable reason, Option<Object> message) throws Exception {
            LOGGER.info("Aphrodite preRestart hook....");
            super.preRestart(reason, message);
        }

        @Override
        public void postRestart(Throwable reason) throws Exception {
            LOGGER.info("Aphrodite postRestart hook....");
            super.postRestart(reason);
        }

        @Override
        public void postStop() throws Exception {
            LOGGER.info("Aphrodite postStop hook....");
            super.postStop();
        }
    }

    private static class Hera extends AbstractActor {

        private ActorRef childRef;

        public Hera() {
            receive(ReceiveBuilder
                    .match(
                            Object.class,
                            msg -> {
                                LOGGER.info(String.format("Hera received %s", msg));
                                childRef.tell(msg, ActorRef.noSender());
                                Thread.sleep(100);
                            })
                    .build());
        }

        private SupervisorStrategy.Directive matchExceptionWithDirective(Throwable t) {
            if (t instanceof Aphrodite.ResumeException) {
                return resume();
            }
            if (t instanceof Aphrodite.StopException) {
                return stop();
            }
            if (t instanceof Aphrodite.RestartException) {
                return restart();
            }

            return escalate();
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return new OneForOneStrategy(
                    10,
                    Duration.create(1, TimeUnit.SECONDS),
                    this::matchExceptionWithDirective
            );
        }

        @Override
        public void preStart() throws Exception {
            // Create Aphrodite Actor
            childRef = context().actorOf(Props.create(Aphrodite.class), "Aphrodite");
            Thread.sleep(100);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create the 'supervision' actor system
        ActorSystem system = ActorSystem.create("supervision");

        // Create Hera Actor
        ActorRef hera = system.actorOf(Props.create(Hera.class), "hera");

//        hera.tell("Resume", ActorRef.noSender());
//        Thread.sleep(1000);
//        LOGGER.info("");

//        hera.tell("Restart", ActorRef.noSender());
//        Thread.sleep(1000);
//        LOGGER.info("");

        hera.tell("Stop", ActorRef.noSender());
        Thread.sleep(1000);
        LOGGER.info("");

        system.terminate();
    }
}
