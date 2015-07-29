package joints2;

import java.io.Serializable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * @author codistmonk (creation 2015-07-28)
 */
public abstract interface JL extends Serializable {
	
	public abstract void enable(Constant flag);
	
	public abstract void disable(Constant flag);
	
	public abstract void viewport(int x, int y, int w, int h);
	
	public abstract void scissor(int x, int y, int w, int h);
	
	/**
	 * @author codistmonk (creation 2015-07-28)
	 */
	public static enum Constant {
		
		SCISSOR_TEST;
		
		public static final Point3f ORIGIN = new Point3f();
		
		public static final Vector3f ZERO = new Vector3f();
		
		public static final Vector3f UNIT_X = new Vector3f(+1.0F, +0.0F, +0.0F);
		
		public static final Vector3f UNIT_Y = new Vector3f(+0.0F, +1.0F, +0.0F);
		
		public static final Vector3f UNIT_Z = new Vector3f(+0.0F, +0.0F, +1.0F);
		
	}
	
}
