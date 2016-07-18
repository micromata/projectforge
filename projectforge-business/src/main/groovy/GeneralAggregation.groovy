class GeneralAggregation {
	
	public static void main(def args){
	
		// Aequivalent, wenn ...
		Closure equiv = {l1, l2 -> l1[0] % 5 == l2[0] % 5}
	
		// Arithmisches Mittel der 4. und Standardabweichung der 5. Spalte (Indizierung der Spalten beginnt mit 0)
		Closure init = {l -> 	l[3]=[1,l[3]]
								l[4]=[1,l[4]**2]
								l}
		
		Closure agg = {l,a -> 	a[3]=[a[3][0]+=1,a[3][1]+=l[3]]
								a[4]=[a[4][0]+=1,a[4][1]+=l[4]**2]}
		
		Closure fin = {a ->		a[3] = a[3][1]/a[3][0]
								a[4] = (a[4][1]/a[4][0])**0.5}
		
		Closure iaf = {i -> def aggfun = [init,agg,fin]
							return [i]}
		
		// Beispieldaten: Beachte, dass dritter Eintrag konstant auf aequivalenzklasse sein muss
		List list = (0..100).collect{n -> [n,2*n,n % 2,n,n]}

		aggregate2(list,equiv,init,agg,fin).each{println it}
		
		def lol = (1..10).collect{n -> (1..3).collect{m -> n*m}}
		//println lolToLoo(lol,["first","second","third"]).each{o -> println o.first+" "+o.second+" "+o.third}
		
		
		// Alternativ mit HashMap
		List mlist = (0..100).collect{n -> 	def m = new HashMap()
										 	m['first'] = n
										 	m['second'] = 2*n
										 	m['third'] = n % 2
										 	m['fourth'] = n
										 	m['fifth'] = n
										 	return m}
		
		Closure equiv2 = {l1, l2 -> l1.first % 5 == l2.first % 5}

		Closure init2 = {l -> 	l.fourth=[1,l.fourth]
								l.fifth=[1,l.fifth**2]
								l}

		Closure agg2 = {l,a -> 	a.fourth=[a.fourth[0]+=1,a.fourth[1]+=l.fourth]
								a.fifth=[a.fifth[0]+=1,a.fifth[1]+=l.fifth**2]}
		
		Closure fin2 = {a ->	a.fourth = a.fourth[1]/a.fourth[0]
								a.fifth = (a.fifth[1]/a.fifth[0])**0.5}
		
		aggregate(mlist,equiv2,init2,agg2,fin2).each{println it}
		
	}
	
	/*
	 * lol ist Liste von Listen, die Tabelle repraesentiert (Jedes Element ist Liste, die eine Zeile der Tabelle beschreibt)
	 * equiv ist Aequivalenzrelation auf n Spalten der Liste
	 * init ist Closure, das Initialisierung der Aggregationsfunktion beschreibt
	 * agg ist Closure, das Aggregation der Aggregationsfunktion beschreibt
	 * finalize ist Closure, das Finalisierung der Aggregationsfunktion beschreibt
	 */
	public static List aggregate2(List<List> lol, Closure equiv, Closure init = {x -> x}, Closure agg, Closure finalize = {x -> x}) {
		def resultTable = [] // Tabelle mit aggregierten Werten
		def rep = [] // Zeile in resultTable mit Repraesentant der aequivalenzklasse und Aggregat

		lol.each{ row ->
			if((rep = resultTable.find{r -> equiv(row,r)}) != null) agg(row,rep)
			else resultTable << init(row)
		}
		
		resultTable.each{finalize(it)}
	}
	
  /*
   * Closure for generating equivalence relation for aggregation
   */
	public static Closure createEquiv(List<String> fields) {
    def getter = HashMap.class.getMethod("get",Object)
		return {x,y ->  fields.inject(true) {cumPropValue, field -> cumPropValue && getter.invoke(x,field) == getter.invoke(y,field)}}
	}
	
	/*
	 * lom ist Liste von Maps
	 */
	
	public static List aggregate(List<HashMap> lom, Closure equiv, Closure init = {x -> x}, Closure agg, Closure finalize = {x -> x}) {
		def resultTable = [] // Tabelle mit aggregierten Werten
		def rep = [] // Zeile in resultTable mit Repraesentant der Aequivalenzklasse und Aggregat
	
		lom.each{row ->
			if((rep = resultTable.find{r -> equiv(row,r)}) != null) agg(row,rep)
			else resultTable << init(row)
		}
		
		resultTable.each{finalize(it)}
	}
	
	// Bildet Objekt und Liste von Attributen (Strings) auf Liste der Werte der Attribute ab
	public static List looToLol(List loo, List attlist) {
		Closure o2l = {obj,attList -> attList.collect{att -> obj."${att}"}}
		loo.collect{obj -> o2l(obj,attList)}
	}
	
	public static List lolToLoo(List lol, List attnames) {
		return lol.collect{list -> def x = new Expando()
							(0..(attnames.size-1)).each{n -> x."${attnames[n]}" = list[n]}
							return x}
	}
	
}