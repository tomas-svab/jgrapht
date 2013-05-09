package org.jgrapht.alg;

import java.util.*;
import org.jgrapht.DirectedGraph;

/**
 * Fast Dinic algorithm for computing maximum flow in network.
 * It has been written so it can be used instead of standard
 * EdmondsKarp network flow algorithm (class EdmondsKarpMaximumFlow).
 * 
 * @author Tomas Svab
 */
public final class DinicMaximumFlow< V, E > {

	// ---------------------------------------------------------------
	// Network
	// ---------------------------------------------------------------

	/**
	 * Data class for flow network that can be used by network-flow
	 * algorithms to compute maximal flow (and other related quantities).
	 * 
	 * Representation of graph is somewhat atypical because network-flow
	 * algorithms often need access to backward edge and this representation
	 * allows to this in constant time (backward edge is right next to the given
	 * forward edge).
	 */
	final class Network< V, E >
	{
		// ---------------------------------------------------------------
		// Convertors
		// ---------------------------------------------------------------
		
		public Map< V, Integer > vertexToID;
		public Map< Integer, V > idToVertex;

		public Map< E, Integer > edgeToID;
		public Map< Integer, E > idToEdge;
		
		// ---------------------------------------------------------------
		// Private network representation
		// ---------------------------------------------------------------
		
		/** First edge belongs to given vertex (addressed by vertices). */
		public int firstEdge[];

		/** Next edge that belongs to this vertex (addressed by edges). */
		public int next[];

		/** To which vertex is the edge headed (addressed by edges). */
		public int to[];

		/** Current flow on every edge. */
		public double flow[];

		/** Capacity of every edge. */
		public double capacity[];

		/** Total number of vertices in the network. */
		public int numVertices;

		/**
		 * Total number of edges in the network (for each edge from input graph,
		 * there is also backward edge).
		 */
		public int numEdges;

		/** Helpful variable for adding vertices. */
		private int currentVertexID = 0;
		
		/** Helpful variable for adding edges. */
		private int currentEdgeID = 0;
		
		// ---------------------------------------------------------------
		// Methods
		// ---------------------------------------------------------------
		
		public Network ( DirectedGraph< V, E > graph )
		{ 
			vertexToID = new HashMap();
			idToVertex = new HashMap();

			edgeToID = new HashMap();
			idToEdge = new HashMap();
		
			// create vertex convertors:
			Set< V > vertices = graph.vertexSet();
			this.numVertices = vertices.size();
			Iterator< V > iterV = vertices.iterator();
			while ( iterV.hasNext() ) {
				V vertex = iterV.next();
				vertexToID.put( vertex, currentVertexID );
				idToVertex.put( currentVertexID, vertex );
				++currentVertexID;
			}

			firstEdge = new int[ numVertices ];
		
			// get edges:
			Set< E > edges = graph.edgeSet();
			this.numEdges = 2 * edges.size();
			next = new int [ numEdges ];
			to = new int[ numEdges ];
			flow = new double[ numEdges ];
			capacity = new double[ numEdges ];
			for ( int i = 0; i < numVertices; ++i ) {
				firstEdge[ i ] = -1;
			}
			
			// add edges:
			Iterator< E > iterE = edges.iterator();
			while ( iterE.hasNext() ) {
				E edge = iterE.next();
				edgeToID.put( edge, currentEdgeID );
				idToEdge.put( currentEdgeID, edge );
				
				V v1 = graph.getEdgeSource( edge );
				V v2 = graph.getEdgeTarget( edge );
				Double c = graph.getEdgeWeight( edge );
				add( vertexToID.get( v1 ), vertexToID.get( v2 ), c );
			}
		}

		/**
		 * Adds new edge with capacity c between vertices u and v.
		 */
		public void add ( int u, int v, double c )
		{
			to[ currentEdgeID ] = v;
			capacity[ currentEdgeID ] = c;
			flow[ currentEdgeID ] = 0;
			next[ currentEdgeID ] = firstEdge[ u ];
			firstEdge[ u ] = currentEdgeID;
			++currentEdgeID;

			to[ currentEdgeID ] = u;
			capacity[ currentEdgeID ] = c;
			flow[ currentEdgeID ] = 0;
			next[ currentEdgeID ] = firstEdge[ v ];
			firstEdge[ v ] = currentEdgeID;
			++currentEdgeID;
		}

		/**
		 * Returns id of the edge that is between the same vertices but it's
		 * direction is opposite.
		 */
		public int getBackwardEdge ( int edgeID )
		{
			return edgeID ^ 1;
		}
	};

	// ---------------------------------------------------------------
	// Data
	// ---------------------------------------------------------------

	private static final long INF = 1000000000000l;
	public static final double DEFAULT_EPSILON = 0.000000001;

	private Network network;

	/**
	 * Holds distances from source to given vertices (also represents
	 * in which layer is the vertex).
	 */
	private int dist[];

	/**
	 * Used as temporary storage for modified array graph.firstEdge.
	 */
	private int currFirstEdge[];
	
	/**
	 * Epsilon value to determine minimal value of flow.
	 */
	private double epsilon;
	
	private Double maximumFlow = null;

	private int currentSource = -1;

	private int currentSink = -1;

	// ---------------------------------------------------------------
	// Public methods
	// ---------------------------------------------------------------
	
	DinicMaximumFlow ( DirectedGraph<V, E> graph )
	{
		this( graph, DEFAULT_EPSILON );
	}
	
