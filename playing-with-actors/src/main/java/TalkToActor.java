import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static akka.pattern.Patterns.ask;
import static scala.compat.java8.FutureConverters.globalExecutionContext;

public class TalkToActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TalkToActor.class);

    private static class User {
        private final String userName;
        private final String email;

        User(String userName, String email) {
            this.userName = userName;
            this.email = email;
        }

        @Override
        public String toString() {
            return "User{" +
                    "userName='" + userName + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return Objects.equals(userName, user.userName) &&
                    Objects.equals(email, user.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userName, email);
        }
    }

    private static class Storage extends AbstractActor {
        List<User> users = new ArrayList<>();

        Storage() {
            receive(ReceiveBuilder
                .match(AddUser.class,
                        addUser -> addUser.user != null,
                        addUserMsg -> {
                            LOGGER.info("Storage: {} added", addUserMsg.user);
                            users.add(addUserMsg.user);
                        })
                .matchAny(msg -> LOGGER.info("Storage: Unknown message received {}", msg))
                .build());
        }

        private interface StorageMsg {
        }

        //StorageMessage
        private static class AddUser implements StorageMsg {
            private final User user;

            AddUser(User user) {
                this.user = user;
            }

            @Override
            public String toString() {
                return "AddUser{" +
                        "user=" + user +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                AddUser addUser = (AddUser) o;
                return Objects.equals(user, addUser.user);
            }

            @Override
            public int hashCode() {
                return Objects.hash(user);
            }
        }
    }

    private static class Checker extends AbstractActor {
        private final List<User> blackList = Collections.singletonList(new User("Adam", "adam@mail.com"));

        public Checker() {
            receive(ReceiveBuilder
                .match(
                    CheckUser.class,
                    checkUserMsg -> checkUserMsg.user != null
                            && blackList.contains(checkUserMsg.user),
                    checkUserMsg -> {
                        LOGGER.info("Checker: {} is in the blacklist", checkUserMsg.user);
                        sender().tell(new BlackUser(checkUserMsg.user), self());
                    }
                )
                .match(
                    CheckUser.class,
                    checkUser -> checkUser.user != null,
                    checkUserMsg -> {
                        LOGGER.info("Checker: {} is not in the blacklist", checkUserMsg.user);
                        sender().tell(new WhiteUser(checkUserMsg.user), self());
                    }
                )
                .matchAny(msg -> LOGGER.info("Checker: Unknown message received {}", msg))
                .build()
            );
        }

        private interface CheckerMsg {
        }

        // Checker Messages
        private static class CheckUser implements CheckerMsg {
            private final User user;

            CheckUser(User user) {
                this.user = user;
            }

            @Override
            public String toString() {
                return "CheckUser{" +
                        "user=" + user +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CheckUser checkUser = (CheckUser) o;
                return Objects.equals(user, checkUser.user);
            }

            @Override
            public int hashCode() {
                return Objects.hash(user);
            }
        }

        // Checker Responses
        private interface CheckerResponse {
        }

        private static class BlackUser implements CheckerResponse {
            private final User user;

            BlackUser(User user) {
                this.user = user;
            }

            @Override
            public String toString() {
                return "BlackUser{" +
                        "user=" + user +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                BlackUser blackUser = (BlackUser) o;
                return Objects.equals(user, blackUser.user);
            }

            @Override
            public int hashCode() {
                return Objects.hash(user);
            }
        }

        private static class WhiteUser implements CheckerResponse {
            private final User user;

            WhiteUser(User user) {
                this.user = user;
            }

            @Override
            public String toString() {
                return "WhiteUser{" +
                        "user=" + user +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                WhiteUser whiteUser = (WhiteUser) o;
                return Objects.equals(user, whiteUser.user);
            }

            @Override
            public int hashCode() {
                return Objects.hash(user);
            }
        }
    }

    private static class Recorder extends AbstractActor {
        private final ActorRef checker;
        private final ActorRef storage;

        public Recorder(ActorRef checker, ActorRef storage) {
            this.checker = checker;
            this.storage = storage;

            receive(ReceiveBuilder
                    .match(NewUser.class, processNewUserMsg())
                    .build()

            );
        }

        private FI.UnitApply<NewUser> processNewUserMsg() {
            return newUserMsg ->
                    ask(this.checker,
                        new Checker.CheckUser(newUserMsg.user),
                        1000)
                    .map(checkerResponse -> {
                        if (checkerResponse instanceof Checker.WhiteUser) { // Poor man's pattern matching
                            LOGGER.info("Recorder: Received white user {}, saving", checkerResponse);
                            this.storage.tell(new Storage.AddUser(((Checker.WhiteUser) checkerResponse).user), self());
                        } else if (checkerResponse instanceof Checker.BlackUser) {
                            LOGGER.info("Recorder: Received  user in the black list {}, ignoring", checkerResponse);
                        } else {
                            LOGGER.info("Recorder: Received unknown checker response {}", checkerResponse);
                        }
                        return null;
                    },
                    globalExecutionContext());
        }

        private interface RecorderMsg {
        }

        // Recorder Messages
        private static class NewUser implements RecorderMsg {
            private final User user;

            NewUser(User user) {
                this.user = user;
            }

            @Override
            public String toString() {
                return "NewUser{" +
                        "user=" + user +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                NewUser newUser = (NewUser) o;
                return Objects.equals(user, newUser.user);
            }

            @Override
            public int hashCode() {
                return Objects.hash(user);
            }
        }


        private static Props props(ActorRef checker, ActorRef storage) {
            return Props.create(Recorder.class, checker, storage);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // Create the 'talk-to-actor' actor system
        ActorSystem system = ActorSystem.create("talk-to-actor");

        // Create the 'checker' actor
        ActorRef checker = system.actorOf(Props.create(Checker.class), "checker");

        // Create the 'storage' actor
        ActorRef storage = system.actorOf(Props.create(Storage.class), "storage");

        // Create the 'recorder' actor
        ActorRef recorder = system.actorOf(Recorder.props(checker, storage), "recorder");

        //send NewUser Message to Recorder
        recorder.tell(new Recorder.NewUser(new User("Jon", "jon@packt.com")), ActorRef.noSender());

        //send a NewUser Message with a user in black list to Recorder
        recorder.tell(new Recorder.NewUser(new User("Adam", "adam@mail.com")), ActorRef.noSender());

        Thread.sleep(100);

        system.terminate();
    }
}
