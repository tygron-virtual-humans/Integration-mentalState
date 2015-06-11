package swiPrologMentalState;

import java.util.Collection;
import java.util.Set;

import jpl.Atom;
import jpl.Term;
import jpl.Variable;
import krTools.errors.exceptions.KRDatabaseException;
import krTools.errors.exceptions.KRInitFailedException;
import krTools.errors.exceptions.KRQueryFailedException;
import krTools.language.DatabaseFormula;
import mentalState.BASETYPE;
import swiprolog.database.SWIPrologDatabase;
import swiprolog.language.JPLUtils;

public class TypedSWIPrologDatabase extends SWIPrologDatabase {
	private final SwiPrologMentalState state;
	/**
	 * The name of the agent that owns this database.
	 */
	private final String owner;
	/**
	 * Type of the database, e.g., belief, knowledge, ... base.
	 */
	private final BASETYPE type;

	/**
	 * @param databaseType
	 *            the database type to be made
	 * @param content
	 *            the theory containing initial database contents
	 * @param name
	 *            the name of base (unique name)
	 * @throws KRInitFailedException
	 *             If database creation failed.
	 * @throws KRDatabaseException
	 * @throws KRQueryFailedException
	 */
	public TypedSWIPrologDatabase(SwiPrologMentalState state,
			BASETYPE databaseType, Collection<DatabaseFormula> content,
			String name, Set<Term> dynamicBeliefs, Set<Term> dynamicGoals)
			throws KRInitFailedException, KRDatabaseException,
			KRQueryFailedException {
		super(content);
		// Used for initialization purposes; enables to keep track of related
		// databases that make up the mental state of an agent.
		this.owner = name;
		this.type = databaseType;
		this.state = state;
		System.out.println("PRINTING LDJL:SJDFLSJDFLJSDLFJLS:DJF:LJSDFLJ " + this.type);

		if (!this.type.equals(BASETYPE.KNOWLEDGEBASE)) {
			// Create SWI Prolog module that will act as our database.
			// synchronized(this) {
			rawquery(JPLUtils.createCompound(":", getJPLName(),
					new Atom("true")));

			// FIXME: SWI Prolog needs access to various libaries at runtime and
			// loads these
			// dynamically; if many agents try to do this at the same time this
			// gives access
			// errors ('No permission to load'). We can now decide to fix this
			// by loading these
			// libraries upfront when we need them (that implies work to check
			// whether we need
			// a library of course). Benefit is that we ONLY need to synchronize
			// creating of
			// databases (this code) and NOT all calls to rawquery... We should
			// check whether
			// this impacts performance or not.
			// FOr now, solved this issue by adding synchronized modifier to
			// rawquery.

			// EXAMPLE BELOW: only loads lists.pl but no other libraries.
			// Term loadlists = JPLUtils.createCompound("ensure_loaded", new
			// Atom("c:/program files/pl/library/lists.pl"));
			// SWIQuery.rawquery(JPLUtils.createCompound(":", this.name,
			// loadlists));
			// }
		}

		// Create an anonymous variable.
		Variable anonymousVar = new Variable("_");
		// Variable anonymousVarE = new Variable("_");

		switch (this.type) {
		case KNOWLEDGEBASE:
			// Nothing to do: we copy the knowledge into goal- and
			// belief bases. This also avoids the need to declare predicates
			// dynamic in a non-used knowledge base, see TRAC #2109.
			// We only check that we're not adding a second knowledge base.
			break;
		case BELIEFBASE:
			declareDynamic(dynamicBeliefs);
			// Add initial content to database.
			add(content);
			// Add content from knowledge base.
			add(getKnowledgeBaseContent());
			// Import percept predicate into belief base, if percept base
			// exists.
			importPerceptsIntoBB();
			importEmotionsIntoBB();
			// Import received and sent predicates into belief base, if mailbox
			// exists.
			importMailsInBB();
			break;
		case GOALBASE:
			declareDynamic(dynamicGoals);
			// Add initial content to database.
			add(content);
			// Add content from knowledge base.
			add(getKnowledgeBaseContent());
			break;
		case MAILBOX:
			Term received = JPLUtils.createCompound("received", anonymousVar,
					anonymousVar);
			Term db_received = JPLUtils.createCompound(":", getJPLName(),
					received);
			Term export_received = JPLUtils.createCompound("export",
					db_received);
			Term sent = JPLUtils.createCompound("sent", anonymousVar,
					anonymousVar);
			Term db_sent = JPLUtils.createCompound(":", getJPLName(), sent);
			Term export_sent = JPLUtils.createCompound("export", db_sent);

			try {
				rawquery(JPLUtils.createCompound("dynamic", db_received));
				rawquery(JPLUtils.createCompound("dynamic", db_sent));
				rawquery(JPLUtils.createCompound(":", getJPLName(),
						export_received));
				rawquery(JPLUtils
						.createCompound(":", getJPLName(), export_sent));
			} catch (KRQueryFailedException e) {
				throw new KRInitFailedException("initialization of the mail"
						+ "box of agent " + this.owner + "failed", e);
			}
			// Ignore initial content; mailbox is empty initially.
			// Import received and sent predicates into belief base, if it
			// exists.
			importMailsInBB();
			break;
		case EMOTIONBASE:
			// emotions
			Term emotion = JPLUtils.createCompound("emotion", anonymousVar);
			Term db_emotion = JPLUtils.createCompound(":", getJPLName(),
					emotion);
			Term export_emotion = JPLUtils.createCompound("export", db_emotion);
			Term emotion2 = JPLUtils.createCompound("emotion", anonymousVar,
					anonymousVar);
			Term db_emotion2 = JPLUtils.createCompound(":", getJPLName(),
					emotion2);
			Term export_emotion2 = JPLUtils.createCompound("export",
					db_emotion2);

			rawquery(JPLUtils.createCompound("dynamic", db_emotion));
			rawquery(JPLUtils.createCompound(":", getJPLName(), export_emotion));
			rawquery(JPLUtils.createCompound("dynamic", db_emotion2));
			rawquery(JPLUtils
					.createCompound(":", getJPLName(), export_emotion2));
						
			// Ignore initial content; emotion base is empty initially.
			// Import emotion predicate into belief base, if it exists.
			importEmotionsIntoBB();
			break;
		case PERCEPTBASE:
			Term percept = JPLUtils.createCompound("percept", anonymousVar);
			Term db_percept = JPLUtils.createCompound(":", getJPLName(),
					percept);
			Term export_percept = JPLUtils.createCompound("export", db_percept);
			Term percept2 = JPLUtils.createCompound("percept", anonymousVar,
					anonymousVar);
			Term db_percept2 = JPLUtils.createCompound(":", getJPLName(),
					percept2);
			Term export_percept2 = JPLUtils.createCompound("export",
					db_percept2);

			rawquery(JPLUtils.createCompound("dynamic", db_percept));
			rawquery(JPLUtils.createCompound(":", getJPLName(), export_percept));
			rawquery(JPLUtils.createCompound("dynamic", db_percept2));
			rawquery(JPLUtils
					.createCompound(":", getJPLName(), export_percept2));
			
			
			// Ignore initial content; percept base is empty initially.
			// Import percept predicate into belief base, if it exists.
			importPerceptsIntoBB();
			break;
		}
	}

