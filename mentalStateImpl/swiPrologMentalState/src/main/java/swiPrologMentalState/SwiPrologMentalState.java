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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.Atom;
import jpl.Compound;
import jpl.Variable;
import krTools.KRInterface;
import krTools.database.Database;
import krTools.errors.exceptions.KRDatabaseException;
import krTools.errors.exceptions.KRInitFailedException;
import krTools.errors.exceptions.KRQueryFailedException;
import krTools.language.DatabaseFormula;
import krTools.language.Query;
import krTools.language.Substitution;
import krTools.language.Term;
import krTools.language.Update;
import krTools.language.Var;
import krTools.parser.SourceInfo;
import languageTools.program.agent.ActionSpecification;
import languageTools.program.agent.AgentId;
import languageTools.program.agent.AgentProgram;
import languageTools.program.agent.Module;
import languageTools.program.agent.actions.ActionCombo;
import languageTools.program.agent.actions.AdoptAction;
import languageTools.program.agent.actions.DeleteAction;
import languageTools.program.agent.actions.DropAction;
import languageTools.program.agent.actions.InsertAction;
import languageTools.program.agent.actions.UserSpecAction;
import languageTools.program.agent.msc.BelLiteral;
import languageTools.program.agent.msc.MentalLiteral;
import languageTools.program.agent.msg.Message;
import languageTools.program.agent.rules.Rule;
import mentalState.BASETYPE;
import mentalState.DependencyGraph;
import mentalState.MentalState;
import swiprolog.SWIPrologInterface;
import swiprolog.database.SWIPrologDatabase;
import swiprolog.language.JPLUtils;
import swiprolog.language.PrologDBFormula;
import swiprolog.language.PrologQuery;
import swiprolog.language.PrologSubstitution;
import swiprolog.language.PrologTerm;
import swiprolog.language.PrologUpdate;
import swiprolog.parser.PrologOperators;
import eis.iilang.Action;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import eis.iilang.TruthValue;

/**
 * The knowledge representation (KR) interface with GOAL specific extra
 * functionality.
 */
public class SwiPrologMentalState implements MentalState {
	private static final jpl.Term ANON_VAR = new Variable("_");
	/**
	 * Contains all databases that are maintained by SWI Prolog. The key is the
	 * owner of the database. The value is a list of databases associated with
	 * that agent. An owner that has no associated databases should be removed
	 * from the map.
	 */
	private final Map<String, Set<TypedSWIPrologDatabase>> databases = new HashMap<>();
	/**
	 * Stores content of knowledge base for later reference (when constructing a
	 * belief or goal base). Every program has one set of knowledge. This
	 * knowledge is collected by GOAL and inserted here into all databases. Key
	 * is the name of the agent.
	 */
	private final Map<String, Collection<DatabaseFormula>> knowledge = new HashMap<>();
	/**
	 * The set of dynamic declarations that need to be made when a new belief is
	 * inserted by an agent. Indexed by agent names and initialized by
	 * initialize method.
	 */
	private final Hashtable<String, Set<jpl.Term>> dynamicDeclarationsForBeliefBase = new Hashtable<>();
	/**
	 * The set of dynamic declarations that need to be made when a new goal is
	 * inserted by an agent. Indexed by agent names and initialized by
	 * initialize method.
	 */
	private final Hashtable<String, Set<jpl.Term>> dynamicDeclarationsForGoals = new Hashtable<>();
	private final Set<String> reserved = new LinkedHashSet<String>();

	@Override
	public Class<? extends KRInterface> getKRInterface() {
		return SWIPrologInterface.class;
	}

