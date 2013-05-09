package org.jgrapht.alg;

import junit.framework.TestCase;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.alg.HeavyLightDecomposition;
import org.jgrapht.graph.DefaultEdge;

/**
 *
 * @author Tomas Svab
 */
public class HeavyLightDecompositionTest extends TestCase {
	
	public void test1 ()
	{
		// create graph:
		DirectedWeightedMultigraph<Integer, DefaultEdge> graph =
            new DirectedWeightedMultigraph<Integer, DefaultEdge>(
                DefaultWeightedEdge.class);
		for ( int i = 0; i < 13; ++i ) {
			graph.addVertex( i );
		}	
		graph.addEdge( 0, 1 );
		graph.addEdge( 1, 4 );
		graph.addEdge( 4, 5 );
		graph.addEdge( 0, 2 );
		graph.addEdge( 2, 6 );
		graph.addEdge( 6, 8 );
		graph.addEdge( 6, 9 );
		graph.addEdge( 2, 7 );
		graph.addEdge( 0, 3 );
		graph.addEdge( 3, 10 );
		graph.addEdge( 10, 11 );
		graph.addEdge( 11, 12 );
		
		HeavyLightDecomposition< Integer, DefaultWeightedEdge > hl =
			new HeavyLightDecomposition( graph );
		
		// check all queries:
		int d = 0;
		d = hl.distance( 0, 1 );
		assertEquals( 1, d, 0 );
		d = hl.distance( 0, 4 );
		assertEquals( 2, d, 0 );
		d = hl.distance( 0, 5 );
		assertEquals( 3, d, 0 );
		d = hl.distance( 0, 9 );
		assertEquals( 3, d, 0 );
		d = hl.distance( 0, 12 );
		assertEquals( 4, d, 0 );
		d = hl.distance( 5, 12 );
		assertEquals( 7, d, 0 );
		d = hl.distance( 7, 11 );
		assertEquals( 5, d, 0 );
		d = hl.distance( 2, 9 );
		assertEquals( 2, d, 0 );
		d = hl.distance( 1, 3 );
		assertEquals( 2, d, 0 );
		d = hl.distance( 4, 8 );
		assertEquals( 5, d, 0 );
	}
	
}
