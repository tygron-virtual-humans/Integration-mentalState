package mentalState;

import java.util.LinkedList;

import eis.iilang.DataContainer;
import eis.iilang.IILObjectVisitor;
import eis.iilang.IILVisitor;
import eis.iilang.Parameter;

/**
 * A percept.
 * A percept consists of a name and some parameters.
 * 
 * @author tristanbehrens
 *
 */
public class Emotion2 extends DataContainer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5929676291607949546L;

	/**
	 * Constructs a percept from a name.
	 * 
	 * @param name
	 */
	public Emotion2(String name) {
		super(name);
	}
	
	/** 
	 * Contructs a percept from a name and an array of parameters.
	 * 
	 * @param name the name.
	 * @param parameters the parameters.
	 */
	public Emotion2(String name, Parameter...parameters) {
		super(name, parameters);
	}

	/** 
	 * Contructs a percept from a name and an array of parameters.
	 * 
	 * @param name the name.
	 * @param parameters the parameters.
	 */
	public Emotion2(String name, LinkedList<Parameter> parameters) {
		super(name, parameters);
	}

	@Override
	protected String toXML(int depth) {

		String xml = "";
		
		xml += indent(depth) + "<emotion name=\"" + name + "\">" + "\n";
		
		for( Parameter p : params ) {
			
			xml += indent(depth+1) + "<emotionParameter>" + "\n";
			// xml += p.toXML(depth+2);
			xml += indent(depth+1) + "</emotionParameter>" + "\n";
			
		}

		xml += indent(depth) + "</emotion>" + "\n";

		return xml;

	}

	@Override
	public String toProlog() {
		
		String ret = "";
		
		ret+=name;

		if( params.isEmpty() == false) {
			ret += "(";
			
			ret += params.getFirst().toProlog();
			
			for( int a = 1 ; a < params.size(); a++ ) {
				Parameter p = params.get(a);
				ret += "," + p.toProlog();
			} 
			
			ret += ")";
		}
		
		return ret;
	
	}

	/*	public String toProlog() {
		
		String ret = "percept";
		
		ret+="(";
		
		ret+=name;
		
		for( Parameter p : params ) 
			ret += "," + p.toProlog();
		
		ret+=")";
		
		return ret;
	
	}*/

	@Override
	public Object clone() {

		Emotion2 ret = new Emotion2(this.name, this.getClonedParameters());
		
		ret.source = this.source;
		
		return ret;
	
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if ( obj == null )
			return false;
		if ( obj == this )
			return true;
		
		if( !(obj instanceof Emotion2) )
			return false;
		
		return super.equals(obj);

	}

	@Override
	public Object accept(IILObjectVisitor visitor, Object object) {

		return visitor.visit(this,object);

	}

	@Override
	public void accept(IILVisitor visitor) {
		
		visitor.visit(this);
		
	}

}