	@Override
	public Term convert(Parameter parameter) {
		if (parameter instanceof Identifier) {
			// do not do quoting of the term, that is only for printing.
			return new PrologTerm(
					new Atom(((Identifier) parameter).getValue()), null);
		} else if (parameter instanceof Numeral) {
			// check if parameter that is passed is a float.
			// note that LONG numbers are converted to float
			Number number = ((Numeral) parameter).getValue();
			if (number instanceof Double || number instanceof Float) {
				return new PrologTerm(new jpl.Float(number.doubleValue()), null);
			} else {
				return new PrologTerm(JPLUtils.createIntegerNumber(number
						.longValue()), null);
			}
		} else if (parameter instanceof Function) {
			Function f = (Function) parameter;
			List<jpl.Term> terms = new ArrayList<>(f.getParameters().size());
			for (Parameter p : f.getParameters()) {
				PrologTerm t = (PrologTerm) convert(p);
				terms.add(t.getTerm());
			}
			return new PrologTerm(new Compound(f.getName(),
					terms.toArray(new jpl.Term[terms.size()])), null);
		} else if (parameter instanceof ParameterList) {
			ParameterList pl = (ParameterList) parameter;
			List<jpl.Term> terms = new ArrayList<>(pl.size());
			for (Parameter p : pl) {
				PrologTerm t = (PrologTerm) convert(p);
				terms.add(t.getTerm());
			}
			return new PrologTerm(JPLUtils.termsToList(terms), null);
		} else if (parameter instanceof TruthValue) {
			return new PrologTerm(
					new Atom(((TruthValue) parameter).getValue()), null);
		} else {
			throw new IllegalArgumentException("Encountered EIS parameter "
					+ parameter + " of unsupported type "
					+ parameter.getClass().getCanonicalName());
		}
	}

	@Override
	public Parameter convert(Term term1) {
		if (!(term1 instanceof PrologTerm)) {
			throw new IllegalArgumentException("term " + term1
					+ " is not a SWI prolog term");
		}
		jpl.Term term = ((PrologTerm) term1).getTerm();
		if (term.isInteger()) {
			return new Numeral(((jpl.Integer) term).intValue());
		} else if (term.isFloat()) {
			return new Numeral(((jpl.Float) term).floatValue());
		}

		// All other types have name() method; get it.
		String name = term.name();
		// Strip single or double quotes in term's name, if any.
		// if (name.startsWith("\'") || name.startsWith("\"")) {
		// name = name.substring(1, name.length()-1);
		// }
		if (term.isAtom()) {
			return new Identifier(name);
		} else if (term.isVariable()) {
			throw new UnsupportedOperationException(
					"conversion of the variable "
							+ term
							+ " to EIS parameter is not possible: EIS does not support variables.");
		} else if (term.isCompound()) {
			LinkedList<Parameter> parameters = new LinkedList<>();
			// Check whether we're dealing with a list or other operator.
			if (name.equals(".")) {
				for (jpl.Term arg : JPLUtils.getOperands(".", term)) {
					parameters.add(convert(new PrologTerm(arg, null)));
				}
				// Remove the empty list.
				parameters.removeLast();
				return new ParameterList(parameters);
			} else {
				for (jpl.Term arg : term.args()) {
					parameters.add(convert(new PrologTerm(arg, null)));
				}
				return new Function(name, parameters);
			}
		} else {
			throw new UnsupportedOperationException("conversion of term "
					+ term + " of type " + term.getClass().getCanonicalName()
					+ " to EIS parameter is not supported.");
		}
	}

	@Override
	public Term makeList(List<Term> termList) {
		SourceInfo source = null;
		if (!termList.isEmpty()) {
			source = termList.get(0).getSourceInfo();
		}
		List<jpl.Term> terms = new ArrayList<>(termList.size());
		for (Term t : termList) {
			terms.add(((PrologTerm) t).getTerm());
		}
		return new PrologTerm(JPLUtils.termsToList(terms), source);
	}

	@Override
	public Action convert(UserSpecAction action) {
		LinkedList<Parameter> parameters = new LinkedList<>();
		for (Term term : action.getParameters()) {
			parameters.add(convert(term));
		}
		return new Action(action.getName(), parameters);
	}

	@Override
	public Update filterMailUpdates(Update update, boolean selectMails) {
		PrologUpdate update1 = (PrologUpdate) update;
		List<jpl.Term> conjuncts = JPLUtils.getOperands(",", update1.getTerm());
		List<jpl.Term> newterm = new LinkedList<>();
		for (jpl.Term term : conjuncts) {
			if (isMailOperator(term) == selectMails) {
				newterm.add(term);
			}
		}
		return new PrologUpdate(JPLUtils.termsToConjunct(newterm),
				update.getSourceInfo());
	}

