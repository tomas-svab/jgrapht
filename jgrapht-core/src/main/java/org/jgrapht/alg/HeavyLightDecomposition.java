/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jgrapht.alg;

import java.util.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.UndirectedGraph;

/**
 * Heavy-light decomposition on a tree is preprocessing that takes
 * O( number_of_vertices + number_of_edges) time and allows to
 * compute distances between any two vertices in O(log number_of_vertices) time.
 * 
 * @author Tomas Svab
 */
public class HeavyLightDecomposition< V, E > {
	
	// ---------------------------------------------------------------
	// Graph representation
	// ---------------------------------------------------------------

	private Map< V, Integer > vertexToID;
	private Map< Integer, V > idToVertex;
	
	/** First edge belongs to given vertex (indexed by vertices). */
	private int firstEdge[];

	/** Next edge that belongs to this vertex (indexed by edges). */
	private int next[];

	/** To which vertex is the edge headed (indexed by edges). */
	private int to[];
	
	/** Helpful variable for adding vertices. */
	private int currentVertexID = 0;
	
	// ---------------------------------------------------------------
	// Heavy-light decomposition data
	// ---------------------------------------------------------------

	/** Which vertices have been visited? (indexed by vertices). */
	private boolean isVisited[];

	/**
	 * Which vertices are heavy? (indexed by vertices).
	 * Vertex is considered heavy if it part of the heavy-path.
	 */
	private boolean isHeavy[];
	
	/**
	 * Depth of each vertex in the tree with respect to the selected root
	 * (indexed by vertices).
	 */
	private int depth[];
	
	/** Parent of the vertex (indexed by vertices) */
	private int parent[];
	
	/**
	 * Number of nodes in the subtree that has given vertex as root
	 * (indexed by vertices).
	 */
	private int numNodes[];
	
	/** Vertex that is at the top of the heavy-path. */
	private int chainHead[];
	
	/** Total number of vertices in the tree. */
	private int numVertices;
	
	/**
	 * Total number of edges in the tree (for each edge from input graph,
	 * there is also backward edge).
	 */
	private int numEdges;
	
	private int rootID = 0;
	
	// ---------------------------------------------------------------
	// Public methods
	// ---------------------------------------------------------------
	
	/**
	 * Creates tree with heavy-light decomposition.
	 * For each edge on the input it is also created backward edge.
	 */
	public HeavyLightDecomposition ( DirectedGraph< V, E > graph )
/*
		  int numVertices,
		int rootID,
		int [] edgesStart,
		int [] edgesEnd )
*/
	{
		vertexToID = new HashMap();
		idToVertex = new HashMap();
		
		Set< V > vertices = graph.vertexSet();
		Set< E > edges = graph.edgeSet();
		this.numVertices = vertices.size();
		this.numEdges = 2 * edges.size();
				
		// graph representation:
		this.firstEdge = new int[ numVertices ];
		this.next = new int[ numEdges ];
		this.to = new int[ numEdges ];
		Arrays.fill( firstEdge, -1 );

		// create vertex convertors:
		Iterator< V > iterV = vertices.iterator();
		while ( iterV.hasNext() ) {
			V vertex = iterV.next();
			vertexToID.put( vertex, currentVertexID );
			idToVertex.put( currentVertexID, vertex );
			++currentVertexID;
		}
		
		// create graph:
		int currentEdgeID = 0;
		Iterator< E > iterE = edges.iterator();
		while ( iterE.hasNext() ) {
			E edge = iterE.next();
			V v1 = graph.getEdgeSource( edge );
			V v2 = graph.getEdgeTarget( edge );
			int edgeStart = vertexToID.get( v1 );
			int edgeEnd = vertexToID.get( v2 );

			to[ currentEdgeID ] = edgeEnd;
			next[ currentEdgeID ] = firstEdge[ edgeStart ];
			firstEdge[ edgeStart ] = currentEdgeID;
			++currentEdgeID;

			to[ currentEdgeID ] = edgeStart;
			next[ currentEdgeID ] = firstEdge[ edgeEnd ];
			firstEdge[ edgeEnd ] = currentEdgeID;
			++currentEdgeID;
		}		

		if ( !isTree() ) {
			throw new IllegalArgumentException( "graph is not a tree" );
		}
				
		// heavy-light decomposition data:
		this.isVisited = new boolean[ numVertices ];
		this.isHeavy = new boolean[ numVertices ];
		this.depth = new int[ numVertices ];
		this.parent = new int[ numVertices ];
		this.numNodes = new int[ numVertices ];
		this.chainHead = new int[ numVertices ];
		
		// heavy-light decomposition:
		DFS( rootID, 0 );
		parent[ rootID ] = -1;
		isHeavy[ 1 ] = true;
		computeChainHeads( rootID );
	}
	
