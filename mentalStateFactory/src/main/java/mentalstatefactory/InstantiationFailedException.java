package mentalstatefactory;

/**
 * Thrown when we failed to create an instance
 * 
 * @author W.Pasman jan'15. Notice, {@link InstantiationException} is very
 *         similar but it does not support exception chaining.
 *
 */
@SuppressWarnings("serial")
public class InstantiationFailedException extends Exception {

	public InstantiationFailedException(String mess) {
		super(mess);
	}

	public InstantiationFailedException(String mess, Exception err) {
		super(mess, err);
	}
}