	DinicMaximumFlow ( DirectedGraph<V, E> graph, double epsilon )
	{
		// verify input:
		if ( graph == null ) {
			throw new NullPointerException( "network is null" );
		}
		if ( epsilon <= 0) {
			throw new IllegalArgumentException( "invalid epsilon (must be positive)" );
		}
		for ( E e : graph.edgeSet() ) {
			if ( graph.getEdgeWeight( e ) < -epsilon ) {
				throw new IllegalArgumentException(
					"invalid capacity (must be non-negative)");
			}
		}
			
		this.network = new Network( graph );
		dist = new int[ network.numVertices ];
		currFirstEdge = new int[ network.numVertices ];
		this.epsilon = epsilon;
	}
	
	/**
	 * Computes maximum flow for given source and sink.
	 */
	public void calculateMaximumFlow ( V source, V sink )
	{
		if ( network.vertexToID.get( source ) == null ) {
			throw new IllegalArgumentException(
				"invalid source (null or not from this network)" );
		}
		if ( network.vertexToID.get( sink ) == null ) {
			throw new IllegalArgumentException(
				"invalid sink (null or not from this network)" );
		}
		if ( source.equals( sink ) ) {
			throw new IllegalArgumentException( "source is equal to sink" );
		}
		
		Integer sourceID = (Integer)network.vertexToID.get( source );
		Integer sinkID = (Integer)network.vertexToID.get( sink );
		currentSource = sourceID;
		currentSink = sinkID;
		
		maximumFlow = new Double( 0.0 );
		while ( true ) {
			partitionGraph( sourceID, sinkID );
			if ( dist[ sinkID ] == -1 ) {
				// we cannot reach sink:
				break;
			}

			for ( int i = 0; i < network.numVertices; ++i ) {
				currFirstEdge[ i ] = network.firstEdge[ i ];
			}
			
			// add delta-flow while you can:
			double deltaFlow = 0.0;
			do {
				deltaFlow = findDeltaFlow( sourceID, sinkID, INF );
				maximumFlow += deltaFlow;
			}
			while ( deltaFlow > epsilon );
		}
	}

	/**
    * Returns maximum flow value, that was calculated during last <tt>
    * calculateMaximumFlow</tt> call, or <tt>null</tt>, if there was no <tt>
    * calculateMaximumFlow</tt> calls.
    *
    * @return maximum flow value
    */
	public Double getMaximumFlowValue ()
	{
		return maximumFlow;
	}

	/**
    * Returns maximum flow, that was calculated during last <tt>
    * calculateMaximumFlow</tt> call, or <tt>null</tt>, if there was no <tt>
    * calculateMaximumFlow</tt> calls.
    *
    * @return <i>read-only</i> mapping from edges to doubles - flow values
    */
	public Map<E, Double> getMaximumFlow ()
	{
		if ( maximumFlow == null ) {
			return null;
		}
		
		// add every edge (but only those in forward direction!):
		Map< E, Double > f = new HashMap();
		for ( int edgeID = 0; edgeID < network.numEdges; edgeID += 2 ) {
			E edge = (E)network.idToEdge.get( edgeID );
			f.put( edge, Double.valueOf( network.flow[ edgeID ] ) );
		}
		
		return Collections.unmodifiableMap( f );
	}

	/**
    * Returns current source vertex, or <tt>null</tt> if there was no <tt>
    * calculateMaximumFlow</tt> calls.
    *
    * @return current source
    */
	public V getCurrentSource ()
	{
		if ( currentSource == -1 ) {
			return null;
		}
		return (V)network.idToVertex.get( currentSource );
	}

   /**
    * Returns current sink vertex, or <tt>null</tt> if there was no <tt>
    * calculateMaximumFlow</tt> calls.
    *
    * @return current sink
    */
	public V getCurrentSink ()
	{
		if ( currentSink == -1 ) {
			return null;
		}
		return (V)network.idToVertex.get( currentSink );
	}
	
	// ---------------------------------------------------------------
	// Private methods
	// ---------------------------------------------------------------
		
	/** 
	 * Partitions graph into layers by breadth first search.
	 */
	private void partitionGraph ( int sourceID, int sinkID )
	{
		// initialize distance from source:
		for ( int i = 0; i < network.numVertices; ++i ) {
			dist[ i ] = -1;
		}
		dist[ sourceID ] = 0;

		// partition graph by BFS:
		Queue< Integer > q = new LinkedList< Integer >();
		q.add( sourceID );
		while ( !q.isEmpty() ) {
			int u = q.poll();
			for ( int e = network.firstEdge[ u ]; e >= 0; e = network.next[ e ] ) {
				int v = network.to[ e ];
				if ( network.flow[ e ] < network.capacity[ e ] && dist[ v ] == -1 ) {
					dist[ v ] = dist[ u ] + 1;
					if ( v != sinkID ) {
						q.add( v );
					}
				}
			}
		}	
	}

	/**
	 * Finds delta flow by depth first search.
	 */
	private double findDeltaFlow ( int vertexID, int sinkID, double minFlowOnPath )
	{
		if ( vertexID == sinkID ) {
			return minFlowOnPath;
		}

		for ( int e = currFirstEdge[ vertexID ]; e >= 0; e = network.next[ e ], currFirstEdge[ vertexID ] = e ) {
			int nextVertexID = network.to[ e ];
			if ( network.flow[ e ] < network.capacity[ e ] && dist[ nextVertexID ] == dist[ vertexID ] + 1 ) {
				double newMinFlow = Math.min( network.capacity[ e ] - network.flow[ e ], minFlowOnPath );
				double newFlow = findDeltaFlow( nextVertexID, sinkID, newMinFlow ); 
				if ( newFlow > 0 ) {
					network.flow[ e ] += newFlow;
					network.flow[ network.getBackwardEdge( e ) ] -= newFlow;
					return newFlow;
				}
			}
		}
		return 0;
	}
	
}