	/**
	 * Checks if given term is a mail operator. If the head of the term is not,
	 * we peek inside the not to determine. the two mail operators are "sent/2"
	 * and "received/2".
	 *
	 * @param term
	 * @return true if term is mail operator (possibly inside a not), else
	 *         false.
	 */
	private boolean isMailOperator(jpl.Term term) {
		String signature = JPLUtils.getSignature(term);
		if (signature.equals("not/1")) {
			return isMailOperator(term.arg(1));
		}
		return (signature.equals("sent/2") || signature.equals("received/2"));
	}

	@Override
	public Database makeDatabase(BASETYPE type,
			Collection<DatabaseFormula> theory, AgentProgram agent)
					throws KRInitFailedException, KRDatabaseException,
					KRQueryFailedException {
		if (agent == null) {
			throw new NullPointerException("agent=null");
		}
		final String name = agent.getSourceFile().getName();
		if (!this.dynamicDeclarationsForBeliefBase.containsKey(name)) {
			Set<jpl.Term> kbCalls = new LinkedHashSet<>();
			Set<jpl.Term> dynDecl = new LinkedHashSet<>();
			Set<jpl.Term> check = new LinkedHashSet<>();
			Set<jpl.Term> bbDecl, kbDecl;

			// GOAL reserved predicates that should not be declared dynamic.
			// Represent as strings as Term does not implement hashcode/equal
			// methods...
			this.reserved.add("/(percept,2)");
			this.reserved.add("/(percept,1)");
			this.reserved.add("/(received,2)");
			this.reserved.add("/(sent,2)");
			// dynDecl.removeAll(reserved);

			// ************ compute predicates for belief base **************/
			/*
			 * Add dynamic declarations to the belief base that do not occur in
			 * the belief base or knowledge sections. The predicates that need
			 * to be declared dynamically are those that occur in the adopt
			 * action, and conditions in action rules (e.g. conditions of the
			 * form bel(...), goal(...)), and those that occur in the
			 * precondition of action specifications. Note that the dynamic
			 * declarations are agent-specific. As the belief base is
			 * implemented by a single module in SWI Prolog that is created at
			 * 'compile time' this initialization has to be performed only once.
			 */
			// Add calls from bodies of clauses in the knowledge base
			kbCalls = getCalls(agent.getAllKnowledge());
			dynDecl.addAll(kbCalls);
			// Add calls from bodies of clauses in the belief base
			dynDecl.addAll(getCalls(agent.getAllBeliefs()));
			// Add predicates used in goals in the goal base
			for (Query goal : agent.getAllGoals()) {
				dynDecl.addAll(getDeclarations(goal.toUpdate().getAddList()));
			}
			// Add predicates used in conditions from action rules on the belief
			// base
			dynDecl.addAll(getBBConditionDeclarationsFromProgram(agent));
			// Add predicates used in a-goal or goal-a conditions from action
			// rules
			dynDecl.addAll(getGoalConditionsFromProgram(agent));
			// Add predicates that occur in preconditions of user-specified
			// actions
			dynDecl.addAll(getPreConditionDeclarations(agent
					.getAllActionSpecs()));
			// Add predicates that occur in the built-in actions adopt,
			// adoptOne,
			// drop TODO and sendOnce
			dynDecl.addAll(getDeclarationsFromProgram(agent));

			// declared predicates in belief base are covered, and can be
			// removed
			bbDecl = getDeclarations(agent.getAllBeliefs());
			// dynDecl.removeAll(bbDecl);
			// declared predicates in knowledge base are covered, and can be
			// removed
			kbDecl = getDeclarations(agent.getAllKnowledge());
			// dynDecl.removeAll(kbDecl);

			// Store results for belief base
			this.dynamicDeclarationsForBeliefBase.put(name, dynDecl);

			// check for name clashes
			check.addAll(bbDecl);
			check.retainAll(kbDecl);
			if (!check.isEmpty()) {
				throw new KRInitFailedException(
						"for agent "
								+ name
								+ " the belief section defines "
								+ check.toString().substring(1,
										check.toString().length() - 1)
										+ " which "
										+ (check.size() == 1 ? "has" : "have")
										+ " been defined in the knowledge section already.\n"
										+ "The SWI Prolog modules used would produce name clashes.");
			}
		}
		if (!this.dynamicDeclarationsForGoals.containsKey(name)) {
			Set<jpl.Term> kbCalls = new LinkedHashSet<>();
			Set<jpl.Term> dynDecl = new LinkedHashSet<>();
			Set<jpl.Term> kbDecl;
			// ************ compute predicates for goal base **************/
			/*
			 * Add dynamic declarations to the goal base that do not occur in
			 * the goal base or knowledge sections in the program. The
			 * predicates that need to be dynamically declared are those that
			 * occur in e.g. adopt and drop actions (i.e. all built-in actions
			 * that modify the goal base) and those that occur in
			 * goal-conditions in action rules (e.g. conditions of the form
			 * goal(...)). Note that the dynamic declarations are
			 * agent-specific. As for each goal that is adopted by the agent a
			 * new SWI Prolog database (module) is created at runtime, this
			 * initialization also needs to be performed at runtime when such a
			 * new module is created. To support this, a hash table is
			 * introduced from agent names to sets of predicates that need to be
			 * declared at runtime.
			 */
			// Add calls from bodies of clauses in the knowledge base
			kbCalls = getCalls(agent.getAllKnowledge());
			// Add calls from bodies of clauses in the knowledge base
			dynDecl.addAll(kbCalls);
			// Add calls from goal conditions that occur in action rules
			dynDecl.addAll(getGoalConditionsFromProgram(agent));
			// Add calls that result from built-in adopt, drop, and TODO
			// adoptOne
			// actions.
			dynDecl.addAll(getDeclarationsFromProgram(agent));
			/*
			 * CHECK Also add the predicates used in the goal bases. Adding this
			 * fixes 'undefined predicate' warnings when querying an unused goal
			 * that _is_ defined in a goalbase
			 */
			for (Query goal : agent.getAllGoals()) {
				dynDecl.addAll(getDeclarations(goal.toUpdate().getAddList()));
			}

			// declared predicates in knowledge base are covered, and can be
			// removed
			kbDecl = getDeclarations(agent.getAllKnowledge());
			dynDecl.removeAll(kbDecl);

			// Store results for goal base
			this.dynamicDeclarationsForGoals.put(name, dynDecl);
		}

		// Check whether an attempt is made to create multiple databases of the
		// same type
		// of database for name. This is only allowed for goal bases.
		TypedSWIPrologDatabase found = null;
		if (this.databases.containsKey(name)) {
			for (TypedSWIPrologDatabase database : this.databases.get(name)) {
				if (database.getType() == type) {
					found = database;
					break;
				}
			}
		}
		if (!type.equals(BASETYPE.GOALBASE) && found != null) {
			throw new KRInitFailedException("attempt to add second " + type);
		}

		// TODO: HACKY way to do this... but we need access to the
		// content of the knowledge base later somehow.
		if (type.equals(BASETYPE.KNOWLEDGEBASE)) {
			this.knowledge.put(name, theory);
		}

		// Create new database of given type, content;
		// use name as base name for name of database.
		TypedSWIPrologDatabase database = new TypedSWIPrologDatabase(this,
				type, theory, name,
				this.dynamicDeclarationsForBeliefBase.get(name),
				this.dynamicDeclarationsForGoals.get(name));
		// Add database to list of databases maintained by SWI Prolog and
		// associated with name.
		if (this.databases.containsKey(name)) {
			this.databases.get(name).add(database);
		} else {
			// Initialize list of databases for name.
			Set<TypedSWIPrologDatabase> databaselist = new HashSet<>();
			databaselist.add(database);
			this.databases.put(name, databaselist);
		}
		// Return new database.
		return database;
	}

