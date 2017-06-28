import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

public class Counter extends AbstractActor {
    private static class Inc {
        private final int num;

        private Inc(int num) {
            this.num = num;
        }
    }

    private static class Dec {
        private final int num;

        private Dec(int num) {
            this.num = num;
        }
    }

    private int count = 0;

    public Counter() {
        receive(ReceiveBuilder
                .match(Inc.class, msg -> count += msg.num)
                .match(Dec.class, msg -> count -= msg.num)
                .build()
        );
    }
}
