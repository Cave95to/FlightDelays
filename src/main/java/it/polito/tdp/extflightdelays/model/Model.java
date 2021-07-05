package it.polito.tdp.extflightdelays.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {

	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	
	private ExtFlightDelaysDAO dao;
	
	private Map <Integer, Airport> idMap;
	
	private Map<Airport, Airport> predecessoreMap;
	
	public Model() {
		
		this.dao = new ExtFlightDelaysDAO();
		this.idMap = new HashMap<>();
		this.dao.loadAllAirports(this.idMap);
	}
	
	public void creaGrafo(int x) {
		
		this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		// aggiungo vertici FILTRATI in base al numero di compagnie x
		Graphs.addAllVertices(this.grafo, this.dao.getVertici(x, this.idMap));
		
		//aggiungo archi
		
		for(RottaAdiacenza r : this.dao.getRotte(idMap)) {
			
			// le rotte sono tra tutti gli aeroporti MA noi ne abbiamo solo alcuni dentro il grafo -> CONTROLLO
			
			if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
				
				DefaultWeightedEdge e = this.grafo.getEdge(r.getA1(), r.getA2());
				
				if( e == null) { // se arco non esiste
					
					Graphs.addEdgeWithVertices(this.grafo, r.getA1(), r.getA2(), r.getN());
					
				}else { // visto che l'insieme degli aeroporti è lo stesso partenza/arrivo potremmo avere gia
						// inserito l'arco! in tal caso dobbiamo sommare i due pesi
					
					double pesoVecchio = this.grafo.getEdgeWeight(e);
					double pesoNuovo = pesoVecchio + r.getN();
					this.grafo.setEdgeWeight(e, pesoNuovo);
					
				}
				
				
			}
		}
		
		//System.out.println("vertici : "+ this.grafo.vertexSet().size()+ " archi: "+this.grafo.edgeSet().size());
		
	}

	public Set<Airport>  getVertici() {
		if(grafo!=null)
			return this.grafo.vertexSet();
		return null;
	}
	
	public List<Airport> trovaPercorso(Airport a1, Airport a2) {
		
		List<Airport> percorso = new LinkedList<>();
		
		BreadthFirstIterator<Airport, DefaultWeightedEdge> bfv = new BreadthFirstIterator<>(this.grafo, a1);
		
		// CON IL TRAVERSAL LISTENER
		this.predecessoreMap = new HashMap<>();
		this.predecessoreMap.put(a1, null);
		
		bfv.addTraversalListener(new TraversalListener<Airport,DefaultWeightedEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				DefaultWeightedEdge arco = e.getEdge();
				// ricordiamo che NON sono la vera sorgente destinazione
				Airport sorgente = grafo.getEdgeSource(arco);
				Airport destinazione = grafo.getEdgeTarget(arco);
				if(predecessoreMap.containsKey(sorgente) && !predecessoreMap.containsKey(destinazione))
					predecessoreMap.put(destinazione, sorgente);
				else if(!predecessoreMap.containsKey(sorgente) && predecessoreMap.containsKey(destinazione))
					predecessoreMap.put(sorgente, destinazione);
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
				// TODO Auto-generated method stub
				
			}});
		
		//visito il grafo
		while(bfv.hasNext()) {
			bfv.next();
		}
				
				
		//ottengo il percorso dall'albero di visita
				
		//se uno dei due aeroporti non è presente nell'albero di visita
		//   -> non c'è nessun percorso
		if(!this.predecessoreMap.containsKey(a1) || !this.predecessoreMap.containsKey(a2)) {
			return null;
		}
				
		//altrimenti, parto dal fondo e "risalgo" l'albero
		percorso.add(a2);
		Airport step = a2;
				
		while (this.predecessoreMap.get(step) != null) {
			step = this.predecessoreMap.get(step);
			percorso.add(0,step);
		}
		
		/* SOLUZIONE VELOCE
		while(bfv.hasNext()) {
			bfv.next();
		}
		
		while(a2!=null) {
			percorso.add(0,a2);
			a2=bfv.getParent(a2);
		}
		*/
		
		return percorso;
		
		
	}

	public int getNVertici() {
		if(grafo!=null)
			return this.grafo.vertexSet().size();
		return 0;
	}

	public int getNArchi() {
		if(grafo!=null)
			return this.grafo.edgeSet().size();
		return 0;
	}
	
	
}