	/**
	 * Internal use. Get the knowledge of some agent.
	 *
	 * @param agentname
	 *            name of the agent owning the knowledge
	 * @return set of {@link DatabaseFormula}s with the knowledge.
	 */
	public Collection<DatabaseFormula> getKnowledge(String agentname) {
		return this.knowledge.get(agentname);
	}

	/**
	 * Returns a database of a particular type associated with a given agent.
	 * <p>
	 * <b>Warning</b>: Cannot be used to get goal bases because in contrast with
	 * all other types of databases an agent can have multiple databases that
	 * are used for storing goals.
	 * </p>
	 *
	 * @param agent
	 *            The name of an agent.
	 * @param type
	 *            The type that is requested.
	 * @returns The database associated with a given agent of a given type, or
	 *          {@code null} if no database of the given type exists.
	 */
	public TypedSWIPrologDatabase getDatabase(String agent, BASETYPE type) {
		if (this.databases.containsKey(agent)) {
			for (TypedSWIPrologDatabase database : this.databases.get(agent)) {
				if (database.getType() == type) {
					return database;
				}
			}
		}
		return null;
	}

	/**
	 * @author KH Jul08 DECLARATION HANDLING: code to extract info to perform
	 *         right imports/exports and introduction of dynamic predicates for
	 *         SWI-Prolog databases (modules). CHECK Also includes code to check
	 *         that predicates introduced into knowledge base are never updated
	 *         (asserted or retracted) in a given belief or goal base.
	 *
	 */
	private Set<jpl.Term> getDeclarations(Iterable<DatabaseFormula> formulae) {
		Set<jpl.Term> declarations = new LinkedHashSet<>();
		for (DatabaseFormula formula : formulae) {
			jpl.Term term = ((PrologDBFormula) formula).getTerm();
			declarations.addAll(getDeclarationNames(term));
		}
		return declarations;
	}

