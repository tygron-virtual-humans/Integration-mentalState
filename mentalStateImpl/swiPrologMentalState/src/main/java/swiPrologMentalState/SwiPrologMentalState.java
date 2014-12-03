/**
 * Knowledge Representation Tools. Copyright (C) 2014 Koen Hindriks.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package swiPrologMentalState;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jpl.Compound;
import krTools.language.Term;
import mentalState.mentalState;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.TruthValue;

/**
 * The knowledge representation (KR) interface with GOAL specific extra
 * functionality.
 * 
 */
public class SwiPrologMentalState implements mentalState {

	@Override
	public Term convert(Parameter parameter) {
		if (parameter instanceof Identifier) {
			// do not do quoting of the term, that is only for printing.
			return (Term) new jpl.Atom(((Identifier) parameter).getValue());
		}
		if (parameter instanceof Numeral) {
			// check if parameter that is passed is a float.
			// note that LONG numbers are converted to float
			Number number = ((Numeral) parameter).getValue();
			if (number instanceof Double || number instanceof Float) {
				return (Term) new jpl.Float(number.doubleValue());
			} else {
				// int or long. Check if it fits
				if (number instanceof Long
						&& (number.longValue() < Integer.MIN_VALUE || number
								.longValue() > Integer.MAX_VALUE)) {
					throw new ArithmeticException("EIS long value " + number
							+ " does not fit into a JPL integer");
				}
				return (Term) new jpl.Integer(number.intValue());
			}
		}
		if (parameter instanceof Function) {
			Function f = (Function) parameter;
			ArrayList<jpl.Term> terms = new ArrayList<jpl.Term>();
			for (Parameter p : f.getParameters()) {
				terms.add((jpl.Term) convert(p));
			}
			return (Term) new jpl.Compound(f.getName(),
					terms.toArray(new jpl.Term[0]));
		}
		if (parameter instanceof ParameterList) {
			ArrayList<jpl.Term> terms = new ArrayList<jpl.Term>();
			for (Parameter p : (ParameterList) parameter) {
				terms.add((jpl.Term) convert(p));
			}
			return (Term) termsToList(terms);
		}
		if (parameter instanceof TruthValue) {
			return (Term) new jpl.Atom(((TruthValue) parameter).getValue());
		}
		throw new IllegalArgumentException("Failed to convert EIS parameter "
				+ parameter + " to Prolog.");
	}

	@Override
	public Parameter convert(Term term1) {
		if (!(term1 instanceof jpl.Term)) {
			throw new IllegalArgumentException("term " + term1
					+ " is not a jpl term");
		}
		jpl.Term term = (jpl.Term) term1;
		if (term.isInteger()) {
			return new eis.iilang.Numeral(((jpl.Integer) term).intValue());
		}
		if (term.isFloat()) {
			return new eis.iilang.Numeral(((jpl.Float) term).floatValue());
		}

		// All other types have name() method; get it.
		String name = term.name();
		// Strip single or double quotes in term's name, if any.
		// if (name.startsWith("\'") || name.startsWith("\"")) {
		// name = name.substring(1, name.length()-1);
		// }
		if (term.isAtom()) {
			return new eis.iilang.Identifier(name);
		}
		if (term.isVariable()) {
			throw new UnsupportedOperationException(
					"Trying to convert variable "
							+ term
							+ " to EIS parameter but EIS does not support variables.");
		}
		if (term.isCompound()) {
			LinkedList<eis.iilang.Parameter> parameters = new LinkedList<Parameter>();
			// Check whether we're dealing with a list or other operator.
			if (name.equals(".")) {
				for (jpl.Term arg : getOperands(".", term)) {
					parameters.add(convert((Term) arg));
				}
				// Remove the empty list.
				parameters.removeLast();
				return new eis.iilang.ParameterList(parameters);
			} else {
				for (jpl.Term arg : term.args()) {
					parameters.add(convert((Term) arg));
				}
				return new eis.iilang.Function(name, parameters);
			}
		}

		throw new UnsupportedOperationException(
				"Trying to convert term "
						+ term
						+ " to EIS parameter but EIS conversion of this type of term is not supported.");
	}

	/**
	 * Returns the operands of a (repeatedly used) right associative binary
	 * operator.
	 * <p>
	 * Can be used, for example, to get the conjuncts of a conjunction or the
	 * elements of a list. Note that the <i>second</i> conjunct or element in a
	 * list concatenation can be a conjunct or list itself again.
	 * </p>
	 * <p>
	 * A list (term) of the form '.'(a,'.'(b,'.'(c, []))), for example, returns
	 * the elements a, b, c, <i>and</i> the empty list []. A conjunction of the
	 * form ','(e0,','(e1,','(e2...((...,en)))...) returns the list of conjuncts
	 * e0, e1, e2, etc.
	 * </p>
	 * 
	 * @param operator
	 *            The binary operator.
	 * @param term
	 *            The term to be unraveled.
	 * @return A list of operands.
	 */
	private static List<jpl.Term> getOperands(String operator, jpl.Term term) {
		List<jpl.Term> list = new ArrayList<jpl.Term>();

		if (term.isCompound() && term.name().equals(operator)
				&& term.arity() == 2) {
			list.add(term.arg(1));
			list.addAll(getOperands(operator, term.arg(2)));
		} else {
			list.add(term);
		}
		return list;
	}

	/**
	 * Returns a (possibly empty) Prolog list with the given terms as elements
	 * of the list.
	 * 
	 * @param terms
	 *            The elements to be included in the list.
	 * @return A Prolog list using the "." and "[]" list constructors.
	 */
	private static jpl.Term termsToList(List<jpl.Term> terms) {
		// Start with element in list, since innermost term of Prolog list is
		// the last term.
		jpl.Term list = new jpl.Atom("[]");
		for (int i = terms.size() - 1; i >= 0; i--) {
			list = new Compound(".", new jpl.Term[] { terms.get(i), list });
		}
		return list;
	}
}
