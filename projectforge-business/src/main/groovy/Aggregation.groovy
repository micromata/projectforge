import java.lang.reflect.*
import java.lang.Integer
import java.text.SimpleDateFormat


// an Kai übergeben um in Projectforge bereit gestellt zu werden

public class Aggregation{
	
	static Aggregator sum = new Aggregator("Sum",{x -> x},{x,y -> x + y},{x -> x})
	static Aggregator avg = new Aggregator("Average",{x -> [x,1]},{x,y -> [x[0]+=y,x[1]++]},{x -> x[0]/x[1]})
	static Aggregator max = new Aggregator("Max",{x -> x},{x, y -> Math.max(x,y)},{x -> x})
	static Aggregator min = new Aggregator("Min",{x -> x},{x, y -> Math.min(x,y)},{x -> x})
	static Closure monthly = {d1, d2 -> Closure month = {d -> 	Calendar myCal = Calendar.getInstance(); 
																myCal.setTime(d);
																myCal.get(Calendar.MONTH)}
										Closure year = {d -> 	Calendar myCal = Calendar.getInstance(); 
																myCal.setTime(d);
																myCal.get(Calendar.MONTH)}
										month(d1) == month(d2) && year(d1) == year(d2)}
	static Closure total = {x,y -> true}
		
	// Convenience-Methoden bei Kai anfordern (user x buchungssatz)
	public static void main(def args){
			
		// Testdaten: Liste mit 100 Daten mit je drei Tagen Abstand
		Calendar cal = Calendar.getInstance()
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy")
		cal.se	tTime(df.parse("01.01.2009"))
		List list = (1..100).collect{n -> cal.add(Calendar.DAY_OF_YEAR,3); [cal.getTime(),n]}
		
		Closure total = {d1, d2 -> true}
		
		// Sortieren einer HashMap
		def sortHashMap =	{ h ->	m = [:]
		 							h.keySet().toList().sort().each{n -> m[n]=h[n]}    
									return m
		}
		
		// Berechnung der Aggregate (pro Monat und Gesamt)
		[Agg.SUM,Agg.AVG,Agg.MAX,Agg.MIN].each{
			{	agg ->	println "Monatlich aggregiert:"
						println "---------------------"
						HashMap res = aggmap(monthly,agg).call(list)
						res.keySet().toList().sort().each(){
							h -> cal.setTime(h); println "Monat: "+cal.get(Calendar.MONTH)+" "+agg.name+": "+res[h]
						}	
						println agg.name+" (gesamt): "+aggmap(total,agg).call(list).values().toList().get(0)+"\n"
			}.call(getAggregator(it))}
	}
	
	static getAggregator(Agg agg){
		switch(agg){
			case Agg.SUM : return sum; break;
			case Agg.AVG : return avg; break;
			case Agg.MAX : return max; break;
			case Agg.MIN : return min; break;
			// Exception
			default : return null; 
		}
	}
	
	// Convenience-Mehtode zur Berechnungen der Monatssumme (summiert über valueAttribut)
	public static HashMap monthlySum(List list, Method keyAttribut, Method valueAttribut){
		return aggmap(monthly,sum,keyAttribut,valueAttribut).call(list)
	}
	
	// Convenience-Mehtode zur Berechnungen der Gesamtsumme (summiert über valueAttribut)
	public static HashMap totalSum(List list, Method keyAttribut, Method valueAttribut){
		return aggmap(total,sum,keyAttribut,valueAttribut).call(list)
	}
	
	// ohne equiv, initial und ohne finalize
	public static Closure aggmap(Closure aggfunc, Method a1, Method a2){ 
		return aggmap({x,y -> true},{x -> x},aggfunc,{x -> x},a1,a2)
	}
	
	// ohne initial und ohne finalize
	public static Closure aggmap(Closure equiv, Closure aggfunc, Method a1, Method a2){ 
		return aggmap(equiv,{x -> x},aggfunc,{x -> x},a1,a2)
	}
	
	// ohne finalize
	public static Closure aggmap(Closure equiv, Closure initial, Closure aggfunc, Method a1, Method a2){ 
		return aggmap(equiv,initial,aggfunc,{x -> x},a1,a2)
	}
	
	// mit Methoden (für Listenelemente)
	public static Closure aggmap(Closure equiv, Closure initial, Closure aggfunc, Closure finalize, Method a1, Method a2){
		Closure closure = {method, obj -> method.invoke(obj)}
		return aggmap(equiv,initial,aggfunc,finalize,closure.curry(a1),closure.curry(a2))
	}
	
	// mit Closures (für Listenelemente)
	public static Closure aggmap(Closure equiv, Closure initial, Closure aggfunc, Closure m1, Closure m2){
		aggmap(equiv, initial, aggfunc,{x -> x}, m1, m2)
	}
	
	// Mit Aggregator und Methode für Liste von Listen
	public static Closure aggmap(Closure equiv, Aggregator agg){
		Closure el = {i,obj -> ArrayList.getMethod("get",int).invoke(obj,i)}
		aggmap(equiv, agg.initialize, agg.aggregate, agg.finalize, el.curry(0),el.curry(1))
	}
	
	// Mit Aggregator und Methode für Liste von Listen
	public static Closure aggmap(Closure equiv, Aggregator agg, Method m1, Method m2){
		aggmap(equiv, agg.initialize, agg.aggregate, agg.finalize, m1,m2)
	}
	
	// mit Closures (für Listenelemente)
	public static Closure aggmap(Closure equiv, Closure initial, Closure aggfunc, Closure finalize, Closure m1, Closure m2){
		return {list -> Map map = new HashMap();
						Object key;
						
						list.each{ obj ->
							if((key=map.keySet().find{equiv(it,m1(obj))}) != null) map[key] = aggfunc(map[key],m2(obj)) 
							else map[m1(obj)] = initial(m2(obj)) 
						}
						
						map.each{x -> map[x.key] = finalize(x.value)}
						return map
				}
	}
}

enum Agg {SUM, AVG, MAX, MIN}

class Aggregator{
	String name
	Closure initialize
	Closure aggregate
	Closure finalize
	
	Aggregator(String n, Closure i, Closure a, Closure f){
		initialize = i
		aggregate = a
		finalize = f
		name = n
	}
}