	/**
	 * <p>
	 * Finds signature of <br>
	 * (1) the head of term, if term is clause (i.e. has the form head:-body)<br>
	 * (2) declaration names of all sub-terms, if term is conjunction<br>
	 * (3) the term itself, if term is not built-in<br>
	 * </p>
	 *
	 * @return signatures of functions occurring in the parts of the term.
	 *         returns empty list if term is built-in prolog function.
	 */
	private Set<jpl.Term> getDeclarationNames(jpl.Term term) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		String name = term.name();
		int arity = term.arity();

		// TODO: make code below more robust, add checks
		// ASSUMES formula is either a clause (main operator = ':-'),
		// conjunction (main operator = ','), or predicate
		if (name.equals(":-")) { // clause, get head of clause
			names.addAll(getDeclarationNames((((Compound) term).arg(1))));
		} else if (name.equals(",")) { // conjunction
			names.addAll(getDeclarationNames((((Compound) term).arg(1))));
			names.addAll(getDeclarationNames((((Compound) term).arg(2))));
		} else if (!PrologOperators.prologBuiltin(term.name())
				&& !this.reserved.contains("/(" + term.name() + "," + arity
						+ ")")) { // predicate
			names.add(JPLUtils.createCompound("/", new Atom(name),
					new jpl.Integer(arity)));
		}
		return names;
	}

	/**
	 * Finds all the "calls" that are made inside a set of clauses. Here, a
	 * functional view is taken on Prolog. It sees certain predicates such as :-
	 * and forall as function definitions that can call other functions.
	 *
	 * @param formulae
	 *            is a list of clauses and other DatabaseFormulas
	 * @return set of all {@link #getCallNames} of the body of clauses (a clause
	 *         is a formula of the form head:-body) Non-clauses in the formulae
	 *         list are ignored.
	 */
	private Set<jpl.Term> getCalls(Iterable<DatabaseFormula> formulae) {
		Set<jpl.Term> calls = new LinkedHashSet<>();
		for (DatabaseFormula formula : formulae) {
			jpl.Term term = ((PrologDBFormula) formula).getTerm();
			// Only add names of calls in body of clause.
			if (term.name().equals(":-") && term.arity() == 2) {
				calls.addAll(getCallNames(term.arg(2)));
			}
		}
		return calls;
	}

	/**
	 * <p>
	 * This returns the "called functions" inside a body of a clause. Note,
	 * apparently ':-' inside a <em>clause</em> is considered a fucntion call
	 * too.
	 * </p>
	 *
	 * @return list of signatures of called functions
	 */
	private Set<jpl.Term> getCallNames(jpl.Term term) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		String name;
		int arity;

		if (term == null) {
			return names; // empty, typically an empty conjunct
		}

		// Get name of main operator.
		name = term.name();
		arity = term.arity();

		// below all Prolog operators (see method prologBuiltin) should be
		// handled that give rise to additional Prolog calls when the operator
		// is evaluated.

		// CHECK clause inside clause?
		if (name.equals(":-") && arity == 2) {
			// clause, get names in body
			names.addAll(getCallNames(term.arg(2)));
		} else if (name.equals("not") && arity == 1) {
			// negation, get label of argument
			names.addAll(getCallNames(term.arg(1)));
		} else if (name.equals(";") && arity == 2) {
			// disjunction
			names.addAll(getCallNames(term.arg(1)));
			names.addAll(getCallNames(term.arg(2)));
		} else if (name.equals(",") && arity == 2) {
			// conjunction
			names.addAll(getCallNames(term.arg(1)));
			names.addAll(getCallNames(term.arg(2)));
		} // Do NOT add the cases below to getDeclarationNames.
		else if (name.equals("forall") && arity == 2) {
			// forall quantifier
			names.addAll(getCallNames(term.arg(1)));
			names.addAll(getCallNames(term.arg(2)));
		} else if ((name.equals("findall") || name.equals("setof"))
				&& arity == 3) {
			// findall, setof operator
			names.addAll(getCallNames(term.arg(2)));
		} else if (name.equals("aggregate") || name.equals("aggregate_all")) {
			// aggregate operators, SWI Prolog specific (not part of ISO
			// standard)
			if (arity == 3) {
				names.addAll(getCallNames(term.arg(2)));
			} else if (arity == 4) {
				// 4 arguments
				names.addAll(getCallNames(term.arg(3)));
			}
		} else if (name.equals("include") && arity == 3) {
			// special. First element will be called, but with 1 argument added.
			jpl.Term stubfunc = new Compound(term.arg(1).name(),
					new jpl.Term[] { ANON_VAR });
			names.add(stubfunc);
		} else if (name.equals("predsort") && arity == 3) {
			// special. First element will be called, but with 3 arguments
			// added.
			jpl.Term stubfunc = new Compound(term.arg(1).name(),
					new jpl.Term[] { ANON_VAR, ANON_VAR, ANON_VAR });
			names.add(stubfunc);
		} else {
			// predicate
			if (!PrologOperators.prologBuiltin(name + "/" + arity)
					&& !this.reserved.contains("/(" + name + "," + arity + ")")) {
				names.add(JPLUtils.createCompound("/", new Atom(name),
						new jpl.Integer(arity)));
			}
		}
		return names;
	}

	private Set<jpl.Term> getDeclarationsFromProgram(AgentProgram program) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		for (Module module : program.getModules()) {
			if (module.getRules() != null) {
				names.addAll(getDeclarationsFromRules(module.getRules()));
			}
		}
		return names;
	}

	/**
	 * Get all declarations from the actions in the rules. These actions can
	 * create new terms in the databases.
	 *
	 * @param rules
	 *            the rules that contain
	 *            {@link languageTools.program.agent.actions.Action}s.
	 * @return set of {@link jpl.Term}s that can be created by these rules.
	 */
	private Set<jpl.Term> getDeclarationsFromRules(List<Rule> rules) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		for (Rule rule : rules) {
			ActionCombo acts = rule.getAction();
			for (languageTools.program.agent.actions.Action<?> act : acts) {
				if (act instanceof AdoptAction) {
					Update u = ((AdoptAction) act).getUpdate();
					names.addAll(getCallNames(((PrologUpdate) u).getTerm()));
				} else if (act instanceof DropAction) {
					Update u = ((DropAction) act).getUpdate();
					names.addAll(getCallNames(((PrologUpdate) u).getTerm()));
				} else if (act instanceof InsertAction) {
					Update u = ((InsertAction) act).getUpdate();
					names.addAll(getCallNames(((PrologUpdate) u).getTerm()));
				} else if (act instanceof DeleteAction) {
					Update u = ((DeleteAction) act).getUpdate();
					names.addAll(getCallNames(((PrologUpdate) u).getTerm()));
				}
				// The sent predicate is already handled elsewhere.

				/*
				 * #3468 Even though the Insert and Delete themselves do not
				 * cause query calls and therefore do not really need to declare
				 * them at this point to run the program, it may be that the
				 * test framework needs these terms
				 */
			}
		}
		return names;
	}

	private Set<jpl.Term> getBBConditionDeclarationsFromProgram(
			AgentProgram program) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		for (Module module : program.getModules()) {
			if (module.getRules() != null) {
				names.addAll(getBBConditionDeclarationsFromRules(module
						.getRules()));
			}
		}
		return names;
	}

	private Set<jpl.Term> getBBConditionDeclarationsFromRules(List<Rule> rules) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		for (Rule rule : rules) {
			// ASSUMES grammar does not allow mental state conditions that have
			// arguments containing clauses (':-')
			for (MentalLiteral lit : rule.getCondition().getAllLiterals()) {
				if (lit instanceof BelLiteral) {
					Query q = lit.getFormula();
					names.addAll(getCallNames(((PrologQuery) q).getTerm()));
				}
			}
		}
		return names;
	}

	private Set<jpl.Term> getGoalConditionsFromProgram(AgentProgram program) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		for (Module module : program.getModules()) {
			if (module.getRules() != null) {
				names.addAll(getGoalConditionsFromRules(module.getRules()));
			}
		}
		return names;
	}

	private Set<jpl.Term> getGoalConditionsFromRules(List<Rule> rules) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		for (Rule rule : rules) {
			for (MentalLiteral lit : rule.getCondition().getAllLiterals()) {
				if (!(lit instanceof BelLiteral)) {
					Query q = lit.getFormula();
					names.addAll(getCallNames(((PrologQuery) q).getTerm()));
				}
			}
		}
		return names;
	}

	private Set<jpl.Term> getPreConditionDeclarations(
			Iterable<ActionSpecification> actionSpecifications) {
		Set<jpl.Term> names = new LinkedHashSet<>();
		for (ActionSpecification rule : actionSpecifications) {
			for (MentalLiteral lit : rule.getPreCondition().getAllLiterals()) {
				Query q = lit.getFormula();
				names.addAll(getCallNames(((PrologQuery) q).getTerm()));
			}
		}
		return names;
	}

	@Override
	public Collection<String> getReceiversOfMessage(Database database,
			Message message) throws KRQueryFailedException {
		TypedSWIPrologDatabase swidb = (TypedSWIPrologDatabase) database;
		Variable recipient = new Variable("Recipient");
		jpl.Term msg = ((PrologDBFormula) message.getContent()).getTerm();
		jpl.Term sent = JPLUtils.createCompound("sent", recipient, msg);
		jpl.Term db_sent = JPLUtils.createCompound(":", swidb.getJPLName(),
				sent);

		Set<PrologSubstitution> results = SWIPrologDatabase.rawquery(db_sent);
		Set<String> names = new LinkedHashSet<>();
		for (Substitution subst : results) {
			for (Var var : subst.getVariables()) {
				if (((Variable) var).name().equals(recipient.name())) {
					names.add(subst.get(var).toString());
				}
			}
		}
		return names;
	}

	@Override
	public Set<DatabaseFormula> insert(Database database, Message message,
			boolean received) throws KRDatabaseException {
		TypedSWIPrologDatabase swidb = (TypedSWIPrologDatabase) database;
		jpl.Term msg = ((PrologUpdate) message.getContent()).getTerm();
		switch (message.getMood()) {
		case IMPERATIVE:
			msg = JPLUtils.createCompound("imp", msg);
			break;
		case INDICATIVE:
			// we do not add a mood modifier for indicatives.
			break;
		case INTERROGATIVE:
			msg = JPLUtils.createCompound("int", msg);
			break;
		}

		Set<DatabaseFormula> updates = new HashSet<DatabaseFormula>();
		if (received) {
			jpl.Term sender = new jpl.Atom(message.getSender().getName());
			jpl.Term fact = JPLUtils.createCompound("received", sender, msg);
			swidb.insert(fact);
			updates.add(new PrologDBFormula(fact, null));
		} else {
			// sent
			for (AgentId id : message.getReceivers()) {
				jpl.Term sender = new jpl.Atom(id.getName());
				jpl.Term fact = JPLUtils.createCompound("sent", sender, msg);
				swidb.insert(fact);
				updates.add(new PrologDBFormula(fact, null));
			}
		}
		return updates;
	}

	@Override
	public DatabaseFormula insert(Database database, Percept percept)
			throws KRDatabaseException {
		TypedSWIPrologDatabase swidb = (TypedSWIPrologDatabase) database;
		jpl.Term db_percept = JPLUtils.createCompound("percept",
				perceptToTerm(percept));
		swidb.insert(db_percept);
		return new PrologDBFormula(db_percept, null);
	}

	@Override
	public DatabaseFormula delete(Database database, Percept percept)
			throws KRDatabaseException {
		TypedSWIPrologDatabase swidb = (TypedSWIPrologDatabase) database;
		jpl.Term db_percept = JPLUtils.createCompound("percept",
				perceptToTerm(percept));
		swidb.delete(db_percept);
		return new PrologDBFormula(db_percept, null);
	}

	/**
	 * Translates an EIS percept into a JPL term.
	 *
	 * @param percept
	 *            The EIS percept to be translated.
	 * @return A JPL term translation of the percept.
	 */
	private jpl.Term perceptToTerm(Percept percept) {
		// Get main operator name and parameters of the percept.
		String name = percept.getName();
		List<Parameter> parameters = percept.getParameters();
		// Construct a JPL term from the percept operator and parameters.
		jpl.Term term;
		if (parameters.size() == 0) {
			term = new jpl.Atom(name);
		} else {
			List<jpl.Term> terms = new ArrayList<>(parameters.size());
			for (Parameter parameter : parameters) {
				PrologTerm add = (PrologTerm) convert(parameter);
				terms.add(add.getTerm());
			}
			term = new Compound(name, terms.toArray(new jpl.Term[0]));
		}
		return term;
	}

	@Override
	public Set<DatabaseFormula> updateAgentFact(Database database,
			boolean insert, AgentId id, boolean me) throws KRDatabaseException {
		TypedSWIPrologDatabase swidb = (TypedSWIPrologDatabase) database;
		Set<DatabaseFormula> updates = new HashSet<>();
		// Turn name into JPL term and create agent fact.
		PrologTerm prolog = (PrologTerm) convert(new Identifier(id.getName()));
		jpl.Term[] arg = { prolog.getTerm() };
		jpl.Term term = JPLUtils.createCompound("agent", arg);
		// Insert or delete agent fact.
		if (insert) {
			swidb.insert(term);
		} else {
			swidb.delete(term);
		}
		updates.add(new PrologDBFormula(term, null));
		if (me) {
			term = JPLUtils.createCompound("me", arg);
			if (insert) {
				// Insert me fact.
				swidb.insert(term);
			} else {
				// Delete me fact.
				swidb.delete(term);
			}
			updates.add(new PrologDBFormula(new Compound("me", arg), null));
		}
		return updates;
	}

	@Override
	public Update convert(Message message, boolean isSent, AgentId receiver) {
		jpl.Term term = JPLUtils.createCompound((isSent ? "sent" : "received"),
				new Atom(receiver.getName()), convert(message));
		return new PrologUpdate(term, message.getContent().getSourceInfo());
	}

	/**
	 * returns imp(message.getContent) or int(message.getContent) depending on
	 * message.mood.
	 *
	 * @param message
	 *            a {@link Message}
	 * @return jpl.Term containing converted Message.
	 */
	private jpl.Term convert(Message message) {
		jpl.Term term = ((PrologUpdate) message.getContent()).getTerm();
		switch (message.getMood()) {
		case IMPERATIVE:
			term = JPLUtils.createCompound("imp", term);
			break;
		case INTERROGATIVE:
			term = JPLUtils.createCompound("int", term);
			break;
		default:
			break;
		}
		return term;
	}

	@Override
	public DependencyGraph<?> createDependencyGraph() {
		return new SwiDependencyGraph();
	}
}
