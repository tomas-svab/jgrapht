package org.jgrapht.alg;

import java.util.*;
import org.jgrapht.UndirectedGraph;

/**
 * Simple implementation of Hopcroft Karp matching algorithm.
 * It has the same public methods as original implementation
 * (class HopcroftKarpBipartiteMatching).
 * 
 * @author Tomas Svab
 */
public final class HopcroftKarpSimple< V, E > {

	// ---------------------------------------------------------------
	// GraphInner
	// ---------------------------------------------------------------

	/**
	 * Simple graph representation.
	 * Vertices are index from one because some special algorithms
	 * (for example Hopcroft-Karp) can use the first vertex for special
	 * purposes.
	 */
	class GraphInner
	{
		/**
		 * For each vertex we can have several edges that go from him.
		 * AbstractList< Integer > contains indices of these vertices.
		 */
		public AbstractList< Integer > edges[];
		
		/**
		 * Original edges for each pari(vertex,vertex). It is necessary
		 * to save this in order to return the matching edges in the end.
		 */
		public AbstractList< E > edgesOrig[];

		public GraphInner ( int vertices )
		{
			edges = new ArrayList[ vertices + 1 ];
			edgesOrig = new ArrayList[ vertices + 1 ];
			for ( int i = 0; i < vertices + 1; ++i ) {
				edges[ i ] = new ArrayList();
				edgesOrig[ i ] = new ArrayList();
			}
		}

		public int getNumVertices ()
		{
			return edges.length;
		}

		public void addEdge ( int from, int to, E edge )
		{
			if (
				from <= 0 || from >= edges.length ||
				to <= 0 || to >= edges.length ) {
				throw new ArrayIndexOutOfBoundsException();
			}

			edges[ from ].add( to );
			edgesOrig[ from ].add( edge );
		}
	}
	
	// ---------------------------------------------------------------
	// Data
	// ---------------------------------------------------------------
	
	private static final int INF = 100000000;
	private static final int NULL = 0;
	
	private Map< V, Integer > vertexToID;
	private Map< Integer, V > idToVertex;
		
	/** Representation of the bipartite graph. */
	private GraphInner graph;

	/** Position in the layer that is created in the first pass. */
	private int layer[];
	
	/** Index of the vertex that is matched with current one. */
	private int match[];
	
	/** Indices of vertices that belong to the left partity. */
	private int partitionleft[];
	
	/** Helpful variable for adding vertices. */
	private int currentVertexID = 1;
	
	/** Maximum matching for given undirected graph. */
	private int matching = 0;
	
	// ---------------------------------------------------------------
	// Public methods
	// ---------------------------------------------------------------
	
	public HopcroftKarpSimple ( UndirectedGraph< V, E > graphIn, Set< V > partitionL )
	{
		vertexToID = new HashMap();
		idToVertex = new HashMap();
		
		Set< V > vertices = graphIn.vertexSet();
		graph = new GraphInner( vertices.size() );
			
		match = new int[ graph.getNumVertices() ];
		layer = new int[ graph.getNumVertices() ];

		// create vertex convertors:
		Iterator< V > iterV = vertices.iterator();
		while ( iterV.hasNext() ) {
			V vertex = iterV.next();
			vertexToID.put( vertex, currentVertexID );
			idToVertex.put( currentVertexID, vertex );
			++currentVertexID;
		}
		
		// create left partition:
		int currID = 0;
		partitionleft = new int[ partitionL.size() ];
		iterV = partitionL.iterator();
		while ( iterV.hasNext() ) {
			V vertex = iterV.next();
			partitionleft[ currID ] = vertexToID.get( vertex );
			++currID;
		}
		
		// add edges:
		Set< E > edges = graphIn.edgeSet();
		Iterator< E > iterE = edges.iterator();
		while ( iterE.hasNext() ) {
			E edge = iterE.next();
			V v1 = graphIn.getEdgeSource( edge );
			V v2 = graphIn.getEdgeTarget( edge );
			graph.addEdge( vertexToID.get( v1 ), vertexToID.get( v2 ), edge );
			graph.addEdge( vertexToID.get( v2 ), vertexToID.get( v1 ), edge );
		}
		
		this.maxMatching();
	}

