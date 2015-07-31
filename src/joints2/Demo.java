package joints2;

import static javax.swing.SwingUtilities.invokeLater;
import static multij.swing.SwingTools.show;
import static multij.swing.SwingTools.useSystemLookAndFeel;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-28)
 */
public final class Demo {
	
	private Demo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		useSystemLookAndFeel();
		invokeLater(() -> show(new JointsEditorPanel(), Demo.class.getName(), false));
	}
	
}
