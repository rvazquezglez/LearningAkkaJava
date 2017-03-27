import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.Future;

import static akka.pattern.Patterns.ask;
import static scala.compat.java8.FutureConverters.globalExecutionContext;

public class HelloAkkaJava {

    // Define Actor Messages
    private static final class WhoToGreet {
        private final String who;

        WhoToGreet(String who) {
            this.who = who;
        }
    }

    // Define Greeter Actor
    private static class Greeter extends AbstractActor {
        public Greeter() {
            receive(ReceiveBuilder
                    .match(WhoToGreet.class, m -> System.out.println("Hello " + m.who))
                    .build());
        }
    }

    public static void main(String[] args) throws Exception {

        // Create the 'hello akka' actor system
        ActorSystem system = ActorSystem.create("Hello-Akka");

        // Create the 'greeter' actor
        ActorRef greeter = system.actorOf(Props.create(Greeter.class));

        // Send WhoToGreet Message to actor
        // ask is sync, returns a Future
        Future<Object> future = ask(greeter, new WhoToGreet("akka java"), 1000);

        future.onComplete(
                x -> system.terminate(), //shutdown actorsystem
                globalExecutionContext());

        // Send WhoToGreet Message to actor
        // tell is async the most times the system terminates before greeter receives message
        // greeter.tell(new WhoToGreet("akka"), ActorRef.noSender());
        // system.terminate(); //shutdown actor system
    }
}
