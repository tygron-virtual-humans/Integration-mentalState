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

	/**
	 * DOC
	 * 
	 * @param parameter
	 *            EIS parameter.
	 * @return
	 */
	@Override
	public static jpl.Term convert(eis.iilang.Parameter parameter) {
		if (parameter instanceof Identifier) {
			// do not do quoting of the term, that is only for printing.
			return new jpl.Atom(((Identifier) parameter).getValue());
		}
		if (parameter instanceof Numeral) {
			// check if parameter that is passed is a float.
			// note that LONG numbers are converted to float
			Number number = ((Numeral) parameter).getValue();
			if (number instanceof Double || number instanceof Float) {
				return new jpl.Float(number.doubleValue());
			} else {
				// int or long. Check if it fits
				if (number instanceof Long
						&& (number.longValue() < Integer.MIN_VALUE || number
								.longValue() > Integer.MAX_VALUE)) {
					throw new ArithmeticException("EIS long value " + number
							+ " does not fit into a JPL integer");
				}
				return new jpl.Integer(number.intValue());
			}
		}
		if (parameter instanceof Function) {
			Function f = (Function) parameter;
			ArrayList<jpl.Term> terms = new ArrayList<jpl.Term>();
			for (Parameter p : f.getParameters()) {
				terms.add(convert(p));
			}
			return new jpl.Compound(f.getName(), terms.toArray(new jpl.Term[0]));
		}
		if (parameter instanceof ParameterList) {
			ArrayList<jpl.Term> terms = new ArrayList<jpl.Term>();
			for (Parameter p : (ParameterList) parameter) {
				terms.add(convert(p));
			}
			return termsToList(terms);
		}
		if (parameter instanceof TruthValue) {
			return new jpl.Atom(((TruthValue) parameter).getValue());
		}
		throw new IllegalArgumentException("Failed to convert EIS parameter "
				+ parameter + " to Prolog.");
	}

	/**
	 * Converts a JPL term to an EIS parameter.
	 * 
	 * @param term
	 *            The JPL term.
	 * @return An EIS parameter.
	 */
	@Override
	public static eis.iilang.Parameter convert(jpl.Term term) {
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
					parameters.add(convert(arg));
				}
				// Remove the empty list.
				parameters.removeLast();
				return new eis.iilang.ParameterList(parameters);
			} else {
				for (jpl.Term arg : term.args()) {
					parameters.add(convert(arg));
				}
				return new eis.iilang.Function(name, parameters);
			}
		}

		throw new UnsupportedOperationException(
				"Trying to convert term "
						+ term
						+ " to EIS parameter but EIS conversion of this type of term is not supported.");
	}

}
