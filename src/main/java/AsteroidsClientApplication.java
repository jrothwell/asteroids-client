import alexh.Fluent;
import alexh.weak.Dynamic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

public class AsteroidsClientApplication {
    public static final String serverURI = "ws://127.0.0.1:8065/ship"; // TODO: change me to server URL
    public static final String tag = "XYZ"; // TODO: change me to your three letter tag

    public ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        new AsteroidsClientApplication().run();
    }

    public void run() {
        // create websocket connection
        WebSocketClient client = new AsteroidsClient(URI.create(serverURI + "/" + tag));
        client.run();
    }

    public class AsteroidsClient extends WebSocketClient {

        public AsteroidsClient(URI uri) {
            super(uri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            System.out.println("Connected!");
        }


        /**
         * This method gets called every time we get a fresh message from the server.
         * This happens once every 0.1 seconds.
         * NOTE: you can send messages to the server more often than this, if you want.
         * @param messageFromServer
         */
        @Override
        public void onMessage(String messageFromServer) { // fresh JSON frame from server
            try {
                processFrame(Dynamic.from(objectMapper.readValue(messageFromServer, Map.class)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {

        }

        @Override
        public void onError(Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }

        /**
         * Processes a frame of the game state.
         * @param frame
         */
        public void processFrame(Dynamic frame) {
            // Our ship's own bearing (or 'theta') in radians.
            final Double myBearing = frame.get("theta").as(Double.class);

            /* Get the rocks and the ships, map them to Java objects,
             * then sort them by distance (ascending.) Nearest first. */
            final List<Asteroid> asteroids = frame.get("rocks").children()
                    .map(Asteroid::new)
                    .sorted((a1, a2) -> Double.compare(a1.getDistance(), a2.getDistance()))
                    .collect(toList());
            final List<Ship> ships = frame.get("ships").children()
                    .map(Ship::new)
                    .sorted((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()))
                    .collect(toList());

            final Stream<Target> targets = Stream.concat(asteroids.stream(), ships.stream());

            try {
                if(targets.anyMatch(target -> isPointingAt(myBearing, target)))
                    send(objectMapper.writeValueAsString(singletonMap("fire", true)));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            final Optional<Asteroid> nearestAsteroidMaybe = asteroids.stream()
                    .findFirst(); // take the first object in the stream, and...

            nearestAsteroidMaybe.ifPresent(nearestAsteroid -> {
                // ...if it exists... turn towards it.

                System.out.println("My target is rock id " + nearestAsteroid.getId() +
                        " which is at " + nearestAsteroid.getBearing() +
                        " and is " + nearestAsteroid.getDistance() + "m away");

                final Map<String, Double> instructionMap = new Fluent.HashMap()
                    .append("theta", nearestAsteroid.getBearing()) // turn to the ship...
                    .append("fire", isPointingAt(myBearing, nearestAsteroid));
                    // and if we're pointing at it, SHOOT IT

                try {
                    // send the instruction back over the WebSocket
                    send(objectMapper.writeValueAsString(instructionMap));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        }

        private boolean isPointingAt(Double myBearing, Target target) {
            return Math.abs(myBearing - target.getBearing()) < 0.25;
        }
    }
}