	/**
	 * Returns the name of the agent owner of this database.
	 *
	 * @return The name of the agent that owns this database.
	 */
	public String getOwner() {
		return this.owner;
	}

	/**
	 * Returns the type of the database. Types represent different purposes that
	 * a database may be used for, i.e. representing percepts, mail messages,
	 * knowledge, beliefs, goals, and other agent's beliefs and goals.
	 *
	 * @return the {@link BASETYPE} of this database.
	 */
	public BASETYPE getType() {
		return this.type;
	}

	/**
	 * Get knowledge base content (should be done for belief and goal bases).
	 *
	 * @throws KRInitFailedException
	 *             If knowledge base has not been created yet.
	 */
	private Collection<DatabaseFormula> getKnowledgeBaseContent()
			throws KRInitFailedException {
		Collection<DatabaseFormula> knowledge = this.state
				.getKnowledge(this.owner);
		// A knowledge base must already have been created; get its content.
		if (knowledge == null) {
			throw new IllegalStateException("attempt to create belief or goal "
					+ "base before knowledge base has been created");
		}
		return knowledge;
	}

	/**
	 * DOC
	 *
	 * @throws KRInitFailedException
	 * @throws KRQueryFailedException
	 */
	private void importPerceptsIntoBB() throws KRInitFailedException,
			KRQueryFailedException {
		SWIPrologDatabase beliefbase, perceptbase;
		switch (this.type) {
		case BELIEFBASE:
			beliefbase = this;
			perceptbase = this.state.getDatabase(this.owner,
					BASETYPE.PERCEPTBASE);
			// If percept base does not yet exist, then nothing to do; return.
			if (perceptbase == null) {
				return;
			}
			break;
		case PERCEPTBASE:
			perceptbase = this;
			beliefbase = this.state
					.getDatabase(this.owner, BASETYPE.BELIEFBASE);
			// If belief base does not yet exist, then nothing to do; return.
			if (beliefbase == null) {
				return;
			}
			break;
		default:
			throw new UnsupportedOperationException(
					"can not import percepts from " + this.type);
		}

		// Create an anonymous variable.
		Variable anonymousVar = new Variable("_");

		Term percept = JPLUtils.createCompound("percept", anonymousVar,
				anonymousVar);
		Term pb_percept = JPLUtils.createCompound(":",
				perceptbase.getJPLName(), percept);
		Term import_percept = JPLUtils.createCompound("import", pb_percept);
		Term bb_import_percept = JPLUtils.createCompound(":",
				beliefbase.getJPLName(), import_percept);
		//

		Term percept1 = JPLUtils.createCompound("percept", anonymousVar);
		Term pb_percept1 = JPLUtils.createCompound(":",
				perceptbase.getJPLName(), percept1);
		Term import_percept1 = JPLUtils.createCompound("import", pb_percept1);
		Term bb_import_percept1 = JPLUtils.createCompound(":",
				beliefbase.getJPLName(), import_percept1);
		
		//

		rawquery(JPLUtils.createCompound(",", new Atom("true"),
				bb_import_percept));
		rawquery(JPLUtils.createCompound(",", new Atom("true"),
				bb_import_percept1));

	}
	
