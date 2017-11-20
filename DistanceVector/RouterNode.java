import javax.swing.*;        
import java.util.*;


public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;
  private HashMap<Integer,Integer> costs;
  private HashMap<Integer,Integer> routes; //a que ruter hay que ir para llegar a array[a donde tengo que ir] = a donde quiero ir
  private HashMap<Integer, HashMap<Integer, Integer>> vectors; // el costo general de todos los routers
  private boolean poisonedReverse = true;
  private HashMap<Integer,Integer> neighbours; //guardo mis vecinos
  private int MaxNode;


public RouterNode(int ID, RouterSimulator sim, HashMap<Integer,Integer> costs) {

	MaxNode = ID;
	myID = ID;

	this.sim = sim;
	myGUI = new GuiTextArea("  Output window for Router #"+ ID + "  ");
	this.costs = new HashMap<Integer,Integer>();
	this.costs.putAll(costs);


	HashMap<Integer, Integer> aux;
	vectors = new HashMap<Integer, HashMap<Integer, Integer>>();
	routes = new HashMap<Integer, Integer>();
	neighbours = new HashMap<Integer, Integer>(costs);

	for(HashMap.Entry<Integer,Integer> entry : costs.entrySet()){
	    
	    if (vectors.get(myID) == null)
	        aux = new HashMap<Integer,Integer>();
	    else
	        aux = vectors.get(myID);
	    if (entry.getKey()> MaxNode)
	        MaxNode = entry.getKey();
	    aux.put(entry.getKey(),entry.getValue());
	    vectors.put(myID,aux);
	    
	    routes.put(entry.getKey(),entry.getKey());
	}
	sendNeighbours();
}

public void recvUpdate(RouterPacket pkt) {
  HashMap<Integer, Integer> vectorupd = new HashMap<Integer, Integer>(pkt.mincost);
	vectors.put(pkt.sourceid,vectorupd);	
  for(HashMap.Entry<Integer,Integer> entry : vectorupd.entrySet()){
    if (entry.getKey()> MaxNode) 
      MaxNode = entry.getKey();
  }
  boolean change;
  change = recalculate();
  if (change == true)
    sendNeighbours();
}

private void sendUpdate(RouterPacket pkt) {
	sim.toLayer2(pkt);
}

public void printDistanceTable() {
// System.out.println(myID + " " + vectors);
	myGUI.println("Current table for " + myID +
	        "  at time " + sim.getClocktime());

	StringBuilder b;

	myGUI.println("Distancetable:");
	b = new StringBuilder(F.format("dst", 7) + " | ");
	for (int i = 0; i <= MaxNode; i++)
	  b.append(F.format(i, 5));
	myGUI.println(b.toString());

	for (int i = 0; i < b.length(); i++)
	  myGUI.print("-");
	myGUI.println();

	for (int source = 0; source <= MaxNode; source++)
	{
	/*      if (source == myID)
	    continue;*/
	  HashMap<Integer, Integer> iteratesource  = new HashMap<Integer, Integer> ();
	  iteratesource = vectors.get(source);
	  b = new StringBuilder("nbr" + F.format(source, 3) + " | ");
	  if (iteratesource == null) {
	    for (int i = 0; i <= MaxNode; i++)
	   		if (i==source)
	      		b.append(F.format(0, 5));
	      	else				
	      		b.append(F.format(RouterSimulator.INFINITY, 5));
	    myGUI.println(b.toString());
	  }
	  else{
	    for (int i = 0; i <= MaxNode; i++){
	      if (iteratesource.get(i) == null)
	      	if (i==source)
	      		b.append(F.format(0, 5));	
	      	else 
		        b.append(F.format(RouterSimulator.INFINITY, 5));
	      else
	        b.append(F.format(iteratesource.get(i), 5));
	      }
	    myGUI.println(b.toString());
	  }
	}

	myGUI.println("\nOur distance vector and routes:");

	b = new StringBuilder(F.format("dst", 7) + " | ");
	for (int i = 0; i <= MaxNode; i++)
	  b.append(F.format(i, 5));
	myGUI.println(b.toString());

	for (int i = 0; i < b.length(); i++)
	  myGUI.print("-");
	myGUI.println();

	b = new StringBuilder(F.format("cost", 7) + " | ");
	HashMap<Integer, Integer> iteratecost = new HashMap<Integer, Integer>();
	iteratecost = this.costs;
	for (int i = 0; i <= MaxNode; i++){
	  if (iteratecost.get(i) == null)
	  	if (i == myID)
		    b.append(F.format(0, 5));
		else
			b.append(F.format(RouterSimulator.INFINITY, 5));
	  else
	    b.append(F.format(iteratecost.get(i), 5));
	}
	myGUI.println(b.toString());

	b = new StringBuilder(F.format("route", 7) + " | ");
	HashMap<Integer, Integer> iterateroutes = new HashMap<Integer, Integer>();
	iterateroutes = this.routes;

	for (int i = 0; i <= MaxNode; i++)
	{
	  if (iterateroutes.get(i) == null)
	    b.append(F.format("-", 5));
	  else
	    b.append(F.format(iterateroutes.get(i), 5));
	}
	myGUI.println(b.toString());
	myGUI.println();
}

