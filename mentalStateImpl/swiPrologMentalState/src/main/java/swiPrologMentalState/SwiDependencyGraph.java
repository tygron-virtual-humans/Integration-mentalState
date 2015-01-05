/**
 * GOAL interpreter that facilitates developing and executing GOAL multi-agent
 * programs. Copyright (C) 2011 K.V. Hindriks, W. Pasman
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
import java.util.List;

import jpl.Compound;
import jpl.Term;
import krTools.errors.exceptions.KRDatabaseException;
import krTools.errors.exceptions.KRException;
import krTools.language.DatabaseFormula;
import krTools.language.Query;
import krTools.parser.SourceInfo;
import mentalState.DependencyGraph;
import mentalState.Node;
import swiprolog.language.PrologDBFormula;
import swiprolog.language.PrologQuery;
import swiprolog.language.PrologTerm;
import swiprolog.parser.PrologOperators;

/**
 * A {@link DependencyGraph} for the {@link SWIPrologLanguage}.
 *
 * @author K.Hindriks
 * @author W.Pasman modified to suit direct-on-jpl
 */
public class SwiDependencyGraph extends DependencyGraph<PrologTerm> {

	private static final Term ANON_VAR = new jpl.Variable("_");

	/**
	 * {@inheritDoc} <br>
	 *
	 * Assumes the given {@link DatabaseFormula} is a {@link PrologDBFormula},
	 * i.e. either a simple fact (i.e., a {@link PrologTerm}), or a clause of
	 * the form p(...):-(...) using the operator :-/2.
	 *
	 * @throws GOALUserError
	 */
	@Override
	public void add(DatabaseFormula formula, boolean defined, boolean queried)
			throws KRException {
		jpl.Term term = ((PrologDBFormula) formula).getTerm();
		String signature = term.name() + "/" + term.arity();

		/**
		 * The :- function needs to be treated differently from other terms; the
		 * head term is defined, whereas the terms in the body are queried.
		 */
		if (signature.equals(":-/2")) {
			if (defined) {
				// List<PrologTerm> args = ((FuncTerm) term).getArguments();
				// The first argument is the term that is being defined.
				List<Node<PrologTerm>> definitionNode = addTerm(term.arg(1),
						formula.getSourceInfo(), true, false);
				// The other argument consists of terms that are queried.
				List<Node<PrologTerm>> queryNodes = addTerm(term.arg(2),
						formula.getSourceInfo(), false, true);
				for (Node<PrologTerm> node : queryNodes) {
					definitionNode.get(0).addDependency(node);
				}
			}
			if (queried) {
				throw new KRDatabaseException(
						"A clause with main operator :-/2 cannot be queried.");
			}
		} else {
			if (reserved(signature) && defined) {
				throw new KRDatabaseException(
						"Attempt to redefine "
								+ signature
								+ "; Prolog built-in or reserved GOAL operators should not be redefined.");
			}
			addTerm(term, formula.getSourceInfo(), defined, queried);
		}
	}

	/**
	 *
	 */
	@Override
	public void add(Query query) throws KRException {
		jpl.Term term = ((PrologQuery) query).getTerm();

		if (term.name().equals(":-") && term.arity() == 2) {
			throw new KRDatabaseException(
					"A clause with main operator :-/2 cannot be queried.");
		} else {
			addTerm(term, query.getSourceInfo(), false, true);
		}
	}

