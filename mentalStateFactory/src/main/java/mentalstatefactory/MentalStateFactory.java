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

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import krTools.KRInterface;
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
	private static Map<Class<? extends KRInterface>, Class<? extends MentalState>> mentalstateInterfaces;

	/**
	 * The default interface that get be obtained by
	 * {@link MentalStateFactory#getDefaultLanguage()}.
	 */
	private static MentalState defaultInterface;

	// Initialize KR interfaces map and default language interface.
	static {
		init();
	}

	/**
	 * KRFactory is a utility class; constructor is hidden.
	 */
	private MentalStateFactory() {
	}

	/**
	 * Static init code for this class. Adds the default. FIXME can we do this
	 * nicer?
	 */
	private static void init() {
		mentalstateInterfaces = new Hashtable<>();
		// Add SWI Prolog and set as default.
		defaultInterface = new SwiPrologMentalState();
		MentalStateFactory.addInterface(defaultInterface);
	}

	/**
	 * Provides an interface for an available knowledge representation
	 * technology. Use {@link MentalStateFactory#getSupportedInterfaces()} to
	 * get names of supported interfaces.
	 *
	 * @param kri
	 *            The required KR interface. Should not be null.
	 * @return A {@link MentalState} implementation for the given language
	 *         technology.
	 * @throws InstantiationException
	 *             If kr interface does not support constructing a mental state.
	 */
	public static MentalState getInterface(Class<? extends KRInterface> kri)
			throws InstantiationFailedException {
		if (!mentalstateInterfaces.containsKey(kri)) {
			throw new InstantiationFailedException(
					"The knowledge representation language " + kri.getName()
							+ " does not support constructing a mental state.");
		}

		try {
			return mentalstateInterfaces.get(kri).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new InstantiationFailedException("Failed to instantiate "
					+ kri.getName(), e);
		}
	}

	/**
	 * Adds a KR interface to the list of supported language interfaces.
	 *
	 * @param msInterface
	 *            {@link MentalState} to be added to the factory. Should not be
	 *            null
	 * @throws IllegalStateException
	 *             if KR interface is already present, or no (valid) interface
	 *             was provided.
	 */
	public static void addInterface(MentalState msInterface) {
		if (mentalstateInterfaces.containsKey(msInterface.getKRInterface())) {
			throw new IllegalStateException("Interface "
					+ msInterface.getKRInterface().getName()
					+ " is already present");
		}
		mentalstateInterfaces.put(msInterface.getKRInterface(),
				msInterface.getClass());
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
	 */
	public static MentalState getDefaultInterface()
			throws KRInitFailedException {
		return defaultInterface;
	}

}