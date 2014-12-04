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

package goalhub.krTools;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import krTools.KRInterface;
import krTools.errors.exceptions.KRException;
import krTools.errors.exceptions.KRInitFailedException;
import krTools.errors.exceptions.KRInterfaceNotSupportedException;

/**
 * Factory of KR interfaces. Currently, the factory supports:
 * <ul>
 * 	<li>SWI Prolog v6.0.2</li>
 * </ul>
 */
public class KRFactory {
	
	/**
	 * Static strings for names of supported KR Languages.
	 */
	public static String SWI_PROLOG;

	/**
	 * A map of names to {@link KRInterface}s that are supported.
	 */
	private static Map<String, KRInterface> languages = new Hashtable<String, KRInterface>();
	/**
	 * The default interface that get be obtained by {@link KRFactory#getDefaultLanguage()}.
	 */
	private static KRInterface defaultInterface;

	// Initialize KR interfaces map and default language interface.
	static {
		// Add SWI Prolog and set as default.
		try {
			defaultInterface = swiprolog.SWIPrologInterface.getInstance();
			KRFactory.addInterface(defaultInterface);
			SWI_PROLOG = defaultInterface.getName();
		} catch (KRInitFailedException e) {
			// TODO
			System.out.println("Failed to initialize the SWI Prolog interface because " + e.getMessage());
		} catch (KRException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * KRFactory is a utility class; constructor is hidden.
	 */
	private KRFactory() {
	}

	/**
	 * Provides an interface for an available knowledge representation technology. Use
	 * {@link KRFactory#getSupportedInterfaces()} to get names of supported interfaces.
	 * 
	 * @param name The name of the interface, e.g., "swiprolog".
	 * @return A KR interface implementation for the knowledge representation technology.
	 * @throws KRInterfaceNotSupportedException If interface is unknown.
	 */
	public static KRInterface getInterface(String name) throws KRInterfaceNotSupportedException {
		KRInterface krInterface = languages.get(name.toLowerCase());
		if (krInterface == null) {
			throw new KRInterfaceNotSupportedException("Could not find interface " + name
					+ "; the following interfaces are available: " + languages.keySet());
		}
		return krInterface;
	}

	/**
	 * Adds a KR interface to the list of supported language interfaces.
	 * 
	 * @param A KR interface.
	 * @throws KRException If KR interface is already present, or no (valid) interface was provided.
	 */
	public static void addInterface(KRInterface krInterface) throws KRException {
		if (krInterface == null) {
			throw new KRException(
					"Cannot add null");
		}
		if (!languages.containsKey(krInterface.getName())) {
			languages.put(krInterface.getName().toLowerCase(), krInterface);
		} else {
			throw new KRException("Interface " + krInterface.getName()
					+ " already present");
		}
	}

	/**
	 * @return A set of names with supported KR interfaces.
	 */
	public static Set<String> getSupportedInterfaces() {
		return languages.keySet();
	}

	/**
	 * Returns the default KR interface.
	 * 
	 * @return The default KR interface.
	 * @throw KRInitFailedException If no default interface is not available.
	 */
	public static KRInterface getDefaultInterface() throws KRInitFailedException {
		if (defaultInterface == null) {
			throw new KRInitFailedException("Something went wrong; could not locate default interface.");
		}
		return defaultInterface;
	}
	
	/**
	 * Sets the default KR interface.
	 * 
	 * @param krInterface The KR interface that should be used as default.
	 */
	public static void setDefaultInterface(KRInterface krInterface) {
		defaultInterface = krInterface;
	}

}