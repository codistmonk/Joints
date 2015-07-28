package joints;

import static java.util.Arrays.copyOfRange;

import java.io.Serializable;
import java.util.Arrays;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class DoubleList implements Serializable {
	
	private double[] values;
	
	private int first;
	
	private int end;
	
	private boolean beingTraversed;
	
	public DoubleList() {
		this(16);
	}
	
	public DoubleList(final int initialCapacity) {
		this.values = new double[initialCapacity];
	}
	
	public final DoubleList clear() {
		this.first = 0;
		this.end = 0;
		
		return this;
	}
	
	public final int size() {
		return this.end - this.first;
	}
	
	public final DoubleList add(final double value) {
		if (this.values.length <= this.end) {
			if (0 < this.first) {
				System.arraycopy(this.values, this.first, this.values, 0, this.size());
				this.end -= this.first;
				this.first = 0;
			} else {
				this.values = Arrays.copyOf(this.values, 2 * this.size());
			}
		}
		
		this.values[this.end++] = value;
		
		return this;
	}
	
	public final DoubleList addAll(final double... values) {
		for (final double value : values) {
			this.add(value);
		}
		
		return this;
	}
	
	public final double get(final int index) {
		return this.values[this.first + index];
	}
	
	public final DoubleList set(final int index, final double value) {
		this.values[this.first + index] = value;
		
		return this;
	}
	
	public final boolean isBeingTraversed() {
		return this.beingTraversed;
	}
	
	public final DoubleList resize(final int newSize) {
		if (this.values.length < newSize) {
			final double[] newValues = new double[newSize];
			System.arraycopy(this.values, this.first, newValues, 0, this.size());
			this.values = newValues;
			this.first = 0;
		} else {
			if (0 < this.first) {
				System.arraycopy(this.values, this.first, this.values, 0, this.size());
				this.first = 0;
			}
		}
		
		this.end = this.first + newSize;
		
		return this;
	}
	
	public final DoubleList pack() {
		if (this.values.length != this.size()) {
			this.values = copyOfRange(this.values, this.first, this.end);
			this.first = 0;
			this.end = this.values.length;
		}
		
		return this;
	}
	
	public final double remove(final int index) {
		if (index == 0) {
			return this.values[this.first++];
		}
		
		final double result = this.get(index);
		
		System.arraycopy(this.values, this.first + index + 1, this.values, this.first + index, this.size() - 1 - index);
		--this.end;
		
		return result;
	}
	
	public final boolean isEmpty() {
		return this.size() <= 0;
	}
	
	public final DoubleList sort() {
		Arrays.sort(this.values, this.first, this.end);
		
		return this;
	}
	
	public final double[] toArray() {
		return this.pack().values;
	}
	
	public final DoubleList forEach(final Processor processor) {
		this.beingTraversed = true;
		
		try {
			for (int first = this.first, i = first; i < this.end; i += 1 + this.first - first, first = this.first) {
				if (!processor.process(this.values[i])) {
					break;
				}
			}
		} finally {
			this.beingTraversed = false;
		}
		
		return this;
	}
	
	@Override
	public final String toString() {
		final StringBuilder resultBuilder = new StringBuilder();
		
		resultBuilder.append('[');
		
		if (!this.isEmpty()) {
			resultBuilder.append(this.get(0));
			
			final int n = this.size();
			
			for (int i = 1; i < n; ++i) {
				resultBuilder.append(' ').append(this.get(i));
			}
		}
		
		resultBuilder.append(']');
		
		return resultBuilder.toString();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -4559136848618882482L;
	
	public static final DefaultFactory<DoubleList> FACTORY = DefaultFactory.forClass(DoubleList.class);
	
	/**
	 * @author codistmonk (creation 2013-04-27)
	 */
	public static abstract interface Processor extends Serializable {
		
		public abstract boolean process(double value);
		
	}
	
}