	/**
	 * Computes distance between two vertices in logarithmic time.
	 */
	public int distance ( int vertexID1, int vertexID2 )
	{
		int lcaID = lca( vertexID1, vertexID2 );
		int dist = depth[ vertexID1 ] + depth[ vertexID2 ] - 2*depth[ lcaID ];
		return dist;
	}
	
	// ---------------------------------------------------------------
	// Private methods
	// ---------------------------------------------------------------
	
	private boolean isTree ()
	{
		boolean[] vis = new boolean[ numVertices ];
		for ( int i = 0; i < numVertices; ++i )
		{
			if ( vis[ i ] ) {
				continue;
			}
			
			Stack< Integer > stack = new Stack< Integer >();
			stack.add( i );
			vis[ i ] = true;
			while ( !stack.isEmpty() ) {
				int vertexID = stack.peek();
				stack.pop();
				
				int visitedCount = 0;
				for ( int e = firstEdge[ vertexID ]; e >= 0; e = next[ e ] ) {
					int nextVertexID = to[ e ];
					if ( !vis[ nextVertexID ] ) {
						vis[ nextVertexID ] = true;
						stack.add( nextVertexID );
					} else {
						++visitedCount;
					}
				}
				
				if ( visitedCount > 1 ) {
					// we found a cycle:
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Computes most of the heavy-light decomposition.
	 */
	private void DFS ( int vertexID, int currDepth )
	{
		isVisited[ vertexID ] = true;
		depth[ vertexID ] = currDepth;
		numNodes[ vertexID ] = 1;
		chainHead[ vertexID ] = vertexID;

		// compute data by recursion:
		for ( int e = firstEdge[ vertexID ]; e >= 0; e = next[ e ] ) {
			int nextEdgeID = to[ e ];
			if ( !isVisited[ nextEdgeID ] ) {
				DFS( nextEdgeID, currDepth + 1 );
				parent[ nextEdgeID ] = vertexID;
				numNodes[ vertexID ] += numNodes[ nextEdgeID ];
			}
		}
		
		// determine heavy vertex:
		for ( int e = firstEdge[ vertexID ]; e >= 0; e = next[ e ] ) {
			int nextVertexID = to[ e ];
			if (
				parent[ nextVertexID ] == vertexID &&
				2 * numNodes[ nextVertexID ] >= numNodes[ vertexID ]
				) {
				isHeavy[ nextVertexID ] = true;
			}
		}
	}
	
	/**
	 * Second pass of heavy-light decomposition that resolves
	 * chain heads (top vectors of heavy-paths) from information about heavy
	 * vertices.
	 */
	private void computeChainHeads ( int vertexID )
	{
		if ( isHeavy[ vertexID ] ) {
			chainHead[ vertexID ] = chainHead[ parent[ vertexID ] ];
		} else {
			chainHead[ vertexID ] = vertexID;
		}
		
		for ( int e = firstEdge[ vertexID ]; e >= 0; e = next[ e ] ) {
			int nextVertexID = to[ e ];
			if ( vertexID == parent[ nextVertexID ] ) {
				computeChainHeads( nextVertexID );
			}
		}
	}
	
	/**
	 * Returns lowest common ancestor of two vertices.
	 */
	private int lca ( int x, int y )
	{
		while ( chainHead[ x ] != chainHead[ y ] ) {
			if ( depth[ chainHead[ x ] ] < depth[ chainHead[ y ] ] ) {
				y = parent[ chainHead[ y ] ];
			} else {
				x = parent[ chainHead[ x ] ];
			}
		}
	
		if ( depth[ x ] < depth[ y ] ) {
			return x;
		} else {
			return y;
		}
	}

}
