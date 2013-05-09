package org.jgrapht.alg;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;

/**
 * Test is exactly the same as EdmondsKarpMaximumFlowTest
 * (only for different maxflow algorithm).
 * 
 * @author Tomas Svab
 */
public final class DinicMaximumFlowTest extends TestCase {
    //~ Methods ----------------------------------------------------------------

    /**
     * .
     */
    public void testCornerCases ()
    {
        DirectedWeightedMultigraph<Integer, DefaultWeightedEdge> simple =
            new DirectedWeightedMultigraph<Integer, DefaultWeightedEdge>(
                DefaultWeightedEdge.class);
        simple.addVertex(0);
        simple.addVertex(1);
        DefaultWeightedEdge e = simple.addEdge(0, 1);
        try {
            new DinicMaximumFlow<Integer, DefaultWeightedEdge>(null);
            fail();
        } catch (NullPointerException ex) {
        }
        try {
            new DinicMaximumFlow<Integer, DefaultWeightedEdge>(
                simple,
                -0.1);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            simple.setEdgeWeight(e, -1.0);
            new DinicMaximumFlow<Integer, DefaultWeightedEdge>(simple);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            simple.setEdgeWeight(e, 1.0);
            DinicMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new DinicMaximumFlow<Integer, DefaultWeightedEdge>(
                    simple);
            solver.calculateMaximumFlow(0, 1);
            Map<DefaultWeightedEdge, Double> flow = solver.getMaximumFlow();
            flow.put(e, 25.0);
            fail();
        } catch (UnsupportedOperationException ex) {
        }
        try {
            DinicMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new DinicMaximumFlow<Integer, DefaultWeightedEdge>(
                    simple);
            solver.calculateMaximumFlow(2, 0);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            DinicMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new DinicMaximumFlow<Integer, DefaultWeightedEdge>(
                    simple);
            solver.calculateMaximumFlow(1, 2);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            DinicMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new DinicMaximumFlow<Integer, DefaultWeightedEdge>(
                    simple);
            solver.calculateMaximumFlow(0, 0);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            DinicMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new DinicMaximumFlow<Integer, DefaultWeightedEdge>(
                    simple);
            solver.calculateMaximumFlow(null, 0);
            fail();
        } catch (IllegalArgumentException ex) {
        }
        try {
            DinicMaximumFlow<Integer, DefaultWeightedEdge> solver =
                new DinicMaximumFlow<Integer, DefaultWeightedEdge>(
                    simple);
            solver.calculateMaximumFlow(0, null);
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    /**
     * .
     */
    public void testLogic()
    {
        runTest(
            new int[] {},
            new int[] {},
            new double[] {},
            new int[] { 1 },
            new int[] { 4057218 },
            new double[] { 0.0 });
        runTest(
            new int[] { 3, 1, 4, 3, 2, 8, 2, 5, 7 },
            new int[] { 1, 4, 8, 2, 8, 6, 5, 7, 6 },
            new double[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            new int[] { 3 },
            new int[] { 6 },
            new double[] { 2 });
        runTest(
            new int[] { 5, 5, 5, 1, 1, 4, 2, 7, 8, 3 },
            new int[] { 1, 4, 2, 7, 8, 3, 8, 6, 6, 6 },
            new double[] { 7, 8, 573146, 31337, 1, 1, 1, 1, 2391717, 170239 },
            new int[] { 5 },
            new int[] { 6 },
            new double[] { 4.0 });
        runTest(
            new int[] { 1, 1, 2, 2, 3 },
            new int[] { 2, 3, 3, 4, 4 },
            new double[] {
                1000000000.0, 1000000000.0, 1.0, 1000000000.0, 1000000000.0
            },
            new int[] { 1 },
            new int[] { 4 },
            new double[] { 2000000000.0 });
    }

    private void runTest(
        int [] tails,
        int [] heads,
        double [] capacities,
        int [] sources,
        int [] sinks,
        double [] expectedResults)
    {
        assertTrue(tails.length == heads.length);
        assertTrue(tails.length == capacities.length);
        DirectedWeightedMultigraph<Integer, DefaultWeightedEdge> network =
            new DirectedWeightedMultigraph<Integer, DefaultWeightedEdge>(
                DefaultWeightedEdge.class);
        int m = tails.length;
        for (int i = 0; i < m; i++) {
            network.addVertex(tails[i]);
            network.addVertex(heads[i]);
            DefaultWeightedEdge e = network.addEdge(tails[i], heads[i]);
            network.setEdgeWeight(e, capacities[i]);
        }
        assertTrue(sources.length == sinks.length);
        int q = sources.length;
        for (int i = 0; i < q; i++) {
            network.addVertex(sources[i]);
            network.addVertex(sinks[i]);
        }
        DinicMaximumFlow<Integer, DefaultWeightedEdge> solver =
            new DinicMaximumFlow<Integer, DefaultWeightedEdge>(network);
        assertTrue(solver.getCurrentSource() == null);
        assertTrue(solver.getCurrentSink() == null);
        assertTrue(solver.getMaximumFlowValue() == null);
        assertTrue(solver.getMaximumFlow() == null);
        for (int i = 0; i < q; i++) {
            solver.calculateMaximumFlow(sources[i], sinks[i]);
            assertTrue(solver.getCurrentSource().equals(sources[i]));
            assertTrue(solver.getCurrentSink().equals(sinks[i]));
            double flowValue = solver.getMaximumFlowValue();
            Map<DefaultWeightedEdge, Double> flow = solver.getMaximumFlow();
            assertEquals(
                expectedResults[i],
                flowValue,
                DinicMaximumFlow.DEFAULT_EPSILON);
            for (DefaultWeightedEdge e : network.edgeSet()) {
                assertTrue(flow.containsKey(e));
            }
            for (DefaultWeightedEdge e : flow.keySet()) {
                assertTrue(network.containsEdge(e));
                assertTrue(
                    flow.get(e) >= -DinicMaximumFlow.DEFAULT_EPSILON);
                assertTrue(
                    flow.get(e)
                    <= (network.getEdgeWeight(e)
                        + DinicMaximumFlow.DEFAULT_EPSILON));
            }
            for (Integer v : network.vertexSet()) {
                double balance = 0.0;
                for (DefaultWeightedEdge e : network.outgoingEdgesOf(v)) {
                    balance -= flow.get(e);
                }
                for (DefaultWeightedEdge e : network.incomingEdgesOf(v)) {
                    balance += flow.get(e);
                }
                if (v.equals(sources[i])) {
                    assertEquals(
                        -flowValue,
                        balance,
                        DinicMaximumFlow.DEFAULT_EPSILON);
                } else if (v.equals(sinks[i])) {
                    assertEquals(
                        flowValue,
                        balance,
                        DinicMaximumFlow.DEFAULT_EPSILON);
                } else {
                    assertEquals(
                        0.0,
                        balance,
                        DinicMaximumFlow.DEFAULT_EPSILON);
                }
            }
        }
    }
	
}
