import akka.actor.AbstractFSMWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSM {

    private static final Logger LOGGER = LoggerFactory.getLogger(FSM.class);

    //FSM State
    enum DbState {
        CONNECTED, DISCONNECTED
    }

    //FSM data
    interface Data {
    }

    static class EmptyData implements Data {

    }

    enum DBoperation {
        CREATE, UPDATE, READ, DELETE
    }

    static class Connect {
    }

    static class Disconnect {
    }

    static class Operation {
        private final DBoperation operation;
        private final User user;

        Operation(DBoperation operation, User user) {
            this.operation = operation;
            this.user = user;
        }
    }

    static class User {
        private final String email;
        private final String username;

        User(String username, String email) {
            this.username = username;
            this.email = email;
        }
    }

    static class UserStorageFSM extends AbstractFSMWithStash<DbState, Data> {


        {
            // 1. define start with
            startWith(DbState.DISCONNECTED, new EmptyData());

            // 2. define states
            when(
                    DbState.DISCONNECTED,
                    matchEvent(
                            Connect.class,
                            Data.class,
                            (connect, anyData) -> {
                                LOGGER.info("UserStorage connected to DB");
                                unstashAll();
                                return goTo(DbState.CONNECTED);
                            }
                    ).anyEvent(
                            (disconnect, anyData) -> {
                                stash();
                                return stay();
                            }
                    )

            );

            when(
                    DbState.CONNECTED,
                    matchEvent(
                            Disconnect.class,
                            Data.class,
                            (disconnect, anyData) -> {
                                LOGGER.info("UserStorage disconnected to DB");
                                return goTo(DbState.DISCONNECTED);
                            }
                    ).event(
                            Operation.class,
                            Data.class,
                            (operation, anyData) -> {
                                LOGGER.info("UserStorage receive {} operation to do in user: {}", operation.operation, operation.user);
                                return stay();
                            }
                    )

            );

            initialize();


        }
    }

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("Hotswap-FSM");

        ActorRef userStorage = system.actorOf(Props.create(UserStorageFSM.class), "userStorage-fsm");

        userStorage.tell(new Connect(), ActorRef.noSender());

        userStorage.tell(new Operation(DBoperation.CREATE, new User("Admin", "admin@domain.com")), ActorRef.noSender());

        userStorage.tell(new Disconnect(), ActorRef.noSender());

        Thread.sleep(100);

        system.terminate();
    }
}
