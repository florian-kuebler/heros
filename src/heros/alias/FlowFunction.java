/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.alias;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A flow function computes which of the finitely many D-type values are reachable
 * from the current source values. Typically there will be one such function
 * associated with every possible control flow. 
 * 
 * <b>NOTE:</b> To be able to produce <b>deterministic benchmarking results</b>, we have found that
 * it helps to return {@link LinkedHashSet}s from {@link #computeTargets(Object)}. This is
 * because the duration of IDE's fixed point iteration may depend on the iteration order.
 * Within the solver, we have tried to fix this order as much as possible, but the
 * order, in general, does also depend on the order in which the result set
 * of {@link #computeTargets(Object)} is traversed.
 * 
 * <b>NOTE:</b> Methods defined on this type may be called simultaneously by different threads.
 * Hence, classes implementing this interface should synchronize accesses to
 * any mutable shared state.
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public interface FlowFunction<FieldRef, D extends FieldSensitiveFact<?, FieldRef, D>> {

	/**
	 * Returns the target values reachable from the source.
	 */
	Set<AnnotatedFact<FieldRef, D>> computeTargets(D source);
	
	//TODO: rename to ConstrainedFact
	public static class AnnotatedFact<FieldRef, D extends FieldSensitiveFact<?, FieldRef, D>> {
		
		private D fact;
		private Constraint<FieldRef> constraint;
		
		//TODO: Refactor API to make things more intuitive
		/**
		 * 
		 * @param fact
		 * @param readField Giving a field reference here means the base value of a field access was tainted, i.e., we have to concretize the source value
		 * @param writtenField
		 */
		public AnnotatedFact(D fact, Constraint<FieldRef> constraint) {
			this.fact = fact;
			this.constraint = constraint;
		}
		
		public D getFact() {
			return fact;
		}
		
		public Constraint<FieldRef> getConstraint() {
			return constraint;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((constraint == null) ? 0 : constraint.hashCode());
			result = prime * result + ((fact == null) ? 0 : fact.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof AnnotatedFact))
				return false;
			AnnotatedFact other = (AnnotatedFact) obj;
			if (constraint == null) {
				if (other.constraint != null)
					return false;
			} else if (!constraint.equals(other.constraint))
				return false;
			if (fact == null) {
				if (other.fact != null)
					return false;
			} else if (!fact.equals(other.fact))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return fact.toString()+"<"+constraint+">";
		}
	}
	
	public interface Constraint<FieldRef> {
		AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath);
	}
	
	public class WriteFieldConstraint<FieldRef> implements Constraint<FieldRef> {
		private FieldRef fieldRef;

		public WriteFieldConstraint(FieldRef fieldRef) {
			this.fieldRef = fieldRef;
		}

		@Override
		public AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath) {
			if(accPath.hasExclusions())
				return accPath.getExclusions(0).addExclusion(fieldRef);
			else
				return accPath.appendExcludedFieldReference(fieldRef);
		}
		
		@Override
		public String toString() {
			return "^"+fieldRef.toString();
		}
	}
	
	public class ReadFieldConstraint<FieldRef> implements Constraint<FieldRef> {

		private FieldRef fieldRef;

		public ReadFieldConstraint(FieldRef fieldRef) {
			this.fieldRef = fieldRef;
		}
		
		@Override
		public AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath) {
			return accPath.addFieldReference(fieldRef);
		}
		
		@Override
		public String toString() {
			return fieldRef.toString();
		}
	}
}
