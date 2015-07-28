package joints;

import static java.lang.Math.round;
import static javax.swing.SwingUtilities.invokeAndWait;
import static net.sourceforge.aprog.tools.Tools.gc;

import java.awt.Component;
import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-02-18)
 */
public abstract class Animator implements Serializable {
	
	private final Component component;
	
	private double frameRate;
	
	private long frameMilliseconds;
	
	public Animator(final Component component) {
		this.component = component;
		
	}
	
	public final double getFrameRate() {
		return this.frameRate;
	}
	
	public final Animator setFrameRate(final double frameRate) {
		this.frameRate = frameRate;
		this.frameMilliseconds = round(1000.0 / frameRate);
		
		return this;
	}
	
	public final long getFrameMilliseconds() {
		return this.frameMilliseconds;
	}
	
	public final Animator animate() {
		waitAWT();
		
		while (this.animateFrame()) {
			repaintAndWait(this.component);
			gc(this.getFrameMilliseconds());
		}
		
		repaintAndWait(this.component);
		
		return this;
	}
	
	protected abstract boolean animateFrame();
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -5561688092809450709L;
	
	public static final void repaintAndWait(final Component component) {
		component.repaint();
		waitAWT();
	}
	
	public static final void waitAWT() {
		try {
			invokeAndWait(new Runnable() {
				
				@Override
				public final void run() {
					// TODO Auto-generated method stub
					
				}
				
			});
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
	}
	
}
