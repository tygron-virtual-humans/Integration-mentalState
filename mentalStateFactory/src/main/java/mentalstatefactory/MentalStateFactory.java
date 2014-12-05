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

package mentalstatefactory;

import java.rmi.activation.UnknownObjectException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import krTools.KRInterface;
import krTools.errors.exceptions.KRException;
import krTools.errors.exceptions.KRInitFailedException;
import mentalState.MentalState;
import swiPrologMentalState.SwiPrologMentalState;

/**
 * Factory of Mental State Interfaces.
 * 
 * @author W.Pasman 4dec14
 */
public class MentalStateFactory {

	/**
	 * A map of names to {@link MentalSt}s that are supported.
	 */
	private static Map<Class<? extends KRInterface>, MentalState> mentalstateInterfaces = 
			new Hashtable<>();

	/**
	 * The default interface that get be obtained by
	 * {@link MentalStateFactory#getDefaultLanguage()}.
	 */
	private static MentalState defaultInterface;

	// Initialize KR interfaces map and default language interface.
	static {

		// Add SWI Prolog and set as default.
		try {
			defaultInterface = new SwiPrologMentalState();
			MentalStateFactory.addInterface(defaultInterface);
		} catch (IllegalStateException e) {
			System.out
					.println("Failed to initialize the SWI Prolog MentalState interface because "
							+ e.getMessage());
		}
	}

	/**
	 * KRFactory is a utility class; constructor is hidden.
	 */
	private MentalStateFactory() {
	}

	/**
	 * Provides an interface for an available knowledge representation
	 * technology. Use {@link MentalStateFactory#getSupportedInterfaces()} to
	 * get names of supported interfaces.
	 * 
	 * @param name
	 *            The name of the interface, eg "SwiPrologMentalState".
	 * @return A {@link MentalState} implementation for the given language
	 *         technology.
	 * @throws UnknownObjectException
	 *             If interface is unknown.
	 */
	public static MentalState getInterface(Class<? extends KRInterface> kri)
			throws UnknownObjectException {
		MentalState msInterface = mentalstateInterfaces.get(kri);
		if (msInterface == null) {
			throw new UnknownObjectException("Could not find interface " + kri
					+ "; the following interfaces are available: "
					+ mentalstateInterfaces.keySet());
		}
		return msInterface;
	}

	/**
	 * Adds a KR interface to the list of supported language interfaces.
	 * 
	 * @param A
	 *            KR interface.
	 * @throws KRException
	 *             If KR interface is already present, or no (valid) interface
	 *             was provided.
	 */
	public static void addInterface(MentalState msInterface) {
		if (msInterface == null) {
			throw new IllegalArgumentException("Cannot add null");
		} else if (mentalstateInterfaces.containsKey(msInterface.getKRInterface())) {
			throw new IllegalStateException("Interface "
					+ msInterface.getKRInterface() + " already present");
		} else {
			mentalstateInterfaces.put(msInterface.getKRInterface(), msInterface);
		}
	}

	/**
	 * @return A set of names with supported KR interfaces.
	 */
	public static Set<Class<? extends KRInterface>> getSupportedInterfaces() {
		return mentalstateInterfaces.keySet();
	}

	/**
	 * Returns the default KR interface.
	 * 
	 * @return The default KR interface.
	 * @throw {@link IllegalStateException} If no default interface is not
	 *        available.
	 */
	public static MentalState getDefaultInterface()
			throws KRInitFailedException {
		if (defaultInterface == null) {
			throw new IllegalStateException(
					"Something went wrong; could not locate default interface.");
		}
		return defaultInterface;
	}

}