	/**
	 * DOC
	 *
	 * @throws KRInitFailedException
	 * @throws KRQueryFailedException
	 */
	private void importEmotionsIntoBB() throws KRInitFailedException,
			KRQueryFailedException {
		SWIPrologDatabase beliefbase, emotionbase;
		switch (this.type) {
		case BELIEFBASE:
			beliefbase = this;
			emotionbase = this.state.getDatabase(this.owner, 
					BASETYPE.EMOTIONBASE);
			
			// If emotion base does not yet exist, then nothing to do; return.
			if (emotionbase == null) {
				return;
			}
			break;
		case EMOTIONBASE:
			emotionbase = this;
			beliefbase = this.state
					.getDatabase(this.owner, BASETYPE.BELIEFBASE);
			// If belief base does not yet exist, then nothing to do; return.
			if (beliefbase == null) {
				return;
			}
			break;
		default:
			throw new UnsupportedOperationException(
					"can not import percepts from " + this.type);
		}

		// emotion
		Variable anonymousVar = new Variable("_");

		Term percept = JPLUtils.createCompound("emotion", anonymousVar,
				anonymousVar);
		Term pb_percept = JPLUtils.createCompound(":",
				emotionbase.getJPLName(), percept);
		Term import_percept = JPLUtils.createCompound("import", pb_percept);
		Term bb_import_percept = JPLUtils.createCompound(":",
				beliefbase.getJPLName(), import_percept);
			
		// emotion
		Term percept1 = JPLUtils.createCompound("emotion", anonymousVar);
		Term pb_percept1 = JPLUtils.createCompound(":",
				emotionbase.getJPLName(), percept1);
		Term import_percept1 = JPLUtils.createCompound("import", pb_percept1);
		Term bb_import_percept1 = JPLUtils.createCompound(":",
				beliefbase.getJPLName(), import_percept1);
		
		// emotion
		rawquery(JPLUtils.createCompound(",", new Atom("true"),
				bb_import_percept));
		rawquery(JPLUtils.createCompound(",", new Atom("true"),
				bb_import_percept1));
	}

	/**
	 * DOC
	 *
	 * @throws KRInitFailedException
	 * @throws KRQueryFailedException
	 */
	private void importMailsInBB() throws KRInitFailedException,
			KRQueryFailedException {
		SWIPrologDatabase beliefbase, mailbox;
		switch (this.type) {
		case BELIEFBASE:
			beliefbase = this;
			mailbox = this.state.getDatabase(this.owner, BASETYPE.MAILBOX);
			// If mail box does not yet exist, then nothing to do; return.
			if (mailbox == null) {
				return;
			}
			break;
		case MAILBOX:
			mailbox = this;
			beliefbase = this.state
					.getDatabase(this.owner, BASETYPE.BELIEFBASE);
			// If belief base does not yet exist, then nothing to do; return.
			if (beliefbase == null) {
				return;
			}
			break;
		default:
			throw new UnsupportedOperationException(
					"cannot import messages from " + this.type);
		}
		// Create an anonymous variable.
		Variable anonymousVar = new Variable("_");

		Term received = JPLUtils.createCompound("received", anonymousVar,
				anonymousVar);
		Term mailbox_received = JPLUtils.createCompound(":",
				mailbox.getJPLName(), received);
		Term import_received = JPLUtils.createCompound("import",
				mailbox_received);
		Term bb_import_received = JPLUtils.createCompound(":",
				beliefbase.getJPLName(), import_received);

		Term sent = JPLUtils.createCompound("sent", anonymousVar, anonymousVar);
		Term mailbox_sent = JPLUtils.createCompound(":", mailbox.getJPLName(),
				sent);
		Term import_sent = JPLUtils.createCompound("import", mailbox_sent);
		Term bb_import_sent = JPLUtils.createCompound(":",
				beliefbase.getJPLName(), import_sent);

		// Import received and sent predicate into belief base.
		rawquery(JPLUtils.createCompound(",", new Atom("true"),
				bb_import_received));
		rawquery(JPLUtils.createCompound(",", new Atom("true"), bb_import_sent));
	}

	/**
	 * Adds all content, i.e. the set of {@link DatabaseFormula}, to the
	 * database.
	 * 
	 * @throws KRDatabaseException
	 */
	private void add(Collection<DatabaseFormula> content)
			throws KRDatabaseException {
		for (DatabaseFormula formula : content) {
			insert((formula));
		}
	}

	/**
	 * Declares predicates as dynamic predicates in the database so they can be
	 * queried without introducing existence errors.
	 * 
	 * @throws KRQueryFailedException
	 */
	private void declareDynamic(Set<Term> dynamicDeclarations)
			throws KRQueryFailedException {
		for (Term term : dynamicDeclarations) {
			Term declaration = JPLUtils.createCompound(":", getJPLName(), term);
			rawquery(JPLUtils.createCompound("dynamic", declaration));
		}
	}
}
