package ar.edu.itba.pod.client;

import ar.edu.itba.pod.Airport;
import ar.edu.itba.pod.Movement;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Client {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        logger.info("tp Client Starting ...");

        // Load params

        // Load csv to list
        CsvParser<Airport> airportCsvParser = new AirportParser();
        List<Airport> airports = airportCsvParser.loadFile(Paths.get("aeropuertos.csv"));

        CsvParser<Movement> movementCsvParser = new MovementParser();
        List<Movement> movements = movementCsvParser.loadFile(Paths.get("movimientos.csv"));

        /* Connect client to hazelcast */
        HazelcastInstance hz = HazelcastClient.newHazelcastClient();

        // Add list to hz
        IList<Airport> hzAirports = hz.getList("airports");
        hzAirports.addAll(airports);

        IList<Movement> hzMovement = hz.getList("movements");
        hzMovement.addAll(movements);

        // Create new map-reduce job

        // Shutdown this Hazelcast client
        //hz.shutdown();
    }
}
