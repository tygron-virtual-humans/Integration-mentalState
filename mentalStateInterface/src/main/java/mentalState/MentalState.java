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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import krTools.database.Database;
import krTools.errors.exceptions.KRDatabaseException;
import krTools.errors.exceptions.KRInitFailedException;
import krTools.errors.exceptions.KRQueryFailedException;
import krTools.language.DatabaseFormula;
import krTools.language.Term;
import krTools.language.Update;
import languageTools.program.agent.AgentId;
import languageTools.program.agent.AgentProgram;
import languageTools.program.agent.actions.UserSpecAction;
import languageTools.program.agent.msg.Message;
import eis.iilang.Action;
import eis.iilang.Parameter;
import eis.iilang.Percept;

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
	Term convert(Parameter parameter);

	/**
	 * Converts a {@link Term} to a {@link Parameter}.
	 * 
	 * @param term
	 *            The JPL term.
	 * @return An EIS parameter.
	 */
	Parameter convert(Term term);
	
	/**
	 * Creates a Term containing an (ordered) list of Terms.
	 *
	 * @param termList
	 *            The list of Terms to convert to a single Term.
	 *
	 * @return A Term containing the given Terms as a list.
	 */
	Term makeList(List<Term> termList);
	
	/**
	 * Converts a {@link UserSpecAction} to a {@link Action}.
	 * 
	 * @param term
	 *            The UserSpecAction.
	 * @return An EIS action.
	 */
	Action convert(UserSpecAction action);
	
	/**
	 * Get a subset update, with either only mail updates or with all other
	 * updates.
	 *
	 * @param update
	 * 			  The update to filter.
	 * @param selectMails
	 *            {@code true} if you want only mail updates; {@code false} if
	 *            you want all but mail updates. Mail updates are updates that
	 *            have a 'sent/2' or 'received/2' main operator.
	 * @return A {@link Update} with either only mail updates or all other
	 *         updates.
	 */
	Update filterMailUpdates(Update update, boolean selectMails);
	
	/**
	 * Creates new database using the content. It is the responsibility of the
	 * KR technology to differentiate databases (e.g. by associating unique
	 * identifiers with a database).
	 *
	 * @param type
	 *            database type, i.e. belief base, goal base, mailbox, percept
	 *            base.
	 * @param content
	 *            set of formulas to be inserted to database.
	 * @param agent
	 *            the agent that requests the database; its name is used as
	 *            the database identifier.
	 *
	 * @return The database that has been created.
	 *
	 * @throws KRInitFailedException
	 * @throws KRDatabaseException
	 * @throws KRQueryFailedException
	 */
	Database makeDatabase(BASETYPE type, Collection<DatabaseFormula> content, AgentProgram agent) 
			throws KRInitFailedException, KRDatabaseException, KRQueryFailedException;
	
	/**
	 * Performs a query on a database returning all receivers of the given
	 * message according to the message base.
	 *
	 * @param database
	 * 			  The database.
	 * @param message
	 *            The message.
	 * @return The receivers of the given message.
	 * 
	 * @throws KRQueryFailedException
	 */
	Collection<String> getReceiversOfMessage(Database database, Message message)
			throws KRQueryFailedException;
	
	/**
	 * Inserts a sent or received fact for the given message into a database.
	 *
	 * @param database
	 * 			  The database.
	 * @param message
	 *            The message that has been sent or received.
	 * @param received
	 *            {@code true} if the message has been received; {@code false}
	 *            if it has been sent.
	 * @return The set of database formulas that have been inserted into the
	 *         database.
	 * 
	 * @throws KRDatabaseException
	 */
	Set<DatabaseFormula> insert(Database database, Message message, boolean received)
			throws KRDatabaseException;
	
	/**
	 * Inserts a percept into a database.
	 *
	 * @param database
	 * 			  The database.
	 * @param percept
	 *            The EIS percept to be inserted.
	 * @return The formula that was added to the percept base.
	 * 
	 * @throws KRDatabaseException
	 */
	DatabaseFormula insert(Database database, Percept percept)
			throws KRDatabaseException;
	
	/**
	 * Removes a percept from a database.
	 *
	 * @param database
	 * 			  The database.
	 * @param percept
	 *            The EIS percept to be deleted.
	 * @return The formula that was deleted from the percept base.
	 * 
	 * @throws KRDatabaseException
	 */
	DatabaseFormula delete(Database database, Percept percept)
			throws KRDatabaseException;
	
	/**
	 * Updates the 'agent(name)' fact for an agent in a database.
	 *
	 * @param database
	 *            The database that needs to be updated.
	 * @param insert
	 *            {@code true} if the fact needs to be inserted; {@code false}
	 *            if the fact needs to be removed;
	 * @param id
	 *            Id of the agent whose related agent fact needs to be updated.
	 * @param me
	 *            {@code true} if the related 'me(name)' fact also needs to be
	 *            updated.
	 * @return The facts that were inserted or removed.
	 * @throws KRDatabaseException
	 */
	Set<DatabaseFormula> updateAgentFact(Database database, boolean insert, AgentId id, boolean me)
			throws KRDatabaseException;
}
