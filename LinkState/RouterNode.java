import javax.swing.*;        
import java.util.*;

public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;
  private HashMap<Integer,Integer> costs;
  private HashMap<Integer,Integer> routes; //a que ruter hay que ir para llegar a array[a donde tengo que ir] = a donde quiero ir
  private HashMap<Integer, HashMap<Integer, Integer>> vectors; // el costo general de todos los routers
  private HashMap<Integer, HashMap<Integer, Integer>> neighbours; //guardo mis vecinos
  private int MaxNode;

  //--------------------------------------------------
  public RouterNode(int ID, RouterSimulator sim, HashMap<Integer,Integer> costs) {
    myID = ID;
    this.sim = sim;
    myGUI = new GuiTextArea("  Output window for Router #"+ ID + "  ");

    HashMap<Integer,Integer> aux;
    HashMap<Integer, Integer> auxnei;
    MaxNode = ID;
    this.costs = new HashMap<Integer,Integer>();
    this.routes = new HashMap<Integer,Integer>();
    this.vectors = new HashMap<Integer, HashMap<Integer, Integer>>();
    this.neighbours = new HashMap<Integer, HashMap<Integer, Integer>>();

    for(HashMap.Entry<Integer,Integer> entry : costs.entrySet()){
		this.costs.put(entry.getKey(),entry.getValue());
		this.routes.put(entry.getKey(),entry.getKey());

		if (this.vectors.get(myID) == null)
			aux = new HashMap<Integer,Integer>();
		else
			aux = vectors.get(myID);

		aux.put(entry.getKey(),entry.getValue());
		this.vectors.put(myID,aux);

		if (this.neighbours.get(myID) == null)
			auxnei = new HashMap<Integer, Integer>();
		else
			auxnei = neighbours.get(myID);

		auxnei.put(entry.getKey(),entry.getValue());
		this.neighbours.put(myID,auxnei);

		if (entry.getKey() > MaxNode)
			MaxNode = entry.getKey();
    }
    sendNeighbours(true);
  }

  //--------------------------------------------------