	/**
	 * Creates nodes for terms, if not already present. Note that the ":-/2"
	 * operator is taken care of by
	 * {@link #add(DatabaseFormula, boolean, boolean)}.
	 *
	 * @param prologTerm
	 * @param defined
	 * @param queried
	 * @return The list of nodes associated with the term (either created or
	 *         already existing nodes).
	 */
	private List<Node<PrologTerm>> addTerm(jpl.Term prologTerm,
			SourceInfo source, boolean defined, boolean queried) {
		List<Node<PrologTerm>> nodes = new ArrayList<Node<PrologTerm>>();
		Node<PrologTerm> node;
		String signature;

		// Unpack the term if needed (if so, we're handling a query).
		List<jpl.Term> terms = unpack(prologTerm);

		for (jpl.Term term : terms) {
			signature = term.name() + "/" + term.arity();
			// Ignore built-in operators of Prolog as well as reserved GOAL
			// operators.
			if (!reserved(signature)) {
				node = super.graph.get(signature);
				if (node == null) {
					node = new Node<PrologTerm>(signature);
					super.graph.put(signature, node);
				}
				if (defined) {
					node.addDefinition(new PrologTerm(term, source));
				}
				if (queried) {
					node.addQuery(new PrologTerm(term, source));
				}
				nodes.add(node);
			}
		}
		return nodes;
	}

	/**
	 * Unpacks the given {@link PrologTerm} and returns all simple facts that do
	 * not have any occurrences of built-in Prolog operators or reserved GOAL
	 * operators.
	 * <p>
	 * Unpacking is needed if the term contains at top level a build-in
	 * predicate and that built-in predicate will cause further querying in the
	 * SWI engine. For example not(pred) will cause invocation of pred.
	 *
	 * @param term
	 *            The term that is unpacked.
	 * @return The resulting terms without any built-in or reserved operators.
	 *         May be the empty list.
	 */
	private List<jpl.Term> unpack(jpl.Term term) {
		String signature = term.name() + "/" + term.arity();
		List<jpl.Term> terms = new ArrayList<jpl.Term>();

		// If we need to unpack the operators below, we're dealing with a query.
		if (signature.equals("not/1")) {
			terms.addAll(unpack(term.arg(1)));
		} else if (signature.equals("include/3")) {
			/*
			 * special case. first argument of include/3 is the NAME of the func
			 * but without the required argument. We have to make up the correct
			 * term.
			 */
			// CHECK we assume here that arg is plain atom. What if not??
			jpl.Term stubfunc = new Compound(term.arg(1).name(),
					new jpl.Term[] { ANON_VAR });
			terms.add(stubfunc);
		} else if (signature.equals(";/2") || signature.equals(",/2")
				|| signature.equals("forall/2")) {
			// Unpack the conjunction, disjunction and forall /2-operators.
			for (jpl.Term argument : term.args()) {
				terms.addAll(unpack(argument));
			}
			// findall, setof aggregate and aggregate_all /3-operators only
			// have a query in the second argument.
		} else if (signature.equals("findall/3") || signature.equals("setof/3")
				|| signature.equals("aggregate/3")
				|| signature.equals("aggregate_all/3")) {
			terms.addAll(unpack(term.arg(2)));
			// aggregate and aggregate_all /4-operators have the query in
			// the third argument.
		} else if (signature.equals("aggregate/4")
				|| signature.equals("aggregate_all/4")) {
			terms.addAll(unpack(term.arg(3)));
		} else if (signature.equals("predsort/3")) {
			/*
			 * special case. first argument of predsort is the NAME of the func
			 * but without the required 3 arguments. We have to make up the
			 * correct term. We will be using 3 anonymous variables.
			 */
			// CHECK we assume here that arg is plain atom. What if not??
			jpl.Term stubfunc = new Compound(term.arg(1).name(),
					new jpl.Term[] { ANON_VAR, ANON_VAR, ANON_VAR });
			terms.add(stubfunc);
		} else {
			terms.add(term);
		}
		return terms;
	}

	/**
	 * Indicates whether the given signature represents a reserved (Prolog or
	 * GOAL) operator.
	 *
	 * @param signature
	 * @return {@code true} if the signature represents a reserved operator.
	 */
	private boolean reserved(String signature) {
		if (PrologOperators.prologBuiltin(signature)
				|| signature.equals("percept/1") || signature.equals("sent/2")
				|| signature.equals("received/2")
				|| signature.equals("agent/1") || signature.equals("me/1")) {
			return true;
		}
		return false;
	}
}
