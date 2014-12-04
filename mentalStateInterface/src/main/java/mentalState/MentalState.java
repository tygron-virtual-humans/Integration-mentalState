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

package mentalState;

import krTools.language.Term;
import eis.iilang.Parameter;

/**
 * The knowledge representation (KR) interface with GOAL specific extra
 * functionality.
 * 
 */
public interface MentalState {
	/**
	 * get name of interface
	 * 
	 * @return name of interface.
	 */
	String getName();

	/**
	 * Converts an {@link Parameter} parameter to a {@link Term}.
	 * 
	 * @param parameter
	 *            EIS parameter.
	 * @return a GOAL {@link Term}
	 */
	public Term convert(Parameter parameter);

	/**
	 * Converts a {@link Term} to a {@link Parameter}.
	 * 
	 * @param term
	 *            The JPL term.
	 * @return An EIS parameter.
	 */

	public Parameter convert(Term term);

}
