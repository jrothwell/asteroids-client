import alexh.Fluent;
import alexh.weak.Dynamic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        public Logger logger = LogManager.getLogger(AsteroidsClient.class);


        public AsteroidsClient(URI uri) {
            super(uri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            System.out.println("Connected!");
        }


        /**
         * This method gets called every time we get a fresh message from the server.
         * This happens once every second.
         * NOTE: you can send messages to the server more often than this, if you want.
         * @param messageFromServer
         */
        @Override
        public void onMessage(String messageFromServer) { // fresh JSON frame from server
            try {
                logger.trace("Message received from server");
                processFrame(Dynamic.from(objectMapper.readValue(messageFromServer, Map.class)));
            } catch (IOException e) {
                logger.error("Failed deserialising message from server", e);
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            logger.info("Connection closed: " + i + "," + s + "," + b);
        }

        @Override
        public void onError(Exception e) {
            logger.error("Error received from server", e);
        }

        /**
         * Processes a frame of the game state.
         * @param frame
         */
        public void processFrame(Dynamic frame) {
            // TODO: change this method to make me faster, cleverer, etc.

            // Our ship's own bearing (or 'theta') in radians.
            final Double myBearing = frame.get("theta").as(Double.class);
            logger.trace("My bearing is " + myBearing + " radians");

            /* Get the rocks and the ships, map them to Java objects,
             * then sort them by distance (ascending.) Nearest first. */
            final List<Asteroid> asteroids = frame.get("rocks").children()
                    .map(Asteroid::new)
                    .collect(toList());
            final List<Ship> ships = frame.get("ships").children()
                    .map(Ship::new)
                    .collect(toList());

            final List<Target> targets = new ArrayList(asteroids);
            targets.addAll(ships);

            Fluent.HashMap instructionMap = new Fluent.HashMap();

            // If I can see a target, add a 'fire' instruction to the instruction map.
            if(targets.stream().anyMatch(target -> isPointingAt(myBearing, target))) {
                logger.info("I see a target... FIRING!");
                instructionMap.append("fire", true);
            }

            // Find a target - any target - and turn towards it.
            targets.stream()
                    .findFirst()
                    .map(Target::getBearing)
                    .ifPresent(newBearing -> {
                        logger.info("Turning to new target at " + newBearing + " radians");
                        instructionMap.append("theta", newBearing);
                    });

            try {
                logger.trace("Sending instruction map to server");
                send(objectMapper.writeValueAsString(instructionMap));
            } catch (JsonProcessingException e) {
                logger.error("Failed serialising message to server", e);
            }
        }

        private boolean isPointingAt(Double myBearing, Target target) {
            return Math.abs(myBearing - target.getBearing()) < 0.25;
        }
    }
}
