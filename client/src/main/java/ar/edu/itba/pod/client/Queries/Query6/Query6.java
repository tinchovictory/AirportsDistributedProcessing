package ar.edu.itba.pod.client.Queries.Query6;

import ar.edu.itba.pod.Airport;
import ar.edu.itba.pod.Movement;
import ar.edu.itba.pod.Query6.CitiesMovementsCombinerFactory;
import ar.edu.itba.pod.Query6.CitiesMovementsMapper;
import ar.edu.itba.pod.Query6.CitiesMovementsReducerFactory;
import ar.edu.itba.pod.Query6.CitiesTuple;
import ar.edu.itba.pod.client.Queries.Query;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IList;
import com.hazelcast.mapreduce.Job;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.KeyValueSource;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class Query6 implements Query {
    private final List<Movement> movements;
    private final List<Airport> airports;
    private final HazelcastInstance hz;
    private final int minMovements;

    public Query6(List<Movement> movements, List<Airport> airports, HazelcastInstance hz, int minMovements) {
        this.movements = movements;
        this.airports = airports;
        this.hz = hz;
        this.minMovements = minMovements;
    }

    /*
     * Generate oaci -> city map.
     * Create map reduce job iterates over movements list
     * and gets (oaci -> city map) by constructor,
     * emit citiesTuple -> 1 if cities are diferent,
     * reducer sums movements.
     * */

    @Override
    public void run() throws InterruptedException, ExecutionException {
        /* Add movements list to hazelcast */
        IList<Movement> hzMovement = hz.getList("movements");
        hzMovement.addAll(movements);

        /* Create Query 6 Job */
        JobTracker jobTracker = hz.getJobTracker("Query6");

        /* Key is Oaci, Value is City */
        Map<String, String> oaciCityMap = getOaciCityMap(airports);

        /* Get cities movements
         * Key is citiesTuple, Value is amount of movements */
        Map<CitiesTuple, Integer> citiesMovements = getCitiesMovements(jobTracker, hzMovement, oaciCityMap);

        /* Get query output */
        List<QueryOutputRow> queryOutput = getQueryOutput(citiesMovements);

        /* Print query output */
        printOutput(queryOutput);
    }

    private Map<String, String> getOaciCityMap(List<Airport> airports) {
        Map<String, String> oaciCityMap = new HashMap<>();

        for(Airport airport : airports) {
            if(airport.getOaci() != null && airport.getCity() != null) {
                oaciCityMap.put(airport.getOaci(), airport.getCity());
            }
        }

        return oaciCityMap;
    }

    private Map<CitiesTuple, Integer> getCitiesMovements(JobTracker jobTracker, IList<Movement> hzMovements, Map<String, String> oaciCityMap) throws InterruptedException, ExecutionException {
        /* Key is collection name */
        KeyValueSource<String, Movement> source = KeyValueSource.fromList(hzMovements);
        Job<String, Movement> job = jobTracker.newJob(source);

        /* Run map reduce */
        ICompletableFuture<Map<CitiesTuple, Integer>> future = job
                .mapper(new CitiesMovementsMapper(oaciCityMap))
                .combiner(new CitiesMovementsCombinerFactory())
                .reducer(new CitiesMovementsReducerFactory())
                .submit();

        /* Get map reduce output */
        return future.get();
    }

    private List<QueryOutputRow> getQueryOutput(Map<CitiesTuple, Integer> citiesMovements) {
        List<QueryOutputRow> queryOutput = new ArrayList<>();

        for(CitiesTuple citiesTuple : citiesMovements.keySet()) {
            int movements = citiesMovements.get(citiesTuple);

            if(minMovements <= movements) {
                String cityA = citiesTuple.getCity1();
                String cityB = citiesTuple.getCity2();

                if(citiesTuple.getCity1().compareTo(citiesTuple.getCity2()) > 0) {
                    cityA = citiesTuple.getCity1();
                    cityB = citiesTuple.getCity2();
                }

                queryOutput.add(new QueryOutputRow(cityA, cityB, citiesMovements.get(citiesTuple)));
            }
        }

        Collections.sort(queryOutput);

        return queryOutput;
    }

    private void printOutput(List<QueryOutputRow> queryOutput) {
        System.out.println("Provincia A;Provincia B;Movimeintos");

        for(QueryOutputRow row : queryOutput) {
            System.out.println(row);
        }
    }

    private class QueryOutputRow implements Comparable<QueryOutputRow> {
        private final String cityA;
        private final String cityB;
        private final int movements;

        public QueryOutputRow(String cityA, String cityB, int movements) {
            this.cityA = cityA;
            this.cityB = cityB;
            this.movements = movements;
        }

        @Override
        public String toString() {
            return cityA + ";" + cityB + ";" + movements;
        }

        @Override
        public int compareTo(QueryOutputRow o) {
            return o.movements - movements;
        }
    }
}