public void updateLinkCost(int dest, int newcost) {

	if (dest > MaxNode) 
		MaxNode = dest;

	HashMap<Integer,Integer> aux = vectors.get(myID);
	aux.put(dest,newcost);
	vectors.put(myID,aux);
	this.costs.put(dest,newcost);
	this.neighbours.put(dest,newcost);
	boolean change;

	change = recalculate();

	// if (change == true)
		sendNeighbours();	
}


public void sendNeighbours(){

  	HashMap<Integer,Integer> aux = new HashMap<Integer,Integer>();
  	for(HashMap.Entry<Integer,Integer> entry : neighbours.entrySet()){
  		for(HashMap.Entry<Integer,Integer> routesiter : routes.entrySet()){
  			//Poisoned reverse distance vector
  			if (this.poisonedReverse && entry.getKey() == routesiter.getValue() && entry.getKey() != routesiter.getKey()){
  				Integer keyRS = routesiter.getKey();
			    aux.put(keyRS,RouterSimulator.INFINITY);
  			}else{
			    Integer keyRS = routesiter.getKey();
			    aux.put(keyRS,costs.get(keyRS));
        	}  
  		}
  		RouterPacket  routpkt = new RouterPacket(myID, entry.getKey(), aux);
  		sendUpdate(routpkt);
      	aux.clear();
  	} 
  }

public boolean recalculate(){

    boolean change = false;
    HashMap<Integer,Integer> aux;
    HashMap<Integer,Integer> valuesRo;

    for(HashMap.Entry<Integer,Integer> entryRoutes : this.routes.entrySet()){  
      if (entryRoutes.getValue() != entryRoutes.getKey()){
      	valuesRo = this.vectors.get(entryRoutes.getValue());
      	if (valuesRo.get(entryRoutes.getKey()) != null){
	      	if ((valuesRo.get(entryRoutes.getKey()) + this.costs.get(entryRoutes.getValue())) != this.costs.get(entryRoutes.getKey())) {
	      		change = true;
	    			HashMap<Integer,Integer> auxvect = this.vectors.get(myID);
	    			auxvect.put(entryRoutes.getKey(),valuesRo.get(entryRoutes.getKey()) + this.costs.get(entryRoutes.getValue()));
	    			this.vectors.put(myID,auxvect);
	    			this.costs.put(entryRoutes.getKey(),valuesRo.get(entryRoutes.getKey()) + this.costs.get(entryRoutes.getValue()));
	        }
    	}
    	}		
    }
    for(HashMap.Entry<Integer,Integer> entryNeig : this.neighbours.entrySet()){  

      if (this.costs.get(entryNeig.getKey()) > entryNeig.getValue()){
        change = true;
        HashMap<Integer,Integer> auxvect = this.vectors.get(myID);
        auxvect.put(entryNeig.getKey(),entryNeig.getValue());
        this.vectors.put(myID,auxvect);
        this.costs.put(entryNeig.getKey(),entryNeig.getValue());
        routes.put(entryNeig.getKey(),entryNeig.getKey());
      }
    }
    for(HashMap.Entry<Integer,Integer> entry : this.neighbours.entrySet()){         
      Integer nei = entry.getKey(); //id neighbours
      Integer costNei = this.costs.get(nei); //cost to go to the neighbour
      aux = this.vectors.get(nei);
      
      if (!(aux == null)){
        for(HashMap.Entry<Integer,Integer> entryAux : aux.entrySet()){
          Integer dest = entryAux.getKey();
          Integer costDest = entryAux.getValue();
          if (dest != myID){
	          if (!(this.costs.get(dest) == null)) {
	            if(costDest + costNei < this.costs.get(dest)){
	              change = true;
	              HashMap<Integer,Integer> auxvect = this.vectors.get(myID);
	              auxvect.put(dest,entryAux.getValue() + costNei);
	              this.vectors.put(myID,auxvect);
	              this.costs.put(dest,entryAux.getValue() + costNei);
	              Integer auxroute = routes.get(nei);
	               if (auxroute != null)
	               	routes.put(dest,auxroute);
	               else 
	              	routes.put(dest,nei);		

	            }
	          }
	          else{
              change = true;
              HashMap<Integer,Integer> auxvect = this.vectors.get(myID);
              auxvect.put(dest,entryAux.getValue() + costNei);
              this.vectors.put(myID,auxvect);
              this.costs.put(dest,entryAux.getValue() + costNei);
              Integer auxroute = routes.get(nei);
	           if (auxroute != null)
	           	routes.put(dest,auxroute);
	           else 
	          	routes.put(dest,nei);	
	          }
          }
        }
      }
    }
    return change;   
    }  
}