public void recvUpdate(RouterPacket pkt) {
  	HashMap<Integer, Integer> vectorupd = new HashMap<Integer, Integer>(pkt.mincost);
  	// if ((vectorupd.get(-1) == null) || ((vectorupd.get(-1) != null) )) {
  	boolean firstTime = false;
	Boolean different = false;

	// System.out.println("pkt.sourceid " + pkt.sourceid + " pkt.destid " + pkt.destid + " pkt.mincost " + pkt.mincost);

  	if (vectorupd.get(-1) != null){
		firstTime = true;
  		vectorupd.remove(-1);
  		// System.out.println("myID " + myID + " pkt.sourceid " + pkt.sourceid +" vectorupd " + vectorupd);
		if(this.neighbours.get(pkt.sourceid) == null){
			different = true;	
		}
		else{
			HashMap<Integer, Integer> control = new HashMap<Integer, Integer>(this.neighbours.get(pkt.sourceid));
			if (control.size() == vectorupd.size()){
				for(HashMap.Entry<Integer,Integer> entry : control.entrySet()){
					if (entry.getValue() != vectorupd.get(entry.getKey())){
						different = true;
						break;
					}
				}	
			}else{
				different = true;
			}		
		}
  		HashMap<Integer, Integer> neinew = new HashMap<Integer, Integer>(vectorupd);
  		neighbours.put(pkt.sourceid,neinew);
	}

	for(HashMap.Entry<Integer,Integer> entry : vectorupd.entrySet()){
		if (entry.getKey() > MaxNode) 
			MaxNode = entry.getKey();		
  	}	

	if(firstTime == false){
		HashMap<Integer,Integer> local = vectors.get(pkt.sourceid);
		if (local == null){
			different = true;	
		}
		else{
			if (local.size() == vectorupd.size()){
				for(HashMap.Entry<Integer,Integer> entry : local.entrySet()){
					if (entry.getValue() != vectorupd.get(entry.getKey())){
						different = true;
						break;
					}
				}	
			}else{
				different = true;
			}		
		}
	}

	if (different){
		vectors.put(pkt.sourceid,vectorupd);
		HashMap<Integer,Integer> auxnei = this.neighbours.get(myID);
		for(HashMap.Entry<Integer,Integer> entryAuxNei : auxnei.entrySet()){
			if (entryAuxNei.getKey() != pkt.sourceid){
				HashMap<Integer,Integer> auxRP = new HashMap<Integer,Integer>(pkt.mincost);
				RouterPacket routpkt = new RouterPacket(pkt.sourceid, entryAuxNei.getKey(), auxRP);
				sendUpdate(routpkt);	
			}
		}		
	}

	boolean found;
	found = true;
	// System.out.println("found " + found);
	for(HashMap.Entry<Integer,HashMap<Integer,Integer>> entrycostsAux : this.neighbours.entrySet()){
		for(HashMap.Entry<Integer,Integer> entrycostsAuxAux : entrycostsAux.getValue().entrySet()){
			if(this.neighbours.get(entrycostsAuxAux.getKey()) == null)
				found = false;
		}
	}
	// System.out.println("found " + found);
	// System.out.println("this.neighbours " + this.neighbours);
	// System.out.println("this.costs " + this.costs);
	boolean recalc;
	if(found)
		recalc = recalculate();
	else
		recalc = false;

	if (recalc)
		sendNeighbours(false);							
// }
}
  

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    sim.toLayer2(pkt);

  }
  

  //--------------------------------------------------
  public void printDistanceTable() {

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
          b.append(F.format(RouterSimulator.INFINITY, 5));
        myGUI.println(b.toString());
      }
      else{
        for (int i = 0; i <= MaxNode; i++){
          if (iteratesource.get(i) == null) 
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
	

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {

    if (dest > MaxNode) 
    	MaxNode = dest;

    HashMap<Integer,Integer> aux = vectors.get(myID);
    aux.put(dest,newcost);
    vectors.put(myID,aux);
    this.costs.put(dest,newcost);
    HashMap<Integer,Integer> auxnei  = this.neighbours.get(myID);
    auxnei.put(dest,newcost);
    this.neighbours.put(myID,auxnei);
    boolean change;


	boolean found;
	found = true;
	// System.out.println("found " + found);
	for(HashMap.Entry<Integer,HashMap<Integer,Integer>> entrycostsAux : this.neighbours.entrySet()){
		for(HashMap.Entry<Integer,Integer> entrycostsAuxAux : entrycostsAux.getValue().entrySet()){
			if(this.neighbours.get(entrycostsAuxAux.getKey()) == null)
				found = false;
		}
	}

	boolean recalc;
	if(found)
		recalc = recalculate();
	else
		recalc = false;

    sendNeighbours(true); 

    if (recalc)
    	sendNeighbours(false);  
  }


public void sendNeighbours(Boolean firstTime){

  	HashMap<Integer,Integer> aux;
	
	if (firstTime){
		aux = new HashMap<Integer,Integer>(this.neighbours.get(this.myID));
		aux.put(-1,-1);		
	}else{
		aux = new HashMap<Integer,Integer>(this.costs);
	}
	
	HashMap<Integer,Integer> nei;
	nei = this.neighbours.get(myID);
 	for(HashMap.Entry<Integer,Integer> entry : nei.entrySet()){
		RouterPacket routpkt = new RouterPacket(myID, entry.getKey(), aux);
		sendUpdate(routpkt);
	}	
}



 public boolean recalculate(){

 	boolean change = false;

 	HashMap<Integer,Integer> compareCosts = new HashMap<Integer,Integer>(this.costs);
 	List<Integer> Nprima = new ArrayList<Integer>(); 
 	Nprima.add(this.myID); //Nprima = {u}
 	HashMap<Integer,Integer> nei = this.neighbours.get(this.myID);
 	HashMap<Integer,Integer> copycostOne = new HashMap<Integer,Integer>(this.costs);
	for(HashMap.Entry<Integer,Integer> entryCosts : copycostOne.entrySet()){ //for todo nodo v
		if(nei.get(entryCosts.getKey()) != null){ //if v es un vecino de u
			this.costs.put(entryCosts.getKey(),nei.get(entryCosts.getKey())); //then D(v) = c(u,v)
			this.routes.put(entryCosts.getKey(),entryCosts.getKey());
		}
		else{
			this.costs.put(entryCosts.getKey(),RouterSimulator.INFINITY); //else D(v) = INFINITY
			this.routes.remove(entryCosts.getKey());
		}
	}
	// System.out.println("myID " + myID + " this.costs " + this.costs + " Nprima " + Nprima);
	while(Nprima.size() < this.neighbours.size()){ //Bucle
		//encontrar w no perteneciente a Nprima tal que D(w) sea un minimo 
		Integer minimo = -1;
		Integer valueMin = 9999;
		HashMap<Integer,Integer> copycost = new HashMap<Integer,Integer>(this.costs);
		for(HashMap.Entry<Integer,Integer> auxentryCosts : copycost.entrySet()){
			if(valueMin > auxentryCosts.getValue()) {
				Boolean find =  false;
				for (int i = 0; i <= Nprima.size() - 1; i++) {
					if(Nprima.get(i) == auxentryCosts.getKey()){
						find = true;
						break;
					}
				}
				if(! find){
					minimo = auxentryCosts.getKey();
					valueMin = auxentryCosts.getValue();
				}
			}
		}
		// System.out.println("myID " + myID + " minimo " + minimo + " valueMin " + valueMin);
		if(minimo == -1)
			System.out.println("error no encontro valor menor a 9999");
		else{
			Nprima.add(minimo); //sumar w a Nprima
			nei = this.neighbours.get(minimo);
			// System.out.println("myID " + myID + " minimo " + minimo + " valueMin " + valueMin + " nei " + nei + " this.costs " + this.costs);
			//actualizar D(v) para cada vecino v de w, que no pertenezca a Nprima 
			for(HashMap.Entry<Integer,Integer> entryNei : nei.entrySet()){
				Boolean find =  false;
				for (int i = 0; i <= Nprima.size() - 1; i++) {
					if(Nprima.get(i) == entryNei.getKey()){
						find = true;
						break;
					}
				}
				if(! find){
					//D(v) = min(D(v), D(w) + c(w,v) )
					if((this.costs.get(entryNei.getKey()) == null) || (this.costs.get(entryNei.getKey()) > this.costs.get(minimo) + entryNei.getValue())) {
						this.costs.put(entryNei.getKey(),this.costs.get(minimo) + entryNei.getValue());
						Integer auxRo = this.routes.get(minimo);
						this.routes.put(entryNei.getKey(),auxRo);
					}

				}
				//el nuevo coste a v es o bien el antiguo coste a v o el coste de la ruta de coste
				//minimo a w mas el coste desde w a v
			}
		}
	}//until N = Nprima
	HashMap<Integer, Integer> auxCosts = new HashMap<Integer, Integer>(this.costs);
	this.vectors.put(this.myID,auxCosts);

	if(this.costs.size() == compareCosts.size()){
		for(HashMap.Entry<Integer,Integer> entryCompare : compareCosts.entrySet()){
			if(entryCompare.getValue() != this.costs.get(entryCompare.getKey())){
				change = true;
				break;
			}
		}
	}else
		change = true;


	return change;
 }
}