	/**
	 * Computes maximum matching in the supplied graph.
	 */
	public void maxMatching ()
	{
		for ( int i = 0; i < graph.getNumVertices(); ++i ) {
			match[ i ] = NULL;
		}
		
		matching = 0;
		while ( true ) {
			// partition graph into layers:
			if ( !partitionGraph() ) {
				break;
			}
			
			// try to match new vertices:
			for ( int i = 0; i < partitionleft.length; ++i ) {
				int vertexID = partitionleft[ i ];
				if ( match[ vertexID ] == NULL && matchVertex( vertexID ) ) {
					++matching;
				}
			}
		}
	}
	
	/**
	 * Returns the edges which are part of the maximum matching.
	 */
	public Set< E > getMatching ()
	{
		Set< E > m = new HashSet();
		for ( int i = 1; i < match.length; ++i ) {
			if ( match[ i ] < i ) {
				continue;
			}
			
			for ( int j = 0; j < graph.edges[ i ].size(); ++j ) {
				if ( graph.edges[ i ].get( j ) == match[ i ] ) {
					// found edge given:
					m.add( graph.edgesOrig[ i ].get( j ) );
					break;
				}
			}
		}
		
		return Collections.unmodifiableSet( m );
	}

	/**
	 * Returns the number of edges which are part of the maximum matching
	 */
	public int getSize ()
	{
		return matching;
	}
	
	// ---------------------------------------------------------------
	// Private methods
	// ---------------------------------------------------------------
	
	/**
	 * Partitions graph into layers. Vertices at first layer must be unmatched.
	 * All the vertices in the layers belong to some alternating path that
	 * can be later used to improve the result.
	 */
	private boolean partitionGraph ()
	{
		// add all unmatched vertices from left partite to the queue:
		Queue< Integer > q = new ArrayDeque< Integer >();
		for ( int i = 0; i < partitionleft.length; ++i ) {
			int vertexID = partitionleft[ i ];
			if ( match[ vertexID ] == NULL ) {
				layer[ vertexID ] = 0;
				q.add( vertexID );
			}
			else {
				layer[ vertexID ] = INF;
			}
		}
		layer[ NULL ] = INF;

		// create layers by alternating paths:
		while ( !q.isEmpty() ) {
			int vertexID = q.poll();
			if ( vertexID == NULL ) {
				continue;
			}

			int numEdges = graph.edges[ vertexID ].size();
			for ( int i = 0; i < numEdges; ++i ) {
				int nextVertexID = graph.edges[ vertexID ].get( i );
				if ( layer[ match[ nextVertexID ] ] == INF ) {
					layer[ match[ nextVertexID ] ] = layer[ vertexID ] + 1;
					q.add( match[ nextVertexID ] );
				}
			}
		}
		
		boolean possibleToImprove = layer[ NULL ] != INF;
		return possibleToImprove;
	}

	/**
	 * Tries to find alternating path that starts at given vertex and
	 * thus improves maximum matching by one (if we simply swap matched and
	 * unmatched edges on that path).
	 */
	private boolean matchVertex ( int vertexID )
	{
		if ( vertexID != NULL ) {
			int len = graph.edges[ vertexID ].size();
			for ( int i = 0; i < len; ++i ) {
				int nextVertexID = graph.edges[ vertexID ].get( i );
				if ( layer[ match[ nextVertexID ] ] == layer[ vertexID ] + 1 ) {
					if ( matchVertex( match[ nextVertexID ] ) ) {
						match[ nextVertexID ] = vertexID;
						match[ vertexID ] = nextVertexID;
						return true;
					}
				}
			}
			layer[ vertexID ] = INF;
			return false;
		}
		return true;
	}
	
}
