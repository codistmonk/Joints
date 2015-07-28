package joints;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static multij.tools.MathTools.square;
import static multij.tools.Tools.cast;

import java.io.Serializable;

import multij.tools.Tools;

/**
 * @author codistmonk (creation 2014-03-17)
 */
public final class Constraint implements Serializable, Comparable<Constraint> {
	
	private final int index1;
	
	private final int index2;
	
	private double minimumDistance;
	
	private double maximumDistance;
	
	private double preferredDistance;
	
	private double strength;
	
	public Constraint(final int index1, final int index2) {
		this.index1 = min(index1, index2);
		this.index2 = max(index1, index2);
		this.minimumDistance = 0.0;
		this.maximumDistance = Double.POSITIVE_INFINITY;
		this.preferredDistance = Double.NaN;
		this.strength = 1.0;
	}
	
	public final double getMinimumDistance() {
		return this.minimumDistance;
	}
	
	public final double getPreferredDistance() {
		return this.preferredDistance;
	}
	
	public final Constraint setPreferredDistance(final double preferredDistance) {
		this.preferredDistance = preferredDistance;
		
		return this;
	}
	
	public final Constraint setClampedPreferredDistance(final double preferredDistance) {
		this.preferredDistance = max(this.getMinimumDistance(), min(preferredDistance, this.getMaximumDistance()));
		
		return this;
	}
	
	public final Constraint setMinimumDistance(final double minimumDistance) {
		this.minimumDistance = minimumDistance;
		
		return this;
	}
	
	public final double getMaximumDistance() {
		return this.maximumDistance;
	}
	
	public final Constraint setMaximumDistance(final double maximumDistance) {
		this.maximumDistance = maximumDistance;
		
		return this;
	}
	
	public final double getStrength() {
		return this.strength;
	}
	
	public final Constraint setStrength(final double strength) {
		this.strength = strength;
		
		return this;
	}
	
	public final int getIndex1() {
		return this.index1;
	}
	
	public final int getIndex2() {
		return this.index2;
	}
	
	public final double apply(final double[] locations, final double[] masses) {
		double result = 0.0;
		final double currentDistance = distance(locations, this.getIndex1(), this.getIndex2());
		final double targetDistance;
		
		if (!Double.isNaN(this.getPreferredDistance())) {
			result = abs(this.getPreferredDistance() - currentDistance);
			targetDistance = this.getPreferredDistance();
		} else if (currentDistance < this.getMinimumDistance()) {
			result = this.getMinimumDistance() - currentDistance;
			targetDistance = this.getMinimumDistance();
		} else if (this.getMaximumDistance() < currentDistance) {
			result = currentDistance - this.getMaximumDistance();
			targetDistance = this.getMaximumDistance();
		} else {
			return result;
		}
		
		updateLocations(locations, masses, this.getIndex1(), this.getIndex2(),
				currentDistance, lerp(currentDistance, targetDistance, this.getStrength()), this.getIndex1() + this.getIndex2());
		
		return result * this.getStrength();
	}
	
	@Override
	public final int hashCode() {
		return this.getIndex1() + this.getIndex2();
	}
	
	@Override
	public final boolean equals(final Object object) {
		final Constraint that = cast(this.getClass(), object);
		
		return that != null && this.getIndex1() == that.getIndex1() && this.getIndex2() == that.getIndex2();
	}
	
	@Override
	public final int compareTo(final Constraint that) {
		final int protoresult = this.getIndex1() - that.getIndex1();
		
		return protoresult != 0 ? protoresult : this.getIndex2() - that.getIndex2();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8278593491675731229L;
	
	/**
	 * {@value}.
	 */
	public static final int X = 0;
	
	/**
	 * {@value}.
	 */
	public static final int Y = 1;
	
	/**
	 * {@value}.
	 */
	public static final int Z = 2;
	
	public static final double applyExplicit(final Iterable<Constraint> constraints, final double[] locations, final double[] masses) {
		double result = 0.0;
		
		for (final Constraint constraint : constraints) {
			result = max(result, constraint.apply(locations, masses));
		}
		
		Tools.debugPrint(result);
		
		return result;
	}
	
	public static final double distance(final double[] locations, final int i1, final int i2) {
		final int offset1 = 3 * i1;
		final int offset2 = 3 * i2;
		
		return sqrt(square(locations[offset1 + X] - locations[offset2 + X]) +
				square(locations[offset1 + Y] - locations[offset2 + Y]) +
				square(locations[offset1 + Z] - locations[offset2 + Z]));
	}
	
	public static final void updateLocations(final double[] locations, final double[] masses, final int i1, final int i2, final double currentDistance,
			final double targetDistance, final double randomAngle) {
		final int offset1 = 3 * i1;
		final int offset2 = 3 * i2;
		final double m1 = masses[i1];
		final double m2 = masses[i2];
		
		if (Double.isInfinite(m1) && Double.isInfinite(m2)) {
			return;
		}
		
		final double x1 = locations[offset1 + X];
		final double y1 = locations[offset1 + Y];
		final double x2 = locations[offset2 + X];
		final double y2 = locations[offset2 + Y];
		final double m12 = m1 + m2;
		final double k1 = ratio(m1, m12);
		final double k2 = ratio(m2, m12);
		
		if (currentDistance != 0.0) {
			final double z1 = locations[offset1 + Z];
			final double z2 = locations[offset2 + Z];
			final double middleX = k1 * x1 + k2 * x2;
			final double middleY = k1 * y1 + k2 * y2;
			final double middleZ = k1 * z1 + k2 * z2;
			final double scale = targetDistance / currentDistance;
			
			locations[offset1 + X] = middleX + scale * (x1 - middleX);
			locations[offset1 + Y] = middleY + scale * (y1 - middleY);
			locations[offset1 + Z] = middleZ + scale * (z1 - middleZ);
			locations[offset2 + X] = middleX + scale * (x2 - middleX);
			locations[offset2 + Y] = middleY + scale * (y2 - middleY);
			locations[offset2 + Z] = middleZ + scale * (z2 - middleZ);
		} else {
			final double dx = cos(randomAngle) * targetDistance;
			final double dy = sin(randomAngle) * targetDistance;
			locations[offset1 + X] = x1 - dx * k2;
			locations[offset1 + Y] = y1 - dy * k2;
			locations[offset2 + X] = x2 + dx * k1;
			locations[offset2 + Y] = y2 + dy * k1;
		}
	}
	
	public static final double lerp(final double a, final double b, final double t) {
		return a + t * (b - a);
	}
	
	public static final double ratio(final double a, final double b) {
		return Double.isInfinite(a) && Double.isInfinite(b) ? signum(a) * signum(b) : a / b;
	}
	
}
